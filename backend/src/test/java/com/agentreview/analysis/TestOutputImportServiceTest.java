package com.agentreview.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentreview.analysis.dto.TestOutputImportRequest;
import com.agentreview.audit.AuditLogService;
import com.agentreview.common.AgentTool;
import com.agentreview.common.ResourceNotFoundException;
import com.agentreview.common.TestStatus;
import com.agentreview.session.AgentSession;
import com.agentreview.session.AgentSessionRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestOutputImportServiceTest {

	@Mock
	private AgentSessionRepository agentSessionRepository;

	@Mock
	private TestEvidenceRepository testEvidenceRepository;

	@Mock
	private TestOutputParserService testOutputParserService;

	@Mock
	private AuditLogService auditLogService;

	@InjectMocks
	private TestOutputImportService testOutputImportService;

	@Test
	void importTestOutputReplacesEvidenceForSession() {
		AgentSession session = new AgentSession(
				"codex-run-001",
				AgentTool.CODEX,
				"David",
				"agentreview",
				"main",
				"Implemented diff import"
		);
		session.setId(1L);
		when(agentSessionRepository.findById(1L)).thenReturn(Optional.of(session));
		when(testOutputParserService.detectStatus("Tests run: 42, Failures: 0, Errors: 0 BUILD SUCCESS"))
				.thenReturn(TestStatus.PASSED);
		when(testEvidenceRepository.save(any(TestEvidence.class))).thenAnswer(invocation -> {
			TestEvidence evidence = invocation.getArgument(0);
			evidence.setId(1L);
			return evidence;
		});
		TestOutputImportRequest request = new TestOutputImportRequest(
				" ./mvnw test ",
				" Tests run: 42, Failures: 0, Errors: 0 BUILD SUCCESS "
		);

		var response = testOutputImportService.importTestOutput(1L, request);

		ArgumentCaptor<TestEvidence> captor = ArgumentCaptor.forClass(TestEvidence.class);
		verify(testEvidenceRepository).deleteBySessionId(1L);
		verify(testEvidenceRepository).save(captor.capture());
		verify(auditLogService).recordTestOutputImported(session, TestStatus.PASSED);
		assertThat(captor.getValue().getSession()).isEqualTo(session);
		assertThat(captor.getValue().getTestCommand()).isEqualTo("./mvnw test");
		assertThat(captor.getValue().getStatus()).isEqualTo(TestStatus.PASSED);
		assertThat(response.sessionId()).isEqualTo(1L);
		assertThat(response.status()).isEqualTo(TestStatus.PASSED);
	}

	@Test
	void importTestOutputThrowsWhenSessionDoesNotExist() {
		when(agentSessionRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> testOutputImportService.importTestOutput(
				99L,
				new TestOutputImportRequest("./mvnw test", "BUILD SUCCESS")
		))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Agent session not found: 99");
	}
}
