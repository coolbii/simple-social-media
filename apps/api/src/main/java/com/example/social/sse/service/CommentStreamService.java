package com.example.social.sse.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.example.social.comment.dto.CommentResponse;
import com.example.social.sse.dto.CommentCreatedEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class CommentStreamService {

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(long postId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(postId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(postId, emitter));
        emitter.onTimeout(() -> removeEmitter(postId, emitter));
        emitter.onError(error -> removeEmitter(postId, emitter));
        return emitter;
    }

    public void publish(long postId, CommentResponse comment) {
        CommentCreatedEvent event = new CommentCreatedEvent("comment.created", postId, comment);
        for (SseEmitter emitter : emitters.getOrDefault(postId, new CopyOnWriteArrayList<>())) {
            try {
                emitter.send(SseEmitter.event().name("comment.created").data(event));
            } catch (IOException exception) {
                emitter.completeWithError(exception);
                removeEmitter(postId, emitter);
            }
        }
    }

    private void removeEmitter(long postId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> postEmitters = emitters.get(postId);
        if (postEmitters == null) {
            return;
        }
        postEmitters.remove(emitter);
        if (postEmitters.isEmpty()) {
            emitters.remove(postId);
        }
    }
}
