package com.agentreview.event.dto;

import com.agentreview.common.EventType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record ImportAgentEventRequest(
		@NotNull EventType eventType,
		@Size(max = 255) String filePath,
		@Size(max = 2000) String command,
		@Size(max = 2000) String summary,
		@NotNull Instant timestamp
) {
}
