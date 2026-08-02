package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.entity.Employee;
import com.foodtraceability.entity.Position;
import com.foodtraceability.mapper.EmployeeMapper;
import com.foodtraceability.mapper.PositionMapper;
import com.foodtraceability.service.PositionService;
import com.foodtraceability.utils.PositionCodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PositionServiceImpl implements PositionService {

    private static final Logger log = LoggerFactory.getLogger(PositionServiceImpl.class);

    private final PositionMapper positionMapper;
    private final EmployeeMapper employeeMapper;
    private final PositionCodeGenerator positionCodeGenerator;

    public PositionServiceImpl(PositionMapper positionMapper, EmployeeMapper employeeMapper, PositionCodeGenerator positionCodeGenerator) {
        this.positionMapper = positionMapper;
        this.employeeMapper = employeeMapper;
        this.positionCodeGenerator = positionCodeGenerator;
    }

    // #region debug-point A:service-entry
    private static void debugEvent(String hypothesisId, String location, String msg, Throwable t) {
        try {
            String env = java.nio.file.Files.readString(java.nio.file.Path.of("P:\\my-new-project\\.dbg\\position-employee-empty-error.env"));
            String url = env.lines().filter(l -> l.startsWith("DEBUG_SERVER_URL=")).map(l -> l.substring(17)).findFirst().orElse("http://127.0.0.1:7777/event");
            String sid = env.lines().filter(l -> l.startsWith("DEBUG_SESSION_ID=")).map(l -> l.substring(17)).findFirst().orElse("position-employee-empty-error");
            StringBuilder data = new StringBuilder("{\"location\":\"").append(location).append("\"");
            if (t != null) {
                data.append(",\"errorClass\":\"").append(t.getClass().getName()).append("\"");
                String m = t.getMessage();
                if (m != null) data.append(",\"errorMessage\":\"").append(m.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")).append("\"");
            }
            data.append("}");
            String body = "{\"sessionId\":\"" + sid + "\",\"runId\":\"pre-fix\",\"hypothesisId\":\"" + hypothesisId + "\",\"location\":\"" + location + "\",\"msg\":\"[DEBUG] " + msg + "\",\"data\":" + data + "}";
            java.net.http.HttpClient.newHttpClient().send(java.net.http.HttpRequest.newBuilder().uri(java.net.URI.create(url)).header("Content-Type", "application/json").POST(java.net.http.HttpRequest.BodyPublishers.ofString(body)).build(), java.net.http.HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
        }
    }
    // #endregion

    @Override
    public Map<String, Object> getPositions(int current, int size, Long departmentId, String status, String keyword) {
        // #region debug-point A:service-entry
        debugEvent("A", "PositionServiceImpl.getPositions", "getPositions called current=" + current + " size=" + size, null);
        // #endregion
        try {
            Page<Position> page = new Page<>(current, size);
            Integer statusValue = null;
            if (status != null && !status.isEmpty()) {
                statusValue = "active".equals(status) ? 1 : 0;
            }
            String keywordPattern = keyword != null && !keyword.isEmpty() ? "%" + keyword + "%" : null;
            Page<Position> resultPage = positionMapper.selectPositionsWithFilters(page, departmentId, statusValue, keywordPattern);
            // #region debug-point A:mapper-result
            debugEvent("A", "PositionServiceImpl.getPositions", "selectPositionsWithFilters returned records=" + (resultPage != null ? resultPage.getRecords().size() : -1), null);
            // #endregion

            // 实时统计每个职位的在岗员工数（仅统计在职和试用期，与部门人员规模口径一致）
            for (Position position : resultPage.getRecords()) {
                if (position.getId() != null) {
                    QueryWrapper<Employee> employeeQueryWrapper = new QueryWrapper<>();
                    employeeQueryWrapper.eq("position_id", position.getId());
                    employeeQueryWrapper.in("status", 1, 2);
                    Long count = employeeMapper.selectCount(employeeQueryWrapper);
                    position.setEmployeeCount(count != null ? count.intValue() : 0);
                } else {
                    position.setEmployeeCount(0);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("items", resultPage.getRecords());
            result.put("total", resultPage.getTotal());
            result.put("current", resultPage.getCurrent());
            result.put("size", resultPage.getSize());
            result.put("pages", resultPage.getPages());
            // #region debug-point A:service-success
            debugEvent("A", "PositionServiceImpl.getPositions", "getPositions success total=" + resultPage.getTotal(), null);
            // #endregion
            return result;
        } catch (Exception e) {
            // #region debug-point A:service-error
            debugEvent("A", "PositionServiceImpl.getPositions", "getPositions exception", e);
            // #endregion
            throw e;
        }
    }

    @Override
    public List<Position> getAllPositions() {
        List<Position> positions = positionMapper.selectAllPositionsWithDepartmentSort();

        // 实时统计每个职位的在岗员工数（仅统计在职和试用期，与部门人员规模口径一致）
        for (Position position : positions) {
            if (position.getId() != null) {
                QueryWrapper<Employee> employeeQueryWrapper = new QueryWrapper<>();
                employeeQueryWrapper.eq("position_id", position.getId());
                employeeQueryWrapper.in("status", 1, 2);
                Long count = employeeMapper.selectCount(employeeQueryWrapper);
                position.setEmployeeCount(count != null ? count.intValue() : 0);
            } else {
                position.setEmployeeCount(0);
            }
        }

        return positions;
    }

    @Override
    public Position getPositionById(Long id) {
        Position position = positionMapper.selectById(id);
        if (position != null && position.getId() != null) {
            QueryWrapper<Employee> employeeQueryWrapper = new QueryWrapper<>();
            employeeQueryWrapper.eq("position_id", position.getId());
            employeeQueryWrapper.in("status", 1, 2);
            Long count = employeeMapper.selectCount(employeeQueryWrapper);
            position.setEmployeeCount(count != null ? count.intValue() : 0);
        } else if (position != null) {
            position.setEmployeeCount(0);
        }
        return position;
    }

    @Override
    public Position createPosition(Position position) {
        if (position.getPositionName() == null || position.getPositionName().trim().isEmpty()) {
            throw new BusinessException(400, "职位名称不能为空");
        }

        // 编制人数默认至少为 1
        if (position.getHeadCount() == null || position.getHeadCount() < 1) {
            position.setHeadCount(1);
        }

        // 生成职位编码
        String positionCode = positionCodeGenerator.generatePositionCode(
            position.getDepartmentId() != null ? position.getDepartmentId().toString() : null,
            position.getPositionName()
        );
        position.setPositionCode(positionCode);

        positionMapper.insert(position);
        return position;
    }

    @Override
    public Position updatePosition(Long id, Position position) {
        Position existingPosition = positionMapper.selectById(id);
        if (existingPosition == null) {
            throw new BusinessException(404, "职位不存在");
        }

        // 只更新传入的非 null 字段，防止前端未传字段被覆盖为空
        if (position.getPositionName() != null) {
            existingPosition.setPositionName(position.getPositionName());
        }
        if (position.getDepartmentId() != null) {
            existingPosition.setDepartmentId(position.getDepartmentId());
        }
        if (position.getDescription() != null) {
            existingPosition.setDescription(position.getDescription());
        }
        if (position.getHeadCount() != null) {
            existingPosition.setHeadCount(position.getHeadCount());
        }
        if (position.getStatus() != null) {
            existingPosition.setStatus(position.getStatus());
        }

        // 如果职位名称或部门变更，重新生成编码
        boolean nameChanged = position.getPositionName() != null
                && !existingPosition.getPositionName().equals(position.getPositionName());
        boolean departmentChanged = position.getDepartmentId() != null
                && !position.getDepartmentId().equals(existingPosition.getDepartmentId());
        if (nameChanged || departmentChanged) {
            String positionCode = positionCodeGenerator.generatePositionCode(
                existingPosition.getDepartmentId() != null ? existingPosition.getDepartmentId().toString() : null,
                existingPosition.getPositionName()
            );
            existingPosition.setPositionCode(positionCode);
        }

        positionMapper.updateById(existingPosition);
        return existingPosition;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePosition(Long id) {
        Position position = positionMapper.selectById(id);
        if (position == null) {
            return;
        }
        // 检查是否有关联的在职员工（含试用期）
        QueryWrapper<Employee> employeeQueryWrapper = new QueryWrapper<>();
        employeeQueryWrapper.eq("position_id", id);
        employeeQueryWrapper.in("status", 1, 2);
        long employeeCount = employeeMapper.selectCount(employeeQueryWrapper);
        if (employeeCount > 0) {
            throw new BusinessException(400, "该职位下还有员工，无法删除");
        }

        // 使用 MyBatis-Plus 逻辑删除：统一由 @TableLogic 处理
        positionMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupPositionsByPrefix(String prefix) {
        log.info("开始清理岗位编码前缀为 [{}] 的数据", prefix);
        // 查询所有匹配前缀且未逻辑删除的岗位
        QueryWrapper<Position> queryWrapper = new QueryWrapper<>();
        queryWrapper.likeRight("position_code", prefix);
        queryWrapper.eq("deleted", 0);
        List<Position> positions = positionMapper.selectList(queryWrapper);

        int cleanedCount = 0;
        for (Position position : positions) {
            Long positionId = position.getId();
            if (positionId == null) {
                continue;
            }
            // 检查是否有关联的在职员工（含试用期）
            QueryWrapper<Employee> employeeQueryWrapper = new QueryWrapper<>();
            employeeQueryWrapper.eq("position_id", positionId);
            employeeQueryWrapper.in("status", 1, 2);
            long employeeCount = employeeMapper.selectCount(employeeQueryWrapper);
            if (employeeCount > 0) {
                log.info("跳过仍有员工的岗位: {} ({})", position.getPositionCode(), position.getPositionName());
                continue;
            }
            // 物理删除无员工关联的岗位
            positionMapper.deleteById(positionId);
            cleanedCount++;
            log.info("已清理岗位: {} ({})", position.getPositionCode(), position.getPositionName());
        }
        log.info("岗位清理完成: 共清理 {} 个", cleanedCount);
        return cleanedCount;
    }

    @Override
    public void updatePositionStatus(Long id, Boolean status) {
        Position position = positionMapper.selectById(id);
        if (position != null) {
            position.setStatus(status ? 1 : 0);
            positionMapper.updateById(position);
        }
    }

    @Override
    public void updatePositionStatusWithCascade(Long id, Boolean status) {
        // 先更新职位状态
        updatePositionStatus(id, status);
        
        // 级联更新关联的员工状态
        QueryWrapper<Employee> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("position_id", id);
        List<Employee> employees = employeeMapper.selectList(queryWrapper);
        for (Employee employee : employees) {
            // Employee.status 已统一为 Integer：1 在职 / 0 离职（与项目规范第24章状态映射一致）
            employee.setStatus(status ? 1 : 0);
            employeeMapper.updateById(employee);
        }
    }

    @Override
    public List<Position> getPositionsByDepartment(Long departmentId) {
        List<Position> positions = positionMapper.selectPositionsByDepartmentWithSort(departmentId);
        
        // 实时统计每个职位的在岗员工数（仅统计在职和试用期，与部门人员规模口径一致）
        for (Position position : positions) {
            if (position.getId() != null) {
                QueryWrapper<Employee> employeeQueryWrapper = new QueryWrapper<>();
                employeeQueryWrapper.eq("position_id", position.getId());
                employeeQueryWrapper.in("status", 1, 2);
                Long count = employeeMapper.selectCount(employeeQueryWrapper);
                position.setEmployeeCount(count != null ? count.intValue() : 0);
            } else {
                position.setEmployeeCount(0);
            }
        }

        return positions;
    }

    @Override
    public void incrementEmployeeCount(Long positionId) {
        Position position = positionMapper.selectById(positionId);
        if (position != null) {
            int currentCount = position.getEmployeeCount() != null ? position.getEmployeeCount() : 0;
            position.setEmployeeCount(currentCount + 1);
            positionMapper.updateById(position);
        }
    }

    @Override
    public void decrementEmployeeCount(Long positionId) {
        Position position = positionMapper.selectById(positionId);
        if (position != null) {
            int currentCount = position.getEmployeeCount() != null ? position.getEmployeeCount() : 0;
            if (currentCount > 0) {
                position.setEmployeeCount(currentCount - 1);
                positionMapper.updateById(position);
            }
        }
    }

    @Override
    public void clearAllPositions() {
        QueryWrapper<Position> queryWrapper = new QueryWrapper<>();
        List<Position> positions = positionMapper.selectList(queryWrapper);
        for (Position position : positions) {
            position.setDeleted(1);
            positionMapper.updateById(position);
        }
    }

    @Override
    public String getPositionCodePrefix(String positionName) {
        return positionCodeGenerator.generatePositionPrefix(positionName);
    }

    @Override
    public String getDepartmentCode(String departmentId) {
        return positionCodeGenerator.getDepartmentCode(departmentId);
    }

    @Override
    public Integer getNextPositionSeq(String codePrefix) {
        // 这个方法暂时返回1，因为我们现在使用更复杂的序号生成逻辑
        return 1;
    }

    @Override
    public String generatePositionCode(String departmentId, String positionName) {
        return positionCodeGenerator.generatePositionCode(departmentId, positionName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchUpdatePositionCodes() {
        log.info("开始批量更新职位编码");

        QueryWrapper<Position> queryWrapper = new QueryWrapper<>();
        List<Position> allPositions = positionMapper.selectList(queryWrapper);

        int successCount = 0;
        int failCount = 0;
        List<String> errorMessages = new ArrayList<>();
        List<String> updatedCodes = new ArrayList<>();

        for (Position position : allPositions) {
            try {
                String oldCode = position.getPositionCode();
                String newCode = positionCodeGenerator.generatePositionCode(
                    position.getDepartmentId() != null ? position.getDepartmentId().toString() : null,
                    position.getPositionName()
                );

                if (!newCode.equals(oldCode)) {
                    // 验证编码格式
                    if (!isValidPositionCode(newCode)) {
                        throw new RuntimeException("生成的编码格式无效: " + newCode);
                    }
                    
                    position.setPositionCode(newCode);
                    positionMapper.updateById(position);
                    successCount++;
                    updatedCodes.add(oldCode + " -> " + newCode);
                    log.info("更新职位编码: {} -> {}", oldCode, newCode);
                } else {
                    log.info("职位编码无需更新: {}", position.getPositionCode());
                }
            } catch (Exception e) {
                failCount++;
                String errorMsg = String.format("职位[%s]更新失败: %s", position.getPositionName(), e.getMessage());
                errorMessages.add(errorMsg);
                log.error("更新职位编码失败: {}", position.getPositionName(), e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", allPositions.size());
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("errorMessages", String.join("; ", errorMessages));
        result.put("updatedCodes", updatedCodes);

        log.info("批量更新职位编码完成: 总数={}, 成功={}, 失败={}", 
            allPositions.size(), successCount, failCount);

        return result;
    }
    
    private boolean isValidPositionCode(String positionCode) {
        // 基本验证
        if (positionCode == null || positionCode.isEmpty()) {
            return false;
        }
        
        // 检查长度限制
        if (positionCode.length() > 50) {
            return false;
        }
        
        // 对于自定义格式模板，我们只做基本验证
        // 因为格式模板是用户定义的，可能有各种不同的格式
        
        return true;
    }
}
