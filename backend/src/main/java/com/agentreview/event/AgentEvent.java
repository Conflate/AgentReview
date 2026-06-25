package com.agentreview.event;

import com.agentreview.common.EventType;
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
@Table(name = "agent_events")
public class AgentEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_id", nullable = false)
	private AgentSession session;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EventType eventType;

	private String filePath;

	@Column(length = 2000)
	private String command;

	@Column(length = 2000)
	private String summary;

	@Column(nullable = false)
	private Instant eventTimestamp;

	@Column(nullable = false, updatable = false)
	private Instant importedAt;

	protected AgentEvent() {
	}

	public AgentEvent(
			AgentSession session,
			EventType eventType,
			String filePath,
			String command,
			String summary,
			Instant eventTimestamp
	) {
		this.session = session;
		this.eventType = eventType;
		this.filePath = filePath;
		this.command = command;
		this.summary = summary;
		this.eventTimestamp = eventTimestamp;
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

	public EventType getEventType() {
		return eventType;
	}

	public String getFilePath() {
		return filePath;
	}

	public String getCommand() {
		return command;
	}

	public String getSummary() {
		return summary;
	}

	public Instant getEventTimestamp() {
		return eventTimestamp;
	}

	public Instant getImportedAt() {
		return importedAt;
	}
}
