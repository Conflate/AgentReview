package com.agentreview.analysis.dto;

import com.agentreview.analysis.PolicyFlag;
import com.agentreview.common.RiskLevel;
import java.time.Instant;

public record PolicyFlagResponse(
		Long id,
		RiskLevel riskLevel,
		String message,
		Instant createdAt
) {
	public static PolicyFlagResponse from(PolicyFlag policyFlag) {
		return new PolicyFlagResponse(
				policyFlag.getId(),
				policyFlag.getRiskLevel(),
				policyFlag.getMessage(),
				policyFlag.getCreatedAt()
		);
	}
}
