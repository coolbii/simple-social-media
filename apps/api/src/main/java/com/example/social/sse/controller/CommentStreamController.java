package com.example.social.sse.controller;

import com.example.social.sse.service.CommentStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@Tag(name = "Comments")
public class CommentStreamController {

    private final CommentStreamService commentStreamService;

    public CommentStreamController(CommentStreamService commentStreamService) {
        this.commentStreamService = commentStreamService;
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(operationId = "streamComments", summary = "Subscribe to live comment events.")
    public SseEmitter streamComments(@PathVariable long postId) {
        return commentStreamService.subscribe(postId);
    }
}
