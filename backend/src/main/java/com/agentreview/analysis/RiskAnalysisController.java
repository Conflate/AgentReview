package com.agentreview.analysis;

import com.agentreview.analysis.dto.RiskAnalysisResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}/risk-analysis")
public class RiskAnalysisController {

	private final RiskAnalysisService riskAnalysisService;

	public RiskAnalysisController(RiskAnalysisService riskAnalysisService) {
		this.riskAnalysisService = riskAnalysisService;
	}

	@PostMapping
	public ResponseEntity<RiskAnalysisResponse> analyze(@PathVariable Long sessionId) {
		return ResponseEntity.ok(riskAnalysisService.analyze(sessionId));
	}
}
