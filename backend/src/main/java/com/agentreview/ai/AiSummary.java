package com.agentreview.ai;

import com.agentreview.review.ReviewPacket;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "ai_summaries")
public class AiSummary {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "review_packet_id", nullable = false)
	private ReviewPacket reviewPacket;

	@Column(nullable = false)
	private String provider;

	@Column(nullable = false, columnDefinition = "text")
	private String reviewerSummary;

	@Column(nullable = false, columnDefinition = "text")
	private String suggestedInspectionAreas;

	@Column(nullable = false, columnDefinition = "text")
	private String riskExplanation;

	@Column(nullable = false, updatable = false)
	private Instant generatedAt;

	protected AiSummary() {
	}

	public AiSummary(
			ReviewPacket reviewPacket,
			String provider,
			String reviewerSummary,
			String suggestedInspectionAreas,
			String riskExplanation
	) {
		this.reviewPacket = reviewPacket;
		this.provider = provider;
		this.reviewerSummary = reviewerSummary;
		this.suggestedInspectionAreas = suggestedInspectionAreas;
		this.riskExplanation = riskExplanation;
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

	public ReviewPacket getReviewPacket() {
		return reviewPacket;
	}

	public String getProvider() {
		return provider;
	}

	public String getReviewerSummary() {
		return reviewerSummary;
	}

	public String getSuggestedInspectionAreas() {
		return suggestedInspectionAreas;
	}

	public String getRiskExplanation() {
		return riskExplanation;
	}

	public Instant getGeneratedAt() {
		return generatedAt;
	}
}
