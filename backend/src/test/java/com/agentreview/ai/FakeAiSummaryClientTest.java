package com.agentreview.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentreview.common.AgentTool;
import com.agentreview.common.MergeReadiness;
import com.agentreview.common.RiskLevel;
import com.agentreview.review.ReviewPacket;
import com.agentreview.session.AgentSession;
import org.junit.jupiter.api.Test;

class FakeAiSummaryClientTest {

	private final FakeAiSummaryClient client = new FakeAiSummaryClient();

	@Test
	void summarizeReviewPacketProducesDeterministicSummaryWithoutChangingRiskDecision() {
		ReviewPacket packet = packet();

		AiSummaryDraft draft = client.summarizeReviewPacket(packet);

		assertThat(draft.reviewerSummary())
				.contains("HIGH risk")
				.contains("REVIEW_REQUIRED")
				.contains("source of truth");
		assertThat(draft.suggestedInspectionAreas()).contains("review checklist");
		assertThat(draft.riskExplanation()).contains("deterministic rules");
	}

	private ReviewPacket packet() {
		AgentSession session = new AgentSession(
				"codex-run-001",
				AgentTool.CODEX,
				"David",
				"agentreview",
				"main",
				"Implemented AI summary"
		);
		session.setId(1L);
		ReviewPacket packet = new ReviewPacket(
				session,
				RiskLevel.HIGH,
				MergeReadiness.REVIEW_REQUIRED,
				"# AgentReview Packet\n\n## Review Checklist\n- Inspect protected path"
		);
		packet.setId(7L);
		return packet;
	}
}
