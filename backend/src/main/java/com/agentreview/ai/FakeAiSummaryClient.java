package com.agentreview.ai;

import com.agentreview.review.ReviewPacket;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "agentreview.ai.provider", havingValue = "fake", matchIfMissing = true)
public class FakeAiSummaryClient implements AiSummaryClient {

	@Override
	public AiSummaryDraft summarizeReviewPacket(ReviewPacket reviewPacket) {
		String reviewerSummary = "Review packet is "
				+ reviewPacket.getRiskLevel()
				+ " risk with merge readiness "
				+ reviewPacket.getMergeReadiness()
				+ ". Use the deterministic evidence packet as the source of truth.";
		String suggestedInspectionAreas = inspectionAreas(reviewPacket.getPacketMarkdown());
		String riskExplanation = "Risk and readiness were calculated by deterministic rules from policy flags, "
				+ "changed files, command evidence, and test evidence. This summary does not change the risk decision.";
		return new AiSummaryDraft(reviewerSummary, suggestedInspectionAreas, riskExplanation);
	}

	private String inspectionAreas(String packetMarkdown) {
		if (packetMarkdown.contains("## Review Checklist")) {
			return "Start with the review checklist, then inspect policy flags, changed files, and test evidence.";
		}
		return "Inspect policy flags, changed files, command activity, and test evidence.";
	}
}
