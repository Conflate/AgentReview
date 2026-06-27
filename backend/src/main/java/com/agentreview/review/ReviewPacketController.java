package com.agentreview.review;

import com.agentreview.review.dto.ReviewPacketResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}/review-packet")
public class ReviewPacketController {

	private final ReviewPacketService reviewPacketService;

	public ReviewPacketController(ReviewPacketService reviewPacketService) {
		this.reviewPacketService = reviewPacketService;
	}

	@PostMapping
	public ResponseEntity<ReviewPacketResponse> generate(@PathVariable Long sessionId) {
		return ResponseEntity.ok(reviewPacketService.generate(sessionId));
	}

	@GetMapping
	public ResponseEntity<ReviewPacketResponse> getLatest(@PathVariable Long sessionId) {
		return ResponseEntity.ok(reviewPacketService.getLatest(sessionId));
	}
}
