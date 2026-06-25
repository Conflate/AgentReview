package com.agentreview.event.dto;

import com.agentreview.common.EventType;
import com.agentreview.event.AgentEvent;
import java.time.Instant;

public record AgentEventResponse(
		Long id,
		Long sessionId,
		EventType eventType,
		String filePath,
		String command,
		String summary,
		Instant timestamp,
		Instant importedAt
) {

	public static AgentEventResponse from(AgentEvent event) {
		return new AgentEventResponse(
				event.getId(),
				event.getSession().getId(),
				event.getEventType(),
				event.getFilePath(),
				event.getCommand(),
				event.getSummary(),
				event.getEventTimestamp(),
				event.getImportedAt()
		);
	}
}
