package com.agentreview.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentreview.common.ResourceNotFoundException;
import com.agentreview.common.RiskLevel;
import com.agentreview.repository.dto.CreateRepositoryProfileRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepositoryProfileServiceTest {

	@Mock
	private RepositoryProfileRepository repositoryProfileRepository;

	@InjectMocks
	private RepositoryProfileService repositoryProfileService;

	@Test
	void createSavesRepositoryProfile() {
		when(repositoryProfileRepository.save(any(RepositoryProfile.class))).thenAnswer(invocation -> {
			RepositoryProfile profile = invocation.getArgument(0);
			profile.setId(1L);
			return profile;
		});
		CreateRepositoryProfileRequest request = new CreateRepositoryProfileRequest(
				" payments-api ",
				RiskLevel.HIGH,
				List.of(" src/main/java/**/payment/** "),
				List.of(" rm -rf "),
				RiskLevel.MEDIUM
		);

		var response = repositoryProfileService.create(request);

		ArgumentCaptor<RepositoryProfile> captor = ArgumentCaptor.forClass(RepositoryProfile.class);
		verify(repositoryProfileRepository).save(captor.capture());
		RepositoryProfile savedProfile = captor.getValue();
		assertThat(savedProfile.getRepoName()).isEqualTo("payments-api");
		assertThat(savedProfile.getProtectedPathPatterns()).containsExactly("src/main/java/**/payment/**");
		assertThat(savedProfile.getRestrictedCommandPatterns()).containsExactly("rm -rf");
		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.approvalRequiredAt()).isEqualTo(RiskLevel.MEDIUM);
	}

	@Test
	void findAllReturnsProfiles() {
		RepositoryProfile profile = new RepositoryProfile(
				"payments-api",
				RiskLevel.HIGH,
				List.of("src/main/java/**/payment/**"),
				List.of("rm -rf"),
				RiskLevel.MEDIUM
		);
		profile.setId(1L);
		when(repositoryProfileRepository.findAll()).thenReturn(List.of(profile));

		var responses = repositoryProfileService.findAll();

		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).repoName()).isEqualTo("payments-api");
	}

	@Test
	void findByIdThrowsWhenProfileDoesNotExist() {
		when(repositoryProfileRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> repositoryProfileService.findById(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Repository profile not found: 99");
	}
}
