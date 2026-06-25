package com.agentreview.repository.dto;

import com.agentreview.common.RiskLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateRepositoryProfileRequest(
		@NotBlank String repoName,
		@NotNull RiskLevel businessCriticality,
		@NotEmpty List<@NotBlank String> protectedPathPatterns,
		List<@NotBlank String> restrictedCommandPatterns,
		@NotNull RiskLevel approvalRequiredAt
) {
}
