package com.agentreview.analysis.dto;

import com.agentreview.analysis.TestEvidence;
import com.agentreview.common.TestStatus;
import java.time.Instant;

public record TestOutputImportResponse(
		Long id,
		Long sessionId,
		String command,
		TestStatus status,
		String outputExcerpt,
		Instant importedAt
) {

	private static final int EXCERPT_LENGTH = 500;

	public static TestOutputImportResponse from(TestEvidence evidence) {
		return new TestOutputImportResponse(
				evidence.getId(),
				evidence.getSession().getId(),
				evidence.getTestCommand(),
				evidence.getStatus(),
				excerpt(evidence.getOutputText()),
				evidence.getImportedAt()
		);
	}

	private static String excerpt(String outputText) {
		if (outputText.length() <= EXCERPT_LENGTH) {
			return outputText;
		}
		return outputText.substring(0, EXCERPT_LENGTH);
	}
}
