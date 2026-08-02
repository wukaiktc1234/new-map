package com.foodtraceability.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.entity.FileAttachment;
import com.foodtraceability.mapper.FileAttachmentMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Component
@ConditionalOnProperty(name = "file.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class FileCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(FileCleanupScheduler.class);

    public FileCleanupScheduler(FileAttachmentMapper fileAttachmentMapper) {
        this.fileAttachmentMapper = fileAttachmentMapper;
    }

    @Value("${file.upload-path:./data/uploads}")
    private String baseUploadPath;

    @Value("${file.cleanup.logical-delete-days:180}")
    private int logicalDeleteDays;

    @Value("${file.cleanup.physical-cleanup-days:30}")
    private int physicalCleanupDays;

    private final FileAttachmentMapper fileAttachmentMapper;

    @PostConstruct
    public void logConfig() {
        logger.info("文件清理调度器已启用: 逻辑删除前{}天, 物理清理前{}天",
            logicalDeleteDays, physicalCleanupDays);
    }

    @Scheduled(cron = "0 0 5 * * ?")
    public void cleanupExpiredFiles() {
        logger.info("=== 开始执行过期文件清理任务 ===");

        LocalDateTime logicalExpireDate = LocalDateTime.now().minusDays(logicalDeleteDays);
        LambdaQueryWrapper<FileAttachment> oldFilesWrapper = new LambdaQueryWrapper<>();
        oldFilesWrapper.eq(FileAttachment::getDeleted, 0)
                       .eq(FileAttachment::getStatus, "active")
                       .lt(FileAttachment::getCreatedAt, logicalExpireDate);
        List<FileAttachment> oldFiles = fileAttachmentMapper.selectList(oldFilesWrapper);

        if (!oldFiles.isEmpty()) {
            List<Long> ids = new java.util.ArrayList<>();
            for (FileAttachment f : oldFiles) ids.add(f.getAttachmentId());
            if (!ids.isEmpty()) {
                fileAttachmentMapper.deleteBatchIds(ids);
                logger.info("逻辑标记过期文件: {}条, 时间阈值: {}", ids.size(), logicalExpireDate);
            }
        } else {
            logger.debug("无过期活跃文件需要处理");
        }

        LocalDateTime physicalExpireDate = LocalDateTime.now().minusDays(physicalCleanupDays);
        LambdaQueryWrapper<FileAttachment> deletedWrapper = new LambdaQueryWrapper<>();
        deletedWrapper.eq(FileAttachment::getDeleted, 1)
                      .ge(FileAttachment::getUpdatedAt, physicalExpireDate);
        List<FileAttachment> toPhysicallyDelete = fileAttachmentMapper.selectList(deletedWrapper);

        int physicallyDeleted = 0;
        for (FileAttachment f : toPhysicallyDelete) {
            try {
                Path filePath = Paths.get(baseUploadPath, f.getFilePath());
                if (filePath.startsWith(Paths.get(baseUploadPath).normalize()) && Files.exists(filePath)) {
                    Files.deleteIfExists(filePath);
                    physicallyDeleted++;
                }
                if (f.getThumbnailPath() != null && !f.getThumbnailPath().isEmpty()) {
                    Path thumbPath = Paths.get(baseUploadPath, f.getThumbnailPath());
                    if (thumbPath.startsWith(Paths.get(baseUploadPath).normalize())) {
                        Files.deleteIfExists(thumbPath);
                    }
                }
            } catch (Exception e) {
                logger.warn("物理删除文件失败(id={}): {}", f.getAttachmentId(), e.getMessage());
            }
        }

        if (physicallyDeleted > 0 || !toPhysicallyDelete.isEmpty()) {
            logger.info("物理清理已删除文件: 实际删除{}/{}条, 时间阈值: {}",
                physicallyDeleted, toPhysicallyDelete.size(), physicalExpireDate);
        }

        logger.info("=== 过期文件清理任务完成 ===");
    }

    @Scheduled(cron = "0 0 6 * * ?")
    public void checkOrphanedFiles() {
        logger.info("检查孤立物理文件...");
        AtomicLong orphanCount = new AtomicLong(0);
        Path uploadsDir = Paths.get(baseUploadPath);
        if (!Files.exists(uploadsDir)) return;

        try (var paths = Files.walk(uploadsDir)) {
            paths.filter(Files::isRegularFile)
                 .forEach(path -> {
                     String relativePath = uploadsDir.relativize(path).toString();
                     LambdaQueryWrapper<FileAttachment> wrapper = new LambdaQueryWrapper<>();
                     wrapper.eq(FileAttachment::getFilePath, relativePath)
                            .eq(FileAttachment::getDeleted, 0)
                            .last("LIMIT 1");
                     long count = fileAttachmentMapper.selectCount(wrapper);
                     if (count == 0) {
                         try {
                             Files.deleteIfExists(path);
                             orphanCount.incrementAndGet();
                             logger.debug("删除孤立文件: {}", relativePath);
                         } catch (Exception e) {
                             logger.warn("删除孤立文件失败: {}", path, e);
                         }
                     }
                 });
        } catch (Exception e) {
            logger.error("检查孤立文件失败: {}", e.getMessage());
        }

        if (orphanCount.get() > 0) {
            logger.info("发现并清理孤立文件: {}个", orphanCount.get());
        } else {
            logger.debug("无孤立文件");
        }
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationReady() {
        logger.info("文件管理模块定时任务已注册: 每日05:00过期清理, 每日06:00孤立文件扫描");
    }
}
