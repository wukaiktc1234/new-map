package com.foodtraceability.mapstruct;

import com.foodtraceability.dto.ProductRequest;
import com.foodtraceability.dto.ProductResponse;
import com.foodtraceability.entity.Food;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * 产品映射器
 * 用于ProductRequest、ProductResponse和Food实体之间的转换
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductMapper {

    /**
     * 将Food实体转换为ProductResponseDTO
     * @param food Food实体
     * @return ProductResponseDTO
     */
    @Mapping(target = "foodCode", source = "foodCode")
    ProductResponse toProductResponse(Food food);

    /**
     * 将ProductRequestDTO转换为Food实体
     * @param productRequest ProductRequestDTO
     * @return Food实体
     */
    @Mapping(target = "foodCode", ignore = true)
    @Mapping(target = "expirationDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Food toFood(ProductRequest productRequest);

    /**
     * 将Food实体列表转换为ProductResponseDTO列表
     * @param foods Food实体列表
     * @return ProductResponseDTO列表
     */
    List<ProductResponse> toProductResponseList(List<Food> foods);
}
