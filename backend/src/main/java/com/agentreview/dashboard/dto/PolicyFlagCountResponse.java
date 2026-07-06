package com.agentreview.dashboard.dto;

import com.agentreview.analysis.PolicyFlagType;

public record PolicyFlagCountResponse(
		PolicyFlagType type,
		String message,
		long count
) {
}
