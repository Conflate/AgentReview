package com.agentreview.repository;

import com.agentreview.repository.dto.CreateRepositoryProfileRequest;
import com.agentreview.repository.dto.RepositoryProfileResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repository-profiles")
public class RepositoryProfileController {

	private final RepositoryProfileService repositoryProfileService;

	public RepositoryProfileController(RepositoryProfileService repositoryProfileService) {
		this.repositoryProfileService = repositoryProfileService;
	}

	@PostMapping
	public ResponseEntity<RepositoryProfileResponse> create(@Valid @RequestBody CreateRepositoryProfileRequest request) {
		RepositoryProfileResponse response = repositoryProfileService.create(request);
		return ResponseEntity.created(URI.create("/api/repository-profiles/" + response.id())).body(response);
	}

	@GetMapping
	public List<RepositoryProfileResponse> findAll() {
		return repositoryProfileService.findAll();
	}

	@GetMapping("/{id}")
	public RepositoryProfileResponse findById(@PathVariable Long id) {
		return repositoryProfileService.findById(id);
	}
}
