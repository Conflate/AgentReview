package com.agentreview.analysis;

import com.agentreview.common.FileChangeType;
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
@Table(name = "changed_files")
public class ChangedFile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_id", nullable = false)
	private AgentSession session;

	@Column(nullable = false, length = 1000)
	private String filePath;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private FileChangeType changeType;

	@Column(nullable = false, updatable = false)
	private Instant importedAt;

	protected ChangedFile() {
	}

	public ChangedFile(AgentSession session, String filePath, FileChangeType changeType) {
		this.session = session;
		this.filePath = filePath;
		this.changeType = changeType;
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

	public String getFilePath() {
		return filePath;
	}

	public FileChangeType getChangeType() {
		return changeType;
	}

	public Instant getImportedAt() {
		return importedAt;
	}
}
