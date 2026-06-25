package com.agentreview.analysis.dto;

import com.agentreview.analysis.ChangedFile;
import com.agentreview.common.FileChangeType;
import java.time.Instant;

public record ChangedFileResponse(
		Long id,
		Long sessionId,
		String filePath,
		FileChangeType changeType,
		Instant importedAt
) {

	public static ChangedFileResponse from(ChangedFile changedFile) {
		return new ChangedFileResponse(
				changedFile.getId(),
				changedFile.getSession().getId(),
				changedFile.getFilePath(),
				changedFile.getChangeType(),
				changedFile.getImportedAt()
		);
	}
}
