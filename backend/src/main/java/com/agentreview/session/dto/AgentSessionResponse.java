package com.agentreview.session.dto;

import com.agentreview.common.AgentTool;
import com.agentreview.session.AgentSession;
import java.time.Instant;

public record AgentSessionResponse(
		Long id,
		String sessionExternalId,
		AgentTool agentTool,
		String developer,
		String repoName,
		Long repositoryProfileId,
		String branchName,
		String summary,
		Instant createdAt
) {

	public static AgentSessionResponse from(AgentSession session) {
		return new AgentSessionResponse(
				session.getId(),
				session.getSessionExternalId(),
				session.getAgentTool(),
				session.getDeveloper(),
				session.getRepoName(),
				session.getRepositoryProfile() == null ? null : session.getRepositoryProfile().getId(),
				session.getBranchName(),
				session.getSummary(),
				session.getCreatedAt()
		);
	}
}
