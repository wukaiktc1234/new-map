package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.FileAttachment;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public interface FileAttachmentService {

    FileAttachment uploadFile(MultipartFile file, String businessType, String businessId, String userId, String username);

    List<FileAttachment> uploadFiles(MultipartFile[] files, String businessType, String businessId, String userId, String username);

    void downloadFile(Long attachmentId, HttpServletResponse response, String currentUserId, boolean isAdmin);

    void downloadByPath(String filePath, String originalName, HttpServletResponse response);

    FileAttachment getFileInfo(Long attachmentId, String currentUserId, boolean isAdmin);

    IPage<FileAttachment> getFilePage(Page<FileAttachment> page, String businessType, String businessId,
                                       String uploadUserId, String contentType, String originalName,
                                       String status, java.time.LocalDateTime startTime,
                                       java.time.LocalDateTime endTime);

    List<FileAttachment> getFilesByBusiness(String businessType, String businessId);

    boolean deleteFile(Long attachmentId, String userId, boolean isAdmin);

    boolean deleteFiles(List<Long> attachmentIds, String userId, boolean isAdmin);

    Map<String, Object> getStorageStats(String userId);

    boolean existsByMd5(String md5);

    FileAttachment getByMd5(String md5);
}
