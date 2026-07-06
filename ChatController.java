package com.inventory.agent.controller;

import com.inventory.agent.dto.ChatRequest;
import com.inventory.agent.dto.ChatResponse;
import com.inventory.agent.service.GroqService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final GroqService groqService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String answer = groqService.answerInventoryQuestion(request.getMessage());
        return ResponseEntity.ok(new ChatResponse(answer, null));
    }
}
