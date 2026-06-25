package com.agentreview.analysis;

import com.agentreview.analysis.dto.ChangedFileResponse;
import com.agentreview.analysis.dto.DiffImportRequest;
import com.agentreview.analysis.dto.DiffImportResponse;
import com.agentreview.analysis.dto.ParsedChangedFile;
import com.agentreview.common.ResourceNotFoundException;
import com.agentreview.session.AgentSession;
import com.agentreview.session.AgentSessionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiffImportService {

	private final AgentSessionRepository agentSessionRepository;
	private final ChangedFileRepository changedFileRepository;
	private final DiffParserService diffParserService;

	public DiffImportService(
			AgentSessionRepository agentSessionRepository,
			ChangedFileRepository changedFileRepository,
			DiffParserService diffParserService
	) {
		this.agentSessionRepository = agentSessionRepository;
		this.changedFileRepository = changedFileRepository;
		this.diffParserService = diffParserService;
	}

	@Transactional
	public DiffImportResponse importDiff(Long sessionId, DiffImportRequest request) {
		AgentSession session = agentSessionRepository.findById(sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("Agent session not found: " + sessionId));
		List<ChangedFile> changedFiles = diffParserService.parse(request.diffText()).stream()
				.map(parsedFile -> toEntity(session, parsedFile))
				.toList();
		changedFileRepository.deleteBySessionId(sessionId);
		List<ChangedFileResponse> savedFiles = changedFileRepository.saveAll(changedFiles).stream()
				.map(ChangedFileResponse::from)
				.toList();
		return new DiffImportResponse(sessionId, savedFiles.size(), savedFiles);
	}

	private ChangedFile toEntity(AgentSession session, ParsedChangedFile parsedFile) {
		return new ChangedFile(session, parsedFile.filePath(), parsedFile.changeType());
	}
}
