package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.TraceChainNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 追溯链节点Mapper接口
 */
@Mapper
public interface TraceChainNodeMapper extends BaseMapper<TraceChainNode> {

    /**
     * 根据追溯码ID查询所有节点（按序号排序）
     * @param traceCodeId 追溯码ID
     * @return 节点列表
     */
    List<TraceChainNode> selectByTraceCodeIdOrderBySequence(@Param("traceCodeId") Long traceCodeId);

    /**
     * 查询追溯链的最大序号
     * @param traceCodeId 追溯码ID
     * @return 最大序号
     */
    Integer selectMaxSequenceByTraceCodeId(@Param("traceCodeId") Long traceCodeId);

    /**
     * 根据节点类型查询
     * @param nodeType 节点类型
     * @return 节点列表
     */
    List<TraceChainNode> selectByNodeType(@Param("nodeType") Integer nodeType);
}
