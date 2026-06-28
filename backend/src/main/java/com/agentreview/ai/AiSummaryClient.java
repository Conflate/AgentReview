package com.agentreview.ai;

import com.agentreview.review.ReviewPacket;

public interface AiSummaryClient {

	AiSummaryDraft summarizeReviewPacket(ReviewPacket reviewPacket);
}
