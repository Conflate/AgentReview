package com.agentreview.session.dto;

import com.agentreview.common.AgentTool;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAgentSessionRequest(
		@NotBlank String sessionExternalId,
		@NotNull AgentTool agentTool,
		@NotBlank String developer,
		@NotBlank String repoName,
		Long repositoryProfileId,
		@NotBlank String branchName,
		@Size(max = 2000) String summary
) {
}
