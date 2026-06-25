package com.agentreview.event.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AgentEventImportRequest(
		@NotEmpty List<@Valid ImportAgentEventRequest> events
) {
}
