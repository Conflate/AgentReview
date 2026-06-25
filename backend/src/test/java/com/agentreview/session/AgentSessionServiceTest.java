package com.agentreview.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentreview.common.AgentTool;
import com.agentreview.common.ResourceNotFoundException;
import com.agentreview.common.RiskLevel;
import com.agentreview.repository.RepositoryProfile;
import com.agentreview.repository.RepositoryProfileRepository;
import com.agentreview.session.dto.CreateAgentSessionRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentSessionServiceTest {

	@Mock
	private AgentSessionRepository agentSessionRepository;

	@Mock
	private RepositoryProfileRepository repositoryProfileRepository;

	@InjectMocks
	private AgentSessionService agentSessionService;

	@Test
	void createSavesAgentSession() {
		when(agentSessionRepository.save(any(AgentSession.class))).thenAnswer(invocation -> {
			AgentSession session = invocation.getArgument(0);
			session.setId(1L);
			return session;
		});
		CreateAgentSessionRequest request = new CreateAgentSessionRequest(
				" codex-run-123 ",
				AgentTool.CODEX,
				" David ",
				" agentreview ",
				null,
				" feature/repository-profiles ",
				" Added repository profile API "
		);

		var response = agentSessionService.create(request);

		ArgumentCaptor<AgentSession> captor = ArgumentCaptor.forClass(AgentSession.class);
		verify(agentSessionRepository).save(captor.capture());
		AgentSession savedSession = captor.getValue();
		assertThat(savedSession.getSessionExternalId()).isEqualTo("codex-run-123");
		assertThat(savedSession.getDeveloper()).isEqualTo("David");
		assertThat(savedSession.getBranchName()).isEqualTo("feature/repository-profiles");
		assertThat(savedSession.getSummary()).isEqualTo("Added repository profile API");
		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.agentTool()).isEqualTo(AgentTool.CODEX);
		assertThat(response.repositoryProfileId()).isNull();
	}

	@Test
	void createLinksRepositoryProfileWhenProvided() {
		RepositoryProfile profile = new RepositoryProfile(
				"agentreview",
				RiskLevel.MEDIUM,
				List.of("src/main/java/**"),
				List.of("rm -rf"),
				RiskLevel.MEDIUM
		);
		profile.setId(10L);
		when(repositoryProfileRepository.findById(10L)).thenReturn(Optional.of(profile));
		when(agentSessionRepository.save(any(AgentSession.class))).thenAnswer(invocation -> {
			AgentSession session = invocation.getArgument(0);
			session.setId(1L);
			return session;
		});
		CreateAgentSessionRequest request = new CreateAgentSessionRequest(
				"codex-run-123",
				AgentTool.CODEX,
				"David",
				"agentreview",
				10L,
				"main",
				null
		);

		var response = agentSessionService.create(request);

		ArgumentCaptor<AgentSession> captor = ArgumentCaptor.forClass(AgentSession.class);
		verify(agentSessionRepository).save(captor.capture());
		assertThat(captor.getValue().getRepositoryProfile()).isEqualTo(profile);
		assertThat(response.repositoryProfileId()).isEqualTo(10L);
	}

	@Test
	void createThrowsWhenRepositoryProfileDoesNotExist() {
		when(repositoryProfileRepository.findById(99L)).thenReturn(Optional.empty());
		CreateAgentSessionRequest request = new CreateAgentSessionRequest(
				"codex-run-123",
				AgentTool.CODEX,
				"David",
				"agentreview",
				99L,
				"main",
				null
		);

		assertThatThrownBy(() -> agentSessionService.create(request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Repository profile not found: 99");
	}

	@Test
	void findAllReturnsSessions() {
		AgentSession session = new AgentSession(
				"codex-run-123",
				AgentTool.CODEX,
				"David",
				"agentreview",
				"feature/repository-profiles",
				"Added repository profile API"
		);
		session.setId(1L);
		when(agentSessionRepository.findAll()).thenReturn(List.of(session));

		var responses = agentSessionService.findAll();

		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).sessionExternalId()).isEqualTo("codex-run-123");
		assertThat(responses.get(0).repoName()).isEqualTo("agentreview");
	}

	@Test
	void findByIdThrowsWhenSessionDoesNotExist() {
		when(agentSessionRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> agentSessionService.findById(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Agent session not found: 99");
	}
}
