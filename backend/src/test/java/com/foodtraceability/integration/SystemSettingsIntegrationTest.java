package com.foodtraceability.integration;

import com.foodtraceability.common.Result;
import com.foodtraceability.entity.SysSetting;
import com.foodtraceability.service.SysSettingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class SystemSettingsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysSettingService sysSettingService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        System.out.println("========== 测试开始 ==========");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testGetSettingsByGroup() throws Exception {
        System.out.println("测试：根据分组获取设置列表");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/v1/settings/groups/basic")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(0))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        System.out.println("响应内容: " + content);

        Result response = objectMapper.readValue(content, Result.class);
        assertNotNull(response);
        assertEquals(0, response.getCode());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testGetSettingValue() throws Exception {
        System.out.println("测试：获取设置值");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/v1/settings/values/system.name")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(0))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        System.out.println("响应内容: " + content);

        Result response = objectMapper.readValue(content, Result.class);
        assertNotNull(response);
        assertEquals(0, response.getCode());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testGetSettingsBatch() throws Exception {
        System.out.println("测试：批量获取设置值");

        List<String> keys = Arrays.asList("system.name", "system.logo", "system.contact_phone");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/v1/settings/values/batch")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(keys)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(0))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        System.out.println("响应内容: " + content);

        Result response = objectMapper.readValue(content, Result.class);
        assertNotNull(response);
        assertEquals(0, response.getCode());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testSetGlobalSetting() throws Exception {
        System.out.println("测试：设置全局配置");

        String value = "\"测试系统名称\"";

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/v1/settings/global/system.name")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(value))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(0))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        System.out.println("响应内容: " + content);

        Result response = objectMapper.readValue(content, Result.class);
        assertNotNull(response);
        assertEquals(0, response.getCode());

        System.out.println("✓ 全局配置设置成功");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testSetStoreSetting() throws Exception {
        System.out.println("测试：设置门店配置");

        String value = "\"测试门店配置\"";

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/v1/settings/store/system.theme")
                        .header("X-User-Id", "1")
                        .param("storeId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(value))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(0))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        System.out.println("响应内容: " + content);

        Result response = objectMapper.readValue(content, Result.class);
        assertNotNull(response);
        assertEquals(0, response.getCode());

        System.out.println("✓ 门店配置设置成功");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testSetUserSetting() throws Exception {
        System.out.println("测试：设置用户配置");

        String value = "\"测试用户配置\"";

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/v1/settings/user/system.language")
                        .header("X-User-Id", "1")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(value))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(0))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        System.out.println("响应内容: " + content);

        Result response = objectMapper.readValue(content, Result.class);
        assertNotNull(response);
        assertEquals(0, response.getCode());

        System.out.println("✓ 用户配置设置成功");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testInitDefaultSettings() throws Exception {
        System.out.println("测试：初始化默认设置");

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/v1/settings/init")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(0))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        System.out.println("响应内容: " + content);

        Result response = objectMapper.readValue(content, Result.class);
        assertNotNull(response);
        assertEquals(0, response.getCode());

        System.out.println("✓ 默认设置初始化成功");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testSettingsFlow() throws Exception {
        System.out.println("测试：完整的设置流程");

        String testKey = "test.integration.key";
        String testValue = "\"测试值\"";

        System.out.println("步骤1：设置全局配置");
        MvcResult setResult = mockMvc.perform(MockMvcRequestBuilders.post("/v1/settings/global/" + testKey)
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testValue))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(0))
                .andReturn();

        String setContent = setResult.getResponse().getContentAsString();
        Result setResponse = objectMapper.readValue(setContent, Result.class);
        assertEquals(0, setResponse.getCode());
        System.out.println("✓ 步骤1完成：设置成功");

        System.out.println("步骤2：获取设置值");
        MvcResult getResult = mockMvc.perform(MockMvcRequestBuilders.get("/v1/settings/values/" + testKey)
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(0))
                .andReturn();

        String getContent = getResult.getResponse().getContentAsString();
        Result getResponse = objectMapper.readValue(getContent, Result.class);
        assertEquals(0, getResponse.getCode());
        System.out.println("✓ 步骤2完成：获取成功");
    }
}
