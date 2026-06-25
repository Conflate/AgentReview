package com.agentreview.repository.dto;

import com.agentreview.common.RiskLevel;
import com.agentreview.repository.RepositoryProfile;
import java.time.Instant;
import java.util.List;

public record RepositoryProfileResponse(
		Long id,
		String repoName,
		RiskLevel businessCriticality,
		List<String> protectedPathPatterns,
		List<String> restrictedCommandPatterns,
		RiskLevel approvalRequiredAt,
		Instant createdAt
) {

	public static RepositoryProfileResponse from(RepositoryProfile profile) {
		return new RepositoryProfileResponse(
				profile.getId(),
				profile.getRepoName(),
				profile.getBusinessCriticality(),
				List.copyOf(profile.getProtectedPathPatterns()),
				List.copyOf(profile.getRestrictedCommandPatterns()),
				profile.getApprovalRequiredAt(),
				profile.getCreatedAt()
		);
	}
}
