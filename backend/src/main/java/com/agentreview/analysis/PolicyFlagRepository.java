package com.agentreview.analysis;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyFlagRepository extends JpaRepository<PolicyFlag, Long> {

	List<PolicyFlag> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

	void deleteBySessionId(Long sessionId);
}
