package com.agentreview.session;

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

	public AgentSessionService(
			AgentSessionRepository agentSessionRepository,
			RepositoryProfileRepository repositoryProfileRepository
	) {
		this.agentSessionRepository = agentSessionRepository;
		this.repositoryProfileRepository = repositoryProfileRepository;
	}

	@Transactional
	public AgentSessionResponse create(CreateAgentSessionRequest request) {
		RepositoryProfile repositoryProfile = findRepositoryProfile(request.repositoryProfileId());
		AgentSession session = new AgentSession(
				request.sessionExternalId().trim(),
				request.agentTool(),
				request.developer().trim(),
				request.repoName().trim(),
				repositoryProfile,
				request.branchName().trim(),
				normalizeOptionalText(request.summary())
		);
		return AgentSessionResponse.from(agentSessionRepository.save(session));
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

	private RepositoryProfile findRepositoryProfile(Long repositoryProfileId) {
		if (repositoryProfileId == null) {
			return null;
		}
		return repositoryProfileRepository.findById(repositoryProfileId)
				.orElseThrow(() -> new ResourceNotFoundException("Repository profile not found: " + repositoryProfileId));
	}
}
