package com.agentreview.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentreview.analysis.dto.DiffImportRequest;
import com.agentreview.audit.AuditLogService;
import com.agentreview.common.AgentTool;
import com.agentreview.common.FileChangeType;
import com.agentreview.common.ResourceNotFoundException;
import com.agentreview.session.AgentSession;
import com.agentreview.session.AgentSessionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiffImportServiceTest {

	@Mock
	private AgentSessionRepository agentSessionRepository;

	@Mock
	private ChangedFileRepository changedFileRepository;

	@Mock
	private DiffParserService diffParserService;

	@Mock
	private AuditLogService auditLogService;

	@InjectMocks
	private DiffImportService diffImportService;

	@Test
	@SuppressWarnings("unchecked")
	void importDiffReplacesChangedFilesForSession() {
		AgentSession session = new AgentSession(
				"codex-run-001",
				AgentTool.CODEX,
				"David",
				"agentreview",
				"main",
				"Implemented session API"
		);
		session.setId(1L);
		when(agentSessionRepository.findById(1L)).thenReturn(Optional.of(session));
		when(diffParserService.parse("diff text")).thenReturn(List.of(
				new com.agentreview.analysis.dto.ParsedChangedFile("src/main/java/App.java", FileChangeType.MODIFIED)
		));
		when(changedFileRepository.saveAll(anyList())).thenAnswer(invocation -> {
			List<ChangedFile> files = invocation.getArgument(0);
			for (int i = 0; i < files.size(); i++) {
				files.get(i).setId((long) i + 1);
			}
			return files;
		});

		var response = diffImportService.importDiff(1L, new DiffImportRequest("diff text"));

		ArgumentCaptor<List<ChangedFile>> captor = ArgumentCaptor.forClass(List.class);
		verify(changedFileRepository).deleteBySessionId(1L);
		verify(changedFileRepository).saveAll(captor.capture());
		verify(auditLogService).recordDiffImported(session, 1);
		assertThat(captor.getValue()).hasSize(1);
		assertThat(captor.getValue().get(0).getSession()).isEqualTo(session);
		assertThat(captor.getValue().get(0).getFilePath()).isEqualTo("src/main/java/App.java");
		assertThat(response.sessionId()).isEqualTo(1L);
		assertThat(response.changedFileCount()).isEqualTo(1);
		assertThat(response.changedFiles().get(0).changeType()).isEqualTo(FileChangeType.MODIFIED);
	}

	@Test
	void importDiffThrowsWhenSessionDoesNotExist() {
		when(agentSessionRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> diffImportService.importDiff(99L, new DiffImportRequest("diff text")))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Agent session not found: 99");
	}
}
