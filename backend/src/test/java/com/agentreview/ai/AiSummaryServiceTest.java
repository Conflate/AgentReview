package com.agentreview.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentreview.common.AgentTool;
import com.agentreview.common.MergeReadiness;
import com.agentreview.common.ResourceNotFoundException;
import com.agentreview.common.RiskLevel;
import com.agentreview.review.ReviewPacket;
import com.agentreview.review.ReviewPacketRepository;
import com.agentreview.session.AgentSession;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiSummaryServiceTest {

	@Mock
	private ReviewPacketRepository reviewPacketRepository;

	@Mock
	private AiSummaryRepository aiSummaryRepository;

	@Mock
	private AiSummaryClient aiSummaryClient;

	private AiSummaryService aiSummaryService;

	@BeforeEach
	void setUp() {
		aiSummaryService = new AiSummaryService(
				reviewPacketRepository,
				aiSummaryRepository,
				aiSummaryClient,
				"fake"
		);
	}

	@Test
	void generateReplacesExistingSummaryForReviewPacket() {
		ReviewPacket packet = packet();
		when(reviewPacketRepository.findById(7L)).thenReturn(Optional.of(packet));
		when(aiSummaryClient.summarizeReviewPacket(packet)).thenReturn(new AiSummaryDraft(
				"Summary",
				"Inspect protected paths",
				"Rules set HIGH risk"
		));
		when(aiSummaryRepository.save(any(AiSummary.class))).thenAnswer(invocation -> {
			AiSummary summary = invocation.getArgument(0);
			summary.setId(3L);
			return summary;
		});

		var response = aiSummaryService.generate(7L);

		ArgumentCaptor<AiSummary> captor = ArgumentCaptor.forClass(AiSummary.class);
		verify(aiSummaryRepository).deleteByReviewPacketId(7L);
		verify(aiSummaryRepository).save(captor.capture());
		assertThat(captor.getValue().getReviewPacket()).isEqualTo(packet);
		assertThat(captor.getValue().getProvider()).isEqualTo("fake");
		assertThat(response.id()).isEqualTo(3L);
		assertThat(response.reviewPacketId()).isEqualTo(7L);
		assertThat(response.reviewerSummary()).isEqualTo("Summary");
		assertThat(response.suggestedInspectionAreas()).isEqualTo("Inspect protected paths");
		assertThat(response.riskExplanation()).isEqualTo("Rules set HIGH risk");
	}

	@Test
	void getLatestReturnsStoredSummary() {
		ReviewPacket packet = packet();
		AiSummary summary = new AiSummary(
				packet,
				"fake",
				"Summary",
				"Inspect tests",
				"Rules set LOW risk"
		);
		summary.setId(4L);
		when(aiSummaryRepository.findFirstByReviewPacketIdOrderByGeneratedAtDesc(7L)).thenReturn(Optional.of(summary));

		var response = aiSummaryService.getLatest(7L);

		assertThat(response.id()).isEqualTo(4L);
		assertThat(response.reviewPacketId()).isEqualTo(7L);
		assertThat(response.provider()).isEqualTo("fake");
	}

	@Test
	void generateThrowsWhenReviewPacketDoesNotExist() {
		when(reviewPacketRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> aiSummaryService.generate(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Review packet not found: 99");
	}

	@Test
	void getLatestThrowsWhenSummaryDoesNotExist() {
		when(aiSummaryRepository.findFirstByReviewPacketIdOrderByGeneratedAtDesc(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> aiSummaryService.getLatest(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("AI summary not found for review packet: 99");
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
