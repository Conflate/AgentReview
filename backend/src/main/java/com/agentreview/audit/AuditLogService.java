package com.agentreview.audit;

import com.agentreview.common.MergeReadiness;
import com.agentreview.common.RiskLevel;
import com.agentreview.common.TestStatus;
import com.agentreview.audit.dto.AuditLogResponse;
import com.agentreview.session.AgentSession;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

	private final AuditLogRepository auditLogRepository;

	public AuditLogService(AuditLogRepository auditLogRepository) {
		this.auditLogRepository = auditLogRepository;
	}

	@Transactional
	public void recordSessionCreated(AgentSession session) {
		record(session, AuditAction.SESSION_CREATED, "Agent session created for repository: " + session.getRepoName());
	}

	@Transactional
	public void recordEventsImported(AgentSession session, int importedCount) {
		record(session, AuditAction.EVENTS_IMPORTED, "Imported agent events: " + importedCount);
	}

	@Transactional
	public void recordDiffImported(AgentSession session, int changedFileCount) {
		record(session, AuditAction.DIFF_IMPORTED, "Imported changed files: " + changedFileCount);
	}

	@Transactional
	public void recordTestOutputImported(AgentSession session, TestStatus status) {
		record(session, AuditAction.TEST_OUTPUT_IMPORTED, "Imported test output with status: " + status);
	}

	@Transactional
	public void recordRiskAnalysisCompleted(
			AgentSession session,
			RiskLevel riskLevel,
			MergeReadiness mergeReadiness,
			int policyFlagCount
	) {
		record(
				session,
				AuditAction.RISK_ANALYSIS_COMPLETED,
				"Risk analysis completed: " + riskLevel + ", " + mergeReadiness + ", flags=" + policyFlagCount
		);
	}

	@Transactional
	public void recordReviewPacketGenerated(AgentSession session, Long reviewPacketId) {
		record(session, AuditAction.REVIEW_PACKET_GENERATED, "Review packet generated: " + reviewPacketId);
	}

	@Transactional(readOnly = true)
	public List<AuditLogResponse> findAll() {
		return auditLogRepository.findAllByOrderByCreatedAtDesc().stream()
				.map(AuditLogResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<AuditLogResponse> findBySessionId(Long sessionId) {
		return auditLogRepository.findBySessionIdOrderByCreatedAtDesc(sessionId).stream()
				.map(AuditLogResponse::from)
				.toList();
	}

	private void record(AgentSession session, AuditAction action, String message) {
		auditLogRepository.save(new AuditLog(session, action, message));
	}
}
