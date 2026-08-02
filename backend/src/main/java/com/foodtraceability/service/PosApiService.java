package com.foodtraceability.service;

import com.foodtraceability.dto.PosCategoryDTO;
import com.foodtraceability.dto.PosComboDTO;
import com.foodtraceability.dto.PosDishDTO;
import com.foodtraceability.dto.PosMenuDTO;

import java.util.List;

/**
 * 收银终端API服务接口
 */
public interface PosApiService {

    /**
     * 获取菜品分类列表
     * @return 分类列表
     */
    List<PosCategoryDTO> getCategories();

    /**
     * 获取所有可用菜品
     * @return 菜品列表
     */
    List<PosDishDTO> getAllDishes();

    /**
     * 按分类获取菜品
     * @param categoryId 分类ID
     * @return 菜品列表
     */
    List<PosDishDTO> getDishesByCategory(String categoryId);

    /**
     * 获取所有可用套餐
     * @return 套餐列表
     */
    List<PosComboDTO> getAllCombos();

    /**
     * 获取完整菜单（菜品+套餐）
     * @return 完整菜单
     */
    PosMenuDTO getFullMenu();
}
