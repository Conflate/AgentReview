package com.agentreview.analysis.dto;

import com.agentreview.common.MergeReadiness;
import com.agentreview.common.RiskLevel;
import java.util.List;

public record RiskAnalysisResponse(
		Long sessionId,
		RiskLevel riskLevel,
		MergeReadiness mergeReadiness,
		List<PolicyFlagResponse> policyFlags
) {
}
