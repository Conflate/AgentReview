package com.agentreview.session;

import com.agentreview.audit.AuditLogService;
import com.agentreview.common.InvalidRequestException;
import com.agentreview.common.ResourceNotFoundException;
import com.agentreview.repository.RepositoryProfile;
import com.agentreview.repository.RepositoryProfileRepository;
import com.agentreview.session.dto.AgentSessionResponse;
import com.agentreview.session.dto.CreateAgentSessionRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentSessionService {

	private final AgentSessionRepository agentSessionRepository;
	private final RepositoryProfileRepository repositoryProfileRepository;
	private final AuditLogService auditLogService;

	public AgentSessionService(
			AgentSessionRepository agentSessionRepository,
			RepositoryProfileRepository repositoryProfileRepository,
			AuditLogService auditLogService
	) {
		this.agentSessionRepository = agentSessionRepository;
		this.repositoryProfileRepository = repositoryProfileRepository;
		this.auditLogService = auditLogService;
	}

	@Transactional
	public AgentSessionResponse create(CreateAgentSessionRequest request) {
		RepositoryProfile repositoryProfile = findRepositoryProfile(request.repositoryProfileId());
		String repoName = resolveRepoName(request.repoName(), repositoryProfile);
		String sessionExternalId = request.sessionExternalId().trim();
		if (agentSessionRepository.existsByAgentToolAndSessionExternalId(request.agentTool(), sessionExternalId)) {
			throw new InvalidRequestException(
					"Agent session already exists for tool and external id: "
							+ request.agentTool()
							+ "/"
							+ sessionExternalId
			);
		}
		AgentSession session = new AgentSession(
				sessionExternalId,
				request.agentTool(),
				request.developer().trim(),
				repoName,
				repositoryProfile,
				request.branchName().trim(),
				normalizeOptionalText(request.summary())
		);
		AgentSession savedSession = agentSessionRepository.save(session);
		auditLogService.recordSessionCreated(savedSession);
		return AgentSessionResponse.from(savedSession);
	}

	@Transactional(readOnly = true)
	public List<AgentSessionResponse> findAll() {
		return agentSessionRepository.findAll().stream()
				.map(AgentSessionResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public AgentSessionResponse findById(Long id) {
		return agentSessionRepository.findById(id)
				.map(AgentSessionResponse::from)
				.orElseThrow(() -> new ResourceNotFoundException("Agent session not found: " + id));
	}

	private String normalizeOptionalText(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private String resolveRepoName(String requestedRepoName, RepositoryProfile repositoryProfile) {
		String normalizedRepoName = requestedRepoName.trim();
		if (repositoryProfile == null) {
			return normalizedRepoName;
		}
		if (!normalizedRepoName.equalsIgnoreCase(repositoryProfile.getRepoName())) {
			throw new InvalidRequestException(
					"Session repository must match repository profile: " + repositoryProfile.getRepoName()
			);
		}
		return repositoryProfile.getRepoName();
	}

	private RepositoryProfile findRepositoryProfile(Long repositoryProfileId) {
		if (repositoryProfileId == null) {
			return null;
		}
		return repositoryProfileRepository.findById(repositoryProfileId)
				.orElseThrow(() -> new ResourceNotFoundException("Repository profile not found: " + repositoryProfileId));
	}
}
