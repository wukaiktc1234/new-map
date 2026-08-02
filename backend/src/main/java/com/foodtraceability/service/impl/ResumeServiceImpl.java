package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.Resume;
import com.foodtraceability.mapper.ResumeMapper;
import com.foodtraceability.service.ResumeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResumeServiceImpl extends ServiceImpl<ResumeMapper, Resume> implements ResumeService {

    @Override
    public Map<String, Object> getResumes(int current, int size, String status, String requirementId) {
        Page<Resume> page = new Page<>(current, size);
        LambdaQueryWrapper<Resume> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(status)) {
            queryWrapper.eq(Resume::getStatus, status);
        }
        if (StringUtils.hasText(requirementId)) {
            queryWrapper.eq(Resume::getRequirementId, requirementId);
        }

        queryWrapper.orderByDesc(Resume::getCreatedAt);
        IPage<Resume> result = this.page(page, queryWrapper);

        Map<String, Object> response = new HashMap<>();
        response.put("records", result.getRecords());
        response.put("total", result.getTotal());
        response.put("current", result.getCurrent());
        response.put("size", result.getSize());
        response.put("pages", result.getPages());

        return response;
    }

    @Override
    public Resume createResume(Resume resume) {
        resume.setResumeCode(generateResumeCode());
        resume.setStatus("pending");
        resume.setCreatedAt(LocalDateTime.now());
        resume.setUpdatedAt(LocalDateTime.now());
        this.save(resume);
        return resume;
    }

    @Override
    public Resume updateResume(String id, Resume resume) {
        resume.setId(id);
        resume.setUpdatedAt(LocalDateTime.now());
        this.updateById(resume);
        return resume;
    }

    @Override
    public void deleteResume(String id) {
        this.removeById(id);
    }

    @Override
    public void updateResumeStatus(String id, String status) {
        Resume resume = this.getById(id);
        if (resume != null) {
            resume.setStatus(status);
            resume.setUpdatedAt(LocalDateTime.now());
            this.updateById(resume);
        }
    }

    @Override
    public List<Resume> getResumesByRequirement(String requirementId) {
        LambdaQueryWrapper<Resume> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Resume::getRequirementId, requirementId);
        queryWrapper.orderByDesc(Resume::getCreatedAt);
        return this.list(queryWrapper);
    }

    @Override
    public Resume getResumeByPhone(String phone) {
        LambdaQueryWrapper<Resume> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Resume::getPhone, phone);
        return this.getOne(queryWrapper);
    }

    private String generateResumeCode() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "RES" + timestamp;
    }
}
