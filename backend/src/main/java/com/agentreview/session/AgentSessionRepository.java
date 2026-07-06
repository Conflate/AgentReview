package com.agentreview.session;

import com.agentreview.common.AgentTool;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentSessionRepository extends JpaRepository<AgentSession, Long> {

	boolean existsByAgentToolAndSessionExternalId(AgentTool agentTool, String sessionExternalId);
}
