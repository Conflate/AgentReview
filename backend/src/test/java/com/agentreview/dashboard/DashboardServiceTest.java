package com.agentreview.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.agentreview.analysis.PolicyFlag;
import com.agentreview.analysis.PolicyFlagRepository;
import com.agentreview.common.AgentTool;
import com.agentreview.common.MergeReadiness;
import com.agentreview.common.RiskLevel;
import com.agentreview.review.ReviewPacket;
import com.agentreview.review.ReviewPacketRepository;
import com.agentreview.session.AgentSession;
import com.agentreview.session.AgentSessionRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

	@Mock
	private AgentSessionRepository agentSessionRepository;

	@Mock
	private ReviewPacketRepository reviewPacketRepository;

	@Mock
	private PolicyFlagRepository policyFlagRepository;

	@InjectMocks
	private DashboardService dashboardService;

	@Test
	void getDashboardAggregatesSessionPacketAndPolicyFlagMetrics() {
		AgentSession codexSession = session(1L, AgentTool.CODEX);
		AgentSession cursorSession = session(2L, AgentTool.CURSOR);
		ReviewPacket lowPacket = packet(codexSession, RiskLevel.LOW, MergeReadiness.LOW_RISK);
		ReviewPacket highPacket = packet(cursorSession, RiskLevel.HIGH, MergeReadiness.REVIEW_REQUIRED);
		ReviewPacket criticalPacket = packet(cursorSession, RiskLevel.CRITICAL, MergeReadiness.BLOCKED);
		when(agentSessionRepository.findAll()).thenReturn(List.of(codexSession, cursorSession));
		when(reviewPacketRepository.findAll()).thenReturn(List.of(lowPacket, highPacket, criticalPacket));
		when(policyFlagRepository.findAll()).thenReturn(List.of(
				flag(codexSession, RiskLevel.HIGH, "Protected path changed: src/main/java/Auth.java"),
				flag(cursorSession, RiskLevel.HIGH, "Protected path changed: src/main/java/Auth.java"),
				flag(cursorSession, RiskLevel.CRITICAL, "Tests failed")
		));

		var response = dashboardService.getDashboard();

		assertThat(response.totalSessions()).isEqualTo(2);
		assertThat(response.totalReviewPackets()).isEqualTo(3);
		assertThat(response.highRiskPackets()).isEqualTo(1);
		assertThat(response.blockedPackets()).isEqualTo(1);
		assertThat(response.sessionsByTool()).containsEntry(AgentTool.CODEX, 1L);
		assertThat(response.sessionsByTool()).containsEntry(AgentTool.CURSOR, 1L);
		assertThat(response.sessionsByTool()).containsEntry(AgentTool.CLAUDE_CODE, 0L);
		assertThat(response.packetsByRisk()).containsEntry(RiskLevel.LOW, 1L);
		assertThat(response.packetsByRisk()).containsEntry(RiskLevel.HIGH, 1L);
		assertThat(response.packetsByRisk()).containsEntry(RiskLevel.CRITICAL, 1L);
		assertThat(response.topPolicyFlags()).hasSize(2);
		assertThat(response.topPolicyFlags().get(0).message())
				.isEqualTo("Protected path changed: src/main/java/Auth.java");
		assertThat(response.topPolicyFlags().get(0).count()).isEqualTo(2);
		assertThat(response.topPolicyFlags().get(1).message()).isEqualTo("Tests failed");
		assertThat(response.topPolicyFlags().get(1).count()).isEqualTo(1);
	}

	@Test
	void getDashboardReturnsZeroedMetricMapsWhenThereIsNoData() {
		when(agentSessionRepository.findAll()).thenReturn(List.of());
		when(reviewPacketRepository.findAll()).thenReturn(List.of());
		when(policyFlagRepository.findAll()).thenReturn(List.of());

		var response = dashboardService.getDashboard();

		assertThat(response.totalSessions()).isZero();
		assertThat(response.totalReviewPackets()).isZero();
		assertThat(response.highRiskPackets()).isZero();
		assertThat(response.blockedPackets()).isZero();
		assertThat(response.sessionsByTool()).containsEntry(AgentTool.CODEX, 0L);
		assertThat(response.packetsByRisk()).containsEntry(RiskLevel.MEDIUM, 0L);
		assertThat(response.topPolicyFlags()).isEmpty();
	}

	private AgentSession session(Long id, AgentTool agentTool) {
		AgentSession session = new AgentSession(
				"session-" + id,
				agentTool,
				"David",
				"agentreview",
				"main",
				"Dashboard test"
		);
		session.setId(id);
		return session;
	}

	private ReviewPacket packet(AgentSession session, RiskLevel riskLevel, MergeReadiness mergeReadiness) {
		return new ReviewPacket(session, riskLevel, mergeReadiness, "# Packet");
	}

	private PolicyFlag flag(AgentSession session, RiskLevel riskLevel, String message) {
		return new PolicyFlag(session, riskLevel, message);
	}
}
