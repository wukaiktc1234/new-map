package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.SelfPurchase;
import com.foodtraceability.entity.SelfPurchaseItem;

import java.util.List;

/**
 * 自采服务接口
 * 用于定义自采管理的业务逻辑方法
 */
public interface SelfPurchaseService extends IService<SelfPurchase> {
    
    /**
     * 创建自采记录
     * @param selfPurchase 自采记录信息
     * @param items 自采商品明细列表
     * @return 创建结果
     */
    boolean createSelfPurchase(SelfPurchase selfPurchase, List<SelfPurchaseItem> items);
    
    /**
     * 自采入库
     * @param selfPurchaseId 自采记录ID
     * @return 入库结果
     */
    boolean inboundSelfPurchase(String selfPurchaseId);
    
    /**
     * 自采报销
     * @param selfPurchaseId 自采记录ID
     * @return 报销结果
     */
    boolean reimburseSelfPurchase(String selfPurchaseId);
    
    /**
     * 获取自采记录详情
     * @param selfPurchaseId 自采记录ID
     * @return 自采记录信息
     */
    SelfPurchase getSelfPurchaseById(String selfPurchaseId);
    
    /**
     * 获取自采商品明细
     * @param selfPurchaseId 自采记录ID
     * @return 自采商品明细列表
     */
    List<SelfPurchaseItem> getSelfPurchaseItems(String selfPurchaseId);
    
    /**
     * 取消自采记录
     * @param selfPurchaseId 自采记录ID
     * @return 取消结果
     */
    boolean cancelSelfPurchase(String selfPurchaseId);
}
