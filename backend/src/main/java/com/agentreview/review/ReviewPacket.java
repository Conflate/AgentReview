package com.agentreview.review;

import com.agentreview.common.MergeReadiness;
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
@Table(name = "review_packets")
public class ReviewPacket {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_id", nullable = false)
	private AgentSession session;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RiskLevel riskLevel;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MergeReadiness mergeReadiness;

	@Column(nullable = false, columnDefinition = "text")
	private String packetMarkdown;

	@Column(nullable = false, updatable = false)
	private Instant generatedAt;

	protected ReviewPacket() {
	}

	public ReviewPacket(
			AgentSession session,
			RiskLevel riskLevel,
			MergeReadiness mergeReadiness,
			String packetMarkdown
	) {
		this.session = session;
		this.riskLevel = riskLevel;
		this.mergeReadiness = mergeReadiness;
		this.packetMarkdown = packetMarkdown;
	}

	@PrePersist
	void setGeneratedAtIfMissing() {
		if (generatedAt == null) {
			generatedAt = Instant.now();
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

	public MergeReadiness getMergeReadiness() {
		return mergeReadiness;
	}

	public String getPacketMarkdown() {
		return packetMarkdown;
	}

	public Instant getGeneratedAt() {
		return generatedAt;
	}
}
