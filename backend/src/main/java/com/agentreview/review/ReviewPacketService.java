package com.agentreview.review;

import com.agentreview.analysis.ChangedFile;
import com.agentreview.analysis.ChangedFileRepository;
import com.agentreview.analysis.RiskAnalysisService;
import com.agentreview.analysis.TestEvidence;
import com.agentreview.analysis.TestEvidenceRepository;
import com.agentreview.analysis.dto.PolicyFlagResponse;
import com.agentreview.analysis.dto.RiskAnalysisResponse;
import com.agentreview.audit.AuditLogService;
import com.agentreview.common.ResourceNotFoundException;
import com.agentreview.event.AgentEvent;
import com.agentreview.event.AgentEventRepository;
import com.agentreview.review.dto.ReviewPacketResponse;
import com.agentreview.session.AgentSession;
import com.agentreview.session.AgentSessionRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewPacketService {

	private final AgentSessionRepository agentSessionRepository;
	private final ChangedFileRepository changedFileRepository;
	private final AgentEventRepository agentEventRepository;
	private final TestEvidenceRepository testEvidenceRepository;
	private final RiskAnalysisService riskAnalysisService;
	private final ReviewPacketRepository reviewPacketRepository;
	private final AuditLogService auditLogService;

	public ReviewPacketService(
			AgentSessionRepository agentSessionRepository,
			ChangedFileRepository changedFileRepository,
			AgentEventRepository agentEventRepository,
			TestEvidenceRepository testEvidenceRepository,
			RiskAnalysisService riskAnalysisService,
			ReviewPacketRepository reviewPacketRepository,
			AuditLogService auditLogService
	) {
		this.agentSessionRepository = agentSessionRepository;
		this.changedFileRepository = changedFileRepository;
		this.agentEventRepository = agentEventRepository;
		this.testEvidenceRepository = testEvidenceRepository;
		this.riskAnalysisService = riskAnalysisService;
		this.reviewPacketRepository = reviewPacketRepository;
		this.auditLogService = auditLogService;
	}

	@Transactional
	public ReviewPacketResponse generate(Long sessionId) {
		AgentSession session = agentSessionRepository.findById(sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("Agent session not found: " + sessionId));
		RiskAnalysisResponse riskAnalysis = riskAnalysisService.analyze(sessionId);
		List<ChangedFile> changedFiles = changedFileRepository.findBySessionIdOrderByFilePathAsc(sessionId);
		List<AgentEvent> events = agentEventRepository.findBySessionIdOrderByEventTimestampAsc(sessionId);
		Optional<TestEvidence> testEvidence = testEvidenceRepository.findBySessionId(sessionId);
		String packetMarkdown = buildMarkdown(session, riskAnalysis, changedFiles, events, testEvidence);

		reviewPacketRepository.deleteBySessionId(sessionId);
		ReviewPacket packet = reviewPacketRepository.save(new ReviewPacket(
				session,
				riskAnalysis.riskLevel(),
				riskAnalysis.mergeReadiness(),
				packetMarkdown
		));
		auditLogService.recordReviewPacketGenerated(session, packet.getId());
		return ReviewPacketResponse.from(packet);
	}

	@Transactional(readOnly = true)
	public ReviewPacketResponse getLatest(Long sessionId) {
		ReviewPacket packet = reviewPacketRepository.findFirstBySessionIdOrderByGeneratedAtDesc(sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("Review packet not found for session: " + sessionId));
		return ReviewPacketResponse.from(packet);
	}

	private String buildMarkdown(
			AgentSession session,
			RiskAnalysisResponse riskAnalysis,
			List<ChangedFile> changedFiles,
			List<AgentEvent> events,
			Optional<TestEvidence> testEvidence
	) {
		StringBuilder markdown = new StringBuilder();
		markdown.append("# AgentReview Packet\n\n");
		markdown.append("## Session\n");
		appendLine(markdown, "Session ID", session.getId());
		appendLine(markdown, "External ID", session.getSessionExternalId());
		appendLine(markdown, "Agent", session.getAgentTool());
		appendLine(markdown, "Developer", session.getDeveloper());
		appendLine(markdown, "Repository", session.getRepoName());
		appendLine(markdown, "Branch", session.getBranchName());
		appendLine(markdown, "Summary", emptyFallback(session.getSummary(), "Not provided"));

		markdown.append("\n## Risk\n");
		appendLine(markdown, "Risk level", riskAnalysis.riskLevel());
		appendLine(markdown, "Merge readiness", riskAnalysis.mergeReadiness());

		markdown.append("\n## Policy Flags\n");
		if (riskAnalysis.policyFlags().isEmpty()) {
			markdown.append("- None\n");
		}
		else {
			for (PolicyFlagResponse flag : riskAnalysis.policyFlags()) {
				markdown.append("- [")
						.append(flag.riskLevel())
						.append("] ")
						.append(normalize(flag.message()))
						.append("\n");
			}
		}

		markdown.append("\n## Changed Files\n");
		if (changedFiles.isEmpty()) {
			markdown.append("- None submitted\n");
		}
		else {
			for (ChangedFile changedFile : changedFiles) {
				markdown.append("- ")
						.append(changedFile.getChangeType())
						.append(": ")
						.append(changedFile.getFilePath())
						.append("\n");
			}
		}

		markdown.append("\n## Test Evidence\n");
		if (testEvidence.isEmpty()) {
			markdown.append("- No test output submitted\n");
		}
		else {
			TestEvidence evidence = testEvidence.get();
			appendLine(markdown, "Command", evidence.getTestCommand());
			appendLine(markdown, "Status", evidence.getStatus());
			appendLine(markdown, "Output excerpt", excerpt(evidence.getOutputText()));
		}

		markdown.append("\n## Agent Activity\n");
		if (events.isEmpty()) {
			markdown.append("- No events submitted\n");
		}
		else {
			for (AgentEvent event : events) {
				markdown.append("- ")
						.append(event.getEventType())
						.append(": ")
						.append(activitySummary(event))
						.append("\n");
			}
		}

		return markdown.toString();
	}

	private void appendLine(StringBuilder markdown, String label, Object value) {
		markdown.append("- ")
				.append(label)
				.append(": ")
				.append(value == null ? "" : normalize(value.toString()))
				.append("\n");
	}

	private String activitySummary(AgentEvent event) {
		if (event.getCommand() != null && !event.getCommand().isBlank()) {
			return normalize(event.getCommand());
		}
		if (event.getFilePath() != null && !event.getFilePath().isBlank()) {
			return normalize(event.getFilePath());
		}
		return emptyFallback(event.getSummary(), "No summary");
	}

	private String emptyFallback(String value, String fallback) {
		if (value == null || value.isBlank()) {
			return fallback;
		}
		return normalize(value);
	}

	private String excerpt(String value) {
		String normalized = normalize(value);
		if (normalized.length() <= 500) {
			return normalized;
		}
		return normalized.substring(0, 500);
	}

	private String normalize(String value) {
		return value.replace('\r', ' ').replace('\n', ' ').trim();
	}
}
