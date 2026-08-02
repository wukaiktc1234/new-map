package com.foodtraceability.service;

import com.foodtraceability.entity.FileAttachment;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface FileChunkService {

    String initChunkUpload(String fileName, long fileSize, String contentType,
                            String businessType, String userId, String username, int totalChunks);

    boolean uploadChunk(String uploadId, int chunkNumber, MultipartFile chunk);

    Map<String, Object> getUploadProgress(String uploadId);

    FileAttachment mergeChunks(String uploadId);

    boolean cancelUpload(String uploadId);

    void cleanupExpiredChunks();
}
