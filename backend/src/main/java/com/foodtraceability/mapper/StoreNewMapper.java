package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.StoreNew;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 门店Mapper接口（增强版）
 */
@Mapper
public interface StoreNewMapper extends BaseMapper<StoreNew> {

    /**
     * 根据门店编码查询门店
     * @param storeCode 门店编码
     * @return 门店信息
     */
    @Select("SELECT * FROM stores_new WHERE store_code = #{storeCode} AND deleted = 0")
    StoreNew selectByStoreCode(@Param("storeCode") String storeCode);

    /**
     * 查询所有营业中的门店
     * @return 营业中的门店列表
     */
    @Select("SELECT * FROM stores_new WHERE status = 1 AND deleted = 0 ORDER BY store_name")
    List<StoreNew> selectActiveStores();

    /**
     * 按门店类型统计数量
     * @return 统计结果
     */
    @Select("SELECT store_type, COUNT(*) as count FROM stores_new WHERE deleted = 0 GROUP BY store_type")
    List<java.util.Map<String, Object>> countByType();

    /**
     * 按门店状态统计数量
     * @return 统计结果
     */
    @Select("SELECT status, COUNT(*) as count FROM stores_new WHERE deleted = 0 GROUP BY status")
    List<java.util.Map<String, Object>> countByStatus();

    /**
     * 检查门店编码是否已存在
     * @param storeCode 门店编码
     * @param excludeId 排除的门店ID（创建时传 null，更新时传当前门店ID）
     * @return 存在的数量
     *
     * 实现说明：使用 MyBatis 动态 SQL <if> 标签而非 `#{excludeId} IS NULL`，
     * 避免 PostgreSQL JDBC 驱动在 excludeId 为 null 时无法推断参数数据类型
     * （报错：无法确定参数 $N 的数据类型）。
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM stores_new WHERE store_code = #{storeCode} AND deleted = 0" +
            "<if test='excludeId != null'> AND store_id != #{excludeId}</if>" +
            "</script>")
    int countByCode(@Param("storeCode") String storeCode, @Param("excludeId") Long excludeId);
}
