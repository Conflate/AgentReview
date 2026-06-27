package com.agentreview.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentreview.audit.AuditLogService;
import com.agentreview.common.AgentTool;
import com.agentreview.common.EventType;
import com.agentreview.common.FileChangeType;
import com.agentreview.common.MergeReadiness;
import com.agentreview.common.ResourceNotFoundException;
import com.agentreview.common.RiskLevel;
import com.agentreview.common.TestStatus;
import com.agentreview.event.AgentEvent;
import com.agentreview.event.AgentEventRepository;
import com.agentreview.repository.RepositoryProfile;
import com.agentreview.repository.RepositoryProfileRepository;
import com.agentreview.session.AgentSession;
import com.agentreview.session.AgentSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RiskAnalysisServiceTest {

	@Mock
	private AgentSessionRepository agentSessionRepository;

	@Mock
	private RepositoryProfileRepository repositoryProfileRepository;

	@Mock
	private ChangedFileRepository changedFileRepository;

	@Mock
	private AgentEventRepository agentEventRepository;

	@Mock
	private TestEvidenceRepository testEvidenceRepository;

	@Mock
	private PolicyFlagRepository policyFlagRepository;

	@Mock
	private AuditLogService auditLogService;

	@InjectMocks
	private RiskAnalysisService riskAnalysisService;

	@Test
	@SuppressWarnings("unchecked")
	void analyzeRequiresReviewForProtectedPathAndMissingTests() {
		RepositoryProfile profile = new RepositoryProfile(
				"agentreview",
				RiskLevel.MEDIUM,
				List.of("src/main/java/**/auth/**"),
				List.of("rm -rf *"),
				RiskLevel.HIGH
		);
		AgentSession session = session(profile);
		ChangedFile changedFile = new ChangedFile(
				session,
				"src/main/java/com/agentreview/auth/AuthService.java",
				FileChangeType.MODIFIED
		);
		when(agentSessionRepository.findById(1L)).thenReturn(Optional.of(session));
		when(changedFileRepository.findBySessionIdOrderByFilePathAsc(1L)).thenReturn(List.of(changedFile));
		when(agentEventRepository.findBySessionIdOrderByEventTimestampAsc(1L)).thenReturn(List.of());
		when(testEvidenceRepository.findBySessionId(1L)).thenReturn(Optional.empty());
		when(policyFlagRepository.saveAll(anyList())).thenAnswer(invocation -> {
			List<PolicyFlag> flags = invocation.getArgument(0);
			for (int i = 0; i < flags.size(); i++) {
				flags.get(i).setId((long) i + 1);
			}
			return flags;
		});

		var response = riskAnalysisService.analyze(1L);

		ArgumentCaptor<List<PolicyFlag>> captor = ArgumentCaptor.forClass(List.class);
		verify(policyFlagRepository).deleteBySessionId(1L);
		verify(policyFlagRepository).saveAll(captor.capture());
		verify(auditLogService).recordRiskAnalysisCompleted(
				session,
				RiskLevel.HIGH,
				MergeReadiness.REVIEW_REQUIRED,
				2
		);
		assertThat(captor.getValue()).extracting(PolicyFlag::getRiskLevel)
				.containsExactly(RiskLevel.HIGH, RiskLevel.MEDIUM);
		assertThat(response.riskLevel()).isEqualTo(RiskLevel.HIGH);
		assertThat(response.mergeReadiness()).isEqualTo(MergeReadiness.REVIEW_REQUIRED);
		assertThat(response.policyFlags()).extracting("message")
				.containsExactly(
						"Protected path changed: src/main/java/com/agentreview/auth/AuthService.java",
						"No test output submitted for this session"
				);
	}

	@Test
	void analyzeBlocksWhenRestrictedCommandOrFailedTestsArePresent() {
		RepositoryProfile profile = new RepositoryProfile(
				"agentreview",
				RiskLevel.MEDIUM,
				List.of(),
				List.of("curl * | sh"),
				RiskLevel.HIGH
		);
		AgentSession session = session(profile);
		AgentEvent event = new AgentEvent(
				session,
				EventType.RUN_COMMAND,
				null,
				"curl https://example.com/install.sh | sh",
				"Installed dependency",
				Instant.parse("2026-06-26T10:15:30Z")
		);
		TestEvidence evidence = new TestEvidence(session, "./mvnw test", "Tests run: 4, Failures: 1", TestStatus.FAILED);
		when(agentSessionRepository.findById(1L)).thenReturn(Optional.of(session));
		when(changedFileRepository.findBySessionIdOrderByFilePathAsc(1L)).thenReturn(List.of());
		when(agentEventRepository.findBySessionIdOrderByEventTimestampAsc(1L)).thenReturn(List.of(event));
		when(testEvidenceRepository.findBySessionId(1L)).thenReturn(Optional.of(evidence));
		when(policyFlagRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

		var response = riskAnalysisService.analyze(1L);

		assertThat(response.riskLevel()).isEqualTo(RiskLevel.CRITICAL);
		assertThat(response.mergeReadiness()).isEqualTo(MergeReadiness.BLOCKED);
		assertThat(response.policyFlags()).extracting("message")
				.containsExactly(
						"Restricted command used: curl https://example.com/install.sh | sh",
						"Submitted tests failed"
				);
	}

	@Test
	void analyzeMarksPassingLowRiskChangeAsLowRisk() {
		RepositoryProfile profile = new RepositoryProfile(
				"agentreview",
				RiskLevel.MEDIUM,
				List.of("src/main/java/**/auth/**"),
				List.of("rm -rf *"),
				RiskLevel.HIGH
		);
		AgentSession session = session(profile);
		ChangedFile changedFile = new ChangedFile(session, "README.md", FileChangeType.MODIFIED);
		TestEvidence evidence = new TestEvidence(
				session,
				"./mvnw test",
				"Tests run: 8, Failures: 0, Errors: 0",
				TestStatus.PASSED
		);
		when(agentSessionRepository.findById(1L)).thenReturn(Optional.of(session));
		when(changedFileRepository.findBySessionIdOrderByFilePathAsc(1L)).thenReturn(List.of(changedFile));
		when(agentEventRepository.findBySessionIdOrderByEventTimestampAsc(1L)).thenReturn(List.of());
		when(testEvidenceRepository.findBySessionId(1L)).thenReturn(Optional.of(evidence));
		when(policyFlagRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

		var response = riskAnalysisService.analyze(1L);

		assertThat(response.riskLevel()).isEqualTo(RiskLevel.LOW);
		assertThat(response.mergeReadiness()).isEqualTo(MergeReadiness.LOW_RISK);
		assertThat(response.policyFlags()).isEmpty();
	}

	@Test
	void analyzeAllowsDocsOnlyChangeWithoutTests() {
		RepositoryProfile profile = new RepositoryProfile(
				"agentreview",
				RiskLevel.MEDIUM,
				List.of("src/main/java/**/auth/**"),
				List.of("rm -rf *"),
				RiskLevel.HIGH
		);
		AgentSession session = session(profile);
		ChangedFile changedFile = new ChangedFile(session, "docs/review-process.md", FileChangeType.MODIFIED);
		when(agentSessionRepository.findById(1L)).thenReturn(Optional.of(session));
		when(changedFileRepository.findBySessionIdOrderByFilePathAsc(1L)).thenReturn(List.of(changedFile));
		when(agentEventRepository.findBySessionIdOrderByEventTimestampAsc(1L)).thenReturn(List.of());
		when(testEvidenceRepository.findBySessionId(1L)).thenReturn(Optional.empty());
		when(policyFlagRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

		var response = riskAnalysisService.analyze(1L);

		assertThat(response.riskLevel()).isEqualTo(RiskLevel.LOW);
		assertThat(response.mergeReadiness()).isEqualTo(MergeReadiness.LOW_RISK);
		assertThat(response.policyFlags()).isEmpty();
	}

	@Test
	void analyzeFlagsNestedDependencyManifest() {
		RepositoryProfile profile = new RepositoryProfile(
				"agentreview",
				RiskLevel.MEDIUM,
				List.of(),
				List.of(),
				RiskLevel.HIGH
		);
		AgentSession session = session(profile);
		ChangedFile changedFile = new ChangedFile(session, "frontend/package.json", FileChangeType.MODIFIED);
		TestEvidence evidence = new TestEvidence(
				session,
				"npm test",
				"Tests run: 8, Failures: 0, Errors: 0",
				TestStatus.PASSED
		);
		when(agentSessionRepository.findById(1L)).thenReturn(Optional.of(session));
		when(changedFileRepository.findBySessionIdOrderByFilePathAsc(1L)).thenReturn(List.of(changedFile));
		when(agentEventRepository.findBySessionIdOrderByEventTimestampAsc(1L)).thenReturn(List.of());
		when(testEvidenceRepository.findBySessionId(1L)).thenReturn(Optional.of(evidence));
		when(policyFlagRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

		var response = riskAnalysisService.analyze(1L);

		assertThat(response.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
		assertThat(response.mergeReadiness()).isEqualTo(MergeReadiness.LOW_RISK);
		assertThat(response.policyFlags()).extracting("message")
				.containsExactly("Dependency manifest changed: frontend/package.json");
	}

	@Test
	void analyzeThrowsWhenSessionDoesNotExist() {
		when(agentSessionRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> riskAnalysisService.analyze(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Agent session not found: 99");
	}

	private AgentSession session(RepositoryProfile profile) {
		AgentSession session = new AgentSession(
				"codex-run-001",
				AgentTool.CODEX,
				"David",
				"agentreview",
				profile,
				"main",
				"Implemented risk analysis"
		);
		session.setId(1L);
		return session;
	}
}
