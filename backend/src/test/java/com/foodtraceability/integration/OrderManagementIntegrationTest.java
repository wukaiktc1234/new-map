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
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 订单管理集成测试
 * 测试订单管理的完整业务流程
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
// 【隔离性修正 2026-10-27】类级自带鉴权：不依赖库里真实用户/角色（清库后必飘）。
// authorities 覆盖被测端点用到的 order:* 权限；仍保留真实 @PreAuthorize 校验路径（不是关安全）。
@WithMockUser(username = "it-order",
        authorities = {"order:view", "order:add", "order:manage", "order:update", "order:delete"})
public class OrderManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /** 与 OrderCreateDTO 对齐的最小合法报文（orderType 取 Integer；items 字段名按 DTO） */
    private static String createOrderJson() {
        return "{"
                + "\"orderType\":1,"
                + "\"orderSource\":1,"
                + "\"storeId\":1,"
                + "\"diningPeopleCount\":2,"
                + "\"remark\":\"IT-ORDER-FIXTURE\","
                + "\"items\":[{"
                + "\"productType\":1,"
                + "\"productId\":2,"
                + "\"productName\":\"IT 测试菜品\","
                + "\"unitPrice\":5000,"
                + "\"quantity\":2"
                + "}]"
                + "}";
    }

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
        // 【路径修正 2026-10-27】真实映射是 /v1/orders/{orderId}（OrderNewController:91）。
        // 同时去掉「写死 orderId=1」的数据依赖：先创建一单，再用返回的真实 ID 查详情。
        // 断言强度不变（200 + code=0 + data 存在）；不再依赖库里恰好存在 1 号订单。
        String created = mockMvc.perform(MockMvcRequestBuilders.post("/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderJson()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        // 兼容 orderId 可能以字符串/数字返回
        com.fasterxml.jackson.databind.JsonNode node =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(created).path("data");
        String orderId = node.path("orderId").asText(node.path("id").asText(""));
        assertFalse(orderId.isEmpty(), "创建订单应返回 orderId，响应=" + created);

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/orders/" + orderId))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").exists());
    }

    /**
     * 测试订单创建
     */
    @Test
    public void testOrderCreate() throws Exception {
        // 【payload 修正 2026-10-27】原报文与 OrderCreateDTO 完全不匹配：
        //   orderType 是 Integer 1..4（原传字符串 "DINE_IN" → HttpMessageNotReadableException → 500）；
        //   storeId 必填；items[].productType/productId/productName/unitPrice(分)/quantity 均必填。
        // 断言强度不变（200 + code=0 + data 存在），只是把「报文写错」与「功能坏」区分开。
        String orderJson = createOrderJson();

        // 【路径修正 2026-10-27】创建订单真实映射是 POST /v1/orders（OrderNewController:37），不是 /v1/order
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/orders")
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
    @org.junit.jupiter.api.Disabled("产品未实现该端点（当前只有 /{id}/confirm、/{id}/complete）；用例保留以便发现能力缺口，不删除、不假装通过（2026-10-27 Owner 裁定）")
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
    @org.junit.jupiter.api.Disabled("产品未实现该端点（当前只有 /{id}/confirm、/{id}/complete）；用例保留以便发现能力缺口，不删除、不假装通过（2026-10-27 Owner 裁定）")
    public void testOrderDelete() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/v1/order/999"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    /**
     * 测试订单状态更新
     */
    @Test
    @org.junit.jupiter.api.Disabled("产品未实现该端点（当前只有 /{id}/confirm、/{id}/complete）；用例保留以便发现能力缺口，不删除、不假装通过（2026-10-27 Owner 裁定）")
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
    @org.junit.jupiter.api.Disabled("bug-⑥（P2）：GET /v1/orders/search 被 /v1/orders/{orderId} 吞掉 → "
            + "getOrderDetail('search') 抛 RuntimeException(订单不存在: search) → 全局处理器兜成 500；"
            + "正确应为 404/400。缺陷未修前本用例保持禁用，不删、不假装通过（2026-10-27 Owner 裁定）")
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
