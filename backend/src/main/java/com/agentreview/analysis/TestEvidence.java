package com.agentreview.analysis;

import com.agentreview.common.TestStatus;
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
@Table(name = "test_evidence")
public class TestEvidence {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_id", nullable = false)
	private AgentSession session;

	@Column(nullable = false, length = 1000)
	private String testCommand;

	@Column(nullable = false, columnDefinition = "text")
	private String outputText;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TestStatus status;

	@Column(nullable = false, updatable = false)
	private Instant importedAt;

	protected TestEvidence() {
	}

	public TestEvidence(AgentSession session, String testCommand, String outputText, TestStatus status) {
		this.session = session;
		this.testCommand = testCommand;
		this.outputText = outputText;
		this.status = status;
	}

	@PrePersist
	void setImportedAtIfMissing() {
		if (importedAt == null) {
			importedAt = Instant.now();
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

	public String getTestCommand() {
		return testCommand;
	}

	public String getOutputText() {
		return outputText;
	}

	public TestStatus getStatus() {
		return status;
	}

	public Instant getImportedAt() {
		return importedAt;
	}
}
