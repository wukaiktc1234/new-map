package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.PurchaseRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 采购申请Mapper接口
 */
@Mapper
public interface PurchaseRequestMapper extends BaseMapper<PurchaseRequest> {
    
    /**
     * 分页查询采购申请（包含明细）
     * @param page 分页参数
     * @return 分页结果
     */
    @Select("SELECT * FROM purchase_request ORDER BY create_time DESC")
    IPage<PurchaseRequest> selectPageWithItems(Page<PurchaseRequest> page);
    
    /**
     * 根据ID查询采购申请
     * @param requestId 申请ID
     * @return 采购申请
     */
    @Select("SELECT * FROM purchase_request WHERE request_id = #{requestId}")
    PurchaseRequest selectByIdWithItems(@Param("requestId") String requestId);
    
    /**
     * 根据状态查询采购申请列表
     * @param status 状态
     * @return 采购申请列表
     */
    @Select("SELECT * FROM purchase_request WHERE status = #{status} ORDER BY create_time DESC")
    List<PurchaseRequest> selectByStatus(@Param("status") String status);
    
    /**
     * 统计指定状态的采购申请数量
     * @param status 状态
     * @return 数量
     */
    @Select("SELECT COUNT(*) FROM purchase_request WHERE status = #{status}")
    int countByStatus(@Param("status") String status);
}
