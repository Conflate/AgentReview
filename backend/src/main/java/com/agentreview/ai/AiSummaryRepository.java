package com.agentreview.ai;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSummaryRepository extends JpaRepository<AiSummary, Long> {

	Optional<AiSummary> findFirstByReviewPacketIdOrderByGeneratedAtDesc(Long reviewPacketId);

	void deleteByReviewPacketId(Long reviewPacketId);
}
