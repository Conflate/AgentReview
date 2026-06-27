package com.agentreview.review.dto;

import com.agentreview.common.MergeReadiness;
import com.agentreview.common.RiskLevel;
import com.agentreview.review.ReviewPacket;
import java.time.Instant;

public record ReviewPacketResponse(
		Long id,
		Long sessionId,
		RiskLevel riskLevel,
		MergeReadiness mergeReadiness,
		String packetMarkdown,
		Instant generatedAt
) {
	public static ReviewPacketResponse from(ReviewPacket packet) {
		return new ReviewPacketResponse(
				packet.getId(),
				packet.getSession().getId(),
				packet.getRiskLevel(),
				packet.getMergeReadiness(),
				packet.getPacketMarkdown(),
				packet.getGeneratedAt()
		);
	}
}
