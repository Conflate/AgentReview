package com.agentreview.repository;

import com.agentreview.common.ResourceNotFoundException;
import com.agentreview.repository.dto.CreateRepositoryProfileRequest;
import com.agentreview.repository.dto.RepositoryProfileResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepositoryProfileService {

	private final RepositoryProfileRepository repositoryProfileRepository;

	public RepositoryProfileService(RepositoryProfileRepository repositoryProfileRepository) {
		this.repositoryProfileRepository = repositoryProfileRepository;
	}

	@Transactional
	public RepositoryProfileResponse create(CreateRepositoryProfileRequest request) {
		RepositoryProfile profile = new RepositoryProfile(
				request.repoName().trim(),
				request.businessCriticality(),
				normalizePatterns(request.protectedPathPatterns()),
				normalizePatterns(request.restrictedCommandPatterns()),
				request.approvalRequiredAt()
		);
		return RepositoryProfileResponse.from(repositoryProfileRepository.save(profile));
	}

	@Transactional(readOnly = true)
	public List<RepositoryProfileResponse> findAll() {
		return repositoryProfileRepository.findAll().stream()
				.map(RepositoryProfileResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public RepositoryProfileResponse findById(Long id) {
		return repositoryProfileRepository.findById(id)
				.map(RepositoryProfileResponse::from)
				.orElseThrow(() -> new ResourceNotFoundException("Repository profile not found: " + id));
	}

	private List<String> normalizePatterns(List<String> patterns) {
		if (patterns == null) {
			return List.of();
		}
		return patterns.stream()
				.map(String::trim)
				.toList();
	}
}
