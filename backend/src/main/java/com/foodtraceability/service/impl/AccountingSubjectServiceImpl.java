package com.foodtraceability.service.impl;

import com.foodtraceability.entity.AccountingSubject;
import com.foodtraceability.service.AccountingSubjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 会计科目Service Stub实现 (空实现)
 * @deprecated 已废弃，仅用于兼容旧代码编译
 */
@Deprecated
@Service("legacyAccountingSubjectServiceImpl")
public class AccountingSubjectServiceImpl implements AccountingSubjectService {

    private static final Logger log = LoggerFactory.getLogger(AccountingSubjectServiceImpl.class);

    @Override
    public List<AccountingSubject> getByCategory(String category) {
        return Collections.emptyList();
    }

    @Override
    public List<AccountingSubject> getFirstLevelSubjects() {
        return Collections.emptyList();
    }

    @Override
    public List<AccountingSubject> getByParentId(Long parentId) {
        return Collections.emptyList();
    }

    @Override
    public AccountingSubject getByCode(String code) {
        return null;
    }

    @Override
    public List<AccountingSubject> searchSubjects(String keyword, String category) {
        return Collections.emptyList();
    }

    @Override
    public boolean createSubject(AccountingSubject subject) {
        return false;
    }

    @Override
    public boolean updateSubject(AccountingSubject subject) {
        return false;
    }

    @Override
    public boolean deleteSubject(Long id) {
        return false;
    }

    @Override
    public List<AccountingSubject> getSubjectTree() {
        return Collections.emptyList();
    }

    @Override
    public boolean importSubjects(List<AccountingSubject> subjects) {
        return false;
    }

    @Override
    public List<AccountingSubject> exportSubjects() {
        return Collections.emptyList();
    }
}
