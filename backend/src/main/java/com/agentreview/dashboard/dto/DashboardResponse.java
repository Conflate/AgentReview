package com.agentreview.dashboard.dto;

import com.agentreview.common.AgentTool;
import com.agentreview.common.RiskLevel;
import java.util.List;
import java.util.Map;

public record DashboardResponse(
		long totalSessions,
		long totalReviewPackets,
		long highRiskPackets,
		long blockedPackets,
		Map<AgentTool, Long> sessionsByTool,
		Map<RiskLevel, Long> packetsByRisk,
		List<PolicyFlagCountResponse> topPolicyFlags
) {
}
