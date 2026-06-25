package com.agentreview.analysis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TestOutputImportRequest(
		@NotBlank String command,
		@Size(max = 200_000) String output
) {
}
