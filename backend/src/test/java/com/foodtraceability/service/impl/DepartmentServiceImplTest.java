package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodtraceability.entity.Department;
import com.foodtraceability.mapper.DepartmentMapper;
import com.foodtraceability.mapper.EmployeeMapper;
import com.foodtraceability.mapper.PositionMapper;
import com.foodtraceability.service.EmployeeService;
import com.foodtraceability.service.OrganizationChangeLogService;
import com.foodtraceability.service.PositionService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DepartmentServiceImpl} 纯 Mockito 单元测试（不连数据库）。
 *
 * <p>覆盖 R6 修复的两处「顶级口径」缺陷：</p>
 * <ul>
 *   <li>create / update 时 parentId == null 必须归一为 0，且 level 按最终父级重算；</li>
 *   <li>getChildrenDepartments(0) 的查询条件必须同时覆盖 parent_id = 0 与 parent_id IS NULL，
 *       与 buildDepartmentTree 的「顶级」口径一致。</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DepartmentServiceImplTest {

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private PositionMapper positionMapper;

    @Mock
    private OrganizationChangeLogService organizationChangeLogService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PositionService positionService;

    @Mock
    private EmployeeService employeeService;

    private DepartmentServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, Department.class);
    }

    @BeforeEach
    void setUp() {
        service = new DepartmentServiceImpl(
                departmentMapper,
                employeeMapper,
                positionMapper,
                organizationChangeLogService,
                new ObjectMapper(),
                eventPublisher,
                positionService,
                employeeService);

        // 编码唯一性检查：默认无冲突
        when(departmentMapper.countByDeptCode(anyString())).thenReturn(0);
        // insert 回填自增主键，避免后续日志/事件用到 null id
        when(departmentMapper.insert(any(Department.class))).thenAnswer(invocation -> {
            Department entity = invocation.getArgument(0);
            if (entity.getDepartmentId() == null) {
                entity.setDepartmentId(9001L);
            }
            return 1;
        });
        when(departmentMapper.updateById(any(Department.class))).thenReturn(1);
        when(departmentMapper.selectList(any())).thenReturn(new ArrayList<>());
    }

    private static Department department(Long id, Long parentId, Integer level) {
        Department d = new Department();
        d.setDepartmentId(id);
        d.setDepartmentName("测试部门-" + id);
        d.setDepartmentCode("DEPT_TEST_" + id);
        d.setParentId(parentId);
        d.setLevel(level);
        return d;
    }

    @Test
    @DisplayName("R6-a: create(parentId=null) → 落库 parentId=0 且 level=1")
    void createDepartment_parentIdNull_normalizedToZeroAndLevelOne() {
        Department request = department(null, null, null);
        request.setDepartmentCode("DEPT_UT_001");

        service.createDepartment(request);

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentMapper).insert(captor.capture());
        Department saved = captor.getValue();

        assertNotNull(saved.getParentId(), "parentId 不应为 null");
        assertEquals(Long.valueOf(0L), saved.getParentId(), "parentId 应归一为 0（顶级）");
        assertEquals(Integer.valueOf(1), saved.getLevel(), "顶级部门 level 应为 1");
    }

    @Test
    @DisplayName("R6-b: update(parentId=null) → mapper 收到的实体 parentId=0、level=1（不再把 NULL 写回库）")
    void updateDepartment_parentIdNull_normalizedToZero() {
        Long id = 10L;
        Department existing = department(id, 5L, 3);
        when(departmentMapper.selectById(id)).thenReturn(existing);

        Department request = new Department();
        request.setDepartmentName("重命名后的部门");
        request.setParentId(null);

        service.updateDepartment(id, request);

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentMapper).updateById(captor.capture());
        Department saved = captor.getValue();

        assertNotNull(saved.getParentId(), "更新后 parentId 不应为 null");
        assertEquals(Long.valueOf(0L), saved.getParentId(), "update(parentId=null) 应归一为 0");
        assertEquals(Integer.valueOf(1), saved.getLevel(), "归一为顶级后 level 应为 1");
    }

    @Test
    @DisplayName("R6-b2: update(parentId=576) → level = 父级 level + 1")
    void updateDepartment_withParentId_recalculatesLevel() {
        Long id = 11L;
        Department existing = department(id, 0L, 1);
        Department parent = department(576L, 0L, 2);
        when(departmentMapper.selectById(id)).thenReturn(existing);
        when(departmentMapper.selectById(576L)).thenReturn(parent);

        Department request = new Department();
        request.setDepartmentName("挂到 576 下");
        request.setParentId(576L);

        service.updateDepartment(id, request);

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentMapper).updateById(captor.capture());
        Department saved = captor.getValue();

        assertEquals(Long.valueOf(576L), saved.getParentId());
        assertEquals(Integer.valueOf(3), saved.getLevel(), "子部门 level 应为父级 level + 1");
    }

    @Test
    @DisplayName("R6-c: getChildrenDepartments(0) 的查询条件同时覆盖 parent_id = 0 与 parent_id IS NULL")
    void getChildrenDepartments_zero_coversZeroAndNull() {
        service.getChildrenDepartments(0L);

        ArgumentCaptor<Wrapper<Department>> captor = captureWrapper();
        String sql = captor.getValue().getSqlSegment().toUpperCase();

        assertTrue(sql.contains("PARENT_ID"), "条件应包含 parent_id：" + sql);
        assertTrue(sql.contains("IS NULL"), "条件应覆盖 parent_id IS NULL：" + sql);
        assertTrue(sql.contains("OR"), "条件应为 OR 组合：" + sql);
        // 必须是带括号的嵌套条件，否则与逻辑删除条件 deleted=0 的 AND 优先级会串
        assertTrue(sql.trim().startsWith("("), "嵌套 OR 条件必须带括号：" + sql);
    }

    @Test
    @DisplayName("R6-c2: getChildrenDepartments(非 0) 只按等值查询，不混入 NULL 根")
    void getChildrenDepartments_nonZero_doesNotIncludeNullRoots() {
        service.getChildrenDepartments(576L);

        ArgumentCaptor<Wrapper<Department>> captor = captureWrapper();
        String sql = captor.getValue().getSqlSegment().toUpperCase();

        assertTrue(sql.contains("PARENT_ID"), "条件应包含 parent_id：" + sql);
        assertFalse(sql.contains("IS NULL"), "非顶级查询不应包含 parent_id IS NULL：" + sql);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<Wrapper<Department>> captureWrapper() {
        ArgumentCaptor<Wrapper<Department>> captor = ArgumentCaptor.forClass((Class) Wrapper.class);
        verify(departmentMapper).selectList(captor.capture());
        return captor;
    }
}
