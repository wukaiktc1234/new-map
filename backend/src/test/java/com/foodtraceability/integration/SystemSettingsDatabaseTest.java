package com.foodtraceability.integration;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.foodtraceability.entity.SysSetting;
import com.foodtraceability.mapper.SysSettingMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SystemSettingsDatabaseTest {

    @Autowired
    private SysSettingMapper sysSettingMapper;

    private SysSetting testSetting;

    @BeforeEach
    public void setUp() {
        System.out.println("========== 数据库测试开始 ==========");
        
        testSetting = new SysSetting();
        testSetting.setSettingGroup("test");
        testSetting.setSettingKey("test.key");
        testSetting.setScopeType("global");
        testSetting.setScopeId(null);
        testSetting.setSettingValue("test_value");
        testSetting.setValueType("string");
        testSetting.setSettingName("测试设置");
        testSetting.setSettingDesc("这是一个测试设置");
        testSetting.setOptions(null);
        testSetting.setValidationRules(null);
        testSetting.setSortOrder(1);
        testSetting.setRequiredPermission(null);
        testSetting.setIsEnabled(1);
        testSetting.setIsSystem(0);
        testSetting.setCreatedBy("test_user");
        testSetting.setUpdatedBy("test_user");
        testSetting.setCreateTime(LocalDateTime.now());
        testSetting.setUpdateTime(LocalDateTime.now());
    }

    @Test
    public void testInsertSetting() {
        System.out.println("测试：插入系统设置");

        int result = sysSettingMapper.insert(testSetting);
        System.out.println("插入结果: " + result);

        assertTrue(result > 0, "插入应该成功");
        assertNotNull(testSetting.getId(), "ID应该被自动生成");
        System.out.println("✓ 插入成功，ID: " + testSetting.getId());
    }

    @Test
    public void testSelectById() {
        System.out.println("测试：根据ID查询系统设置");

        sysSettingMapper.insert(testSetting);
        Long id = testSetting.getId();

        SysSetting found = sysSettingMapper.selectById(id);
        System.out.println("查询结果: " + found);

        assertNotNull(found, "应该找到设置");
        assertEquals(testSetting.getSettingKey(), found.getSettingKey(), "设置键应该匹配");
        assertEquals(testSetting.getSettingValue(), found.getSettingValue(), "设置值应该匹配");
        System.out.println("✓ 查询成功");
    }

    @Test
    public void testSelectBySettingKey() {
        System.out.println("测试：根据设置键查询系统设置");

        sysSettingMapper.insert(testSetting);

        QueryWrapper<SysSetting> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("setting_key", testSetting.getSettingKey());
        SysSetting found = sysSettingMapper.selectOne(queryWrapper);

        System.out.println("查询结果: " + found);

        assertNotNull(found, "应该找到设置");
        assertEquals(testSetting.getSettingKey(), found.getSettingKey(), "设置键应该匹配");
        System.out.println("✓ 查询成功");
    }

    @Test
    public void testSelectByGroup() {
        System.out.println("测试：根据分组查询系统设置");

        sysSettingMapper.insert(testSetting);

        QueryWrapper<SysSetting> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("setting_group", testSetting.getSettingGroup());
        List<SysSetting> settings = sysSettingMapper.selectList(queryWrapper);

        System.out.println("查询结果数量: " + settings.size());

        assertNotNull(settings, "应该找到设置列表");
        assertFalse(settings.isEmpty(), "列表不应该为空");
        assertTrue(settings.stream().anyMatch(s -> s.getSettingKey().equals(testSetting.getSettingKey())), "应该包含测试设置");
        System.out.println("✓ 查询成功");
    }

    @Test
    public void testSelectByScope() {
        System.out.println("测试：根据作用域查询系统设置");

        testSetting.setScopeType("store");
        testSetting.setScopeId("1");
        sysSettingMapper.insert(testSetting);

        QueryWrapper<SysSetting> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("scope_type", "store");
        queryWrapper.eq("scope_id", "1");
        List<SysSetting> settings = sysSettingMapper.selectList(queryWrapper);

        System.out.println("查询结果数量: " + settings.size());

        assertNotNull(settings, "应该找到设置列表");
        assertFalse(settings.isEmpty(), "列表不应该为空");
        System.out.println("✓ 查询成功");
    }

    @Test
    public void testUpdateSetting() {
        System.out.println("测试：更新系统设置");

        sysSettingMapper.insert(testSetting);
        Long id = testSetting.getId();

        SysSetting update = new SysSetting();
        update.setId(id);
        update.setSettingValue("updated_value");
        update.setUpdatedBy("update_user");
        update.setUpdateTime(LocalDateTime.now());

        int result = sysSettingMapper.updateById(update);
        System.out.println("更新结果: " + result);

        assertTrue(result > 0, "更新应该成功");

        SysSetting updated = sysSettingMapper.selectById(id);
        assertEquals("updated_value", updated.getSettingValue(), "值应该被更新");
        System.out.println("✓ 更新成功");
    }

    @Test
    public void testDeleteSetting() {
        System.out.println("测试：删除系统设置");

        sysSettingMapper.insert(testSetting);
        Long id = testSetting.getId();

        int result = sysSettingMapper.deleteById(id);
        System.out.println("删除结果: " + result);

        assertTrue(result > 0, "删除应该成功");

        SysSetting deleted = sysSettingMapper.selectById(id);
        assertNull(deleted, "应该被删除");
        System.out.println("✓ 删除成功");
    }

    @Test
    public void testSelectEnabledSettings() {
        System.out.println("测试：查询启用的系统设置");

        sysSettingMapper.insert(testSetting);

        QueryWrapper<SysSetting> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_enabled", 1);
        List<SysSetting> settings = sysSettingMapper.selectList(queryWrapper);

        System.out.println("查询结果数量: " + settings.size());

        assertNotNull(settings, "应该找到设置列表");
        assertFalse(settings.isEmpty(), "列表不应该为空");
        assertTrue(settings.stream().allMatch(s -> s.getIsEnabled() == 1), "所有设置都应该启用");
        System.out.println("✓ 查询成功");
    }

    @Test
    public void testSelectSystemSettings() {
        System.out.println("测试：查询系统配置");

        testSetting.setIsSystem(1);
        sysSettingMapper.insert(testSetting);

        QueryWrapper<SysSetting> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_system", 1);
        List<SysSetting> settings = sysSettingMapper.selectList(queryWrapper);

        System.out.println("查询结果数量: " + settings.size());

        assertNotNull(settings, "应该找到设置列表");
        assertFalse(settings.isEmpty(), "列表不应该为空");
        assertTrue(settings.stream().allMatch(s -> s.getIsSystem() == 1), "所有设置都应该是系统配置");
        System.out.println("✓ 查询成功");
    }

    @Test
    public void testSelectByMultipleConditions() {
        System.out.println("测试：根据多个条件查询系统设置");

        sysSettingMapper.insert(testSetting);

        QueryWrapper<SysSetting> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("setting_group", testSetting.getSettingGroup());
        queryWrapper.eq("scope_type", testSetting.getScopeType());
        queryWrapper.eq("is_enabled", 1);
        queryWrapper.eq("is_system", 0);
        List<SysSetting> settings = sysSettingMapper.selectList(queryWrapper);

        System.out.println("查询结果数量: " + settings.size());

        assertNotNull(settings, "应该找到设置列表");
        assertFalse(settings.isEmpty(), "列表不应该为空");
        assertTrue(settings.stream().anyMatch(s -> s.getSettingKey().equals(testSetting.getSettingKey())), "应该包含测试设置");
        System.out.println("✓ 查询成功");
    }

    @Test
    public void testBatchInsertSettings() {
        System.out.println("测试：批量插入系统设置");

        SysSetting setting1 = new SysSetting();
        setting1.setSettingGroup("test");
        setting1.setSettingKey("test.key1");
        setting1.setScopeType("global");
        setting1.setSettingValue("value1");
        setting1.setValueType("string");
        setting1.setSettingName("测试设置1");
        setting1.setIsEnabled(1);
        setting1.setIsSystem(0);
        setting1.setCreatedBy("test_user");
        setting1.setUpdatedBy("test_user");
        setting1.setCreateTime(LocalDateTime.now());
        setting1.setUpdateTime(LocalDateTime.now());

        SysSetting setting2 = new SysSetting();
        setting2.setSettingGroup("test");
        setting2.setSettingKey("test.key2");
        setting2.setScopeType("global");
        setting2.setSettingValue("value2");
        setting2.setValueType("string");
        setting2.setSettingName("测试设置2");
        setting2.setIsEnabled(1);
        setting2.setIsSystem(0);
        setting2.setCreatedBy("test_user");
        setting2.setUpdatedBy("test_user");
        setting2.setCreateTime(LocalDateTime.now());
        setting2.setUpdateTime(LocalDateTime.now());

        int result1 = sysSettingMapper.insert(setting1);
        int result2 = sysSettingMapper.insert(setting2);

        System.out.println("插入结果1: " + result1);
        System.out.println("插入结果2: " + result2);

        assertTrue(result1 > 0, "插入1应该成功");
        assertTrue(result2 > 0, "插入2应该成功");
        System.out.println("✓ 批量插入成功");
    }

    @Test
    public void testSettingValueTypes() {
        System.out.println("测试：不同值类型的系统设置");

        String[] valueTypes = {"string", "number", "boolean", "select", "multiselect", "json", "array", "color", "image", "file"};

        for (String valueType : valueTypes) {
            SysSetting setting = new SysSetting();
            setting.setSettingGroup("test");
            setting.setSettingKey("test." + valueType);
            setting.setScopeType("global");
            setting.setSettingValue("test_value");
            setting.setValueType(valueType);
            setting.setSettingName("测试" + valueType);
            setting.setIsEnabled(1);
            setting.setIsSystem(0);
            setting.setCreatedBy("test_user");
            setting.setUpdatedBy("test_user");
            setting.setCreateTime(LocalDateTime.now());
            setting.setUpdateTime(LocalDateTime.now());

            int result = sysSettingMapper.insert(setting);
            assertTrue(result > 0, "插入" + valueType + "应该成功");
            System.out.println("✓ 插入" + valueType + "成功");
        }

        System.out.println("✓ 所有值类型测试通过");
    }

    @Test
    public void testScopeTypes() {
        System.out.println("测试：不同作用域的系统设置");

        String[] scopeTypes = {"global", "store", "user"};

        for (String scopeType : scopeTypes) {
            SysSetting setting = new SysSetting();
            setting.setSettingGroup("test");
            setting.setSettingKey("test." + scopeType);
            setting.setScopeType(scopeType);
            setting.setScopeId(scopeType.equals("global") ? null : "1");
            setting.setSettingValue("test_value");
            setting.setValueType("string");
            setting.setSettingName("测试" + scopeType);
            setting.setIsEnabled(1);
            setting.setIsSystem(0);
            setting.setCreatedBy("test_user");
            setting.setUpdatedBy("test_user");
            setting.setCreateTime(LocalDateTime.now());
            setting.setUpdateTime(LocalDateTime.now());

            int result = sysSettingMapper.insert(setting);
            assertTrue(result > 0, "插入" + scopeType + "应该成功");
            System.out.println("✓ 插入" + scopeType + "作用域成功");
        }

        System.out.println("✓ 所有作用域测试通过");
    }
}
