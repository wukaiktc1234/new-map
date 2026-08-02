package com.foodtraceability.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 订单管理集成测试
 * 测试订单管理的完整业务流程
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class OrderManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 测试订单列表查询
     */
    @Test
    public void testOrderListQuery() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/v1/orders")
                .param("page", "1")
                .param("pageSize", "10"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * 测试订单详情查询
     */
    @Test
    public void testOrderDetailQuery() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/v1/order/1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").exists());
    }

    /**
     * 测试订单创建
     */
    @Test
    public void testOrderCreate() throws Exception {
        String orderJson = "{"
                + "\"orderType\":\"DINE_IN\","
                + "\"tableNumber\":\"A01\","
                + "\"customerCount\":2,"
                + "\"items\":["
                + "{\"productId\":1,\"productName\":\"测试菜品\",\"quantity\":2,\"price\":50.00}"
                + "],"
                + "\"totalAmount\":100.00,"
                + "\"status\":\"PENDING\""
                + "}";

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").exists());
    }

    /**
     * 测试订单更新
     */
    @Test
    public void testOrderUpdate() throws Exception {
        String orderJson = "{"
                + "\"id\":1,"
                + "\"status\":\"PROCESSING\","
                + "\"remark\":\"测试更新\""
                + "}";

        mockMvc.perform(MockMvcRequestBuilders.put("/v1/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    /**
     * 测试订单删除
     */
    @Test
    public void testOrderDelete() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/v1/order/999"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    /**
     * 测试订单状态更新
     */
    @Test
    public void testOrderStatusUpdate() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/v1/order/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    /**
     * 测试订单搜索
     */
    @Test
    public void testOrderSearch() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/v1/orders/search")
                .param("keyword", "测试")
                .param("page", "1")
                .param("pageSize", "10"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * 测试订单统计
     */
    @Test
    public void testOrderStatistics() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/v1/orders/statistics")
                .param("startDate", "2026-01-01")
                .param("endDate", "2026-01-31"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").exists());
    }

    /**
     * 测试订单导出
     */
    @Test
    public void testOrderExport() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/orders/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startDate\":\"2026-01-01\",\"endDate\":\"2026-01-31\",\"format\":\"excel\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
