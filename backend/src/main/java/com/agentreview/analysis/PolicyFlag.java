package com.agentreview.analysis;

import com.agentreview.common.RiskLevel;
import com.agentreview.session.AgentSession;
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
@Table(name = "policy_flags")
public class PolicyFlag {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_id", nullable = false)
	private AgentSession session;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RiskLevel riskLevel;

	@Column(nullable = false, length = 2000)
	private String message;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected PolicyFlag() {
	}

	public PolicyFlag(AgentSession session, RiskLevel riskLevel, String message) {
		this.session = session;
		this.riskLevel = riskLevel;
		this.message = message;
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

	public AgentSession getSession() {
		return session;
	}

	public RiskLevel getRiskLevel() {
		return riskLevel;
	}

	public String getMessage() {
		return message;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
