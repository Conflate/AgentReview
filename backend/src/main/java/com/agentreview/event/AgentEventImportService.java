package com.agentreview.event;

import com.agentreview.audit.AuditLogService;
import com.agentreview.common.ResourceNotFoundException;
import com.agentreview.event.dto.AgentEventImportRequest;
import com.agentreview.event.dto.AgentEventImportResponse;
import com.agentreview.event.dto.AgentEventResponse;
import com.agentreview.event.dto.ImportAgentEventRequest;
import com.agentreview.session.AgentSession;
import com.agentreview.session.AgentSessionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentEventImportService {

	private final AgentSessionRepository agentSessionRepository;
	private final AgentEventRepository agentEventRepository;
	private final AuditLogService auditLogService;

	public AgentEventImportService(
			AgentSessionRepository agentSessionRepository,
			AgentEventRepository agentEventRepository,
			AuditLogService auditLogService
	) {
		this.agentSessionRepository = agentSessionRepository;
		this.agentEventRepository = agentEventRepository;
		this.auditLogService = auditLogService;
	}

	@Transactional
	public AgentEventImportResponse importEvents(Long sessionId, AgentEventImportRequest request) {
		AgentSession session = agentSessionRepository.findById(sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("Agent session not found: " + sessionId));
		List<AgentEvent> events = request.events().stream()
				.map(event -> toEntity(session, event))
				.toList();
		List<AgentEventResponse> importedEvents = agentEventRepository.saveAll(events).stream()
				.map(AgentEventResponse::from)
				.toList();
		auditLogService.recordEventsImported(session, importedEvents.size());
		return new AgentEventImportResponse(sessionId, importedEvents.size(), importedEvents);
	}

	private AgentEvent toEntity(AgentSession session, ImportAgentEventRequest request) {
		return new AgentEvent(
				session,
				request.eventType(),
				normalizeOptionalText(request.filePath()),
				normalizeOptionalText(request.command()),
				normalizeOptionalText(request.summary()),
				request.timestamp()
		);
	}

	private String normalizeOptionalText(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
