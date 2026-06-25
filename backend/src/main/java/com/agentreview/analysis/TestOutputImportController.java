package com.agentreview.analysis;

import com.agentreview.analysis.dto.TestOutputImportRequest;
import com.agentreview.analysis.dto.TestOutputImportResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}/test-output")
public class TestOutputImportController {

	private final TestOutputImportService testOutputImportService;

	public TestOutputImportController(TestOutputImportService testOutputImportService) {
		this.testOutputImportService = testOutputImportService;
	}

	@PostMapping
	public ResponseEntity<TestOutputImportResponse> importTestOutput(
			@PathVariable Long sessionId,
			@Valid @RequestBody TestOutputImportRequest request
	) {
		return ResponseEntity.ok(testOutputImportService.importTestOutput(sessionId, request));
	}
}
