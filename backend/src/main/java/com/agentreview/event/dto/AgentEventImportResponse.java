package com.agentreview.event.dto;

import java.util.List;

public record AgentEventImportResponse(
		Long sessionId,
		int importedCount,
		List<AgentEventResponse> events
) {
}
