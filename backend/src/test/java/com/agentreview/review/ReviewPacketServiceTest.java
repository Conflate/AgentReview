package com.agentreview.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentreview.analysis.ChangedFile;
import com.agentreview.analysis.ChangedFileRepository;
import com.agentreview.analysis.RiskAnalysisService;
import com.agentreview.analysis.TestEvidence;
import com.agentreview.analysis.TestEvidenceRepository;
import com.agentreview.analysis.dto.PolicyFlagResponse;
import com.agentreview.analysis.dto.RiskAnalysisResponse;
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
class ReviewPacketServiceTest {

	@Mock
	private AgentSessionRepository agentSessionRepository;

	@Mock
	private ChangedFileRepository changedFileRepository;

	@Mock
	private AgentEventRepository agentEventRepository;

	@Mock
	private TestEvidenceRepository testEvidenceRepository;

	@Mock
	private RiskAnalysisService riskAnalysisService;

	@Mock
	private ReviewPacketRepository reviewPacketRepository;

	@Mock
	private AuditLogService auditLogService;

	@InjectMocks
	private ReviewPacketService reviewPacketService;

	@Test
	void generateStoresLatestReviewPacketWithEvidenceSummary() {
		AgentSession session = session();
		ChangedFile changedFile = new ChangedFile(session, "src/main/java/App.java", FileChangeType.MODIFIED);
		AgentEvent event = new AgentEvent(
				session,
				EventType.RUN_COMMAND,
				null,
				"./mvnw test",
				"Ran tests",
				Instant.parse("2026-06-26T10:15:30Z")
		);
		TestEvidence evidence = new TestEvidence(
				session,
				"./mvnw test",
				"Tests run: 12, Failures: 0, Errors: 0",
				TestStatus.PASSED
		);
		RiskAnalysisResponse riskAnalysis = new RiskAnalysisResponse(
				1L,
				RiskLevel.HIGH,
				MergeReadiness.REVIEW_REQUIRED,
				List.of(new PolicyFlagResponse(
						1L,
						RiskLevel.HIGH,
						"Protected path changed: src/main/java/App.java",
						Instant.parse("2026-06-26T10:16:30Z")
				))
		);
		when(agentSessionRepository.findById(1L)).thenReturn(Optional.of(session));
		when(riskAnalysisService.analyze(1L)).thenReturn(riskAnalysis);
		when(changedFileRepository.findBySessionIdOrderByFilePathAsc(1L)).thenReturn(List.of(changedFile));
		when(agentEventRepository.findBySessionIdOrderByEventTimestampAsc(1L)).thenReturn(List.of(event));
		when(testEvidenceRepository.findBySessionId(1L)).thenReturn(Optional.of(evidence));
		when(reviewPacketRepository.save(any(ReviewPacket.class))).thenAnswer(invocation -> {
			ReviewPacket packet = invocation.getArgument(0);
			packet.setId(7L);
			return packet;
		});

		var response = reviewPacketService.generate(1L);

		ArgumentCaptor<ReviewPacket> captor = ArgumentCaptor.forClass(ReviewPacket.class);
		verify(reviewPacketRepository).deleteBySessionId(1L);
		verify(reviewPacketRepository).save(captor.capture());
		verify(auditLogService).recordReviewPacketGenerated(session, 7L);
		assertThat(response.id()).isEqualTo(7L);
		assertThat(response.riskLevel()).isEqualTo(RiskLevel.HIGH);
		assertThat(response.mergeReadiness()).isEqualTo(MergeReadiness.REVIEW_REQUIRED);
		assertThat(captor.getValue().getPacketMarkdown())
				.contains("# AgentReview Packet")
				.contains("Risk level: HIGH")
				.contains("[HIGH] Protected path changed: src/main/java/App.java")
				.contains("MODIFIED: src/main/java/App.java")
				.contains("Command: ./mvnw test")
				.contains("RUN_COMMAND: ./mvnw test");
	}

	@Test
	void getLatestReturnsStoredPacket() {
		AgentSession session = session();
		ReviewPacket packet = new ReviewPacket(
				session,
				RiskLevel.LOW,
				MergeReadiness.LOW_RISK,
				"# AgentReview Packet"
		);
		packet.setId(3L);
		when(reviewPacketRepository.findFirstBySessionIdOrderByGeneratedAtDesc(1L)).thenReturn(Optional.of(packet));

		var response = reviewPacketService.getLatest(1L);

		assertThat(response.id()).isEqualTo(3L);
		assertThat(response.sessionId()).isEqualTo(1L);
		assertThat(response.packetMarkdown()).isEqualTo("# AgentReview Packet");
	}

	@Test
	void generateThrowsWhenSessionDoesNotExist() {
		when(agentSessionRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> reviewPacketService.generate(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Agent session not found: 99");
	}

	@Test
	void getLatestThrowsWhenPacketDoesNotExist() {
		when(reviewPacketRepository.findFirstBySessionIdOrderByGeneratedAtDesc(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> reviewPacketService.getLatest(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Review packet not found for session: 99");
	}

	private AgentSession session() {
		AgentSession session = new AgentSession(
				"codex-run-001",
				AgentTool.CODEX,
				"David",
				"agentreview",
				"main",
				"Implemented risk analysis"
		);
		session.setId(1L);
		return session;
	}
}
