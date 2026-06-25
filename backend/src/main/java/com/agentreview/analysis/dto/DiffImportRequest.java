package com.agentreview.analysis.dto;

import jakarta.validation.constraints.NotBlank;

public record DiffImportRequest(
		@NotBlank String diffText
) {
}
