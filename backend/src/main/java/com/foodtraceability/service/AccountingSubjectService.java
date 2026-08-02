package com.foodtraceability.service;

import com.foodtraceability.entity.AccountingSubject;
import java.util.List;

/**
 * 会计科目Service (已废弃，请使用com.foodtraceability.service.finance.AccountingSubjectService)
 * @deprecated 此接口已废弃，将在后续版本移除
 */
@Deprecated
public interface AccountingSubjectService {

    @Deprecated
    List<AccountingSubject> getByCategory(String category);

    @Deprecated
    List<AccountingSubject> getFirstLevelSubjects();

    @Deprecated
    List<AccountingSubject> getByParentId(Long parentId);

    @Deprecated
    AccountingSubject getByCode(String code);

    @Deprecated
    List<AccountingSubject> searchSubjects(String keyword, String category);

    @Deprecated
    boolean createSubject(AccountingSubject subject);

    @Deprecated
    boolean updateSubject(AccountingSubject subject);

    @Deprecated
    boolean deleteSubject(Long id);

    @Deprecated
    List<AccountingSubject> getSubjectTree();

    @Deprecated
    boolean importSubjects(List<AccountingSubject> subjects);

    @Deprecated
    List<AccountingSubject> exportSubjects();
}
