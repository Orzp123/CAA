package com.caa.workflow.controller;

import com.caa.common.ApiResponse;
import com.caa.workflow.dto.WorkflowRequest;
import com.caa.workflow.dto.WorkflowResponse;
import com.caa.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workflows")
@Tag(name = "Workflows", description = "Workflow definition and execution endpoints")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping
    @Operation(summary = "List workflow definitions")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Workflow list")
    public ResponseEntity<ApiResponse<List<WorkflowResponse>>> listWorkflows() {
        return ResponseEntity.ok(ApiResponse.ok(workflowService.findAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get workflow definition by ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Workflow found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Workflow not found")
    public ResponseEntity<ApiResponse<WorkflowResponse>> getWorkflow(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(workflowService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Create workflow definition")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Workflow created")
    public ResponseEntity<ApiResponse<WorkflowResponse>> createWorkflow(
            @Valid @RequestBody WorkflowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(workflowService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update workflow definition")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Workflow updated")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Workflow not found")
    public ResponseEntity<ApiResponse<WorkflowResponse>> updateWorkflow(
            @PathVariable String id,
            @Valid @RequestBody WorkflowRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(workflowService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete workflow definition")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Workflow deleted")
    public ResponseEntity<ApiResponse<Void>> deleteWorkflow(@PathVariable String id) {
        workflowService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.ok());
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start a workflow execution via Temporal")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Workflow started")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Workflow not found")
    public ResponseEntity<ApiResponse<Map<String, Object>>> startWorkflow(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> input) {
        return ResponseEntity.ok(ApiResponse.ok(
                workflowService.startWorkflow(id, input != null ? input : Map.of())));
    }

    @GetMapping("/{workflowId}/status")
    @Operation(summary = "Get Temporal workflow run status")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status returned")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus(
            @PathVariable String workflowId) {
        return ResponseEntity.ok(ApiResponse.ok(workflowService.getWorkflowStatus(workflowId)));
    }

    @DeleteMapping("/{workflowId}/terminate")
    @Operation(summary = "Terminate a running Temporal workflow")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Workflow terminated")
    public ResponseEntity<ApiResponse<Void>> terminateWorkflow(
            @PathVariable String workflowId,
            @RequestParam(defaultValue = "User requested termination") String reason) {
        workflowService.terminateWorkflow(workflowId, reason);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.ok());
    }
}
