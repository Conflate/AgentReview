package com.agentreview.event;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentEventRepository extends JpaRepository<AgentEvent, Long> {

	List<AgentEvent> findBySessionIdOrderByEventTimestampAsc(Long sessionId);
}
