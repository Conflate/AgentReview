package com.agentreview.analysis;

import com.agentreview.analysis.dto.PolicyFlagResponse;
import com.agentreview.analysis.dto.RiskAnalysisResponse;
import com.agentreview.audit.AuditLogService;
import com.agentreview.common.FileChangeType;
import com.agentreview.common.MergeReadiness;
import com.agentreview.common.ResourceNotFoundException;
import com.agentreview.common.RiskLevel;
import com.agentreview.common.TestStatus;
import com.agentreview.event.AgentEvent;
import com.agentreview.event.AgentEventRepository;
import com.agentreview.repository.RepositoryProfile;
import com.agentreview.repository.RepositoryProfileRepository;
import com.agentreview.session.AgentSession;
import com.agentreview.session.AgentSessionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiskAnalysisService {

	private static final List<String> CI_PATH_PATTERNS = List.of(
			".github/workflows/**",
			".gitlab-ci.yml",
			"Jenkinsfile",
			"azure-pipelines.yml",
			".buildkite/**"
	);
	private static final List<String> DEPENDENCY_MANIFEST_PATTERNS = List.of(
			"pom.xml",
			"**/pom.xml",
			"package.json",
			"**/package.json",
			"package-lock.json",
			"**/package-lock.json"
	);

	private final AgentSessionRepository agentSessionRepository;
	private final RepositoryProfileRepository repositoryProfileRepository;
	private final ChangedFileRepository changedFileRepository;
	private final AgentEventRepository agentEventRepository;
	private final TestEvidenceRepository testEvidenceRepository;
	private final PolicyFlagRepository policyFlagRepository;
	private final AuditLogService auditLogService;

	public RiskAnalysisService(
			AgentSessionRepository agentSessionRepository,
			RepositoryProfileRepository repositoryProfileRepository,
			ChangedFileRepository changedFileRepository,
			AgentEventRepository agentEventRepository,
			TestEvidenceRepository testEvidenceRepository,
			PolicyFlagRepository policyFlagRepository,
			AuditLogService auditLogService
	) {
		this.agentSessionRepository = agentSessionRepository;
		this.repositoryProfileRepository = repositoryProfileRepository;
		this.changedFileRepository = changedFileRepository;
		this.agentEventRepository = agentEventRepository;
		this.testEvidenceRepository = testEvidenceRepository;
		this.policyFlagRepository = policyFlagRepository;
		this.auditLogService = auditLogService;
	}

	@Transactional
	public RiskAnalysisResponse analyze(Long sessionId) {
		AgentSession session = agentSessionRepository.findById(sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("Agent session not found: " + sessionId));
		RepositoryProfile profile = resolveProfile(session).orElse(null);
		List<PolicyFlag> flags = new ArrayList<>();
		List<ChangedFile> changedFiles = changedFileRepository.findBySessionIdOrderByFilePathAsc(sessionId);
		List<AgentEvent> events = agentEventRepository.findBySessionIdOrderByEventTimestampAsc(sessionId);
		Optional<TestEvidence> testEvidence = testEvidenceRepository.findBySessionId(sessionId);

		addFileRiskFlags(session, profile, changedFiles, flags);
		addCommandRiskFlags(session, profile, events, flags);
		addTestRiskFlags(session, changedFiles, testEvidence, flags);

		policyFlagRepository.deleteBySessionId(sessionId);
		List<PolicyFlag> savedFlags = policyFlagRepository.saveAll(flags);
		RiskLevel riskLevel = savedFlags.stream()
				.map(PolicyFlag::getRiskLevel)
				.reduce(RiskLevel.LOW, this::maxRisk);
		MergeReadiness mergeReadiness = calculateMergeReadiness(riskLevel, approvalThreshold(profile));
		List<PolicyFlagResponse> responses = savedFlags.stream()
				.map(PolicyFlagResponse::from)
				.toList();
		auditLogService.recordRiskAnalysisCompleted(session, riskLevel, mergeReadiness, savedFlags.size());
		return new RiskAnalysisResponse(sessionId, riskLevel, mergeReadiness, responses);
	}

	private Optional<RepositoryProfile> resolveProfile(AgentSession session) {
		if (session.getRepositoryProfile() != null) {
			return Optional.of(session.getRepositoryProfile());
		}
		return repositoryProfileRepository.findFirstByRepoNameIgnoreCase(session.getRepoName());
	}

	private void addFileRiskFlags(
			AgentSession session,
			RepositoryProfile profile,
			List<ChangedFile> changedFiles,
			List<PolicyFlag> flags
	) {
		List<String> protectedPathPatterns = profile == null ? List.of() : profile.getProtectedPathPatterns();
		for (ChangedFile changedFile : changedFiles) {
			String filePath = changedFile.getFilePath();
			if (matchesAnyPath(filePath, protectedPathPatterns)) {
				flags.add(new PolicyFlag(
						session,
						RiskLevel.HIGH,
						"Protected path changed: " + filePath
				));
			}
			if (matchesAnyPath(filePath, CI_PATH_PATTERNS)) {
				flags.add(new PolicyFlag(
						session,
						RiskLevel.HIGH,
						"CI or workflow file changed: " + filePath
				));
			}
			if (matchesAnyPath(filePath, DEPENDENCY_MANIFEST_PATTERNS)) {
				flags.add(new PolicyFlag(
						session,
						RiskLevel.MEDIUM,
						"Dependency manifest changed: " + filePath
				));
			}
			if (changedFile.getChangeType() == FileChangeType.DELETED && isSourceFile(filePath)) {
				flags.add(new PolicyFlag(
						session,
						RiskLevel.HIGH,
						"Source file deleted: " + filePath
				));
			}
		}
	}

	private void addCommandRiskFlags(
			AgentSession session,
			RepositoryProfile profile,
			List<AgentEvent> events,
			List<PolicyFlag> flags
	) {
		List<String> restrictedCommandPatterns = profile == null ? List.of() : profile.getRestrictedCommandPatterns();
		for (AgentEvent event : events) {
			String command = event.getCommand();
			if (command != null && matchesAnyText(command, restrictedCommandPatterns)) {
				flags.add(new PolicyFlag(
						session,
						RiskLevel.CRITICAL,
						"Restricted command used: " + command
				));
			}
		}
	}

	private void addTestRiskFlags(
			AgentSession session,
			List<ChangedFile> changedFiles,
			Optional<TestEvidence> testEvidence,
			List<PolicyFlag> flags
	) {
		if (testEvidence.isEmpty()) {
			if (isDocsOnlyChange(changedFiles)) {
				return;
			}
			flags.add(new PolicyFlag(
					session,
					RiskLevel.MEDIUM,
					"No test output submitted for this session"
			));
			return;
		}
		TestStatus status = testEvidence.get().getStatus();
		if (status == TestStatus.FAILED) {
			flags.add(new PolicyFlag(
					session,
					RiskLevel.CRITICAL,
					"Submitted tests failed"
			));
		}
		else if (status == TestStatus.NOT_RUN || status == TestStatus.UNKNOWN) {
			flags.add(new PolicyFlag(
					session,
					RiskLevel.MEDIUM,
					"Submitted test output did not prove tests passed"
			));
		}
	}

	private MergeReadiness calculateMergeReadiness(RiskLevel riskLevel, RiskLevel approvalThreshold) {
		if (riskLevel == RiskLevel.CRITICAL) {
			return MergeReadiness.BLOCKED;
		}
		if (riskLevel.ordinal() >= approvalThreshold.ordinal()) {
			return MergeReadiness.REVIEW_REQUIRED;
		}
		return MergeReadiness.LOW_RISK;
	}

	private RiskLevel approvalThreshold(RepositoryProfile profile) {
		if (profile == null) {
			return RiskLevel.MEDIUM;
		}
		return profile.getApprovalRequiredAt();
	}

	private RiskLevel maxRisk(RiskLevel first, RiskLevel second) {
		if (first.ordinal() >= second.ordinal()) {
			return first;
		}
		return second;
	}

	private boolean isSourceFile(String filePath) {
		return filePath.startsWith("src/") || filePath.matches(".*\\.(java|js|jsx|ts|tsx|py|rb|go|rs|kt|cs)$");
	}

	private boolean isDocsOnlyChange(List<ChangedFile> changedFiles) {
		return !changedFiles.isEmpty() && changedFiles.stream()
				.map(ChangedFile::getFilePath)
				.allMatch(this::isDocumentationFile);
	}

	private boolean isDocumentationFile(String filePath) {
		return filePath.equals("README.md")
				|| filePath.startsWith("docs/")
				|| filePath.matches(".*\\.(md|mdx|txt|adoc)$");
	}

	private boolean matchesAnyPath(String value, List<String> patterns) {
		return patterns.stream().anyMatch(pattern -> globMatches(value, pattern, false));
	}

	private boolean matchesAnyText(String value, List<String> patterns) {
		return patterns.stream().anyMatch(pattern -> globMatches(value, pattern, true));
	}

	private boolean globMatches(String value, String glob, boolean wildcardMatchesSlash) {
		return Pattern.compile(toRegex(glob, wildcardMatchesSlash)).matcher(value).matches();
	}

	private String toRegex(String glob, boolean wildcardMatchesSlash) {
		StringBuilder regex = new StringBuilder("^");
		for (int i = 0; i < glob.length(); i++) {
			char current = glob.charAt(i);
			if (current == '*') {
				boolean doubleStar = i + 1 < glob.length() && glob.charAt(i + 1) == '*';
				if (doubleStar) {
					regex.append(".*");
					i++;
				}
				else {
					regex.append(wildcardMatchesSlash ? ".*" : "[^/]*");
				}
			}
			else if (current == '?') {
				regex.append(wildcardMatchesSlash ? "." : "[^/]");
			}
			else {
				if ("\\.[]{}()+-^$|".indexOf(current) >= 0) {
					regex.append('\\');
				}
				regex.append(current);
			}
		}
		regex.append("$");
		return regex.toString();
	}
}
