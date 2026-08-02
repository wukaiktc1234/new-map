package com.foodtraceability.service;

import com.foodtraceability.common.Result;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 云存储服务接口
 */
public interface CloudStorageService {

    /**
     * 上传文件到云存储
     *
     * @param file     本地文件
     * @param remotePath 远程路径
     * @return 上传结果
     */
    Result<Map<String, Object>> uploadFile(File file, String remotePath);

    /**
     * 从云存储下载文件
     *
     * @param remotePath 远程路径
     * @param localPath  本地保存路径
     * @return 下载结果
     */
    Result<File> downloadFile(String remotePath, String localPath);

    /**
     * 删除云存储中的文件
     *
     * @param remotePath 远程路径
     * @return 删除结果
     */
    Result<Void> deleteFile(String remotePath);

    /**
     * 列出云存储中的文件
     *
     * @param prefix 路径前缀
     * @return 文件列表
     */
    Result<List<Map<String, Object>>> listFiles(String prefix);

    /**
     * 获取文件信息
     *
     * @param remotePath 远程路径
     * @return 文件信息
     */
    Result<Map<String, Object>> getFileInfo(String remotePath);

    /**
     * 检查文件是否存在
     *
     * @param remotePath 远程路径
     * @return 是否存在
     */
    Result<Boolean> fileExists(String remotePath);

    /**
     * 获取存储类型
     *
     * @return 存储类型名称
     */
    String getStorageType();

    /**
     * 检查存储服务是否可用
     *
     * @return 是否可用
     */
    Result<Boolean> checkAvailability();
}
