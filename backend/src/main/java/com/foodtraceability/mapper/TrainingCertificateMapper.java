package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.TrainingCertificate;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 培训证书Mapper接口
 * 提供培训证书相关的数据库操作
 */
@Repository
public interface TrainingCertificateMapper extends BaseMapper<TrainingCertificate> {

    /**
     * 分页查询证书列表
     *
     * @param page 分页对象
     * @param courseId 课程ID
     * @param employeeId 员工ID
     * @param status 证书状态（valid/expiring/expired）
     * @param keyword 关键字（员工姓名或课程标题模糊匹配）
     * @return 分页结果
     */
    IPage<TrainingCertificate> selectCertificatePage(Page<TrainingCertificate> page,
                                                     @Param("courseId") String courseId,
                                                     @Param("employeeId") String employeeId,
                                                     @Param("status") String status,
                                                     @Param("keyword") String keyword);

    /**
     * 统计即将到期证书数量（status = 'expiring'）
     *
     * @return 即将到期证书数量
     */
    Integer countExpiringCertificates();
}
