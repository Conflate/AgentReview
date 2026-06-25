package com.agentreview.analysis;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestEvidenceRepository extends JpaRepository<TestEvidence, Long> {

	Optional<TestEvidence> findBySessionId(Long sessionId);

	void deleteBySessionId(Long sessionId);
}
