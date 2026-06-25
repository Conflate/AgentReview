package com.agentreview.session;

import com.agentreview.common.AgentTool;
import com.agentreview.repository.RepositoryProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "agent_sessions")
public class AgentSession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String sessionExternalId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AgentTool agentTool;

	@Column(nullable = false)
	private String developer;

	@Column(nullable = false)
	private String repoName;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "repository_profile_id")
	private RepositoryProfile repositoryProfile;

	@Column(nullable = false)
	private String branchName;

	@Column(length = 2000)
	private String summary;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected AgentSession() {
	}

	public AgentSession(
			String sessionExternalId,
			AgentTool agentTool,
			String developer,
			String repoName,
			RepositoryProfile repositoryProfile,
			String branchName,
			String summary
	) {
		this.sessionExternalId = sessionExternalId;
		this.agentTool = agentTool;
		this.developer = developer;
		this.repoName = repoName;
		this.repositoryProfile = repositoryProfile;
		this.branchName = branchName;
		this.summary = summary;
	}

	public AgentSession(
			String sessionExternalId,
			AgentTool agentTool,
			String developer,
			String repoName,
			String branchName,
			String summary
	) {
		this(sessionExternalId, agentTool, developer, repoName, null, branchName, summary);
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

	public String getSessionExternalId() {
		return sessionExternalId;
	}

	public AgentTool getAgentTool() {
		return agentTool;
	}

	public String getDeveloper() {
		return developer;
	}

	public String getRepoName() {
		return repoName;
	}

	public RepositoryProfile getRepositoryProfile() {
		return repositoryProfile;
	}

	public String getBranchName() {
		return branchName;
	}

	public String getSummary() {
		return summary;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
