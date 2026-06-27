package com.agentreview.audit;

import com.agentreview.audit.dto.AuditLogResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuditLogController {

	private final AuditLogService auditLogService;

	public AuditLogController(AuditLogService auditLogService) {
		this.auditLogService = auditLogService;
	}

	@GetMapping("/audit-logs")
	public List<AuditLogResponse> findAll() {
		return auditLogService.findAll();
	}

	@GetMapping("/sessions/{sessionId}/audit-logs")
	public List<AuditLogResponse> findBySessionId(@PathVariable Long sessionId) {
		return auditLogService.findBySessionId(sessionId);
	}
}
