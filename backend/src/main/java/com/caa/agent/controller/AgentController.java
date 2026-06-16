package com.caa.agent.controller;

import com.caa.agent.dto.AgentRequest;
import com.caa.agent.dto.AgentResponse;
import com.caa.agent.service.AgentService;
import com.caa.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agents")
@Tag(name = "Agents", description = "Agent management endpoints")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping
    @Operation(summary = "List agents", description = "Returns all agents for the current tenant")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Agent list")
    public ResponseEntity<ApiResponse<List<AgentResponse>>> listAgents(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<AgentResponse> agents = activeOnly
                ? agentService.findActive()
                : agentService.findAll();
        return ResponseEntity.ok(ApiResponse.ok(agents));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get agent by ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Agent found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Agent not found")
    public ResponseEntity<ApiResponse<AgentResponse>> getAgent(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(agentService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Create agent")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Agent created")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Name conflict")
    public ResponseEntity<ApiResponse<AgentResponse>> createAgent(
            @Valid @RequestBody AgentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(agentService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update agent")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Agent updated")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Agent not found")
    public ResponseEntity<ApiResponse<AgentResponse>> updateAgent(
            @PathVariable String id,
            @Valid @RequestBody AgentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(agentService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete agent")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Agent deleted")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Agent not found")
    public ResponseEntity<ApiResponse<Void>> deleteAgent(@PathVariable String id) {
        agentService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.ok());
    }
}
