package com.agentreview.session;

import com.agentreview.session.dto.AgentSessionResponse;
import com.agentreview.session.dto.CreateAgentSessionRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
public class AgentSessionController {

	private final AgentSessionService agentSessionService;

	public AgentSessionController(AgentSessionService agentSessionService) {
		this.agentSessionService = agentSessionService;
	}

	@PostMapping
	public ResponseEntity<AgentSessionResponse> create(@Valid @RequestBody CreateAgentSessionRequest request) {
		AgentSessionResponse response = agentSessionService.create(request);
		return ResponseEntity.created(URI.create("/api/sessions/" + response.id())).body(response);
	}

	@GetMapping
	public List<AgentSessionResponse> findAll() {
		return agentSessionService.findAll();
	}

	@GetMapping("/{id}")
	public AgentSessionResponse findById(@PathVariable Long id) {
		return agentSessionService.findById(id);
	}
}
