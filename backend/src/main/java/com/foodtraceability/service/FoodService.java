package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.product.FoodCreateDTO;
import com.foodtraceability.dto.product.FoodImportResultDTO;
import com.foodtraceability.dto.product.FoodQueryDTO;
import com.foodtraceability.dto.product.FoodUpdateDTO;
import com.foodtraceability.dto.product.FoodVO;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * 菜品服务接口
 * 管理菜品/食品的完整业务逻辑
 */
public interface FoodService {

    /**
     * 创建菜品
     * @param dto 创建请求DTO
     * @return 菜品视图对象
     */
    FoodVO create(FoodCreateDTO dto);

    /**
     * 更新菜品信息
     * @param foodId 菜品ID
     * @param dto 更新请求DTO
     * @return 菜品视图对象
     */
    FoodVO update(Long foodId, FoodUpdateDTO dto);

    /**
     * 根据ID获取菜品详情
     * @param foodId 菜品ID
     * @return 菜品视图对象
     */
    FoodVO getById(Long foodId);

    /**
     * 分页查询菜品列表
     * @param queryDto 查询条件
     * @return 分页结果
     */
    Page<FoodVO> queryPage(FoodQueryDTO queryDto);

    /**
     * 查询在售菜品列表（用于菜单展示）
     * @return 在售菜品列表
     */
    List<FoodVO> listOnSale();

    /**
     * 查询推荐菜品
     * @param limit 数量限制
     * @return 推荐菜品列表
     */
    List<FoodVO> listRecommend(int limit);

    /**
     * 根据分类ID查询菜品列表
     * @param categoryId 分类ID
     * @return 菜品列表
     */
    List<FoodVO> listByCategory(Long categoryId);

    /**
     * 更新菜品状态（上架/下架/售罄）
     * @param foodId 菜品ID
     * @param status 目标状态 1在售 2停售 3售罄
     */
    void updateStatus(Long foodId, Integer status);

    /**
     * 批量更新菜品状态
     * @param foodIds 菜品ID列表
     * @param status 目标状态
     */
    void batchUpdateStatus(List<Long> foodIds, Integer status);

    /**
     * 逻辑删除菜品
     * @param foodId 菜品ID
     */
    void delete(Long foodId);

    /**
     * 批量逻辑删除菜品
     * @param foodIds 菜品ID列表
     */
    void batchDelete(List<Long> foodIds);

    /**
     * 批量导入菜品（Excel）
     * @param file Excel文件
     * @return 导入结果
     */
    FoodImportResultDTO importFoods(MultipartFile file);

    /**
     * 导出菜品数据（Excel）
     * @param queryDto 查询条件
     * @param response HTTP响应
     */
    void exportFoods(FoodQueryDTO queryDto, HttpServletResponse response);

    /**
     * 下载导入模板（Excel）
     * @param response HTTP响应
     */
    void downloadImportTemplate(HttpServletResponse response);
}
