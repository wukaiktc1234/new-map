package com.foodtraceability.service;

import com.foodtraceability.dto.UploadProgressDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UploadProgressService {
    
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<String, UploadProgressDTO> progressMap = new ConcurrentHashMap<>();
    
    public SseEmitter createEmitter(String uploadId) {
        SseEmitter emitter = new SseEmitter(300000L);
        
        emitter.onCompletion(() -> {
            emitters.remove(uploadId);
            progressMap.remove(uploadId);
        });
        
        emitter.onTimeout(() -> {
            emitters.remove(uploadId);
            progressMap.remove(uploadId);
        });
        
        emitter.onError(e -> {
            emitters.remove(uploadId);
            progressMap.remove(uploadId);
        });
        
        emitters.put(uploadId, emitter);
        return emitter;
    }
    
    public void sendProgress(String uploadId, UploadProgressDTO progress) {
        progressMap.put(uploadId, progress);
        
        SseEmitter emitter = emitters.get(uploadId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(progress));
                
                if (UploadProgressDTO.STAGE_COMPLETE.equals(progress.getStage()) ||
                    UploadProgressDTO.STAGE_ERROR.equals(progress.getStage())) {
                    emitter.complete();
                    emitters.remove(uploadId);
                }
            } catch (IOException e) {
                emitters.remove(uploadId);
            }
        }
    }
    
    public void sendError(String uploadId, String errorMessage) {
        UploadProgressDTO progress = UploadProgressDTO.builder()
                .uploadId(uploadId)
                .stage(UploadProgressDTO.STAGE_ERROR)
                .progress(0)
                .message("处理失败")
                .errorMessage(errorMessage)
                .status(UploadProgressDTO.STATUS_ERROR)
                .timestamp(System.currentTimeMillis())
                .build();
        
        sendProgress(uploadId, progress);
    }
    
    public UploadProgressDTO getProgress(String uploadId) {
        return progressMap.get(uploadId);
    }
    
    public void removeEmitter(String uploadId) {
        SseEmitter emitter = emitters.remove(uploadId);
        if (emitter != null) {
            emitter.complete();
        }
        progressMap.remove(uploadId);
    }
}
