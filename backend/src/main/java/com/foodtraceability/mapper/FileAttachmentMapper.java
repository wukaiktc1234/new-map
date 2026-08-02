package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.FileAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FileAttachmentMapper extends BaseMapper<FileAttachment> {

    IPage<FileAttachment> selectFilePage(
        Page<FileAttachment> page,
        @Param("businessType") String businessType,
        @Param("businessId") String businessId,
        @Param("uploadUserId") String uploadUserId,
        @Param("contentType") String contentType,
        @Param("originalName") String originalName,
        @Param("status") String status,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    int incrementDownloadCount(@Param("attachmentId") Long attachmentId);

    List<FileAttachment> selectByBusinessId(@Param("businessType") String businessType,
                                            @Param("businessId") String businessId);

    long countByUser(@Param("userId") String userId);

    long sumFileSizeByUser(@Param("userId") String userId);
}
