@echo off
chcp 65001

REM 这个脚本用于标记新版本文件
REM 请在Git bash或Linux环境下运行

echo ==========================================
echo 标记新版本文件 (V2.2 入职档案模块)
echo ==========================================
echo.

REM 实体类
echo 标记实体类...
REM OnboardingArchive.java 已手动标记
REM ApprovalRecord.java
echo   - ApprovalRecord.java
echo   - InvitationSendRecord.java
echo   - InvitationExpenseRecord.java
echo   - InvitationReminderLog.java

echo.
REM Mapper
echo 标记Mapper接口...
echo   - OnboardingArchiveMapper.java
echo   - ApprovalRecordMapper.java
echo   - InvitationSendRecordMapper.java
echo   - InvitationExpenseRecordMapper.java
echo   - InvitationReminderLogMapper.java

echo.
REM Service
echo 标记Service接口...
echo   - OnboardingArchiveService.java
echo   - ApprovalService.java
echo   - OnboardingInvitationService.java

echo.
REM ServiceImpl
echo 标记Service实现类...
echo   - OnboardingArchiveServiceImpl.java
echo   - ApprovalServiceImpl.java
echo   - OnboardingInvitationServiceImpl.java

echo.
REM Controller
echo 标记Controller...
echo   - OnboardingArchiveController.java
echo   - ApprovalController.java
echo   - InvitationCodeController.java (入职邀请码)

echo.
REM DTO
echo 标记DTO...
echo   - OnboardingArchiveDTO.java

echo.
echo ==========================================
echo 标记完成！
echo ==========================================
echo.
echo 注意：旧版本文件（待清理）包括：
echo   - entity/InvitationCode.java
echo   - mapper/InvitationCodeMapper.java
echo   - service/InvitationCodeService.java
echo   - service/impl/InvitationCodeServiceImpl.java
echo   - controller/AuthController.java 中的旧邀请码API
echo.
pause
