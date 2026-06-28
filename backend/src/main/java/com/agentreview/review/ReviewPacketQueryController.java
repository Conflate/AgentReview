package com.agentreview.review;

import com.agentreview.review.dto.ReviewPacketResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review-packets")
public class ReviewPacketQueryController {

	private final ReviewPacketService reviewPacketService;

	public ReviewPacketQueryController(ReviewPacketService reviewPacketService) {
		this.reviewPacketService = reviewPacketService;
	}

	@GetMapping
	public List<ReviewPacketResponse> findAll() {
		return reviewPacketService.findAll();
	}

	@GetMapping("/{id}")
	public ReviewPacketResponse findById(@PathVariable Long id) {
		return reviewPacketService.findById(id);
	}
}
