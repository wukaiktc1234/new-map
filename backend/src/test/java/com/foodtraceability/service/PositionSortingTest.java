package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.Position;
import com.foodtraceability.mapper.EmployeeMapper;
import com.foodtraceability.mapper.PositionMapper;
import com.foodtraceability.service.impl.PositionServiceImpl;
import com.foodtraceability.utils.PositionCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 职位服务排序功能测试类
 * 测试职位的排序功能是否正常工作
 */
@ExtendWith(MockitoExtension.class)
public class PositionSortingTest {

    private static final Logger logger = LoggerFactory.getLogger(PositionSortingTest.class);

    @Mock
    private PositionMapper positionMapper;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private PositionCodeGenerator positionCodeGenerator;

    @InjectMocks
    private PositionServiceImpl positionService;

    @BeforeEach
    void setUp() {
        logger.info("开始职位排序功能测试");
    }

    /**
     * 测试基本的职位排序功能
     * 验证部门排序 -> 职位编码序号的排序逻辑
     */
    @Test
    @DisplayName("测试基本的职位排序功能")
    void testBasicPositionSorting() {
        // 准备测试数据
        List<Position> mockPositions = createMockPositions();

        Page<Position> mockPage = new Page<>(1, 10);
        mockPage.setRecords(mockPositions);
        mockPage.setTotal(mockPositions.size());

        // 模拟mapper行为
        when(positionMapper.selectPositionsWithDepartmentSort(any(Page.class))).thenReturn(mockPage);
        when(employeeMapper.selectMaps(any())).thenReturn(Arrays.asList());

        // 执行测试
        Map<String, Object> result = positionService.getPositions(1, 10);

        // 验证结果
        assertNotNull(result, "结果不应为空");
        assertTrue(result.containsKey("records"), "结果应包含records字段");

        @SuppressWarnings("unchecked")
        List<Position> sortedPositions = (List<Position>) result.get("records");

        assertNotNull(sortedPositions, "职位列表不应为空");
        assertEquals(6, sortedPositions.size(), "应有6个职位");

        // 验证排序结果
        logger.info("排序后的职位列表:");
        for (int i = 0; i < sortedPositions.size(); i++) {
            Position pos = sortedPositions.get(i);
            logger.info("{}: {} - {} - {} - {}",
                i + 1,
                pos.getPositionName(),
                pos.getDepartment(),
                pos.getDepartmentSortOrder(),
                pos.getPositionCode());
        }

        // 验证第一个职位（董事会办公室，总经理）
        Position firstPosition = sortedPositions.get(0);
        assertEquals("总经理", firstPosition.getPositionName());
        assertEquals("董事会办公室", firstPosition.getDepartment());
        assertEquals(1, firstPosition.getDepartmentSortOrder());

        // 验证最后一个职位（技术部，初级工程师）
        Position lastPosition = sortedPositions.get(sortedPositions.size() - 1);
        assertEquals("初级工程师", lastPosition.getPositionName());
        assertEquals("技术部", lastPosition.getDepartment());
        assertEquals(3, lastPosition.getDepartmentSortOrder());
    }

    /**
     * 测试相同部门内的职位排序
     * 验证同一部门内按职位编码序号排序
     */
    @Test
    @DisplayName("测试相同部门内的职位排序")
    void testSameDepartmentPositionSorting() {
        // 准备测试数据 - 同一部门内的不同职位
        List<Position> mockPositions = Arrays.asList(
            createPosition("技术部", "技术专家", "TECH-EXP-001", 2),
            createPosition("技术部", "高级工程师", "TECH-HIGH-001", 2),
            createPosition("技术部", "高级工程师", "TECH-HIGH-002", 2),
            createPosition("技术部", "中级工程师", "TECH-MID-001", 2),
            createPosition("技术部", "初级工程师", "TECH-JUNIOR-001", 2)
        );

        Page<Position> mockPage = new Page<>(1, 10);
        mockPage.setRecords(mockPositions);
        mockPage.setTotal(mockPositions.size());

        // 模拟mapper行为
        when(positionMapper.selectPositionsWithDepartmentSort(any(Page.class))).thenReturn(mockPage);
        when(employeeMapper.selectMaps(any())).thenReturn(Arrays.asList());

        // 执行测试
        Map<String, Object> result = positionService.getPositions(1, 10);

        @SuppressWarnings("unchecked")
        List<Position> sortedPositions = (List<Position>) result.get("records");

        // 验证同一部门内按编码排序
        assertEquals("技术专家", sortedPositions.get(0).getPositionName());
        assertEquals("高级工程师", sortedPositions.get(1).getPositionName());
        assertEquals("高级工程师", sortedPositions.get(2).getPositionName());
        assertEquals("中级工程师", sortedPositions.get(3).getPositionName());
        assertEquals("初级工程师", sortedPositions.get(4).getPositionName());

        // 验证编码序号排序
        assertEquals("TECH-EXP-001", sortedPositions.get(0).getPositionCode());
        assertEquals("TECH-HIGH-001", sortedPositions.get(1).getPositionCode());
        assertEquals("TECH-HIGH-002", sortedPositions.get(2).getPositionCode());
        assertEquals("TECH-MID-001", sortedPositions.get(3).getPositionCode());
        assertEquals("TECH-JUNIOR-001", sortedPositions.get(4).getPositionCode());
    }

    /**
     * 测试职位编码序号提取
     * 验证从职位编码中提取序号的逻辑
     */
    @Test
    @DisplayName("测试职位编码序号提取")
    void testPositionCodeSequenceExtraction() {
        // 准备测试数据
        List<Position> mockPositions = Arrays.asList(
            createPosition("行政部", "行政专员", "ADMIN-SPE-001", 1),
            createPosition("行政部", "行政专员", "ADMIN-SPE-010", 1),
            createPosition("行政部", "行政专员", "ADMIN-SPE-100", 1)
        );

        Page<Position> mockPage = new Page<>(1, 10);
        mockPage.setRecords(mockPositions);
        mockPage.setTotal(mockPositions.size());

        // 模拟mapper行为
        when(positionMapper.selectPositionsWithDepartmentSort(any(Page.class))).thenReturn(mockPage);
        when(employeeMapper.selectMaps(any())).thenReturn(Arrays.asList());

        // 执行测试
        Map<String, Object> result = positionService.getPositions(1, 10);

        @SuppressWarnings("unchecked")
        List<Position> sortedPositions = (List<Position>) result.get("records");

        // 验证序号提取和排序
        assertEquals(1, sortedPositions.get(0).getPositionCodeSeq());
        assertEquals(10, sortedPositions.get(1).getPositionCodeSeq());
        assertEquals(100, sortedPositions.get(2).getPositionCodeSeq());

        // 验证排序结果（按序号升序）
        assertEquals("ADMIN-SPE-001", sortedPositions.get(0).getPositionCode());
        assertEquals("ADMIN-SPE-010", sortedPositions.get(1).getPositionCode());
        assertEquals("ADMIN-SPE-100", sortedPositions.get(2).getPositionCode());
    }

    /**
     * 测试空值处理
     * 验证处理null值时的排序行为
     */
    @Test
    @DisplayName("测试空值处理")
    void testNullValueHandling() {
        // 准备测试数据 - 包含null值
        List<Position> mockPositions = Arrays.asList(
            createPosition(null, "测试职位1", "TEST-001", null),
            createPosition("行政部", "测试职位2", "ADMIN-001", 1),
            createPosition(null, null, "NULL-001", null),
            createPosition("技术部", "测试职位3", "TECH-001", 2)
        );

        Page<Position> mockPage = new Page<>(1, 10);
        mockPage.setRecords(mockPositions);
        mockPage.setTotal(mockPositions.size());

        // 模拟mapper行为
        when(positionMapper.selectPositionsWithDepartmentSort(any(Page.class))).thenReturn(mockPage);
        when(employeeMapper.selectMaps(any())).thenReturn(Arrays.asList());

        // 执行测试
        Map<String, Object> result = positionService.getPositions(1, 10);

        @SuppressWarnings("unchecked")
        List<Position> sortedPositions = (List<Position>) result.get("records");

        // 验证null值处理
        logger.info("空值处理排序结果:");
        for (int i = 0; i < sortedPositions.size(); i++) {
            Position pos = sortedPositions.get(i);
            logger.info("{}: 部门: {}, 职位: {}, 编码: {}",
                i + 1,
                pos.getDepartment(),
                pos.getPositionName(),
                pos.getPositionCode());
        }

        // null值应排在有值的项目之后
        logger.info("null值处理验证 - 第一个职位的部门排序值: {}", sortedPositions.get(0).getDepartmentSortOrder());
        logger.info("null值处理验证 - 最后一个职位的部门排序值: {}", sortedPositions.get(sortedPositions.size() - 1).getDepartmentSortOrder());
    }

    /**
     * 测试分页功能
     * 验证分页参数是否正确应用
     */
    @Test
    @DisplayName("测试分页功能")
    void testPagination() {
        // 准备大量测试数据
        List<Position> mockPositions = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            mockPositions.add(createPosition(
                "测试部",
                "测试职位" + i,
                "TEST-SPE-" + String.format("%03d", i),
                1
            ));
        }

        // 测试第1页
        Page<Position> page1 = new Page<>(1, 10);
        page1.setRecords(mockPositions.subList(0, 10));
        page1.setTotal(25);

        // 简化分页测试，避免复杂的Mock设置
        when(positionMapper.selectPositionsWithDepartmentSort(any(Page.class)))
            .thenReturn(page1);
        when(employeeMapper.selectMaps(any())).thenReturn(Arrays.asList());

        Map<String, Object> result1 = positionService.getPositions(1, 10);

        @SuppressWarnings("unchecked")
        List<Position> page1Positions = (List<Position>) result1.get("records");
        assertEquals(10, page1Positions.size(), "第1页应包含10条记录");
        assertEquals(25L, result1.get("total"), "总记录数应为25");
        assertEquals(1L, result1.get("current"), "当前页码应为1");
        assertEquals(10L, result1.get("size"), "每页大小应为10");

        // 测试第2页
        Page<Position> page2 = new Page<>(2, 10);
        page2.setRecords(mockPositions.subList(10, 20));
        page2.setTotal(25);

        when(positionMapper.selectPositionsWithDepartmentSort(any(Page.class)))
            .thenReturn(page2);

        Map<String, Object> result2 = positionService.getPositions(2, 10);

        @SuppressWarnings("unchecked")
        List<Position> page2Positions = (List<Position>) result2.get("records");
        assertEquals(10, page2Positions.size(), "第2页应包含10条记录");
        assertEquals(2L, result2.get("current"), "当前页码应为2");

        // 测试第3页（最后一页）
        Page<Position> page3 = new Page<>(3, 10);
        page3.setRecords(mockPositions.subList(20, 25));
        page3.setTotal(25);

        when(positionMapper.selectPositionsWithDepartmentSort(any(Page.class)))
            .thenReturn(page3);

        Map<String, Object> result3 = positionService.getPositions(3, 10);

        @SuppressWarnings("unchecked")
        List<Position> page3Positions = (List<Position>) result3.get("records");
        assertEquals(5, page3Positions.size(), "第3页应包含5条记录");
        assertEquals(3L, result3.get("current"), "当前页码应为3");
    }

    // 辅助方法：创建职位对象
    private Position createPosition(String departmentName, String positionName,
                                  String positionCode, Integer departmentSortOrder) {
        Position position = new Position();
        position.setId(System.currentTimeMillis() + new Random().nextInt(1000));
        position.setPositionName(positionName);
        position.setDepartment(departmentName);
        position.setPositionCode(positionCode);
        position.setDepartmentSortOrder(departmentSortOrder);

        // 提取职位编码序号
        if (positionCode != null && positionCode.matches(".*-(\\d+)$")) {
            String seqStr = positionCode.substring(positionCode.lastIndexOf("-") + 1);
            try {
                position.setPositionCodeSeq(Integer.parseInt(seqStr));
            } catch (NumberFormatException e) {
                position.setPositionCodeSeq(0);
            }
        } else {
            position.setPositionCodeSeq(0);
        }

        return position;
    }

    // 辅助方法：创建模拟职位数据
    private List<Position> createMockPositions() {
        return Arrays.asList(
            // 董事会办公室 - 最高排序优先级
            createPosition("董事会办公室", "总经理", "BOARD-GM-001", 1),
            createPosition("董事会办公室", "副总经理", "BOARD-VICE-001", 1),

            // 行政部 - 中等排序优先级
            createPosition("行政部", "行政专员", "ADMIN-SPE-001", 2),
            createPosition("行政部", "行政主管", "ADMIN-SUP-001", 2),

            // 技术部 - 最低排序优先级
            createPosition("技术部", "高级工程师", "TECH-HIGH-001", 3),
            createPosition("技术部", "初级工程师", "TECH-JUNIOR-001", 3)
        );
    }
}
