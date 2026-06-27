package com.agentreview.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentreview.common.AgentTool;
import com.agentreview.common.MergeReadiness;
import com.agentreview.common.RiskLevel;
import com.agentreview.session.AgentSession;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

	@Mock
	private AuditLogRepository auditLogRepository;

	@InjectMocks
	private AuditLogService auditLogService;

	@Test
	void recordRiskAnalysisCompletedSavesAuditLog() {
		AgentSession session = session();
		when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

		auditLogService.recordRiskAnalysisCompleted(
				session,
				RiskLevel.HIGH,
				MergeReadiness.REVIEW_REQUIRED,
				2
		);

		ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
		verify(auditLogRepository).save(captor.capture());
		assertThat(captor.getValue().getSession()).isEqualTo(session);
		assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.RISK_ANALYSIS_COMPLETED);
		assertThat(captor.getValue().getMessage())
				.isEqualTo("Risk analysis completed: HIGH, REVIEW_REQUIRED, flags=2");
	}

	@Test
	void findBySessionIdMapsAuditLogResponses() {
		AgentSession session = session();
		AuditLog auditLog = new AuditLog(session, AuditAction.SESSION_CREATED, "Agent session created");
		auditLog.setId(5L);
		when(auditLogRepository.findBySessionIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(auditLog));

		var responses = auditLogService.findBySessionId(1L);

		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).id()).isEqualTo(5L);
		assertThat(responses.get(0).sessionId()).isEqualTo(1L);
		assertThat(responses.get(0).action()).isEqualTo(AuditAction.SESSION_CREATED);
	}

	private AgentSession session() {
		AgentSession session = new AgentSession(
				"codex-run-001",
				AgentTool.CODEX,
				"David",
				"agentreview",
				"main",
				"Implemented audit logging"
		);
		session.setId(1L);
		return session;
	}
}
