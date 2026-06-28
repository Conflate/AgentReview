package com.agentreview.ai;

public record AiSummaryDraft(
		String reviewerSummary,
		String suggestedInspectionAreas,
		String riskExplanation
) {
}
