package com.agentreview.analysis;

import com.agentreview.analysis.dto.DiffImportRequest;
import com.agentreview.analysis.dto.DiffImportResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}/diff")
public class DiffImportController {

	private final DiffImportService diffImportService;

	public DiffImportController(DiffImportService diffImportService) {
		this.diffImportService = diffImportService;
	}

	@PostMapping
	public ResponseEntity<DiffImportResponse> importDiff(
			@PathVariable Long sessionId,
			@Valid @RequestBody DiffImportRequest request
	) {
		return ResponseEntity.ok(diffImportService.importDiff(sessionId, request));
	}
}
