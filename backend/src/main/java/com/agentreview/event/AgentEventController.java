package com.agentreview.event;

import com.agentreview.event.dto.AgentEventImportRequest;
import com.agentreview.event.dto.AgentEventImportResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}/events")
public class AgentEventController {

	private final AgentEventImportService agentEventImportService;

	public AgentEventController(AgentEventImportService agentEventImportService) {
		this.agentEventImportService = agentEventImportService;
	}

	@PostMapping("/import")
	public ResponseEntity<AgentEventImportResponse> importEvents(
			@PathVariable Long sessionId,
			@Valid @RequestBody AgentEventImportRequest request
	) {
		return ResponseEntity.ok(agentEventImportService.importEvents(sessionId, request));
	}
}
