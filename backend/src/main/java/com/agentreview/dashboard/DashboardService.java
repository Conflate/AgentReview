package com.agentreview.dashboard;

import com.agentreview.analysis.PolicyFlag;
import com.agentreview.analysis.PolicyFlagRepository;
import com.agentreview.common.AgentTool;
import com.agentreview.common.MergeReadiness;
import com.agentreview.common.RiskLevel;
import com.agentreview.dashboard.dto.DashboardResponse;
import com.agentreview.dashboard.dto.PolicyFlagCountResponse;
import com.agentreview.review.ReviewPacket;
import com.agentreview.review.ReviewPacketRepository;
import com.agentreview.session.AgentSession;
import com.agentreview.session.AgentSessionRepository;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

	private static final int TOP_POLICY_FLAG_LIMIT = 5;

	private final AgentSessionRepository agentSessionRepository;
	private final ReviewPacketRepository reviewPacketRepository;
	private final PolicyFlagRepository policyFlagRepository;

	public DashboardService(
			AgentSessionRepository agentSessionRepository,
			ReviewPacketRepository reviewPacketRepository,
			PolicyFlagRepository policyFlagRepository
	) {
		this.agentSessionRepository = agentSessionRepository;
		this.reviewPacketRepository = reviewPacketRepository;
		this.policyFlagRepository = policyFlagRepository;
	}

	@Transactional(readOnly = true)
	public DashboardResponse getDashboard() {
		List<AgentSession> sessions = agentSessionRepository.findAll();
		List<ReviewPacket> packets = reviewPacketRepository.findAll();
		List<PolicyFlag> policyFlags = policyFlagRepository.findAll();

		return new DashboardResponse(
				sessions.size(),
				packets.size(),
				countHighRiskPackets(packets),
				countBlockedPackets(packets),
				countSessionsByTool(sessions),
				countPacketsByRisk(packets),
				countTopPolicyFlags(policyFlags)
		);
	}

	private long countHighRiskPackets(List<ReviewPacket> packets) {
		return packets.stream()
				.filter(packet -> packet.getRiskLevel() == RiskLevel.HIGH)
				.count();
	}

	private long countBlockedPackets(List<ReviewPacket> packets) {
		return packets.stream()
				.filter(packet -> packet.getMergeReadiness() == MergeReadiness.BLOCKED)
				.count();
	}

	private Map<AgentTool, Long> countSessionsByTool(List<AgentSession> sessions) {
		Map<AgentTool, Long> counts = new EnumMap<>(AgentTool.class);
		for (AgentTool tool : AgentTool.values()) {
			counts.put(tool, 0L);
		}
		for (AgentSession session : sessions) {
			counts.computeIfPresent(session.getAgentTool(), (tool, count) -> count + 1);
		}
		return counts;
	}

	private Map<RiskLevel, Long> countPacketsByRisk(List<ReviewPacket> packets) {
		Map<RiskLevel, Long> counts = new EnumMap<>(RiskLevel.class);
		for (RiskLevel riskLevel : RiskLevel.values()) {
			counts.put(riskLevel, 0L);
		}
		for (ReviewPacket packet : packets) {
			counts.computeIfPresent(packet.getRiskLevel(), (riskLevel, count) -> count + 1);
		}
		return counts;
	}

	private List<PolicyFlagCountResponse> countTopPolicyFlags(List<PolicyFlag> policyFlags) {
		Map<String, Long> countsByMessage = policyFlags.stream()
				.collect(Collectors.groupingBy(
						PolicyFlag::getMessage,
						LinkedHashMap::new,
						Collectors.counting()
				));
		return countsByMessage.entrySet().stream()
				.sorted(Comparator
						.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue)
						.reversed()
						.thenComparing(Map.Entry::getKey))
				.limit(TOP_POLICY_FLAG_LIMIT)
				.map(entry -> new PolicyFlagCountResponse(entry.getKey(), entry.getValue()))
				.toList();
	}
}
