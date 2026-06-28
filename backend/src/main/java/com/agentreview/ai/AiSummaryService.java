package com.agentreview.ai;

import com.agentreview.ai.dto.AiSummaryResponse;
import com.agentreview.common.ResourceNotFoundException;
import com.agentreview.review.ReviewPacket;
import com.agentreview.review.ReviewPacketRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiSummaryService {

	private final ReviewPacketRepository reviewPacketRepository;
	private final AiSummaryRepository aiSummaryRepository;
	private final AiSummaryClient aiSummaryClient;
	private final String provider;

	public AiSummaryService(
			ReviewPacketRepository reviewPacketRepository,
			AiSummaryRepository aiSummaryRepository,
			AiSummaryClient aiSummaryClient,
			@Value("${agentreview.ai.provider:fake}") String provider
	) {
		this.reviewPacketRepository = reviewPacketRepository;
		this.aiSummaryRepository = aiSummaryRepository;
		this.aiSummaryClient = aiSummaryClient;
		this.provider = provider;
	}

	@Transactional
	public AiSummaryResponse generate(Long reviewPacketId) {
		ReviewPacket reviewPacket = reviewPacketRepository.findById(reviewPacketId)
				.orElseThrow(() -> new ResourceNotFoundException("Review packet not found: " + reviewPacketId));
		AiSummaryDraft draft = aiSummaryClient.summarizeReviewPacket(reviewPacket);
		aiSummaryRepository.deleteByReviewPacketId(reviewPacketId);
		AiSummary summary = aiSummaryRepository.save(new AiSummary(
				reviewPacket,
				provider,
				draft.reviewerSummary(),
				draft.suggestedInspectionAreas(),
				draft.riskExplanation()
		));
		return AiSummaryResponse.from(summary);
	}

	@Transactional(readOnly = true)
	public AiSummaryResponse getLatest(Long reviewPacketId) {
		AiSummary summary = aiSummaryRepository.findFirstByReviewPacketIdOrderByGeneratedAtDesc(reviewPacketId)
				.orElseThrow(() -> new ResourceNotFoundException("AI summary not found for review packet: " + reviewPacketId));
		return AiSummaryResponse.from(summary);
	}
}
