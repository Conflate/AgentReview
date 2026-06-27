package com.agentreview.review;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewPacketRepository extends JpaRepository<ReviewPacket, Long> {

	Optional<ReviewPacket> findFirstBySessionIdOrderByGeneratedAtDesc(Long sessionId);

	void deleteBySessionId(Long sessionId);
}
