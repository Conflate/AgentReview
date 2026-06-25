package com.agentreview.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentreview.common.AgentTool;
import com.agentreview.common.EventType;
import com.agentreview.common.ResourceNotFoundException;
import com.agentreview.event.dto.AgentEventImportRequest;
import com.agentreview.event.dto.ImportAgentEventRequest;
import com.agentreview.session.AgentSession;
import com.agentreview.session.AgentSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentEventImportServiceTest {

	@Mock
	private AgentSessionRepository agentSessionRepository;

	@Mock
	private AgentEventRepository agentEventRepository;

	@InjectMocks
	private AgentEventImportService agentEventImportService;

	@Test
	@SuppressWarnings("unchecked")
	void importEventsSavesEventsForSession() {
		AgentSession session = new AgentSession(
				"codex-run-001",
				AgentTool.CODEX,
				"David",
				"agentreview",
				"main",
				"Implemented repository profiles"
		);
		session.setId(1L);
		when(agentSessionRepository.findById(1L)).thenReturn(Optional.of(session));
		when(agentEventRepository.saveAll(anyList())).thenAnswer(invocation -> {
			List<AgentEvent> events = invocation.getArgument(0);
			for (int i = 0; i < events.size(); i++) {
				events.get(i).setId((long) i + 1);
			}
			return events;
		});
		Instant timestamp = Instant.parse("2026-06-01T20:05:00Z");
		AgentEventImportRequest request = new AgentEventImportRequest(List.of(
				new ImportAgentEventRequest(
						EventType.RUN_COMMAND,
						null,
						" ./mvnw test ",
						" Ran backend tests ",
						timestamp
				)
		));

		var response = agentEventImportService.importEvents(1L, request);

		ArgumentCaptor<List<AgentEvent>> captor = ArgumentCaptor.forClass(List.class);
		verify(agentEventRepository).saveAll(captor.capture());
		List<AgentEvent> savedEvents = captor.getValue();
		assertThat(savedEvents).hasSize(1);
		assertThat(savedEvents.get(0).getSession()).isEqualTo(session);
		assertThat(savedEvents.get(0).getEventType()).isEqualTo(EventType.RUN_COMMAND);
		assertThat(savedEvents.get(0).getCommand()).isEqualTo("./mvnw test");
		assertThat(savedEvents.get(0).getSummary()).isEqualTo("Ran backend tests");
		assertThat(response.sessionId()).isEqualTo(1L);
		assertThat(response.importedCount()).isEqualTo(1);
		assertThat(response.events().get(0).eventType()).isEqualTo(EventType.RUN_COMMAND);
	}

	@Test
	void importEventsThrowsWhenSessionDoesNotExist() {
		when(agentSessionRepository.findById(99L)).thenReturn(Optional.empty());
		AgentEventImportRequest request = new AgentEventImportRequest(List.of(
				new ImportAgentEventRequest(
						EventType.EDIT_FILE,
						"src/main/java/com/app/refunds/RefundValidator.java",
						null,
						"Edited refund validation logic",
						Instant.parse("2026-06-01T20:00:00Z")
				)
		));

		assertThatThrownBy(() -> agentEventImportService.importEvents(99L, request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Agent session not found: 99");
	}
}
