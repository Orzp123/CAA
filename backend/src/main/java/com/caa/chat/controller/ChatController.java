package com.caa.chat.controller;

import com.caa.chat.dto.ChatRequest;
import com.caa.chat.dto.ChatResponse;
import com.caa.chat.service.ChatService;
import com.caa.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
@Tag(name = "Chat", description = "AI chat endpoints")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/{agentId}")
    @Operation(summary = "Blocking chat completion")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Chat response")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Agent not found")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @PathVariable String agentId,
            @Valid @RequestBody ChatRequest request) {
        String content = chatService.chat(agentId, null, request.messages());
        ChatResponse response = new ChatResponse(agentId, content, null, null);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping(value = "/{agentId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Streaming chat completion (SSE)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "SSE stream")
    public Flux<String> chatStream(
            @PathVariable String agentId,
            @Valid @RequestBody ChatRequest request) {
        return chatService.chatStream(agentId, null, request.messages());
    }
}
