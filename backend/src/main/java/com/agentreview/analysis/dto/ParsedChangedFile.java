package com.agentreview.analysis.dto;

import com.agentreview.common.FileChangeType;

public record ParsedChangedFile(
		String filePath,
		FileChangeType changeType
) {
}
