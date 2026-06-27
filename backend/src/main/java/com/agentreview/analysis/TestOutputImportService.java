package com.agentreview.analysis;

import com.agentreview.analysis.dto.TestOutputImportRequest;
import com.agentreview.analysis.dto.TestOutputImportResponse;
import com.agentreview.audit.AuditLogService;
import com.agentreview.common.ResourceNotFoundException;
import com.agentreview.common.TestStatus;
import com.agentreview.session.AgentSession;
import com.agentreview.session.AgentSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestOutputImportService {

	private final AgentSessionRepository agentSessionRepository;
	private final TestEvidenceRepository testEvidenceRepository;
	private final TestOutputParserService testOutputParserService;
	private final AuditLogService auditLogService;

	public TestOutputImportService(
			AgentSessionRepository agentSessionRepository,
			TestEvidenceRepository testEvidenceRepository,
			TestOutputParserService testOutputParserService,
			AuditLogService auditLogService
	) {
		this.agentSessionRepository = agentSessionRepository;
		this.testEvidenceRepository = testEvidenceRepository;
		this.testOutputParserService = testOutputParserService;
		this.auditLogService = auditLogService;
	}

	@Transactional
	public TestOutputImportResponse importTestOutput(Long sessionId, TestOutputImportRequest request) {
		AgentSession session = agentSessionRepository.findById(sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("Agent session not found: " + sessionId));
		String outputText = normalizeOutput(request.output());
		TestStatus status = testOutputParserService.detectStatus(outputText);
		testEvidenceRepository.deleteBySessionId(sessionId);
		TestEvidence evidence = testEvidenceRepository.save(new TestEvidence(
				session,
				request.command().trim(),
				outputText,
				status
		));
		auditLogService.recordTestOutputImported(session, evidence.getStatus());
		return TestOutputImportResponse.from(evidence);
	}

	private String normalizeOutput(String output) {
		if (output == null) {
			return "";
		}
		return output.trim();
	}
}
