package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.entity.DiningTableNew;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 桌台Mapper接口
 */
@Mapper
public interface DiningTableNewMapper extends BaseMapper<DiningTableNew> {

    /**
     * 根据桌台编码和门店ID查询
     * storeId为null时仅按编码查询（兼容旧逻辑）
     */
    @Select("<script>" +
            "SELECT * FROM dining_tables WHERE table_code = #{tableCode} AND deleted = 0" +
            "<if test='storeId != null'> AND store_id = #{storeId}</if>" +
            "</script>")
    DiningTableNew selectByTableCode(@Param("tableCode") String tableCode, @Param("storeId") Long storeId);

    /**
     * 查询所有空闲桌台
     */
    @Select("SELECT * FROM dining_tables WHERE status = 1 AND deleted = 0 ORDER BY sort_order ASC, table_id ASC")
    java.util.List<DiningTableNew> selectAvailableTables();

    /**
     * 根据区域查询空闲桌台
     */
    @Select("SELECT * FROM dining_tables WHERE area_id = #{areaId} AND status = 1 AND deleted = 0 ORDER BY sort_order ASC")
    java.util.List<DiningTableNew> selectAvailableByAreaId(@Param("areaId") Long areaId);

    /**
     * 更新桌台状态
     */
    @Update("UPDATE dining_tables SET status = #{status}, current_order_id = #{currentOrderId}, update_time = NOW() WHERE table_id = #{tableId} AND deleted = 0")
    int updateStatus(@Param("tableId") Long tableId, @Param("status") Integer status, @Param("currentOrderId") String currentOrderId);

    /** 统计各状态桌台数量 */
    @Select("SELECT status, COUNT(*) as cnt FROM dining_tables WHERE deleted = 0 GROUP BY status")
    java.util.List<java.util.Map<String, Object>> countByStatus();

    /** 按门店统计各状态桌台数量 */
    @Select("SELECT status, COUNT(*) as cnt FROM dining_tables WHERE store_id = #{storeId} AND deleted = 0 GROUP BY status")
    java.util.List<java.util.Map<String, Object>> countGroupByStatus(@Param("storeId") Long storeId);

    /**
     * 锁定桌台（关联订单）
     */
    @Update("UPDATE dining_tables SET status = 2, current_order_id = #{orderId}, update_time = NOW() WHERE table_id = #{tableId} AND status = 1 AND deleted = 0")
    int lockTable(@Param("tableId") Long tableId, @Param("orderId") String orderId);

    /**
     * 解锁桌台（根据订单ID）
     */
    @Update("UPDATE dining_tables SET status = 1, current_order_id = NULL, update_time = NOW() WHERE current_order_id = #{orderId} AND deleted = 0")
    int unlockTableByOrderId(@Param("orderId") String orderId);

    /**
     * 根据门店ID分页查询桌台，支持状态、类型和关键词过滤
     */
    IPage<DiningTableNew> selectByStoreId(IPage<DiningTableNew> page,
                                          @Param("storeId") Long storeId,
                                          @Param("status") Integer status,
                                          @Param("tableType") Integer tableType,
                                          @Param("keyword") String keyword);
}
