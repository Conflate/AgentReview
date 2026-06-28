package com.agentreview.ai.dto;

import com.agentreview.ai.AiSummary;
import java.time.Instant;

public record AiSummaryResponse(
		Long id,
		Long reviewPacketId,
		String provider,
		String reviewerSummary,
		String suggestedInspectionAreas,
		String riskExplanation,
		Instant generatedAt
) {
	public static AiSummaryResponse from(AiSummary summary) {
		return new AiSummaryResponse(
				summary.getId(),
				summary.getReviewPacket().getId(),
				summary.getProvider(),
				summary.getReviewerSummary(),
				summary.getSuggestedInspectionAreas(),
				summary.getRiskExplanation(),
				summary.getGeneratedAt()
		);
	}
}
