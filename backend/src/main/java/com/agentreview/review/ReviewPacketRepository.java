package com.agentreview.review;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewPacketRepository extends JpaRepository<ReviewPacket, Long> {

	List<ReviewPacket> findAllByOrderByGeneratedAtDesc();

	Optional<ReviewPacket> findFirstBySessionIdOrderByGeneratedAtDesc(Long sessionId);

	void deleteBySessionId(Long sessionId);
}
