package com.agentreview.analysis.dto;

import java.util.List;

public record DiffImportResponse(
		Long sessionId,
		int changedFileCount,
		List<ChangedFileResponse> changedFiles
) {
}
