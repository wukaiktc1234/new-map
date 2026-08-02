package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.Resume;

import java.util.List;
import java.util.Map;

public interface ResumeService extends IService<Resume> {

    Map<String, Object> getResumes(int current, int size, String status, String requirementId);

    Resume createResume(Resume resume);

    Resume updateResume(String id, Resume resume);

    void deleteResume(String id);

    void updateResumeStatus(String id, String status);

    List<Resume> getResumesByRequirement(String requirementId);

    Resume getResumeByPhone(String phone);
}
