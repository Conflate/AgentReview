package com.agentreview.ai;

import com.agentreview.ai.dto.AiSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review-packets/{reviewPacketId}/ai-summary")
public class AiSummaryController {

	private final AiSummaryService aiSummaryService;

	public AiSummaryController(AiSummaryService aiSummaryService) {
		this.aiSummaryService = aiSummaryService;
	}

	@PostMapping
	public ResponseEntity<AiSummaryResponse> generate(@PathVariable Long reviewPacketId) {
		return ResponseEntity.ok(aiSummaryService.generate(reviewPacketId));
	}

	@GetMapping
	public ResponseEntity<AiSummaryResponse> getLatest(@PathVariable Long reviewPacketId) {
		return ResponseEntity.ok(aiSummaryService.getLatest(reviewPacketId));
	}
}
