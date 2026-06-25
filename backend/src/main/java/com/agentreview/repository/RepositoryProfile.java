package com.agentreview.repository;

import com.agentreview.common.RiskLevel;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "repository_profiles")
public class RepositoryProfile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String repoName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RiskLevel businessCriticality;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "repository_profile_protected_paths", joinColumns = @JoinColumn(name = "repository_profile_id"))
	@Column(name = "pattern", nullable = false)
	private List<String> protectedPathPatterns = new ArrayList<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "repository_profile_restricted_commands", joinColumns = @JoinColumn(name = "repository_profile_id"))
	@Column(name = "pattern", nullable = false)
	private List<String> restrictedCommandPatterns = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RiskLevel approvalRequiredAt;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected RepositoryProfile() {
	}

	public RepositoryProfile(
			String repoName,
			RiskLevel businessCriticality,
			List<String> protectedPathPatterns,
			List<String> restrictedCommandPatterns,
			RiskLevel approvalRequiredAt
	) {
		this.repoName = repoName;
		this.businessCriticality = businessCriticality;
		this.protectedPathPatterns = new ArrayList<>(protectedPathPatterns);
		this.restrictedCommandPatterns = new ArrayList<>(restrictedCommandPatterns);
		this.approvalRequiredAt = approvalRequiredAt;
	}

	@PrePersist
	void setCreatedAtIfMissing() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getRepoName() {
		return repoName;
	}

	public RiskLevel getBusinessCriticality() {
		return businessCriticality;
	}

	public List<String> getProtectedPathPatterns() {
		return protectedPathPatterns;
	}

	public List<String> getRestrictedCommandPatterns() {
		return restrictedCommandPatterns;
	}

	public RiskLevel getApprovalRequiredAt() {
		return approvalRequiredAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
