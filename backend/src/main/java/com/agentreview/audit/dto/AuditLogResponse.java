package com.agentreview.audit.dto;

import com.agentreview.audit.AuditAction;
import com.agentreview.audit.AuditLog;
import java.time.Instant;

public record AuditLogResponse(
		Long id,
		Long sessionId,
		AuditAction action,
		String message,
		Instant createdAt
) {
	public static AuditLogResponse from(AuditLog auditLog) {
		return new AuditLogResponse(
				auditLog.getId(),
				auditLog.getSession() == null ? null : auditLog.getSession().getId(),
				auditLog.getAction(),
				auditLog.getMessage(),
				auditLog.getCreatedAt()
		);
	}
}
