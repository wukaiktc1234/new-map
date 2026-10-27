# bug-⑫ 形态②：Result.error 字面量审计（默认 code=500）

扫描：backend/src/main/java 全部 .java；判据：Result.error( 后接字符串字面量

| # | 文件:行 | 方法 | 文案 | 推测语义 | 等价 ErrorCode |
|---|---------|------|------|----------|----------------|
| 1 | `backend/src/main/java/com/foodtraceability/controller/admin/ReportInsightRuleController.java:82` | `getRule` | 规则不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 2 | `backend/src/main/java/com/foodtraceability/controller/AIModelConfigController.java:75` | `getByCode` | AI 模型配置不存在:  | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 3 | `backend/src/main/java/com/foodtraceability/controller/AIModelConfigController.java:88` | `getById` | AI 模型配置不存在: id= | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 4 | `backend/src/main/java/com/foodtraceability/controller/AIModelConfigController.java:133` | `updateStatus` | 更新状态失败：模型不存在或状态无效 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 5 | `backend/src/main/java/com/foodtraceability/controller/AIModelConfigController.java:148` | `syncModel` | 同步失败：模型不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 6 | `backend/src/main/java/com/foodtraceability/controller/AIModelConfigController.java:173` | `testConnection` | 测试连接异常:  | 未判定 | (需人工判定) |
| 7 | `backend/src/main/java/com/foodtraceability/controller/AIModelConfigController.java:185` | `delete` | 删除失败：模型不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 8 | `backend/src/main/java/com/foodtraceability/controller/AlertController.java:57` | `getAlertPage` | 查询告警列表失败 | 未判定 | (需人工判定) |
| 9 | `backend/src/main/java/com/foodtraceability/controller/AlertController.java:69` | `getAllAlerts` | 获取所有告警失败 | 未判定 | (需人工判定) |
| 10 | `backend/src/main/java/com/foodtraceability/controller/AlertController.java:91` | `getActiveAlerts` | 获取活跃告警失败 | 未判定 | (需人工判定) |
| 11 | `backend/src/main/java/com/foodtraceability/controller/AlertController.java:111` | `acknowledgeAlert` | 确认告警失败 | 未判定 | (需人工判定) |
| 12 | `backend/src/main/java/com/foodtraceability/controller/AlertController.java:132` | `resolveAlert` | 解决告警失败 | 未判定 | (需人工判定) |
| 13 | `backend/src/main/java/com/foodtraceability/controller/AlertController.java:148` | `deleteAlert` | 删除告警失败 | 未判定 | (需人工判定) |
| 14 | `backend/src/main/java/com/foodtraceability/controller/AlertController.java:167` | `getStatistics` | 获取告警统计失败 | 未判定 | (需人工判定) |
| 15 | `backend/src/main/java/com/foodtraceability/controller/approval/ApprovalWorkflowController.java:63` | `getList` | 查询审批流程列表失败 | 未判定 | (需人工判定) |
| 16 | `backend/src/main/java/com/foodtraceability/controller/approval/ApprovalWorkflowController.java:81` | `getDefaultWorkflow` | 获取默认审批流程失败 | 未判定 | (需人工判定) |
| 17 | `backend/src/main/java/com/foodtraceability/controller/approval/ApprovalWorkflowController.java:99` | `getCurrentNode` | 获取当前审批节点失败 | 未判定 | (需人工判定) |
| 18 | `backend/src/main/java/com/foodtraceability/controller/approval/ApprovalWorkflowController.java:114` | `submitApproval` | 业务类型和业务ID不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 19 | `backend/src/main/java/com/foodtraceability/controller/approval/ApprovalWorkflowController.java:120` | `submitApproval` | 提交审批失败 | 未判定 | (需人工判定) |
| 20 | `backend/src/main/java/com/foodtraceability/controller/approval/ApprovalWorkflowController.java:133` | `approve` | 业务ID不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 21 | `backend/src/main/java/com/foodtraceability/controller/approval/ApprovalWorkflowController.java:139` | `approve` | 审批通过失败 | 未判定 | (需人工判定) |
| 22 | `backend/src/main/java/com/foodtraceability/controller/approval/ApprovalWorkflowController.java:152` | `reject` | 业务ID不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 23 | `backend/src/main/java/com/foodtraceability/controller/approval/ApprovalWorkflowController.java:158` | `reject` | 审批驳回失败 | 未判定 | (需人工判定) |
| 24 | `backend/src/main/java/com/foodtraceability/controller/approval/ApprovalWorkflowController.java:172` | `withdraw` | 业务ID不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 25 | `backend/src/main/java/com/foodtraceability/controller/approval/ApprovalWorkflowController.java:178` | `withdraw` | 撤回审批失败 | 未判定 | (需人工判定) |
| 26 | `backend/src/main/java/com/foodtraceability/controller/approval/ApprovalWorkflowController.java:198` | `create` | 创建审批流程失败 | 未判定 | (需人工判定) |
| 27 | `backend/src/main/java/com/foodtraceability/controller/approval/ApprovalWorkflowController.java:213` | `getById` | 审批流程不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 28 | `backend/src/main/java/com/foodtraceability/controller/approval/ApprovalWorkflowController.java:218` | `getById` | 获取审批流程详情失败 | 未判定 | (需人工判定) |
| 29 | `backend/src/main/java/com/foodtraceability/controller/approval/ApprovalWorkflowController.java:238` | `update` | 更新审批流程失败 | 未判定 | (需人工判定) |
| 30 | `backend/src/main/java/com/foodtraceability/controller/approval/ApprovalWorkflowController.java:257` | `delete` | 删除审批流程失败 | 未判定 | (需人工判定) |
| 31 | `backend/src/main/java/com/foodtraceability/controller/approval/ApprovalWorkflowController.java:273` | `toggleEnabled` | 启用状态不能为空 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 32 | `backend/src/main/java/com/foodtraceability/controller/approval/ApprovalWorkflowController.java:281` | `toggleEnabled` | 切换审批流程启用状态失败 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 33 | `backend/src/main/java/com/foodtraceability/controller/approval/ApprovalWorkflowController.java:298` | `getApprovalLogs` | 获取审批记录失败 | 未判定 | (需人工判定) |
| 34 | `backend/src/main/java/com/foodtraceability/controller/approval/EmployeeApprovalController.java:57` | `getMyApprovals` | 用户ID不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 35 | `backend/src/main/java/com/foodtraceability/controller/approval/EmployeeApprovalController.java:79` | `getMyApprovals` | 获取我的审批列表失败 | 未判定 | (需人工判定) |
| 36 | `backend/src/main/java/com/foodtraceability/controller/approval/EmployeeApprovalController.java:94` | `getPendingReviews` | 用户ID不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 37 | `backend/src/main/java/com/foodtraceability/controller/approval/EmployeeApprovalController.java:115` | `getPendingReviews` | 获取待我审批列表失败 | 未判定 | (需人工判定) |
| 38 | `backend/src/main/java/com/foodtraceability/controller/approval/EmployeeApprovalController.java:129` | `getDetail` | 审批ID不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 39 | `backend/src/main/java/com/foodtraceability/controller/approval/EmployeeApprovalController.java:143` | `getDetail` | 获取审批详情失败 | 未判定 | (需人工判定) |
| 40 | `backend/src/main/java/com/foodtraceability/controller/approval/EmployeeApprovalController.java:158` | `submit` | 用户ID不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 41 | `backend/src/main/java/com/foodtraceability/controller/approval/EmployeeApprovalController.java:172` | `submit` | 提交审批申请失败 | 未判定 | (需人工判定) |
| 42 | `backend/src/main/java/com/foodtraceability/controller/approval/EmployeeApprovalController.java:187` | `approve` | 审批ID不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 43 | `backend/src/main/java/com/foodtraceability/controller/approval/EmployeeApprovalController.java:207` | `approve` | 通过审批失败 | 未判定 | (需人工判定) |
| 44 | `backend/src/main/java/com/foodtraceability/controller/approval/EmployeeApprovalController.java:222` | `reject` | 审批ID不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 45 | `backend/src/main/java/com/foodtraceability/controller/approval/EmployeeApprovalController.java:232` | `reject` | 驳回原因不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 46 | `backend/src/main/java/com/foodtraceability/controller/approval/EmployeeApprovalController.java:246` | `reject` | 驳回审批失败 | 未判定 | (需人工判定) |
| 47 | `backend/src/main/java/com/foodtraceability/controller/approval/EmployeeApprovalController.java:261` | `withdraw` | 审批ID不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 48 | `backend/src/main/java/com/foodtraceability/controller/approval/EmployeeApprovalController.java:264` | `withdraw` | 用户ID不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 49 | `backend/src/main/java/com/foodtraceability/controller/approval/EmployeeApprovalController.java:278` | `withdraw` | 撤回审批失败 | 未判定 | (需人工判定) |
| 50 | `backend/src/main/java/com/foodtraceability/controller/approval/EmployeeApprovalController.java:293` | `urge` | 审批ID不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 51 | `backend/src/main/java/com/foodtraceability/controller/approval/EmployeeApprovalController.java:296` | `urge` | 用户ID不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 52 | `backend/src/main/java/com/foodtraceability/controller/approval/EmployeeApprovalController.java:310` | `urge` | 催办审批失败 | 未判定 | (需人工判定) |
| 53 | `backend/src/main/java/com/foodtraceability/controller/approval/EmployeeApprovalController.java:324` | `getMyStats` | 用户ID不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 54 | `backend/src/main/java/com/foodtraceability/controller/approval/EmployeeApprovalController.java:331` | `getMyStats` | 获取审批统计失败 | 未判定 | (需人工判定) |
| 55 | `backend/src/main/java/com/foodtraceability/controller/approval/EmployeeApprovalController.java:345` | `batchBasicInfo` | 审批ID列表不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 56 | `backend/src/main/java/com/foodtraceability/controller/approval/EmployeeApprovalController.java:352` | `batchBasicInfo` | 批量获取审批基本信息失败 | 未判定 | (需人工判定) |
| 57 | `backend/src/main/java/com/foodtraceability/controller/ApprovalController.java:60` | `approve` | 审批失败 | 未判定 | (需人工判定) |
| 58 | `backend/src/main/java/com/foodtraceability/controller/ApprovalController.java:74` | `reject` | 审批失败 | 未判定 | (需人工判定) |
| 59 | `backend/src/main/java/com/foodtraceability/controller/ApprovalController.java:88` | `startApprovalProcess` | 启动失败 | 未判定 | (需人工判定) |
| 60 | `backend/src/main/java/com/foodtraceability/controller/asset/AssetCategoryController.java:78` | `getById` | 分类不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 61 | `backend/src/main/java/com/foodtraceability/controller/asset/AssetCategoryController.java:95` | `create` | 分类编码已存在 | 未判定 | (需人工判定) |
| 62 | `backend/src/main/java/com/foodtraceability/controller/asset/AssetCategoryController.java:124` | `update` | 分类不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 63 | `backend/src/main/java/com/foodtraceability/controller/asset/AssetCategoryController.java:129` | `update` | 分类编码已存在 | 未判定 | (需人工判定) |
| 64 | `backend/src/main/java/com/foodtraceability/controller/asset/AssetCategoryController.java:148` | `delete` | 分类不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 65 | `backend/src/main/java/com/foodtraceability/controller/asset/AssetCategoryController.java:153` | `delete` | 存在子分类，无法删除 | 未判定 | (需人工判定) |
| 66 | `backend/src/main/java/com/foodtraceability/controller/asset/AssetDepreciationController.java:94` | `execute` | 折旧期间不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 67 | `backend/src/main/java/com/foodtraceability/controller/asset/AssetDisposalController.java:57` | `getById` | 处置单不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 68 | `backend/src/main/java/com/foodtraceability/controller/asset/AssetDisposalController.java:79` | `create` | 资产ID不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 69 | `backend/src/main/java/com/foodtraceability/controller/asset/AssetDisposalController.java:82` | `create` | 处置类型不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 70 | `backend/src/main/java/com/foodtraceability/controller/asset/AssetDisposalController.java:102` | `approve` | 审批结果不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 71 | `backend/src/main/java/com/foodtraceability/controller/asset/AssetDisposalController.java:106` | `approve` | 审批失败 | 未判定 | (需人工判定) |
| 72 | `backend/src/main/java/com/foodtraceability/controller/asset/AssetInventoryController.java:80` | `getById` | 盘点单不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 73 | `backend/src/main/java/com/foodtraceability/controller/asset/AssetInventoryController.java:130` | `update` | 盘点单不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 74 | `backend/src/main/java/com/foodtraceability/controller/asset/AssetInventoryController.java:152` | `inputItem` | 盘点明细不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 75 | `backend/src/main/java/com/foodtraceability/controller/asset/AssetInventoryController.java:155` | `inputItem` | 明细不属于该盘点单 | 未判定 | (需人工判定) |
| 76 | `backend/src/main/java/com/foodtraceability/controller/asset/AssetInventoryController.java:174` | `complete` | 盘点单不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 77 | `backend/src/main/java/com/foodtraceability/controller/asset/AssetMaintenanceController.java:76` | `getById` | 维护记录不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 78 | `backend/src/main/java/com/foodtraceability/controller/asset/AssetMaintenanceController.java:79` | `getById` | 该记录不是维修类型 | 未判定 | (需人工判定) |
| 79 | `backend/src/main/java/com/foodtraceability/controller/AssetController.java:145` | `getById` | 资产不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 80 | `backend/src/main/java/com/foodtraceability/controller/AssetController.java:156` | `getByCode` | 资产不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 81 | `backend/src/main/java/com/foodtraceability/controller/AssetController.java:192` | `update` | 资产不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 82 | `backend/src/main/java/com/foodtraceability/controller/AssetController.java:209` | `delete` | 使用中的资产无法删除 | 未判定 | (需人工判定) |
| 83 | `backend/src/main/java/com/foodtraceability/controller/AttendanceController.java:48` | `getAttendancePage` | 查询考勤记录失败 | 未判定 | (需人工判定) |
| 84 | `backend/src/main/java/com/foodtraceability/controller/AttendanceController.java:92` | `getAttendanceDetail` | 考勤记录不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 85 | `backend/src/main/java/com/foodtraceability/controller/AttendanceController.java:97` | `getAttendanceDetail` | 获取考勤详情失败 | 未判定 | (需人工判定) |
| 86 | `backend/src/main/java/com/foodtraceability/controller/AttendanceController.java:114` | `createAttendance` | 创建考勤记录失败 | 未判定 | (需人工判定) |
| 87 | `backend/src/main/java/com/foodtraceability/controller/AttendanceController.java:132` | `updateAttendance` | 更新考勤记录失败 | 未判定 | (需人工判定) |
| 88 | `backend/src/main/java/com/foodtraceability/controller/AttendanceController.java:149` | `clockIn` | 打卡失败，请联系管理员 | 未判定 | (需人工判定) |
| 89 | `backend/src/main/java/com/foodtraceability/controller/AttendanceController.java:166` | `clockOut` | 打卡失败，请联系管理员 | 未判定 | (需人工判定) |
| 90 | `backend/src/main/java/com/foodtraceability/controller/AttendanceController.java:186` | `applyLeave` | 请假申请失败 | 未判定 | (需人工判定) |
| 91 | `backend/src/main/java/com/foodtraceability/controller/AttendanceController.java:204` | `registerOvertime` | 加班登记失败 | 未判定 | (需人工判定) |
| 92 | `backend/src/main/java/com/foodtraceability/controller/AttendanceController.java:219` | `getMonthlySummary` | 获取月度考勤统计失败 | 未判定 | (需人工判定) |
| 93 | `backend/src/main/java/com/foodtraceability/controller/AttendanceController.java:234` | `getDepartmentDailyAttendance` | 获取部门当日考勤失败 | 未判定 | (需人工判定) |
| 94 | `backend/src/main/java/com/foodtraceability/controller/AuditLogController.java:75` | `getAuditLogs` | 查询审计日志失败 | 未判定 | (需人工判定) |
| 95 | `backend/src/main/java/com/foodtraceability/controller/AuditLogController.java:119` | `archiveLogs` | 归档保留天数不能少于30天 | 未判定 | (需人工判定) |
| 96 | `backend/src/main/java/com/foodtraceability/controller/AuditLogController.java:137` | `cleanExpiredLogsDeprecated` | 审计日志不可删除（会计法/网络安全法要求），如需归档请使用 POST /v1/audit-logs/archive | 未判定 | (需人工判定) |
| 97 | `backend/src/main/java/com/foodtraceability/controller/AuthController.java:59` | `getCaptcha` | 生成验证码失败 | 未判定 | (需人工判定) |
| 98 | `backend/src/main/java/com/foodtraceability/controller/AuthController.java:86` | `login` | 登录失败 | 未判定 | (需人工判定) |
| 99 | `backend/src/main/java/com/foodtraceability/controller/AuthController.java:95` | `login` | 登录服务异常 | 未判定 | (需人工判定) |
| 100 | `backend/src/main/java/com/foodtraceability/controller/AuthController.java:125` | `register` | 注册失败，请稍后重试 | 未判定 | (需人工判定) |
| 101 | `backend/src/main/java/com/foodtraceability/controller/AuthController.java:140` | `validateInvitationCode` | 邀请码不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 102 | `backend/src/main/java/com/foodtraceability/controller/AuthController.java:146` | `validateInvitationCode` | 邀请码校验失败，请稍后重试 | 未判定 | (需人工判定) |
| 103 | `backend/src/main/java/com/foodtraceability/controller/AuthController.java:182` | `refreshToken` | 无效的令牌格式 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 104 | `backend/src/main/java/com/foodtraceability/controller/AuthController.java:188` | `refreshToken` | 刷新令牌失败 | 未判定 | (需人工判定) |
| 105 | `backend/src/main/java/com/foodtraceability/controller/AuthController.java:198` | `logout` | 无效的令牌格式 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 106 | `backend/src/main/java/com/foodtraceability/controller/AuthController.java:238` | `getUserInfo` | 未登录或未提供用户ID | 未判定 | (需人工判定) |
| 107 | `backend/src/main/java/com/foodtraceability/controller/AuthController.java:271` | `sendVerificationCode` | 发送验证码失败，请稍后重试 | 未判定 | (需人工判定) |
| 108 | `backend/src/main/java/com/foodtraceability/controller/AuthController.java:290` | `verifyForResetPassword` | 验证失败，请稍后重试 | 未判定 | (需人工判定) |
| 109 | `backend/src/main/java/com/foodtraceability/controller/AuthController.java:301` | `resetPasswordWithToken` | 两次输入的密码不一致 | 未判定 | (需人工判定) |
| 110 | `backend/src/main/java/com/foodtraceability/controller/AuthController.java:310` | `resetPasswordWithToken` | 重置密码失败，请稍后重试 | 未判定 | (需人工判定) |
| 111 | `backend/src/main/java/com/foodtraceability/controller/AuthController.java:337` | `resetLoginFailCount` | 重置失败计数失败 | 未判定 | (需人工判定) |
| 112 | `backend/src/main/java/com/foodtraceability/controller/BomCheckController.java:63` | `batchCheck` | 批量检查BOM库存失败 | 未判定 | (需人工判定) |
| 113 | `backend/src/main/java/com/foodtraceability/controller/BomCheckController.java:78` | `preOrderCheck` | 下单前预检查失败 | 未判定 | (需人工判定) |
| 114 | `backend/src/main/java/com/foodtraceability/controller/BomCheckController.java:91` | `getWarningConfig` | 获取BOM预警配置失败 | 未判定 | (需人工判定) |
| 115 | `backend/src/main/java/com/foodtraceability/controller/BomCheckController.java:104` | `saveWarningConfig` | 保存BOM预警配置失败 | 未判定 | (需人工判定) |
| 116 | `backend/src/main/java/com/foodtraceability/controller/BomCheckController.java:119` | `getAlternatives` | 获取替代菜品推荐失败 | 未判定 | (需人工判定) |
| 117 | `backend/src/main/java/com/foodtraceability/controller/BomCheckController.java:134` | `getStats` | 获取BOM检查统计失败 | 未判定 | (需人工判定) |
| 118 | `backend/src/main/java/com/foodtraceability/controller/BomCheckController.java:149` | `varianceAnalysis` | 差异分析失败： | 未判定 | (需人工判定) |
| 119 | `backend/src/main/java/com/foodtraceability/controller/BomCheckController.java:166` | `forecast` | 可做份数预测失败： | 未判定 | (需人工判定) |
| 120 | `backend/src/main/java/com/foodtraceability/controller/BomCheckController.java:181` | `checkDish` | 检查菜品BOM库存失败 | 未判定 | (需人工判定) |
| 121 | `backend/src/main/java/com/foodtraceability/controller/CategoryOperationLogController.java:44` | `getAllOperationLogs` | 获取操作日志失败 | 未判定 | (需人工判定) |
| 122 | `backend/src/main/java/com/foodtraceability/controller/CategoryOperationLogController.java:59` | `getOperationLogsByCategoryId` | 获取分类操作日志失败 | 未判定 | (需人工判定) |
| 123 | `backend/src/main/java/com/foodtraceability/controller/CategoryOperationLogController.java:78` | `getOperationLogsByType` | 获取操作日志失败 | 未判定 | (需人工判定) |
| 124 | `backend/src/main/java/com/foodtraceability/controller/CategoryOperationLogController.java:94` | `getOperationLogsPage` | 获取操作日志分页失败 | 未判定 | (需人工判定) |
| 125 | `backend/src/main/java/com/foodtraceability/controller/ContractTemplateController.java:44` | `createTemplate` | 模板编码已存在 | 未判定 | (需人工判定) |
| 126 | `backend/src/main/java/com/foodtraceability/controller/CustomerAnalysisController.java:44` | `getMemberGrowthTrend` | 获取会员增长趋势失败:  | 未判定 | (需人工判定) |
| 127 | `backend/src/main/java/com/foodtraceability/controller/CustomerAnalysisController.java:61` | `generateCustomerReport` | 生成客户报表失败:  | 未判定 | (需人工判定) |
| 128 | `backend/src/main/java/com/foodtraceability/controller/CustomerAnalysisController.java:77` | `getRetentionCohortAnalysis` | 获取留存队列分析失败:  | 未判定 | (需人工判定) |
| 129 | `backend/src/main/java/com/foodtraceability/controller/CustomerAnalysisController.java:93` | `getRFMDistributionSnapshot` | 获取RFM分布失败:  | 未判定 | (需人工判定) |
| 130 | `backend/src/main/java/com/foodtraceability/controller/CustomerAnalysisController.java:110` | `getMemberSegmentInsight` | 获取分群洞察失败:  | 未判定 | (需人工判定) |
| 131 | `backend/src/main/java/com/foodtraceability/controller/CustomerAnalysisController.java:127` | `getCLVForecast` | 获取CLV预测失败:  | 未判定 | (需人工判定) |
| 132 | `backend/src/main/java/com/foodtraceability/controller/CustomerAnalysisController.java:144` | `queryReports` | 查询客户报表失败:  | 未判定 | (需人工判定) |
| 133 | `backend/src/main/java/com/foodtraceability/controller/CustomerAnalysisController.java:161` | `getReportById` | 获取报表详情失败:  | 未判定 | (需人工判定) |
| 134 | `backend/src/main/java/com/foodtraceability/controller/DashboardController.java:37` | `getDashboardOverview` | 获取看板概览失败:  | 未判定 | (需人工判定) |
| 135 | `backend/src/main/java/com/foodtraceability/controller/DashboardController.java:51` | `getTodaySalesOverview` | 获取今日销售概览失败:  | 未判定 | (需人工判定) |
| 136 | `backend/src/main/java/com/foodtraceability/controller/DashboardController.java:65` | `getInventoryAlerts` | 获取库存预警信息失败:  | 未判定 | (需人工判定) |
| 137 | `backend/src/main/java/com/foodtraceability/controller/DashboardController.java:81` | `getMemberGrowthTrend` | 获取会员增长趋势失败:  | 未判定 | (需人工判定) |
| 138 | `backend/src/main/java/com/foodtraceability/controller/DashboardController.java:95` | `getFinanceSummary` | 获取财务概况失败:  | 未判定 | (需人工判定) |
| 139 | `backend/src/main/java/com/foodtraceability/controller/DashboardController.java:110` | `saveDashboardConfig` | 保存看板配置失败:  | 未判定 | (需人工判定) |
| 140 | `backend/src/main/java/com/foodtraceability/controller/DashboardController.java:131` | `getUserDashboardConfig` | 获取看板配置失败:  | 未判定 | (需人工判定) |
| 141 | `backend/src/main/java/com/foodtraceability/controller/DashboardController.java:146` | `queryDashboardConfigs` | 查询看板配置失败:  | 未判定 | (需人工判定) |
| 142 | `backend/src/main/java/com/foodtraceability/controller/DashboardController.java:165` | `deleteDashboardConfig` | 删除失败 | 未判定 | (需人工判定) |
| 143 | `backend/src/main/java/com/foodtraceability/controller/DashboardController.java:168` | `deleteDashboardConfig` | 删除看板配置失败:  | 未判定 | (需人工判定) |
| 144 | `backend/src/main/java/com/foodtraceability/controller/DepartmentController.java:276` | `batchGetDepartmentBasicInfo` | 批量获取部门基本信息失败:  | 未判定 | (需人工判定) |
| 145 | `backend/src/main/java/com/foodtraceability/controller/DepartmentController.java:290` | `getDepartmentBasicInfo` | 部门不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 146 | `backend/src/main/java/com/foodtraceability/controller/DepartmentController.java:294` | `getDepartmentBasicInfo` | 获取部门基本信息失败:  | 未判定 | (需人工判定) |
| 147 | `backend/src/main/java/com/foodtraceability/controller/device/DeviceAlertController.java:149` | `batchHandleAlerts` | 告警ID列表不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 148 | `backend/src/main/java/com/foodtraceability/controller/DeviceManagementController.java:238` | `getPrintTaskStatus` | 任务不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 149 | `backend/src/main/java/com/foodtraceability/controller/DiningTableManagementController.java:150` | `updateTableStatus` | 状态参数不能为空 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 150 | `backend/src/main/java/com/foodtraceability/controller/DiningTableManagementController.java:154` | `updateTableStatus` | 仅支持设置为空闲(1)、维护中(4)、停用(5) | 未判定 | (需人工判定) |
| 151 | `backend/src/main/java/com/foodtraceability/controller/DishCostController.java:61` | `getDishCostList` | 查询菜品成本列表失败 | 未判定 | (需人工判定) |
| 152 | `backend/src/main/java/com/foodtraceability/controller/DishCostController.java:74` | `recalculateAll` | 触发成本重算失败 | 未判定 | (需人工判定) |
| 153 | `backend/src/main/java/com/foodtraceability/controller/DishCostController.java:89` | `getCostSummary` | 获取成本汇总失败 | 未判定 | (需人工判定) |
| 154 | `backend/src/main/java/com/foodtraceability/controller/DishCostController.java:102` | `getAlertRules` | 获取成本预警规则失败 | 未判定 | (需人工判定) |
| 155 | `backend/src/main/java/com/foodtraceability/controller/DishCostController.java:115` | `saveAlertRule` | 保存成本预警规则失败 | 未判定 | (需人工判定) |
| 156 | `backend/src/main/java/com/foodtraceability/controller/DishCostController.java:132` | `toggleAlertRule` | 切换预警规则状态失败 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 157 | `backend/src/main/java/com/foodtraceability/controller/DishCostController.java:167` | `getDishCost` | 获取菜品成本快照失败 | 未判定 | (需人工判定) |
| 158 | `backend/src/main/java/com/foodtraceability/controller/DishCostController.java:182` | `getCostTrend` | 获取菜品成本趋势失败 | 未判定 | (需人工判定) |
| 159 | `backend/src/main/java/com/foodtraceability/controller/DishCostController.java:196` | `getBomDetail` | 获取菜品BOM明细失败 | 未判定 | (需人工判定) |
| 160 | `backend/src/main/java/com/foodtraceability/controller/DishCostController.java:214` | `updateBomQuantity` | 更新BOM项数量失败 | 未判定 | (需人工判定) |
| 161 | `backend/src/main/java/com/foodtraceability/controller/ElectronicSignatureController.java:78` | `uploadCompanySeal` | 印章数据不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 162 | `backend/src/main/java/com/foodtraceability/controller/ElectronicSignatureController.java:124` | `sendVerifyCode` | 合同ID不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 163 | `backend/src/main/java/com/foodtraceability/controller/EmployeeArchiveDetailController.java:42` | `createDetail` | 该档案已存在详情记录 | 未判定 | (需人工判定) |
| 164 | `backend/src/main/java/com/foodtraceability/controller/EmployeeController.java:52` | `getEmployees` | 获取员工列表失败 | 未判定 | (需人工判定) |
| 165 | `backend/src/main/java/com/foodtraceability/controller/EmployeeController.java:67` | `getEmployeeCount` | 获取员工总数失败 | 未判定 | (需人工判定) |
| 166 | `backend/src/main/java/com/foodtraceability/controller/EmployeeController.java:117` | `getEmployeeById` | 员工不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 167 | `backend/src/main/java/com/foodtraceability/controller/EmployeeController.java:122` | `getEmployeeById` | 获取员工详情失败 | 未判定 | (需人工判定) |
| 168 | `backend/src/main/java/com/foodtraceability/controller/EmployeeController.java:135` | `getEmployeesByDepartmentId` | 获取部门员工列表失败 | 未判定 | (需人工判定) |
| 169 | `backend/src/main/java/com/foodtraceability/controller/EmployeeController.java:148` | `getEmployeesByPositionId` | 获取职位员工列表失败 | 未判定 | (需人工判定) |
| 170 | `backend/src/main/java/com/foodtraceability/controller/EmployeeController.java:161` | `createEmployee` | 创建员工失败，请联系管理员 | 未判定 | (需人工判定) |
| 171 | `backend/src/main/java/com/foodtraceability/controller/EmployeeController.java:175` | `updateEmployee` | 更新员工失败，请联系管理员 | 未判定 | (需人工判定) |
| 172 | `backend/src/main/java/com/foodtraceability/controller/EmployeeController.java:218` | `deleteEmployee` | 删除员工失败 | 未判定 | (需人工判定) |
| 173 | `backend/src/main/java/com/foodtraceability/controller/EmployeeController.java:221` | `deleteEmployee` | 删除员工失败 | 未判定 | (需人工判定) |
| 174 | `backend/src/main/java/com/foodtraceability/controller/EmployeeController.java:234` | `batchUpdateDepartment` | 批量更新员工部门失败 | 未判定 | (需人工判定) |
| 175 | `backend/src/main/java/com/foodtraceability/controller/EmployeeController.java:237` | `batchUpdateDepartment` | 批量更新员工部门失败 | 未判定 | (需人工判定) |
| 176 | `backend/src/main/java/com/foodtraceability/controller/EmployeeController.java:250` | `batchUpdatePosition` | 批量更新员工职位失败 | 未判定 | (需人工判定) |
| 177 | `backend/src/main/java/com/foodtraceability/controller/EmployeeController.java:253` | `batchUpdatePosition` | 批量更新员工职位失败 | 未判定 | (需人工判定) |
| 178 | `backend/src/main/java/com/foodtraceability/controller/EmployeeController.java:266` | `getEmployeeCountByDepartmentId` | 获取部门员工数量失败 | 未判定 | (需人工判定) |
| 179 | `backend/src/main/java/com/foodtraceability/controller/EmployeeController.java:279` | `getEmployeeCountByPositionId` | 获取职位员工数量失败 | 未判定 | (需人工判定) |
| 180 | `backend/src/main/java/com/foodtraceability/controller/EmployeeController.java:292` | `batchGetEmployeeBasicInfo` | 批量获取员工基本信息失败 | 未判定 | (需人工判定) |
| 181 | `backend/src/main/java/com/foodtraceability/controller/EmployeeController.java:303` | `getEmployeeBasicInfo` | 员工不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 182 | `backend/src/main/java/com/foodtraceability/controller/EmployeeController.java:308` | `getEmployeeBasicInfo` | 获取员工基本信息失败 | 未判定 | (需人工判定) |
| 183 | `backend/src/main/java/com/foodtraceability/controller/EmployeeLaborContractController.java:132` | `createContract` | 请求体不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 184 | `backend/src/main/java/com/foodtraceability/controller/EmployeeLaborContractController.java:138` | `createContract` | 创建劳动合同失败 | 未判定 | (需人工判定) |
| 185 | `backend/src/main/java/com/foodtraceability/controller/EmployeeLaborContractController.java:146` | `createContract` | 创建劳动合同失败 | 未判定 | (需人工判定) |
| 186 | `backend/src/main/java/com/foodtraceability/controller/EmployeeLaborContractController.java:157` | `updateContract` | 请求体不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 187 | `backend/src/main/java/com/foodtraceability/controller/EmployeeLaborContractController.java:171` | `updateContract` | 更新劳动合同失败 | 未判定 | (需人工判定) |
| 188 | `backend/src/main/java/com/foodtraceability/controller/EmployeeLaborContractController.java:188` | `deleteContract` | 删除劳动合同失败 | 未判定 | (需人工判定) |
| 189 | `backend/src/main/java/com/foodtraceability/controller/EmployeeLaborContractController.java:221` | `signContract` | 签署合同失败 | 未判定 | (需人工判定) |
| 190 | `backend/src/main/java/com/foodtraceability/controller/EmployeeLaborContractController.java:242` | `extendContract` | 延长合同期限失败 | 未判定 | (需人工判定) |
| 191 | `backend/src/main/java/com/foodtraceability/controller/EmployeeLaborContractController.java:253` | `terminateContract` | 请提供终止原因 | 未判定 | (需人工判定) |
| 192 | `backend/src/main/java/com/foodtraceability/controller/EmployeeLaborContractController.java:257` | `terminateContract` | 终止合同失败 | 未判定 | (需人工判定) |
| 193 | `backend/src/main/java/com/foodtraceability/controller/EmployeeLaborContractController.java:310` | `saveDocument` | 请求体不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 194 | `backend/src/main/java/com/foodtraceability/controller/EmployeeLaborContractController.java:323` | `saveDocument` | 保存合同正文失败 | 未判定 | (需人工判定) |
| 195 | `backend/src/main/java/com/foodtraceability/controller/EmployeeLaborContractController.java:364` | `initSignatures` | 初始化签署记录失败 | 未判定 | (需人工判定) |
| 196 | `backend/src/main/java/com/foodtraceability/controller/EmployeeLaborContractController.java:375` | `signWithSignature` | 请求体不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 197 | `backend/src/main/java/com/foodtraceability/controller/EmployeeLaborContractController.java:385` | `signWithSignature` | 签署合同失败 | 未判定 | (需人工判定) |
| 198 | `backend/src/main/java/com/foodtraceability/controller/EmployeeLaborContractController.java:396` | `signWithSignature` | 签署合同失败 | 未判定 | (需人工判定) |
| 199 | `backend/src/main/java/com/foodtraceability/controller/EmployeeLaborContractController.java:407` | `rejectSignature` | 请求体不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 200 | `backend/src/main/java/com/foodtraceability/controller/EmployeeLaborContractController.java:415` | `rejectSignature` | 拒绝签署失败 | 未判定 | (需人工判定) |
| 201 | `backend/src/main/java/com/foodtraceability/controller/EmployeeLaborContractController.java:426` | `rejectSignature` | 拒绝签署失败 | 未判定 | (需人工判定) |
| 202 | `backend/src/main/java/com/foodtraceability/controller/EmployeeLaborContractController.java:445` | `sendVerifyCode` | 发送验证码失败 | 未判定 | (需人工判定) |
| 203 | `backend/src/main/java/com/foodtraceability/controller/finance/InvoiceVerifyController.java:32` | `verify` | 发票验真服务不可用 | 未判定 | (需人工判定) |
| 204 | `backend/src/main/java/com/foodtraceability/controller/finance/InvoiceVerifyController.java:35` | `verify` | 发票号码不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 205 | `backend/src/main/java/com/foodtraceability/controller/finance/InvoiceVerifyController.java:38` | `verify` | 开票日期不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 206 | `backend/src/main/java/com/foodtraceability/controller/finance/TaxCalculationController.java:60` | `calculateTax` | 不支持的税种类型:  | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 207 | `backend/src/main/java/com/foodtraceability/controller/finance/TaxCalculationController.java:115` | `generateReturn` | 税种和期间不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 208 | `backend/src/main/java/com/foodtraceability/controller/FoodTraceCodeController.java:44` | `generate` | 生成食品追溯码失败:  | 未判定 | (需人工判定) |
| 209 | `backend/src/main/java/com/foodtraceability/controller/FoodTraceCodeController.java:55` | `queryByTraceCode` | 追溯码不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 210 | `backend/src/main/java/com/foodtraceability/controller/FoodTraceCodeController.java:67` | `getTraceInfo` | 追溯码不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 211 | `backend/src/main/java/com/foodtraceability/controller/FoodTraceCodeController.java:124` | `updateMakeStatus` | 更新状态失败:  | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 212 | `backend/src/main/java/com/foodtraceability/controller/FoodTraceCodeController.java:139` | `startMake` | 开始制作失败:  | 未判定 | (需人工判定) |
| 213 | `backend/src/main/java/com/foodtraceability/controller/FoodTraceCodeController.java:152` | `completeMake` | 完成制作失败:  | 未判定 | (需人工判定) |
| 214 | `backend/src/main/java/com/foodtraceability/controller/FoodTraceCodeController.java:165` | `serve` | 出餐失败:  | 未判定 | (需人工判定) |
| 215 | `backend/src/main/java/com/foodtraceability/controller/FoodTraceCodeController.java:179` | `printLabel` | 打印失败:  | 未判定 | (需人工判定) |
| 216 | `backend/src/main/java/com/foodtraceability/controller/FoodTraceCodeController.java:193` | `batchPrint` | 打印失败:  | 未判定 | (需人工判定) |
| 217 | `backend/src/main/java/com/foodtraceability/controller/h5/MiniProgramController.java:27` | `getOpenId` | 微信小程序登录服务未配置，请联系管理员配置 AppID/AppSecret | 未判定 | (需人工判定) |
| 218 | `backend/src/main/java/com/foodtraceability/controller/h5/OnboardingArchiveController.java:44` | `updateArchive` | 档案不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 219 | `backend/src/main/java/com/foodtraceability/controller/h5/OnboardingArchiveController.java:61` | `deleteArchive` | 删除失败 | 未判定 | (需人工判定) |
| 220 | `backend/src/main/java/com/foodtraceability/controller/h5/OnboardingArchiveController.java:72` | `getArchiveById` | 档案不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 221 | `backend/src/main/java/com/foodtraceability/controller/h5/OnboardingArchiveController.java:133` | `submitForApproval` | 提交失败，请检查档案状态 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 222 | `backend/src/main/java/com/foodtraceability/controller/h5/OnboardingArchiveController.java:137` | `submitForApproval` | 提交审批失败： | 未判定 | (需人工判定) |
| 223 | `backend/src/main/java/com/foodtraceability/controller/h5/OnboardingInvitationController.java:68` | `generateInvitationCode` | 生成邀请码失败，请检查档案状态 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 224 | `backend/src/main/java/com/foodtraceability/controller/h5/OnboardingInvitationController.java:72` | `generateInvitationCode` | 生成邀请码失败： | 未判定 | (需人工判定) |
| 225 | `backend/src/main/java/com/foodtraceability/controller/h5/OnboardingInvitationController.java:86` | `getInvitationCode` | 邀请码不存在或已失效 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 226 | `backend/src/main/java/com/foodtraceability/controller/h5/OnboardingInvitationController.java:90` | `getInvitationCode` | 邀请码验证失败，请稍后重试 | 未判定 | (需人工判定) |
| 227 | `backend/src/main/java/com/foodtraceability/controller/h5/OnboardingInvitationController.java:104` | `validateInvitationCode` | 邀请码验证失败 | 未判定 | (需人工判定) |
| 228 | `backend/src/main/java/com/foodtraceability/controller/h5/OnboardingInvitationController.java:118` | `useInvitationCode` | 邀请码使用失败 | 未判定 | (需人工判定) |
| 229 | `backend/src/main/java/com/foodtraceability/controller/h5/OnboardingInvitationController.java:122` | `useInvitationCode` | 邀请码使用失败 | 未判定 | (需人工判定) |
| 230 | `backend/src/main/java/com/foodtraceability/controller/h5/OnboardingInvitationController.java:134` | `extendExpiration` | 邀请码不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 231 | `backend/src/main/java/com/foodtraceability/controller/h5/OnboardingInvitationController.java:141` | `extendExpiration` | 延长有效期失败 | 未判定 | (需人工判定) |
| 232 | `backend/src/main/java/com/foodtraceability/controller/h5/OnboardingInvitationController.java:153` | `revokeInvitationCode` | 邀请码不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 233 | `backend/src/main/java/com/foodtraceability/controller/h5/OnboardingInvitationController.java:160` | `revokeInvitationCode` | 撤销邀请码失败 | 未判定 | (需人工判定) |
| 234 | `backend/src/main/java/com/foodtraceability/controller/hardware/DeviceCommunicationTestController.java:45` | `testCommunication` | 测试设备通信失败:  | 未判定 | (需人工判定) |
| 235 | `backend/src/main/java/com/foodtraceability/controller/hardware/DeviceCommunicationTestController.java:62` | `testPrinterCommunication` | 测试打印机通信失败:  | 未判定 | (需人工判定) |
| 236 | `backend/src/main/java/com/foodtraceability/controller/hardware/DeviceCommunicationTestController.java:79` | `testScannerCommunication` | 测试扫码枪通信失败:  | 未判定 | (需人工判定) |
| 237 | `backend/src/main/java/com/foodtraceability/controller/hardware/DeviceCommunicationTestController.java:96` | `testScaleCommunication` | 测试电子秤通信失败:  | 未判定 | (需人工判定) |
| 238 | `backend/src/main/java/com/foodtraceability/controller/hardware/DeviceCommunicationTestController.java:113` | `testCameraCommunication` | 测试摄像头通信失败:  | 未判定 | (需人工判定) |
| 239 | `backend/src/main/java/com/foodtraceability/controller/hardware/DeviceCommunicationTestController.java:130` | `testKdsCommunication` | 测试KDS通信失败:  | 未判定 | (需人工判定) |
| 240 | `backend/src/main/java/com/foodtraceability/controller/hardware/DeviceCommunicationTestController.java:147` | `sendTestPrint` | 发送测试打印失败:  | 未判定 | (需人工判定) |
| 241 | `backend/src/main/java/com/foodtraceability/controller/hardware/DeviceCommunicationTestController.java:164` | `sendTestScan` | 发送测试扫描失败:  | 未判定 | (需人工判定) |
| 242 | `backend/src/main/java/com/foodtraceability/controller/hardware/DeviceCommunicationTestController.java:181` | `sendTestWeigh` | 发送测试称重失败:  | 未判定 | (需人工判定) |
| 243 | `backend/src/main/java/com/foodtraceability/controller/hardware/DeviceCommunicationTestController.java:198` | `getDiagnostics` | 获取设备诊断信息失败:  | 未判定 | (需人工判定) |
| 244 | `backend/src/main/java/com/foodtraceability/controller/hardware/DeviceCommunicationTestController.java:221` | `quickTestNetworkDevice` | 快速测试失败:  | 未判定 | (需人工判定) |
| 245 | `backend/src/main/java/com/foodtraceability/controller/hardware/DeviceSimulatorController.java:62` | `startAllSimulators` | 启动设备模拟器失败:  | 未判定 | (需人工判定) |
| 246 | `backend/src/main/java/com/foodtraceability/controller/hardware/DeviceSimulatorController.java:84` | `stopAllSimulators` | 停止设备模拟器失败:  | 未判定 | (需人工判定) |
| 247 | `backend/src/main/java/com/foodtraceability/controller/hardware/DeviceSimulatorController.java:103` | `getSimulatorStatus` | 获取模拟设备状态失败:  | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 248 | `backend/src/main/java/com/foodtraceability/controller/hardware/DeviceSimulatorController.java:127` | `startPrinterSimulator` | 启动打印机模拟器失败:  | 未判定 | (需人工判定) |
| 249 | `backend/src/main/java/com/foodtraceability/controller/hardware/DeviceSimulatorController.java:151` | `startScannerSimulator` | 启动扫码枪模拟器失败:  | 未判定 | (需人工判定) |
| 250 | `backend/src/main/java/com/foodtraceability/controller/hardware/DeviceSimulatorController.java:175` | `startScaleSimulator` | 启动电子秤模拟器失败:  | 未判定 | (需人工判定) |
| 251 | `backend/src/main/java/com/foodtraceability/controller/hardware/DeviceSimulatorController.java:199` | `startKdsSimulator` | 启动KDS模拟器失败:  | 未判定 | (需人工判定) |
| 252 | `backend/src/main/java/com/foodtraceability/controller/hardware/EpsonPrinterTestController.java:388` | `printLabels` | 未找到打印机，请检查打印机是否已连接并安装驱动 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 253 | `backend/src/main/java/com/foodtraceability/controller/hardware/EpsonPrinterTestController.java:430` | `printLabels` | 打印失败:  | 未判定 | (需人工判定) |
| 254 | `backend/src/main/java/com/foodtraceability/controller/hardware/EpsonPrinterTestController.java:535` | `printSingleLabel` | 未找到打印机 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 255 | `backend/src/main/java/com/foodtraceability/controller/hardware/EpsonPrinterTestController.java:550` | `printSingleLabel` | 打印失败:  | 未判定 | (需人工判定) |
| 256 | `backend/src/main/java/com/foodtraceability/controller/hardware/HardwareInitController.java:65` | `initHardwareDatabase` | 数据库初始化失败:  | 未判定 | (需人工判定) |
| 257 | `backend/src/main/java/com/foodtraceability/controller/hardware/LabelPrinterController.java:155` | `discoverPrinters` | 发现打印机失败:  | 未判定 | (需人工判定) |
| 258 | `backend/src/main/java/com/foodtraceability/controller/hardware/LabelPrinterController.java:261` | `printTraceLabel` | 追溯码不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 259 | `backend/src/main/java/com/foodtraceability/controller/hardware/LabelPrinterController.java:297` | `batchPrint` | 标签列表不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 260 | `backend/src/main/java/com/foodtraceability/controller/hardware/LabelPrinterController.java:312` | `batchPrint` | 模板不存在:  | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 261 | `backend/src/main/java/com/foodtraceability/controller/hardware/LabelPrinterController.java:678` | `getStatus` | 获取打印机状态失败:  | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 262 | `backend/src/main/java/com/foodtraceability/controller/hardware/LabelPrinterController.java:695` | `printCustom` | TSPL 指令不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 263 | `backend/src/main/java/com/foodtraceability/controller/hardware/SdkPrinterController.java:42` | `printLabel` | 打印失败 | 未判定 | (需人工判定) |
| 264 | `backend/src/main/java/com/foodtraceability/controller/HealthCertificateController.java:112` | `getHealthCertificateById` | 健康证不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 265 | `backend/src/main/java/com/foodtraceability/controller/HealthCertificateController.java:124` | `getHealthCertificateByEmployeeId` | 健康证不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 266 | `backend/src/main/java/com/foodtraceability/controller/HealthCertificateController.java:148` | `addHealthCertificate` | 健康证添加失败 | 未判定 | (需人工判定) |
| 267 | `backend/src/main/java/com/foodtraceability/controller/HealthCertificateController.java:160` | `updateHealthCertificate` | 健康证更新失败 | 未判定 | (需人工判定) |
| 268 | `backend/src/main/java/com/foodtraceability/controller/HealthCertificateController.java:172` | `deleteHealthCertificate` | 健康证删除失败 | 未判定 | (需人工判定) |
| 269 | `backend/src/main/java/com/foodtraceability/controller/HealthCertificateController.java:184` | `batchDeleteHealthCertificate` | 健康证批量删除失败 | 未判定 | (需人工判定) |
| 270 | `backend/src/main/java/com/foodtraceability/controller/HealthCertificateController.java:197` | `updateHealthCertificateStatus` | 健康证状态更新失败 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 271 | `backend/src/main/java/com/foodtraceability/controller/HealthCertificateController.java:210` | `batchUpdateHealthCertificateStatus` | 健康证状态批量更新失败 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 272 | `backend/src/main/java/com/foodtraceability/controller/HealthCertificateController.java:233` | `sendExpiryReminders` | 健康证到期提醒发送失败 | 未判定 | (需人工判定) |
| 273 | `backend/src/main/java/com/foodtraceability/controller/HealthCertificateController.java:259` | `addHealthCertificateExpense` | 只有通过审核的健康证才能申请报销 | 未判定 | (需人工判定) |
| 274 | `backend/src/main/java/com/foodtraceability/controller/HealthCertificateController.java:263` | `addHealthCertificateExpense` | 健康证报销记录添加失败 | 未判定 | (需人工判定) |
| 275 | `backend/src/main/java/com/foodtraceability/controller/HealthCertificateController.java:276` | `updateHealthCertificateExpenseStatus` | 健康证报销状态更新失败 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 276 | `backend/src/main/java/com/foodtraceability/controller/HealthCertificateExpenseController.java:77` | `getExpenseById` | 报销记录不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 277 | `backend/src/main/java/com/foodtraceability/controller/HealthCertificateExpenseController.java:110` | `applyExpense` | 报销申请提交失败 | 未判定 | (需人工判定) |
| 278 | `backend/src/main/java/com/foodtraceability/controller/HealthCertificateExpenseController.java:121` | `updateExpense` | 报销记录更新失败 | 未判定 | (需人工判定) |
| 279 | `backend/src/main/java/com/foodtraceability/controller/HealthCertificateExpenseController.java:132` | `deleteExpense` | 报销记录删除失败 | 未判定 | (需人工判定) |
| 280 | `backend/src/main/java/com/foodtraceability/controller/HealthCertificateExpenseController.java:143` | `batchDeleteExpense` | 报销记录批量删除失败 | 未判定 | (需人工判定) |
| 281 | `backend/src/main/java/com/foodtraceability/controller/HealthCertificateExpenseController.java:162` | `hrReviewExpense` | 人事初审失败 | 未判定 | (需人工判定) |
| 282 | `backend/src/main/java/com/foodtraceability/controller/HealthCertificateExpenseController.java:181` | `financeReviewExpense` | 财务审核失败 | 未判定 | (需人工判定) |
| 283 | `backend/src/main/java/com/foodtraceability/controller/HealthCertificateExpenseController.java:193` | `reimburseExpense` | 财务报销处理失败 | 未判定 | (需人工判定) |
| 284 | `backend/src/main/java/com/foodtraceability/controller/HealthCertificateExpenseController.java:210` | `batchRejectExpense` | 报销申请批量拒绝失败 | 未判定 | (需人工判定) |
| 285 | `backend/src/main/java/com/foodtraceability/controller/HealthCertificateExpenseController.java:225` | `uploadInvoiceAttachment` | 发票附件上传失败 | 未判定 | (需人工判定) |
| 286 | `backend/src/main/java/com/foodtraceability/controller/HrAnalyticsController.java:54` | `getOverview` | 获取人事概览数据失败 | 未判定 | (需人工判定) |
| 287 | `backend/src/main/java/com/foodtraceability/controller/HrAnalyticsController.java:72` | `getDepartmentStats` | 获取部门人事统计失败 | 未判定 | (需人工判定) |
| 288 | `backend/src/main/java/com/foodtraceability/controller/HrAnalyticsController.java:86` | `getTurnoverRate` | 获取员工流动率分析失败 | 未判定 | (需人工判定) |
| 289 | `backend/src/main/java/com/foodtraceability/controller/HrAnalyticsController.java:100` | `getPerformance` | 获取绩效分析数据失败 | 未判定 | (需人工判定) |
| 290 | `backend/src/main/java/com/foodtraceability/controller/HrAnalyticsController.java:114` | `getTrainingEffect` | 获取培训效果分析数据失败 | 未判定 | (需人工判定) |
| 291 | `backend/src/main/java/com/foodtraceability/controller/HrAnalyticsController.java:128` | `getRecruitmentFunnel` | 获取招聘漏斗数据失败 | 未判定 | (需人工判定) |
| 292 | `backend/src/main/java/com/foodtraceability/controller/HrAnalyticsController.java:142` | `getAttendance` | 获取考勤分析数据失败 | 未判定 | (需人工判定) |
| 293 | `backend/src/main/java/com/foodtraceability/controller/HrAnalyticsController.java:156` | `getSalaryDistribution` | 获取薪资分布分析数据失败 | 未判定 | (需人工判定) |
| 294 | `backend/src/main/java/com/foodtraceability/controller/integration/FileAttachmentController.java:62` | `uploadFile` | 上传文件失败 | 未判定 | (需人工判定) |
| 295 | `backend/src/main/java/com/foodtraceability/controller/integration/FileAttachmentController.java:82` | `uploadFiles` | 批量上传失败 | 未判定 | (需人工判定) |
| 296 | `backend/src/main/java/com/foodtraceability/controller/integration/FileAttachmentController.java:126` | `getFileInfo` | 获取文件信息失败 | 未判定 | (需人工判定) |
| 297 | `backend/src/main/java/com/foodtraceability/controller/integration/FileAttachmentController.java:160` | `getFileList` | 查询文件列表失败 | 未判定 | (需人工判定) |
| 298 | `backend/src/main/java/com/foodtraceability/controller/integration/FileAttachmentController.java:194` | `deleteFile` | 删除失败 | 未判定 | (需人工判定) |
| 299 | `backend/src/main/java/com/foodtraceability/controller/integration/FileAttachmentController.java:213` | `deleteFiles` | 批量删除失败 | 未判定 | (需人工判定) |
| 300 | `backend/src/main/java/com/foodtraceability/controller/integration/FileAttachmentController.java:218` | `deleteFiles` | 批量删除失败 | 未判定 | (需人工判定) |
| 301 | `backend/src/main/java/com/foodtraceability/controller/integration/FileChunkController.java:55` | `initUpload` | 初始化失败 | 未判定 | (需人工判定) |
| 302 | `backend/src/main/java/com/foodtraceability/controller/integration/FileChunkController.java:71` | `uploadChunk` | 分片上传失败 | 未判定 | (需人工判定) |
| 303 | `backend/src/main/java/com/foodtraceability/controller/integration/FileChunkController.java:97` | `mergeChunks` | 合并失败:  | 未判定 | (需人工判定) |
| 304 | `backend/src/main/java/com/foodtraceability/controller/integration/UploadController.java:39` | `uploadImage` | 图片上传失败:  | 未判定 | (需人工判定) |
| 305 | `backend/src/main/java/com/foodtraceability/controller/integration/UploadController.java:55` | `deleteImage` | 图片不存在或删除失败 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 306 | `backend/src/main/java/com/foodtraceability/controller/integration/UploadController.java:59` | `deleteImage` | 图片删除失败:  | 未判定 | (需人工判定) |
| 307 | `backend/src/main/java/com/foodtraceability/controller/integration/WeComController.java:49` | `oauthLogin` | 企微登录失败:  | 未判定 | (需人工判定) |
| 308 | `backend/src/main/java/com/foodtraceability/controller/integration/WeComController.java:68` | `sendTextMessage` | 参数不完整 | 未判定 | (需人工判定) |
| 309 | `backend/src/main/java/com/foodtraceability/controller/integration/WeComController.java:79` | `sendTemplateCard` | 参数不完整 | 未判定 | (需人工判定) |
| 310 | `backend/src/main/java/com/foodtraceability/controller/integration/WeComController.java:90` | `getAuthUrl` | 企业微信未配置 | 未判定 | (需人工判定) |
| 311 | `backend/src/main/java/com/foodtraceability/controller/InventoryAnalysisController.java:39` | `getInventoryOverview` | 获取库存概览失败:  | 未判定 | (需人工判定) |
| 312 | `backend/src/main/java/com/foodtraceability/controller/InventoryAnalysisController.java:55` | `generateInventoryReport` | 生成库存报表失败:  | 未判定 | (需人工判定) |
| 313 | `backend/src/main/java/com/foodtraceability/controller/InventoryAnalysisController.java:76` | `getInventoryTurnoverAnalysis` | 获取周转率分析失败:  | 未判定 | (需人工判定) |
| 314 | `backend/src/main/java/com/foodtraceability/controller/InventoryAnalysisController.java:90` | `getABCClassification` | 获取ABC分类失败:  | 未判定 | (需人工判定) |
| 315 | `backend/src/main/java/com/foodtraceability/controller/InventoryAnalysisController.java:112` | `getWasteAnalysis` | 获取报损分析失败:  | 未判定 | (需人工判定) |
| 316 | `backend/src/main/java/com/foodtraceability/controller/InventoryAnalysisController.java:126` | `getStockoutRiskList` | 获取缺货风险列表失败:  | 未判定 | (需人工判定) |
| 317 | `backend/src/main/java/com/foodtraceability/controller/InventoryAnalysisController.java:140` | `getInventoryValueByWarehouse` | 获取仓库库存价值失败:  | 未判定 | (需人工判定) |
| 318 | `backend/src/main/java/com/foodtraceability/controller/InventoryAnalysisController.java:155` | `queryReports` | 查询库存报表失败:  | 未判定 | (需人工判定) |
| 319 | `backend/src/main/java/com/foodtraceability/controller/InventoryAnalysisController.java:171` | `getReportById` | 获取报表详情失败:  | 未判定 | (需人工判定) |
| 320 | `backend/src/main/java/com/foodtraceability/controller/InventoryDeductionController.java:28` | `deductByTraceCode` | 按追溯码扣减失败:  | 未判定 | (需人工判定) |
| 321 | `backend/src/main/java/com/foodtraceability/controller/InventoryDeductionController.java:40` | `deductByRecipe` | 按配方扣减失败:  | 未判定 | (需人工判定) |
| 322 | `backend/src/main/java/com/foodtraceability/controller/InventoryDeductionController.java:52` | `deductByBatchCode` | 按批次扣减失败:  | 未判定 | (需人工判定) |
| 323 | `backend/src/main/java/com/foodtraceability/controller/InventoryDeductionController.java:64` | `deductByMaterialName` | 按原料名称扣减失败:  | 未判定 | (需人工判定) |
| 324 | `backend/src/main/java/com/foodtraceability/controller/InventoryDeductionController.java:74` | `getDeductionRecord` | 未找到扣减记录 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 325 | `backend/src/main/java/com/foodtraceability/controller/InventoryDeductionController.java:79` | `getDeductionRecord` | 获取扣减记录失败:  | 未判定 | (需人工判定) |
| 326 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenOrderController.java:66` | `create` | 创建后厨订单失败:  | 未判定 | (需人工判定) |
| 327 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenOrderController.java:94` | `getById` | 后厨订单不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 328 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenOrderController.java:104` | `getFullById` | 后厨订单不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 329 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenOrderController.java:202` | `getByOrderId` | 后厨订单不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 330 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenOrderController.java:214` | `receiveOrder` | 订单不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 331 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenOrderController.java:224` | `receiveOrder` | 接单失败:  | 未判定 | (需人工判定) |
| 332 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenOrderController.java:235` | `startMake` | 订单不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 333 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenOrderController.java:245` | `startMake` | 开始制作失败:  | 未判定 | (需人工判定) |
| 334 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenOrderController.java:256` | `completeMake` | 订单不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 335 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenOrderController.java:270` | `completeMake` | 完成制作失败:  | 未判定 | (需人工判定) |
| 336 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenOrderController.java:314` | `serve` | 订单不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 337 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenOrderController.java:324` | `serve` | 出餐失败:  | 未判定 | (需人工判定) |
| 338 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenOrderController.java:335` | `cancel` | 订单不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 339 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenOrderController.java:345` | `cancel` | 取消失败:  | 未判定 | (需人工判定) |
| 340 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenOrderController.java:357` | `scanConsumeMaterial` | 扫码消耗失败:  | 未判定 | (需人工判定) |
| 341 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenOrderController.java:383` | `updatePriority` | 更新优先级失败:  | 未判定 | (需人工判定) |
| 342 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenOrderController.java:432` | `getSortedOrders` | 查询失败:  | 未判定 | (需人工判定) |
| 343 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenOrderController.java:448` | `batchUpdateStatus` | 批量更新失败:  | 未判定 | (需人工判定) |
| 344 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenOrderController.java:461` | `getKitchenStats` | 获取统计数据失败:  | 未判定 | (需人工判定) |
| 345 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenOrderController.java:474` | `getOverdueOrders` | 查询超时订单失败:  | 未判定 | (需人工判定) |
| 346 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenOrderController.java:487` | `getOrdersGroupedByFood` | 查询聚合订单失败:  | 未判定 | (需人工判定) |
| 347 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenScanController.java:60` | `scanTraceCode` | 扫描失败:  | 未判定 | (需人工判定) |
| 348 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenScanController.java:73` | `scanTraceCodeWithQuantity` | 扫描失败:  | 未判定 | (需人工判定) |
| 349 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenScanController.java:84` | `scanFoodTraceCode` | 食品追溯码不存在:  | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 350 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenScanController.java:87` | `scanFoodTraceCode` | 该餐品已出餐，请勿重复操作 | 未判定 | (需人工判定) |
| 351 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenScanController.java:92` | `scanFoodTraceCode` | 关联订单不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 352 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenScanController.java:100` | `scanFoodTraceCode` | 订单尚未完成制作，无法出餐 | 未判定 | (需人工判定) |
| 353 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenScanController.java:118` | `scanFoodTraceCode` | 扫描失败:  | 未判定 | (需人工判定) |
| 354 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenScanController.java:189` | `getTraceCodeInfo` | 追溯码不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 355 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenScanController.java:215` | `getKitchenStats` | 获取统计数据失败:  | 未判定 | (需人工判定) |
| 356 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenScanController.java:229` | `getOverdueOrders` | 查询超时订单失败:  | 未判定 | (需人工判定) |
| 357 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenScanController.java:238` | `completeOrder` | 订单不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 358 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenScanController.java:241` | `completeOrder` | 只有制作中的订单可以完成 | 未判定 | (需人工判定) |
| 359 | `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenScanController.java:246` | `completeOrder` | 完成制作失败 | 未判定 | (需人工判定) |
| 360 | `backend/src/main/java/com/foodtraceability/controller/LabelTemplateController.java:95` | `getTemplateList` | 获取模板列表失败:  | 未判定 | (需人工判定) |
| 361 | `backend/src/main/java/com/foodtraceability/controller/LabelTemplateController.java:108` | `getTemplate` | 无效的模板ID格式 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 362 | `backend/src/main/java/com/foodtraceability/controller/LabelTemplateController.java:112` | `getTemplate` | 模板不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 363 | `backend/src/main/java/com/foodtraceability/controller/LabelTemplateController.java:117` | `getTemplate` | 获取模板详情失败 | 未判定 | (需人工判定) |
| 364 | `backend/src/main/java/com/foodtraceability/controller/LabelTemplateController.java:143` | `createTemplate` | 模板名称格式不正确 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 365 | `backend/src/main/java/com/foodtraceability/controller/LabelTemplateController.java:147` | `createTemplate` | 标签宽度必须在20-100mm之间 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 366 | `backend/src/main/java/com/foodtraceability/controller/LabelTemplateController.java:151` | `createTemplate` | 标签高度必须在20-100mm之间 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 367 | `backend/src/main/java/com/foodtraceability/controller/LabelTemplateController.java:190` | `createTemplate` | 创建模板失败 | 未判定 | (需人工判定) |
| 368 | `backend/src/main/java/com/foodtraceability/controller/LabelTemplateController.java:221` | `updateTemplate` | 模板不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 369 | `backend/src/main/java/com/foodtraceability/controller/LabelTemplateController.java:225` | `updateTemplate` | 模板已被其他用户修改,请刷新后重试 | 未判定 | (需人工判定) |
| 370 | `backend/src/main/java/com/foodtraceability/controller/LabelTemplateController.java:261` | `updateTemplate` | 更新失败,模板可能已被其他用户修改 | 未判定 | (需人工判定) |
| 371 | `backend/src/main/java/com/foodtraceability/controller/LabelTemplateController.java:269` | `updateTemplate` | 更新模板失败 | 未判定 | (需人工判定) |
| 372 | `backend/src/main/java/com/foodtraceability/controller/LabelTemplateController.java:283` | `deleteTemplate` | 无效的模板ID格式 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 373 | `backend/src/main/java/com/foodtraceability/controller/LabelTemplateController.java:287` | `deleteTemplate` | 模板不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 374 | `backend/src/main/java/com/foodtraceability/controller/LabelTemplateController.java:290` | `deleteTemplate` | 不能删除默认模板 | 未判定 | (需人工判定) |
| 375 | `backend/src/main/java/com/foodtraceability/controller/LabelTemplateController.java:297` | `deleteTemplate` | 删除模板失败:  | 未判定 | (需人工判定) |
| 376 | `backend/src/main/java/com/foodtraceability/controller/LabelTemplateController.java:310` | `setDefaultTemplate` | 模板不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 377 | `backend/src/main/java/com/foodtraceability/controller/LabelTemplateController.java:330` | `setDefaultTemplate` | 设置默认模板失败:  | 未判定 | (需人工判定) |
| 378 | `backend/src/main/java/com/foodtraceability/controller/LabelTemplateController.java:349` | `generatePreview` | 生成预览失败:  | 未判定 | (需人工判定) |
| 379 | `backend/src/main/java/com/foodtraceability/controller/LabelTemplateController.java:369` | `generatePreviewData` | 生成预览数据失败:  | 未判定 | (需人工判定) |
| 380 | `backend/src/main/java/com/foodtraceability/controller/LabelTemplateController.java:870` | `generateTspl` | 布局验证失败:  | 未判定 | (需人工判定) |
| 381 | `backend/src/main/java/com/foodtraceability/controller/LabelTemplateController.java:880` | `generateTspl` | 生成TSPL指令失败:  | 未判定 | (需人工判定) |
| 382 | `backend/src/main/java/com/foodtraceability/controller/LabelTemplateController.java:1155` | `getDefaultTemplate` | 获取默认模板失败:  | 未判定 | (需人工判定) |
| 383 | `backend/src/main/java/com/foodtraceability/controller/maintenance/DataBackupController.java:78` | `createBackup` | 创建备份失败:  | 未判定 | (需人工判定) |
| 384 | `backend/src/main/java/com/foodtraceability/controller/maintenance/DataBackupController.java:128` | `getBackupList` | 查询备份列表失败 | 未判定 | (需人工判定) |
| 385 | `backend/src/main/java/com/foodtraceability/controller/maintenance/DataBackupController.java:173` | `deleteBackup` | 删除失败 | 未判定 | (需人工判定) |
| 386 | `backend/src/main/java/com/foodtraceability/controller/maintenance/DataBackupController.java:242` | `restoreBackup` | 数据恢复失败:  | 未判定 | (需人工判定) |
| 387 | `backend/src/main/java/com/foodtraceability/controller/maintenance/DataBackupController.java:277` | `getRestoreLog` | 查询恢复日志失败 | 未判定 | (需人工判定) |
| 388 | `backend/src/main/java/com/foodtraceability/controller/maintenance/DataBackupController.java:301` | `rollbackRestore` | 回滚失败 | 未判定 | (需人工判定) |
| 389 | `backend/src/main/java/com/foodtraceability/controller/maintenance/DataBackupController.java:324` | `getStorageUsage` | 获取存储使用情况失败 | 未判定 | (需人工判定) |
| 390 | `backend/src/main/java/com/foodtraceability/controller/maintenance/DataBackupController.java:344` | `getStatistics` | 获取备份统计失败 | 未判定 | (需人工判定) |
| 391 | `backend/src/main/java/com/foodtraceability/controller/maintenance/DataBackupController.java:362` | `getDashboardData` | 获取仪表盘数据失败 | 未判定 | (需人工判定) |
| 392 | `backend/src/main/java/com/foodtraceability/controller/maintenance/DataBackupController.java:388` | `cleanupExpiredBackups` | 清理过期备份失败:  | 未判定 | (需人工判定) |
| 393 | `backend/src/main/java/com/foodtraceability/controller/maintenance/DatabaseCleanupController.java:46` | `cleanupAndRefreshKitchenOrders` | 刷新失败:  | 未判定 | (需人工判定) |
| 394 | `backend/src/main/java/com/foodtraceability/controller/maintenance/DatabaseCleanupController.java:69` | `cleanVoucherTestData` | 清理失败:  | 未判定 | (需人工判定) |
| 395 | `backend/src/main/java/com/foodtraceability/controller/maintenance/DataFixController.java:77` | `executeAllFixes` | 数据修复失败:  | 未判定 | (需人工判定) |
| 396 | `backend/src/main/java/com/foodtraceability/controller/maintenance/DataFixController.java:224` | `checkDataStatus` | 检查数据状态失败:  | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 397 | `backend/src/main/java/com/foodtraceability/controller/maintenance/DataFixController.java:302` | `fixTestData` | 修复测试数据失败:  | 未判定 | (需人工判定) |
| 398 | `backend/src/main/java/com/foodtraceability/controller/maintenance/DataFixController.java:339` | `createLaborContracts` | 创建劳动合同表失败:  | 未判定 | (需人工判定) |
| 399 | `backend/src/main/java/com/foodtraceability/controller/maintenance/EncodingFixController.java:68` | `fixDepartmentEncoding` | 修复失败:  | 未判定 | (需人工判定) |
| 400 | `backend/src/main/java/com/foodtraceability/controller/maintenance/EncodingFixController.java:109` | `fixEmployeeEncoding` | 修复失败:  | 未判定 | (需人工判定) |
| 401 | `backend/src/main/java/com/foodtraceability/controller/maintenance/EncodingFixController.java:130` | `fixAllEncoding` | 修复失败:  | 未判定 | (需人工判定) |
| 402 | `backend/src/main/java/com/foodtraceability/controller/maintenance/SystemInitController.java:165` | `initializeCompany` | 初始化失败:  | 未判定 | (需人工判定) |
| 403 | `backend/src/main/java/com/foodtraceability/controller/MaterialTemplateController.java:126` | `getByBarcode` | 未找到匹配的模板 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 404 | `backend/src/main/java/com/foodtraceability/controller/MaterialTraceCodeController.java:43` | `generate` | 生成原料追溯码失败:  | 未判定 | (需人工判定) |
| 405 | `backend/src/main/java/com/foodtraceability/controller/MaterialTraceCodeController.java:54` | `queryByTraceCode` | 追溯码不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 406 | `backend/src/main/java/com/foodtraceability/controller/MaterialTraceCodeController.java:113` | `updateStatus` | 更新状态失败:  | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 407 | `backend/src/main/java/com/foodtraceability/controller/MaterialTraceCodeController.java:129` | `pickTraceCode` | 领用失败:  | 未判定 | (需人工判定) |
| 408 | `backend/src/main/java/com/foodtraceability/controller/MaterialTraceCodeController.java:143` | `returnTraceCode` | 退回失败:  | 未判定 | (需人工判定) |
| 409 | `backend/src/main/java/com/foodtraceability/controller/MaterialTraceCodeController.java:172` | `markExpiredCodes` | 标记过期失败:  | 未判定 | (需人工判定) |
| 410 | `backend/src/main/java/com/foodtraceability/controller/MaterialTraceCodeController.java:195` | `batchPrint` | 打印失败:  | 未判定 | (需人工判定) |
| 411 | `backend/src/main/java/com/foodtraceability/controller/MaterialTraceCodeController.java:236` | `confirmStockin` | 确认入库失败：追溯码不存在或状态不正确 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 412 | `backend/src/main/java/com/foodtraceability/controller/MaterialTraceCodeController.java:239` | `confirmStockin` | 确认入库失败:  | 未判定 | (需人工判定) |
| 413 | `backend/src/main/java/com/foodtraceability/controller/MaterialTraceCodeController.java:253` | `batchConfirmStockin` | 批量确认入库失败:  | 未判定 | (需人工判定) |
| 414 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:102` | `getTemplateList` | 查询模板列表失败 | 未判定 | (需人工判定) |
| 415 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:133` | `createTemplate` | 创建模板失败 | 未判定 | (需人工判定) |
| 416 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:154` | `updateTemplate` | 更新模板失败 | 未判定 | (需人工判定) |
| 417 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:171` | `deleteTemplate` | 删除模板失败 | 未判定 | (需人工判定) |
| 418 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:191` | `enableTemplate` | 启用模板失败 | 未判定 | (需人工判定) |
| 419 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:211` | `disableTemplate` | 禁用模板失败 | 未判定 | (需人工判定) |
| 420 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:236` | `previewTemplate` | 预览模板失败 | 未判定 | (需人工判定) |
| 421 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:257` | `sendMessage` | 发送消息失败 | 未判定 | (需人工判定) |
| 422 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:276` | `sendBatchMessage` | 批量发送消息失败 | 未判定 | (需人工判定) |
| 423 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:305` | `getRecordList` | 查询发送记录失败 | 未判定 | (需人工判定) |
| 424 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:337` | `retryMessage` | 重试消息失败 | 未判定 | (需人工判定) |
| 425 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:360` | `updateSetting` | 更新设置失败 | 未判定 | (需人工判定) |
| 426 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:363` | `updateSetting` | 更新设置失败 | 未判定 | (需人工判定) |
| 427 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:389` | `getStatistics` | 获取统计数据失败 | 未判定 | (需人工判定) |
| 428 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:402` | `getDashboard` | 获取仪表盘数据失败 | 未判定 | (需人工判定) |
| 429 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:428` | `getTemplateVariables` | 获取模板变量失败 | 未判定 | (需人工判定) |
| 430 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:457` | `getMyNotifications` | 获取通知列表失败 | 未判定 | (需人工判定) |
| 431 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:474` | `getUnreadCount` | 获取未读数量失败 | 未判定 | (需人工判定) |
| 432 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:491` | `markNotificationAsRead` | 标记已读失败 | 未判定 | (需人工判定) |
| 433 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:506` | `markAllNotificationsAsRead` | 标记全部已读失败 | 未判定 | (需人工判定) |
| 434 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:523` | `deleteNotification` | 删除通知失败 | 未判定 | (需人工判定) |
| 435 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:541` | `batchDeleteNotifications` | 批量删除通知失败 | 未判定 | (需人工判定) |
| 436 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:562` | `getMyPreferences` | 获取通知偏好失败 | 未判定 | (需人工判定) |
| 437 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:579` | `updatePreferences` | 更新通知偏好失败 | 未判定 | (需人工判定) |
| 438 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:594` | `initPreferences` | 初始化通知偏好失败 | 未判定 | (需人工判定) |
| 439 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:636` | `triggerTestEvent` | 触发测试事件失败:  | 未判定 | (需人工判定) |
| 440 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:654` | `getDeadLetterPage` | 查询死信列表失败 | 未判定 | (需人工判定) |
| 441 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:674` | `getDeadLetterDetail` | 查询死信详情失败 | 未判定 | (需人工判定) |
| 442 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:697` | `retryDeadLetter` | 重试死信失败:  | 未判定 | (需人工判定) |
| 443 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:721` | `resolveDeadLetter` | 标记死信处理失败 | 未判定 | (需人工判定) |
| 444 | `backend/src/main/java/com/foodtraceability/controller/NotificationController.java:742` | `deleteDeadLetter` | 删除死信失败 | 未判定 | (需人工判定) |
| 445 | `backend/src/main/java/com/foodtraceability/controller/operations/DecisionBoardController.java:51` | `healthScores` | 获取门店健康度评分失败 | 未判定 | (需人工判定) |
| 446 | `backend/src/main/java/com/foodtraceability/controller/operations/DecisionBoardController.java:71` | `storeRanking` | 获取门店排名数据失败 | 未判定 | (需人工判定) |
| 447 | `backend/src/main/java/com/foodtraceability/controller/operations/DecisionBoardController.java:91` | `categorySales` | 获取品类销售分析数据失败 | 未判定 | (需人工判定) |
| 448 | `backend/src/main/java/com/foodtraceability/controller/operations/DecisionBoardController.java:111` | `insights` | 获取 AI 洞察建议失败 | 未判定 | (需人工判定) |
| 449 | `backend/src/main/java/com/foodtraceability/controller/operations/DecisionBoardController.java:133` | `revenueTrend` | 获取营收-成本-利润趋势数据失败 | 未判定 | (需人工判定) |
| 450 | `backend/src/main/java/com/foodtraceability/controller/operations/LiveMonitorController.java:52` | `overview` | 获取实时监控总览数据失败 | 未判定 | (需人工判定) |
| 451 | `backend/src/main/java/com/foodtraceability/controller/operations/LiveMonitorController.java:72` | `stores` | 获取门店实时状态列表失败 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 452 | `backend/src/main/java/com/foodtraceability/controller/operations/LiveMonitorController.java:95` | `trend` | 获取趋势数据失败 | 未判定 | (需人工判定) |
| 453 | `backend/src/main/java/com/foodtraceability/controller/operations/LiveMonitorController.java:115` | `alerts` | 获取实时告警列表失败 | 未判定 | (需人工判定) |
| 454 | `backend/src/main/java/com/foodtraceability/controller/OrderTrendsController.java:68` | `getDailyStats` | 获取按日期分组统计失败 | 未判定 | (需人工判定) |
| 455 | `backend/src/main/java/com/foodtraceability/controller/OverAgeWorkerController.java:68` | `getById` | 超龄劳动者记录不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 456 | `backend/src/main/java/com/foodtraceability/controller/OverAgeWorkerController.java:113` | `update` | 超龄劳动者记录不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 457 | `backend/src/main/java/com/foodtraceability/controller/OverAgeWorkerController.java:145` | `delete` | 删除失败，记录不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 458 | `backend/src/main/java/com/foodtraceability/controller/PasswordPolicyController.java:60` | `update` | 策略不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 459 | `backend/src/main/java/com/foodtraceability/controller/PasswordPolicyController.java:83` | `delete` | 策略不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 460 | `backend/src/main/java/com/foodtraceability/controller/PermissionTemplateController.java:67` | `getTemplateByCode` | 模板不存在:  | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 461 | `backend/src/main/java/com/foodtraceability/controller/PermissionTemplateController.java:80` | `getTemplateEntityByCode` | 模板不存在:  | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 462 | `backend/src/main/java/com/foodtraceability/controller/pos/CallNumberController.java:63` | `markPassed` | 叫号记录不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 463 | `backend/src/main/java/com/foodtraceability/controller/pos/CallNumberController.java:90` | `recallNumber` | 订单不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 464 | `backend/src/main/java/com/foodtraceability/controller/pos/CallNumberController.java:100` | `markPicked` | 订单不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 465 | `backend/src/main/java/com/foodtraceability/controller/pos/DiningTableController.java:44` | `getTableById` | 桌台不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 466 | `backend/src/main/java/com/foodtraceability/controller/pos/DiningTableController.java:53` | `getTableByNumber` | 桌台不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 467 | `backend/src/main/java/com/foodtraceability/controller/pos/DiningTableController.java:64` | `getTableByQrCode` | 无效的桌台二维码 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 468 | `backend/src/main/java/com/foodtraceability/controller/pos/DiningTableController.java:74` | `createTable` | 创建桌台失败:  | 未判定 | (需人工判定) |
| 469 | `backend/src/main/java/com/foodtraceability/controller/pos/DiningTableController.java:83` | `updateTable` | 更新桌台失败:  | 未判定 | (需人工判定) |
| 470 | `backend/src/main/java/com/foodtraceability/controller/pos/DiningTableController.java:93` | `deleteTable` | 删除桌台失败:  | 未判定 | (需人工判定) |
| 471 | `backend/src/main/java/com/foodtraceability/controller/pos/DiningTableController.java:105` | `occupyTable` | 占用桌台失败:  | 未判定 | (需人工判定) |
| 472 | `backend/src/main/java/com/foodtraceability/controller/pos/DiningTableController.java:114` | `releaseTable` | 释放桌台失败:  | 未判定 | (需人工判定) |
| 473 | `backend/src/main/java/com/foodtraceability/controller/pos/DiningTableController.java:125` | `transferTable` | 换台失败:  | 未判定 | (需人工判定) |
| 474 | `backend/src/main/java/com/foodtraceability/controller/pos/DiningTableController.java:134` | `regenerateQrCode` | 桌台不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 475 | `backend/src/main/java/com/foodtraceability/controller/pos/DiningTableController.java:140` | `regenerateQrCode` | 重新生成二维码失败:  | 未判定 | (需人工判定) |
| 476 | `backend/src/main/java/com/foodtraceability/controller/pos/MemberController.java:62` | `getMemberById` | 会员不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 477 | `backend/src/main/java/com/foodtraceability/controller/pos/MemberController.java:73` | `getMemberByPhone` | 会员不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 478 | `backend/src/main/java/com/foodtraceability/controller/pos/MemberController.java:84` | `getMemberByMemberNo` | 会员不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 479 | `backend/src/main/java/com/foodtraceability/controller/pos/MemberController.java:108` | `exportMembers` | 导出会员列表失败:  | 未判定 | (需人工判定) |
| 480 | `backend/src/main/java/com/foodtraceability/controller/pos/MemberController.java:124` | `getMemberStats` | 获取会员统计失败:  | 未判定 | (需人工判定) |
| 481 | `backend/src/main/java/com/foodtraceability/controller/pos/MemberController.java:140` | `getMemberOverview` | 获取会员概览失败:  | 未判定 | (需人工判定) |
| 482 | `backend/src/main/java/com/foodtraceability/controller/pos/MemberController.java:151` | `registerMember` | 注册失败:  | 未判定 | (需人工判定) |
| 483 | `backend/src/main/java/com/foodtraceability/controller/pos/MemberController.java:162` | `updateMember` | 更新失败:  | 未判定 | (需人工判定) |
| 484 | `backend/src/main/java/com/foodtraceability/controller/pos/MemberController.java:173` | `recharge` | 充值失败:  | 未判定 | (需人工判定) |
| 485 | `backend/src/main/java/com/foodtraceability/controller/pos/MemberController.java:184` | `consume` | 消费失败:  | 未判定 | (需人工判定) |
| 486 | `backend/src/main/java/com/foodtraceability/controller/pos/MemberController.java:196` | `deleteMember` | 删除失败:  | 未判定 | (需人工判定) |
| 487 | `backend/src/main/java/com/foodtraceability/controller/PositionController.java:44` | `generatePositionCode` | 生成职位编码失败:  | 未判定 | (需人工判定) |
| 488 | `backend/src/main/java/com/foodtraceability/controller/PositionController.java:60` | `generatePositionCodeSeq` | 生成职位编码序号失败:  | 未判定 | (需人工判定) |
| 489 | `backend/src/main/java/com/foodtraceability/controller/PositionController.java:96` | `getPosition` | 职位不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 490 | `backend/src/main/java/com/foodtraceability/controller/PositionController.java:185` | `batchGetPositionBasicInfo` | 批量获取职位基本信息失败:  | 未判定 | (需人工判定) |
| 491 | `backend/src/main/java/com/foodtraceability/controller/PositionController.java:196` | `getPositionBasicInfo` | 职位不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 492 | `backend/src/main/java/com/foodtraceability/controller/PositionController.java:201` | `getPositionBasicInfo` | 获取职位基本信息失败:  | 未判定 | (需人工判定) |
| 493 | `backend/src/main/java/com/foodtraceability/controller/PositionController.java:215` | `batchUpdatePositionCodes` | 批量更新职位编码失败:  | 未判定 | (需人工判定) |
| 494 | `backend/src/main/java/com/foodtraceability/controller/ProductAnalysisController.java:49` | `getProductProfitabilityAnalysis` | 获取盈利能力分析失败:  | 未判定 | (需人工判定) |
| 495 | `backend/src/main/java/com/foodtraceability/controller/ProductAnalysisController.java:69` | `generateProductReport` | 生成产品报表失败:  | 未判定 | (需人工判定) |
| 496 | `backend/src/main/java/com/foodtraceability/controller/ProductAnalysisController.java:84` | `getMenuEngineeringData` | 获取菜单工程数据失败:  | 未判定 | (需人工判定) |
| 497 | `backend/src/main/java/com/foodtraceability/controller/ProductAnalysisController.java:103` | `getComboPerformance` | 获取套餐表现失败:  | 未判定 | (需人工判定) |
| 498 | `backend/src/main/java/com/foodtraceability/controller/ProductAnalysisController.java:124` | `getPriceElasticity` | 获取价格弹性分析失败:  | 未判定 | (需人工判定) |
| 499 | `backend/src/main/java/com/foodtraceability/controller/ProductAnalysisController.java:139` | `getCustomerPreferenceAnalysis` | 获取客户口味偏好失败:  | 未判定 | (需人工判定) |
| 500 | `backend/src/main/java/com/foodtraceability/controller/ProductAnalysisController.java:155` | `queryReports` | 查询产品报表失败:  | 未判定 | (需人工判定) |
| 501 | `backend/src/main/java/com/foodtraceability/controller/ProductAnalysisController.java:171` | `getReportById` | 获取报表详情失败:  | 未判定 | (需人工判定) |
| 502 | `backend/src/main/java/com/foodtraceability/controller/purchase/MaterialCategoryController.java:67` | `getPage` | 查询失败:  | 未判定 | (需人工判定) |
| 503 | `backend/src/main/java/com/foodtraceability/controller/purchase/MaterialCategoryController.java:82` | `getEnabledList` | 查询失败:  | 未判定 | (需人工判定) |
| 504 | `backend/src/main/java/com/foodtraceability/controller/purchase/MaterialCategoryController.java:97` | `getAllList` | 查询失败:  | 未判定 | (需人工判定) |
| 505 | `backend/src/main/java/com/foodtraceability/controller/purchase/MaterialCategoryController.java:112` | `getById` | 商品分类不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 506 | `backend/src/main/java/com/foodtraceability/controller/purchase/MaterialCategoryController.java:117` | `getById` | 查询失败:  | 未判定 | (需人工判定) |
| 507 | `backend/src/main/java/com/foodtraceability/controller/purchase/MaterialCategoryController.java:135` | `create` | 创建失败:  | 未判定 | (需人工判定) |
| 508 | `backend/src/main/java/com/foodtraceability/controller/purchase/MaterialCategoryController.java:155` | `update` | 更新失败:  | 未判定 | (需人工判定) |
| 509 | `backend/src/main/java/com/foodtraceability/controller/purchase/MaterialCategoryController.java:175` | `delete` | 删除失败:  | 未判定 | (需人工判定) |
| 510 | `backend/src/main/java/com/foodtraceability/controller/purchase/MaterialCategoryController.java:191` | `updateStatus` | 无效的状态值:  | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 511 | `backend/src/main/java/com/foodtraceability/controller/purchase/MaterialCategoryController.java:199` | `updateStatus` | 操作失败:  | 未判定 | (需人工判定) |
| 512 | `backend/src/main/java/com/foodtraceability/controller/purchase/ProcurementTraceController.java:66` | `getTraceByOrderNo` | 未找到采购溯源信息 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 513 | `backend/src/main/java/com/foodtraceability/controller/purchase/ProcurementTraceController.java:103` | `getTraceByStockinCode` | 未找到采购溯源信息 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 514 | `backend/src/main/java/com/foodtraceability/controller/purchase/PurchaseArchiveController.java:73` | `getList` | 查询失败:  | 未判定 | (需人工判定) |
| 515 | `backend/src/main/java/com/foodtraceability/controller/purchase/PurchaseArchiveController.java:89` | `getById` | 商品档案不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 516 | `backend/src/main/java/com/foodtraceability/controller/purchase/PurchaseArchiveController.java:94` | `getById` | 查询失败:  | 未判定 | (需人工判定) |
| 517 | `backend/src/main/java/com/foodtraceability/controller/purchase/PurchaseArchiveController.java:111` | `create` | 创建失败:  | 未判定 | (需人工判定) |
| 518 | `backend/src/main/java/com/foodtraceability/controller/purchase/PurchaseArchiveController.java:130` | `update` | 更新失败:  | 未判定 | (需人工判定) |
| 519 | `backend/src/main/java/com/foodtraceability/controller/purchase/PurchaseArchiveController.java:148` | `delete` | 删除失败:  | 未判定 | (需人工判定) |
| 520 | `backend/src/main/java/com/foodtraceability/controller/purchase/PurchaseArchiveController.java:165` | `updateStatus` | 无效的状态值:  | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 521 | `backend/src/main/java/com/foodtraceability/controller/purchase/PurchaseArchiveController.java:171` | `updateStatus` | 操作失败:  | 未判定 | (需人工判定) |
| 522 | `backend/src/main/java/com/foodtraceability/controller/purchase/PurchasePlanController.java:66` | `getPlanPage` | 查询采购计划列表失败 | 未判定 | (需人工判定) |
| 523 | `backend/src/main/java/com/foodtraceability/controller/PurchaseOrderController.java:50` | `getOrderList` | 查询采购订单列表失败 | 未判定 | (需人工判定) |
| 524 | `backend/src/main/java/com/foodtraceability/controller/PurchaseRequestController.java:49` | `getById` | 采购申请不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 525 | `backend/src/main/java/com/foodtraceability/controller/PurchaseStockinController.java:54` | `getStockinPage` | 查询入库单列表失败 | 未判定 | (需人工判定) |
| 526 | `backend/src/main/java/com/foodtraceability/controller/PurchaseStockinController.java:140` | `updateStockin` | 入库单不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 527 | `backend/src/main/java/com/foodtraceability/controller/PurchaseStockinController.java:178` | `deleteStockin` | 删除失败，入库单可能不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 528 | `backend/src/main/java/com/foodtraceability/controller/PurchaseStockinController.java:198` | `startInspection` | 入库单不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 529 | `backend/src/main/java/com/foodtraceability/controller/PurchaseStockinScanController.java:52` | `getProgress` | 入库单不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 530 | `backend/src/main/java/com/foodtraceability/controller/QuickStockInController.java:77` | `executeStockIn` | 入库失败:  | 未判定 | (需人工判定) |
| 531 | `backend/src/main/java/com/foodtraceability/controller/SalaryBatchController.java:83` | `getBatchById` | 记录不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 532 | `backend/src/main/java/com/foodtraceability/controller/SalaryBatchController.java:99` | `confirmBatch` | 记录不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 533 | `backend/src/main/java/com/foodtraceability/controller/SalaryController.java:66` | `getSalaryRecordById` | 记录不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 534 | `backend/src/main/java/com/foodtraceability/controller/SalaryController.java:110` | `getSalaryRuleById` | 记录不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 535 | `backend/src/main/java/com/foodtraceability/controller/SalaryController.java:154` | `getSalaryAdjustmentById` | 记录不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 536 | `backend/src/main/java/com/foodtraceability/controller/SalaryController.java:238` | `confirmSalary` | 记录不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 537 | `backend/src/main/java/com/foodtraceability/controller/SalesAnalysisController.java:40` | `generateDailyReport` | 生成日报失败:  | 未判定 | (需人工判定) |
| 538 | `backend/src/main/java/com/foodtraceability/controller/SalesAnalysisController.java:58` | `generateWeeklyReport` | 生成周报失败:  | 未判定 | (需人工判定) |
| 539 | `backend/src/main/java/com/foodtraceability/controller/SalesAnalysisController.java:78` | `getSalesTrend` | 获取销售趋势失败:  | 未判定 | (需人工判定) |
| 540 | `backend/src/main/java/com/foodtraceability/controller/SalesAnalysisController.java:96` | `getCategorySalesAnalysis` | 获取品类销售占比失败:  | 未判定 | (需人工判定) |
| 541 | `backend/src/main/java/com/foodtraceability/controller/SalesAnalysisController.java:116` | `getTopSellingFoods` | 获取热销菜品失败:  | 未判定 | (需人工判定) |
| 542 | `backend/src/main/java/com/foodtraceability/controller/SalesAnalysisController.java:132` | `getHourlyDistribution` | 获取时段分布失败:  | 未判定 | (需人工判定) |
| 543 | `backend/src/main/java/com/foodtraceability/controller/SalesAnalysisController.java:150` | `getChannelComparison` | 获取渠道对比失败:  | 未判定 | (需人工判定) |
| 544 | `backend/src/main/java/com/foodtraceability/controller/SalesAnalysisController.java:165` | `queryReports` | 查询销售报表失败:  | 未判定 | (需人工判定) |
| 545 | `backend/src/main/java/com/foodtraceability/controller/SalesAnalysisController.java:180` | `getReportById` | 获取报表详情失败:  | 未判定 | (需人工判定) |
| 546 | `backend/src/main/java/com/foodtraceability/controller/schedule/ScheduleNotificationController.java:126` | `sendNotification` | 通知标题不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 547 | `backend/src/main/java/com/foodtraceability/controller/schedule/ScheduleNotificationController.java:129` | `sendNotification` | 接收人不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 548 | `backend/src/main/java/com/foodtraceability/controller/schedule/SchedulePlanController.java:62` | `getPlanById` | 排班方案不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 549 | `backend/src/main/java/com/foodtraceability/controller/schedule/SchedulePlanController.java:168` | `withdrawPlan` | 撤回原因不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 550 | `backend/src/main/java/com/foodtraceability/controller/schedule/ScheduleShiftTypeController.java:67` | `getShiftTypeById` | 班次类型不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 551 | `backend/src/main/java/com/foodtraceability/controller/schedule/ScheduleSwapController.java:105` | `getSwapById` | 换班请求不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 552 | `backend/src/main/java/com/foodtraceability/controller/schedule/ScheduleSwapController.java:155` | `rejectSwap` | 驳回原因不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 553 | `backend/src/main/java/com/foodtraceability/controller/schedule/ScheduleTemplateController.java:56` | `getTemplateById` | 排班模板不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 554 | `backend/src/main/java/com/foodtraceability/controller/ScheduledTaskController.java:82` | `getTaskList` | 查询任务列表失败 | 未判定 | (需人工判定) |
| 555 | `backend/src/main/java/com/foodtraceability/controller/ScheduledTaskController.java:101` | `getTaskDetail` | 获取任务详情失败 | 未判定 | (需人工判定) |
| 556 | `backend/src/main/java/com/foodtraceability/controller/ScheduledTaskController.java:123` | `createTask` | 创建任务失败 | 未判定 | (需人工判定) |
| 557 | `backend/src/main/java/com/foodtraceability/controller/ScheduledTaskController.java:149` | `updateTask` | 更新任务失败 | 未判定 | (需人工判定) |
| 558 | `backend/src/main/java/com/foodtraceability/controller/ScheduledTaskController.java:173` | `deleteTask` | 删除任务失败 | 未判定 | (需人工判定) |
| 559 | `backend/src/main/java/com/foodtraceability/controller/ScheduledTaskController.java:196` | `enableTask` | 启用任务失败 | 未判定 | (需人工判定) |
| 560 | `backend/src/main/java/com/foodtraceability/controller/ScheduledTaskController.java:219` | `disableTask` | 禁用任务失败 | 未判定 | (需人工判定) |
| 561 | `backend/src/main/java/com/foodtraceability/controller/ScheduledTaskController.java:242` | `pauseTask` | 暂停任务失败 | 未判定 | (需人工判定) |
| 562 | `backend/src/main/java/com/foodtraceability/controller/ScheduledTaskController.java:265` | `resumeTask` | 恢复任务失败 | 未判定 | (需人工判定) |
| 563 | `backend/src/main/java/com/foodtraceability/controller/ScheduledTaskController.java:294` | `triggerNow` | 触发任务失败 | 未判定 | (需人工判定) |
| 564 | `backend/src/main/java/com/foodtraceability/controller/ScheduledTaskController.java:324` | `getExecutionLogs` | 获取执行日志失败 | 未判定 | (需人工判定) |
| 565 | `backend/src/main/java/com/foodtraceability/controller/ScheduledTaskController.java:349` | `getLogDetail` | 获取日志详情失败 | 未判定 | (需人工判定) |
| 566 | `backend/src/main/java/com/foodtraceability/controller/ScheduledTaskController.java:364` | `getStatistics` | 获取统计数据失败 | 未判定 | (需人工判定) |
| 567 | `backend/src/main/java/com/foodtraceability/controller/ScheduledTaskController.java:379` | `getDashboardData` | 获取仪表盘数据失败 | 未判定 | (需人工判定) |
| 568 | `backend/src/main/java/com/foodtraceability/controller/ScheduledTaskController.java:405` | `batchEnable` | 批量启用任务失败 | 未判定 | (需人工判定) |
| 569 | `backend/src/main/java/com/foodtraceability/controller/ScheduledTaskController.java:431` | `batchDisable` | 批量禁用任务失败 | 未判定 | (需人工判定) |
| 570 | `backend/src/main/java/com/foodtraceability/controller/ScheduledTaskController.java:456` | `batchDelete` | 批量删除任务失败 | 未判定 | (需人工判定) |
| 571 | `backend/src/main/java/com/foodtraceability/controller/ScheduledTaskController.java:471` | `getTaskGroups` | 获取任务分组失败 | 未判定 | (需人工判定) |
| 572 | `backend/src/main/java/com/foodtraceability/controller/ScheduledTaskController.java:490` | `getTaskByCode` | 按编码查询任务失败 | 未判定 | (需人工判定) |
| 573 | `backend/src/main/java/com/foodtraceability/controller/ScheduledTaskController.java:505` | `getBuiltinTasks` | 获取内置任务列表失败 | 未判定 | (需人工判定) |
| 574 | `backend/src/main/java/com/foodtraceability/controller/seal/SealController.java:176` | `getSealById` | 印章不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 575 | `backend/src/main/java/com/foodtraceability/controller/seal/SealController.java:230` | `updateStatus` | 状态不能为空 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 576 | `backend/src/main/java/com/foodtraceability/controller/SelfPurchaseController.java:44` | `createSelfPurchase` | 自采记录创建失败 | 未判定 | (需人工判定) |
| 577 | `backend/src/main/java/com/foodtraceability/controller/SelfPurchaseController.java:60` | `inboundSelfPurchase` | 自采入库失败 | 未判定 | (需人工判定) |
| 578 | `backend/src/main/java/com/foodtraceability/controller/SelfPurchaseController.java:76` | `reimburseSelfPurchase` | 自采报销失败 | 未判定 | (需人工判定) |
| 579 | `backend/src/main/java/com/foodtraceability/controller/SelfPurchaseController.java:92` | `getSelfPurchaseById` | 自采记录不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 580 | `backend/src/main/java/com/foodtraceability/controller/SelfPurchaseController.java:120` | `cancelSelfPurchase` | 自采记录取消失败 | 未判定 | (需人工判定) |
| 581 | `backend/src/main/java/com/foodtraceability/controller/SessionController.java:53` | `getUserSessions` | 获取会话列表失败 | 未判定 | (需人工判定) |
| 582 | `backend/src/main/java/com/foodtraceability/controller/SessionController.java:73` | `kickSession` | 踢出设备失败 | 未判定 | (需人工判定) |
| 583 | `backend/src/main/java/com/foodtraceability/controller/SessionController.java:100` | `kickAllOtherSessions` | 踢出所有其他设备失败 | 未判定 | (需人工判定) |
| 584 | `backend/src/main/java/com/foodtraceability/controller/StoreHealthCertificateController.java:77` | `getStoreHealthCertificateById` | 健康证不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 585 | `backend/src/main/java/com/foodtraceability/controller/StoreHealthCertificateController.java:89` | `getStoreHealthCertificateByEmployeeId` | 健康证不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 586 | `backend/src/main/java/com/foodtraceability/controller/StoreHealthCertificateController.java:101` | `addStoreHealthCertificate` | 健康证添加失败 | 未判定 | (需人工判定) |
| 587 | `backend/src/main/java/com/foodtraceability/controller/StoreHealthCertificateController.java:113` | `updateStoreHealthCertificate` | 健康证更新失败 | 未判定 | (需人工判定) |
| 588 | `backend/src/main/java/com/foodtraceability/controller/StoreHealthCertificateController.java:125` | `deleteStoreHealthCertificate` | 健康证删除失败 | 未判定 | (需人工判定) |
| 589 | `backend/src/main/java/com/foodtraceability/controller/StoreHealthCertificateController.java:151` | `addStoreHealthCertificateExpense` | 只有通过审核的健康证才能申请报销 | 未判定 | (需人工判定) |
| 590 | `backend/src/main/java/com/foodtraceability/controller/StoreHealthCertificateController.java:155` | `addStoreHealthCertificateExpense` | 健康证报销记录添加失败 | 未判定 | (需人工判定) |
| 591 | `backend/src/main/java/com/foodtraceability/controller/StoreHealthCertificateController.java:198` | `updateExpenseStatus` | 报销状态不能为空 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 592 | `backend/src/main/java/com/foodtraceability/controller/StoreHealthCertificateController.java:201` | `updateExpenseStatus` | 报销状态更新失败 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 593 | `backend/src/main/java/com/foodtraceability/controller/StoreNewController.java:74` | `getById` | 门店不存在:  | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 594 | `backend/src/main/java/com/foodtraceability/controller/StoreNewController.java:88` | `getByCode` | 门店编码不存在:  | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 595 | `backend/src/main/java/com/foodtraceability/controller/StoreNewController.java:102` | `create` | 门店编码已存在:  | 未判定 | (需人工判定) |
| 596 | `backend/src/main/java/com/foodtraceability/controller/StoreNewController.java:145` | `delete` | 门店删除失败，可能门店不存在:  | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 597 | `backend/src/main/java/com/foodtraceability/controller/SupplierController.java:48` | `getSupplierPage` | 查询供应商列表失败 | 未判定 | (需人工判定) |
| 598 | `backend/src/main/java/com/foodtraceability/controller/SupplierController.java:62` | `getSupplierList` | 获取供应商列表失败 | 未判定 | (需人工判定) |
| 599 | `backend/src/main/java/com/foodtraceability/controller/SupplierController.java:128` | `batchDeleteSuppliers` | 批量删除供应商失败 | 未判定 | (需人工判定) |
| 600 | `backend/src/main/java/com/foodtraceability/controller/SupplierController.java:155` | `getStatistics` | 获取统计信息失败 | 未判定 | (需人工判定) |
| 601 | `backend/src/main/java/com/foodtraceability/controller/SupplierEvaluationController.java:72` | `getEvaluationsBySupplier` | 获取评估历史失败 | 未判定 | (需人工判定) |
| 602 | `backend/src/main/java/com/foodtraceability/controller/SupplierEvaluationController.java:83` | `getLatestEvaluation` | 该供应商暂无评估记录 | 未判定 | (需人工判定) |
| 603 | `backend/src/main/java/com/foodtraceability/controller/SupplierEvaluationController.java:88` | `getLatestEvaluation` | 获取最新评估失败 | 未判定 | (需人工判定) |
| 604 | `backend/src/main/java/com/foodtraceability/controller/supplierportal/SupplierPortalController.java:209` | `getById` | 签署链接不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 605 | `backend/src/main/java/com/foodtraceability/controller/SysConfigController.java:52` | `getValue` | 获取配置值失败 | 未判定 | (需人工判定) |
| 606 | `backend/src/main/java/com/foodtraceability/controller/SysConfigController.java:100` | `getConfigList` | 查询配置列表失败 | 未判定 | (需人工判定) |
| 607 | `backend/src/main/java/com/foodtraceability/controller/SysConfigController.java:123` | `updateValue` | 配置值不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 608 | `backend/src/main/java/com/foodtraceability/controller/SysConfigController.java:135` | `updateValue` | 修改配置失败 | 未判定 | (需人工判定) |
| 609 | `backend/src/main/java/com/foodtraceability/controller/SysConfigController.java:153` | `batchUpdate` | 批量更新失败 | 未判定 | (需人工判定) |
| 610 | `backend/src/main/java/com/foodtraceability/controller/SysConfigController.java:158` | `batchUpdate` | 批量修改配置失败 | 未判定 | (需人工判定) |
| 611 | `backend/src/main/java/com/foodtraceability/controller/SysConfigController.java:178` | `resetToDefault` | 重置配置失败 | 未判定 | (需人工判定) |
| 612 | `backend/src/main/java/com/foodtraceability/controller/SysConfigController.java:193` | `resetGroupToDefault` | 该分组无配置可重置 | 未判定 | (需人工判定) |
| 613 | `backend/src/main/java/com/foodtraceability/controller/SysConfigController.java:196` | `resetGroupToDefault` | 重置失败 | 未判定 | (需人工判定) |
| 614 | `backend/src/main/java/com/foodtraceability/controller/SysConfigController.java:216` | `enableConfig` | 启用配置失败 | 未判定 | (需人工判定) |
| 615 | `backend/src/main/java/com/foodtraceability/controller/SysConfigController.java:236` | `disableConfig` | 禁用配置失败 | 未判定 | (需人工判定) |
| 616 | `backend/src/main/java/com/foodtraceability/controller/SysConfigController.java:263` | `getHistory` | 查询配置历史失败 | 未判定 | (需人工判定) |
| 617 | `backend/src/main/java/com/foodtraceability/controller/SysDictController.java:73` | `updateDict` | 更新失败，字典不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 618 | `backend/src/main/java/com/foodtraceability/controller/SysDictController.java:87` | `deleteDict` | 删除失败，字典不存在或已被删除 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 619 | `backend/src/main/java/com/foodtraceability/controller/SysDictController.java:201` | `updateDictItem` | 更新失败，字典项不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 620 | `backend/src/main/java/com/foodtraceability/controller/SysDictController.java:215` | `deleteDictItem` | 删除失败，字典项不存在或已被删除 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 621 | `backend/src/main/java/com/foodtraceability/controller/SysSettingController.java:79` | `setGlobalSetting` | 设置失败:  | 未判定 | (需人工判定) |
| 622 | `backend/src/main/java/com/foodtraceability/controller/SystemConfigController.java:79` | `saveActivePermissionTemplate` | templateCode 不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 623 | `backend/src/main/java/com/foodtraceability/controller/TableReservationNewController.java:73` | `getStats` | 获取预约统计失败： | 未判定 | (需人工判定) |
| 624 | `backend/src/main/java/com/foodtraceability/controller/TableReservationNewController.java:83` | `getById` | 预约记录不存在，ID:  | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 625 | `backend/src/main/java/com/foodtraceability/controller/TableReservationNewController.java:119` | `confirm` | 未登录或登录态已失效，无法获取操作人信息 | 未判定 | (需人工判定) |
| 626 | `backend/src/main/java/com/foodtraceability/controller/TableReservationNewController.java:134` | `arrive` | 未登录或登录态已失效，无法获取操作人信息 | 未判定 | (需人工判定) |
| 627 | `backend/src/main/java/com/foodtraceability/controller/TableReservationNewController.java:151` | `cancel` | 未登录或登录态已失效，无法获取操作人信息 | 未判定 | (需人工判定) |
| 628 | `backend/src/main/java/com/foodtraceability/controller/TrayController.java:51` | `getIdleTrays` | KDS token store_id 格式无效 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 629 | `backend/src/main/java/com/foodtraceability/controller/TrayController.java:86` | `getByTrayCode` | 托盘不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 630 | `backend/src/main/java/com/foodtraceability/controller/TrayController.java:120` | `bindOrder` | KDS token store_id 格式无效 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 631 | `backend/src/main/java/com/foodtraceability/controller/TrayController.java:125` | `bindOrder` | 托盘不存在:  | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 632 | `backend/src/main/java/com/foodtraceability/controller/TrayController.java:128` | `bindOrder` | 托盘  | 未判定 | (需人工判定) |
| 633 | `backend/src/main/java/com/foodtraceability/controller/TrayController.java:132` | `bindOrder` | KDS 凭据门店( | 未判定 | (需人工判定) |
| 634 | `backend/src/main/java/com/foodtraceability/controller/TrayController.java:194` | `releaseTray` | 托盘不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 635 | `backend/src/main/java/com/foodtraceability/controller/TrayController.java:205` | `startCleaning` | 托盘不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 636 | `backend/src/main/java/com/foodtraceability/controller/TrayController.java:216` | `finishCleaning` | 托盘不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 637 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceAlertServiceImpl.java:65` | `createAlertWithMessage` | 设备不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 638 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceAlertServiceImpl.java:85` | `createAlertWithMessage` | 创建告警失败： | 未判定 | (需人工判定) |
| 639 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceAlertServiceImpl.java:94` | `getAlertById` | 告警不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 640 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceAlertServiceImpl.java:99` | `getAlertById` | 查询告警失败： | 未判定 | (需人工判定) |
| 641 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceAlertServiceImpl.java:119` | `getAlertPage` | 查询告警列表失败： | 未判定 | (需人工判定) |
| 642 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceAlertServiceImpl.java:140` | `getUnhandledAlertsByDevice` | 查询失败： | 未判定 | (需人工判定) |
| 643 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceAlertServiceImpl.java:160` | `getAllUnhandledAlerts` | 查询失败： | 未判定 | (需人工判定) |
| 644 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceAlertServiceImpl.java:170` | `handleAlert` | 告警不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 645 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceAlertServiceImpl.java:174` | `handleAlert` | 该告警已被处理 | 未判定 | (需人工判定) |
| 646 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceAlertServiceImpl.java:190` | `handleAlert` | 处理告警失败： | 未判定 | (需人工判定) |
| 647 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceAlertServiceImpl.java:211` | `batchHandleAlerts` | 批量处理告警失败： | 未判定 | (需人工判定) |
| 648 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceAlertServiceImpl.java:257` | `getAlertStatistics` | 获取失败： | 未判定 | (需人工判定) |
| 649 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceAlertServiceImpl.java:297` | `save` | 设备ID不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 650 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceAlertServiceImpl.java:316` | `save` | 创建告警失败： | 未判定 | (需人工判定) |
| 651 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceAlertServiceImpl.java:326` | `removeById` | 告警不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 652 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceAlertServiceImpl.java:339` | `removeById` | 删除告警失败： | 未判定 | (需人工判定) |
| 653 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceAlertServiceImpl.java:348` | `updateById` | 告警ID不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 654 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceAlertServiceImpl.java:353` | `updateById` | 告警不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 655 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceAlertServiceImpl.java:366` | `updateById` | 更新告警失败： | 未判定 | (需人工判定) |
| 656 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceMonitorServiceImpl.java:111` | `checkDeviceStatus` | 设备状态检查失败： | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 657 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceMonitorServiceImpl.java:120` | `checkDeviceOffline` | 设备不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 658 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceMonitorServiceImpl.java:142` | `checkDeviceOffline` | 检查失败： | 未判定 | (需人工判定) |
| 659 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceMonitorServiceImpl.java:162` | `getDevicesForCheck` | 获取失败： | 未判定 | (需人工判定) |
| 660 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceMonitorServiceImpl.java:172` | `handleDeviceOnline` | 设备不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 661 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceMonitorServiceImpl.java:191` | `handleDeviceOnline` | 处理失败： | 未判定 | (需人工判定) |
| 662 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceMonitorServiceImpl.java:208` | `handleDeviceOffline` | 处理失败： | 未判定 | (需人工判定) |
| 663 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceMonitorServiceImpl.java:246` | `getDeviceStatistics` | 获取失败： | 未判定 | (需人工判定) |
| 664 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceMonitorServiceImpl.java:277` | `getRecentStatusChanges` | 查询失败： | 未判定 | (需人工判定) |
| 665 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceServiceImpl.java:60` | `createDevice` | 设备编号已存在 | 未判定 | (需人工判定) |
| 666 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceServiceImpl.java:80` | `createDevice` | 创建设备失败： | 未判定 | (需人工判定) |
| 667 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceServiceImpl.java:90` | `updateDevice` | 设备不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 668 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceServiceImpl.java:111` | `updateDevice` | 更新设备失败： | 未判定 | (需人工判定) |
| 669 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceServiceImpl.java:121` | `deleteDevice` | 设备不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 670 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceServiceImpl.java:131` | `deleteDevice` | 删除设备失败 | 未判定 | (需人工判定) |
| 671 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceServiceImpl.java:134` | `deleteDevice` | 删除设备失败： | 未判定 | (需人工判定) |
| 672 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceServiceImpl.java:143` | `getDeviceById` | 设备不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 673 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceServiceImpl.java:148` | `getDeviceById` | 查询设备失败： | 未判定 | (需人工判定) |
| 674 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceServiceImpl.java:166` | `getDevicePage` | 查询设备列表失败： | 未判定 | (需人工判定) |
| 675 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceServiceImpl.java:184` | `getDeviceList` | 查询设备列表失败： | 未判定 | (需人工判定) |
| 676 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceServiceImpl.java:204` | `getOnlineDevices` | 查询在线设备失败： | 未判定 | (需人工判定) |
| 677 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceServiceImpl.java:223` | `getDevicesByType` | 按类型查询设备失败： | 未判定 | (需人工判定) |
| 678 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceServiceImpl.java:233` | `updateDeviceStatus` | 设备不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 679 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceServiceImpl.java:252` | `updateDeviceStatus` | 更新设备状态失败： | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 680 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceServiceImpl.java:262` | `updateHeartbeat` | 设备不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 681 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceServiceImpl.java:289` | `updateHeartbeat` | 更新心跳失败： | 未判定 | (需人工判定) |
| 682 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceServiceImpl.java:299` | `toggleDeviceEnabled` | 设备不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 683 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceServiceImpl.java:337` | `getDeviceStoreLowStock` | 设备不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 684 | `backend/src/main/java/com/foodtraceability/service/device/impl/DeviceServiceImpl.java:347` | `getDeviceStoreLowStock` | 查询设备门店库存预警失败： | 未判定 | (需人工判定) |
| 685 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTaskServiceImpl.java:53` | `createPrintTask` | 目标打印机不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 686 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTaskServiceImpl.java:58` | `createPrintTask` | 目标设备不是打印机 | 未判定 | (需人工判定) |
| 687 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTaskServiceImpl.java:75` | `createPrintTask` | 创建打印任务失败： | 未判定 | (需人工判定) |
| 688 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTaskServiceImpl.java:88` | `batchCreatePrintTasks` | 批量创建任务失败： | 未判定 | (需人工判定) |
| 689 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTaskServiceImpl.java:96` | `batchCreatePrintTasks` | 批量创建打印任务失败： | 未判定 | (需人工判定) |
| 690 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTaskServiceImpl.java:105` | `getTaskById` | 打印任务不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 691 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTaskServiceImpl.java:110` | `getTaskById` | 查询打印任务失败： | 未判定 | (需人工判定) |
| 692 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTaskServiceImpl.java:130` | `getTaskPage` | 查询打印任务列表失败： | 未判定 | (需人工判定) |
| 693 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTaskServiceImpl.java:150` | `getPendingTasksByDevice` | 查询待打印任务失败： | 未判定 | (需人工判定) |
| 694 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTaskServiceImpl.java:166` | `updateTaskToCompleted` | 打印任务不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 695 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTaskServiceImpl.java:181` | `updateTaskToCompleted` | 更新打印任务状态失败： | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 696 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTaskServiceImpl.java:197` | `retryFailedTask` | 打印任务不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 697 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTaskServiceImpl.java:201` | `retryFailedTask` | 只有失败的任务才能重试 | 未判定 | (需人工判定) |
| 698 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTaskServiceImpl.java:205` | `retryFailedTask` | 已达到最大重试次数，无法继续重试 | 未判定 | (需人工判定) |
| 699 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTaskServiceImpl.java:224` | `retryFailedTask` | 重试打印任务失败： | 未判定 | (需人工判定) |
| 700 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTaskServiceImpl.java:234` | `removeById` | 打印任务不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 701 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTaskServiceImpl.java:247` | `removeById` | 删除打印任务失败： | 未判定 | (需人工判定) |
| 702 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTaskServiceImpl.java:256` | `updateById` | 任务ID不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 703 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTaskServiceImpl.java:261` | `updateById` | 打印任务不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 704 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTaskServiceImpl.java:274` | `updateById` | 更新打印任务失败： | 未判定 | (需人工判定) |
| 705 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTaskServiceImpl.java:285` | `updateTaskStatus` | 打印任务不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 706 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTaskServiceImpl.java:305` | `updateTaskStatus` | 更新任务状态失败： | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 707 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTemplateServiceImpl.java:53` | `createTemplate` | 同名模板已存在 | 未判定 | (需人工判定) |
| 708 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTemplateServiceImpl.java:72` | `createTemplate` | 创建打印模板失败： | 未判定 | (需人工判定) |
| 709 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTemplateServiceImpl.java:82` | `updateTemplate` | 模板不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 710 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTemplateServiceImpl.java:93` | `updateTemplate` | 同名模板已存在 | 未判定 | (需人工判定) |
| 711 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTemplateServiceImpl.java:115` | `updateTemplate` | 更新打印模板失败： | 未判定 | (需人工判定) |
| 712 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTemplateServiceImpl.java:130` | `deleteTemplate` | 模板不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 713 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTemplateServiceImpl.java:133` | `deleteTemplate` | 删除打印模板失败： | 未判定 | (需人工判定) |
| 714 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTemplateServiceImpl.java:142` | `getTemplateById` | 模板不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 715 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTemplateServiceImpl.java:147` | `getTemplateById` | 查询打印模板失败： | 未判定 | (需人工判定) |
| 716 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTemplateServiceImpl.java:176` | `getTemplatePage` | 查询打印模板列表失败： | 未判定 | (需人工判定) |
| 717 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTemplateServiceImpl.java:203` | `getEnabledTemplates` | 查询启用模板失败： | 未判定 | (需人工判定) |
| 718 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTemplateServiceImpl.java:219` | `getDefaultTemplate` | 未找到默认模板 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 719 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTemplateServiceImpl.java:226` | `getDefaultTemplate` | 查询默认模板失败： | 未判定 | (需人工判定) |
| 720 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTemplateServiceImpl.java:236` | `setAsDefault` | 模板不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 721 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTemplateServiceImpl.java:256` | `setAsDefault` | 设置默认模板失败： | 未判定 | (需人工判定) |
| 722 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTemplateServiceImpl.java:265` | `renderTemplate` | 模板不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 723 | `backend/src/main/java/com/foodtraceability/service/device/impl/PrintTemplateServiceImpl.java:286` | `renderTemplate` | 模板渲染失败： | 未判定 | (需人工判定) |
| 724 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:143` | `performBackup` | 创建备份目录失败:  | 未判定 | (需人工判定) |
| 725 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:149` | `performBackup` | 无法解析数据库名称 | 未判定 | (需人工判定) |
| 726 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:237` | `performBackup` | 备份失败 | 未判定 | (需人工判定) |
| 727 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:253` | `performBackup` | 备份异常:  | 未判定 | (需人工判定) |
| 728 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:514` | `restoreBackup` | 备份记录不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 729 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:518` | `restoreBackup` | 备份文件不可用 | 未判定 | (需人工判定) |
| 730 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:523` | `restoreBackup` | 备份文件不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 731 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:533` | `restoreBackup` | 恢复前备份失败，取消恢复操作 | 未判定 | (需人工判定) |
| 732 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:566` | `restoreBackup` | 恢复失败 | 未判定 | (需人工判定) |
| 733 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:579` | `restoreBackup` | 恢复异常:  | 未判定 | (需人工判定) |
| 734 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:689` | `deleteBackup` | 备份记录不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 735 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:707` | `deleteBackup` | 删除失败:  | 未判定 | (需人工判定) |
| 736 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:738` | `cleanupOldBackups` | 清理失败:  | 未判定 | (需人工判定) |
| 737 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:921` | `checkForeignKeyConstraints` | 发现外键约束违反:  | 未判定 | (需人工判定) |
| 738 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:926` | `checkForeignKeyConstraints` | 检查外键约束失败:  | 未判定 | (需人工判定) |
| 739 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:1031` | `checkCharsetAndCollation` | 检查字符集失败:  | 未判定 | (需人工判定) |
| 740 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:1083` | `checkDataTypesAndLength` | 检查数据类型失败:  | 未判定 | (需人工判定) |
| 741 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:1144` | `verifyBackupIntegrity` | 备份记录不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 742 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:1149` | `verifyBackupIntegrity` | 备份文件不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 743 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:1159` | `verifyBackupIntegrity` | 备份文件大小为0 | 未判定 | (需人工判定) |
| 744 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:1222` | `verifyBackupIntegrity` | 备份文件验证失败 | 未判定 | (需人工判定) |
| 745 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:1227` | `verifyBackupIntegrity` | 验证失败:  | 未判定 | (需人工判定) |
| 746 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:1237` | `uploadBackupToCloud` | 备份记录不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 747 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:1242` | `uploadBackupToCloud` | 备份文件不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 748 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:1248` | `uploadBackupToCloud` | 云存储服务不可用 | 未判定 | (需人工判定) |
| 749 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:1271` | `uploadBackupToCloud` | 上传失败:  | 未判定 | (需人工判定) |
| 750 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:1281` | `downloadBackupFromCloud` | 备份记录不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 751 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:1292` | `downloadBackupFromCloud` | 备份文件不存在于云存储 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 752 | `backend/src/main/java/com/foodtraceability/service/impl/DatabaseBackupServiceImpl.java:1310` | `downloadBackupFromCloud` | 下载失败:  | 未判定 | (需人工判定) |
| 753 | `backend/src/main/java/com/foodtraceability/service/impl/DefaultYoloProductRecognitionService.java:82` | `recognizeProducts` | 推理服务未配置 | 未判定 | (需人工判定) |
| 754 | `backend/src/main/java/com/foodtraceability/service/impl/DefaultYoloProductRecognitionService.java:93` | `recognizeProducts` | 真实推理未实现 | 未判定 | (需人工判定) |
| 755 | `backend/src/main/java/com/foodtraceability/service/impl/LocalCloudStorageServiceImpl.java:59` | `uploadFile` | 文件上传失败:  | 未判定 | (需人工判定) |
| 756 | `backend/src/main/java/com/foodtraceability/service/impl/LocalCloudStorageServiceImpl.java:70` | `downloadFile` | 文件不存在:  | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 757 | `backend/src/main/java/com/foodtraceability/service/impl/LocalCloudStorageServiceImpl.java:81` | `downloadFile` | 文件下载失败:  | 未判定 | (需人工判定) |
| 758 | `backend/src/main/java/com/foodtraceability/service/impl/LocalCloudStorageServiceImpl.java:96` | `deleteFile` | 文件不存在:  | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 759 | `backend/src/main/java/com/foodtraceability/service/impl/LocalCloudStorageServiceImpl.java:101` | `deleteFile` | 文件删除失败:  | 未判定 | (需人工判定) |
| 760 | `backend/src/main/java/com/foodtraceability/service/impl/LocalCloudStorageServiceImpl.java:139` | `listFiles` | 列出文件失败:  | 未判定 | (需人工判定) |
| 761 | `backend/src/main/java/com/foodtraceability/service/impl/LocalCloudStorageServiceImpl.java:150` | `getFileInfo` | 文件不存在:  | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 762 | `backend/src/main/java/com/foodtraceability/service/impl/LocalCloudStorageServiceImpl.java:165` | `getFileInfo` | 获取文件信息失败:  | 未判定 | (需人工判定) |
| 763 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderCreateServiceImpl.java:240` | `createCanonicalOrder` | 订单项不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 764 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderCreateServiceImpl.java:318` | `createCanonicalOrder` | 菜品【 | 未判定 | (需人工判定) |
| 765 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderCreateServiceImpl.java:340` | `createCanonicalOrder` | 订单金额计算异常，请检查商品价格配置 | 未判定 | (需人工判定) |
| 766 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderCreateServiceImpl.java:474` | `createCanonicalOrder` | 订单创建失败:  | 未判定 | (需人工判定) |
| 767 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderCreateServiceImpl.java:505` | `validateCanonicalItems` | 商品数量必须大于0 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 768 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderCreateServiceImpl.java:508` | `validateCanonicalItems` | 商品价格必须大于0 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 769 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderCreateServiceImpl.java:511` | `validateCanonicalItems` | 菜品ID不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 770 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderCreateServiceImpl.java:514` | `validateCanonicalItems` | 菜品名称不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 771 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:170` | `payOrder` | 仅支持POS终端订单支付 | 未判定 | (需人工判定) |
| 772 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:182` | `payOrder` | 订单已支付，请勿重复操作 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 773 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:194` | `payOrder` | 订单金额异常，请联系管理员 | 未判定 | (需人工判定) |
| 774 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:200` | `payOrder` | 支付金额与订单金额不一致，请刷新后重试 | 未判定 | (需人工判定) |
| 775 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:220` | `payOrder` | 订单正在处理中，请勿重复提交 | 未判定 | (需人工判定) |
| 776 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:256` | `payOrder` | 支付状态已变更，请刷新后重试 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 777 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:324` | `payOrder` | 支付失败，请重试或联系客服 | 未判定 | (需人工判定) |
| 778 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:352` | `refundOrder` | 订单已退款 | 未判定 | (需人工判定) |
| 779 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:355` | `refundOrder` | 只能退款已支付的订单 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 780 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:363` | `refundOrder` | 订单退款金额异常，请联系管理员核查 | 未判定 | (需人工判定) |
| 781 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:435` | `checkAndRecoverOrderStatus` | 订单状态已变更，请刷新后重试 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 782 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:439` | `checkAndRecoverOrderStatus` | 订单正在支付中，请稍后重试 | 未判定 | (需人工判定) |
| 783 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:442` | `checkAndRecoverOrderStatus` | 订单状态异常，无法支付 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 784 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:453` | `processBalancePayment` | 余额支付需要用户登录 | 未判定 | (需人工判定) |
| 785 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:461` | `processBalancePayment` | 会员信息不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 786 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:467` | `processBalancePayment` | 余额不足或扣减失败 | 未判定 | (需人工判定) |
| 787 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:624` | `refundNewOrder` | 只能退款已支付的订单 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 788 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:630` | `refundNewOrder` | 订单退款金额异常，请联系管理员核查 | 未判定 | (需人工判定) |
| 789 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:682` | `payNewOrder` | 订单已取消，无法支付 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 790 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:685` | `payNewOrder` | 订单已支付，请勿重复操作 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 791 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:693` | `payNewOrder` | 订单金额异常，请联系管理员 | 未判定 | (需人工判定) |
| 792 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:701` | `payNewOrder` | 支付金额与订单金额不一致，请刷新后重试 | 未判定 | (需人工判定) |
| 793 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:898` | `prepareQrCodePayment` | 订单状态已变更，请刷新后重试 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 794 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:943` | `confirmQrPayment` | 交易号不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 795 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:950` | `confirmQrPayment` | 交易号格式非法 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 796 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:961` | `confirmQrPayment` | 仅支持POS终端订单支付 | 未判定 | (需人工判定) |
| 797 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:966` | `confirmQrPayment` | 订单已支付，请勿重复操作 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 798 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:978` | `confirmQrPayment` | 订单状态已变更，请刷新后重试 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 799 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:1009` | `confirmQrPayment` | 支付状态已变更，请刷新后重试 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 800 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderQueryServiceImpl.java:91` | `getOrderDetail` | 订单不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 801 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderQueryServiceImpl.java:143` | `getOrderDetail` | 获取订单详情失败:  | 未判定 | (需人工判定) |
| 802 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderQueryServiceImpl.java:214` | `getOrders` | 查询订单列表失败:  | 未判定 | (需人工判定) |
| 803 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderQueryServiceImpl.java:262` | `getOrdersByTable` | 根据桌台号查询订单失败:  | 未判定 | (需人工判定) |
| 804 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderQueryServiceImpl.java:293` | `getOrdersByOpenid` | 通过openid查询订单失败:  | 未判定 | (需人工判定) |
| 805 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderQueryServiceImpl.java:309` | `reprintReceipt` | 仅支持重打已支付的订单小票 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 806 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderQueryServiceImpl.java:324` | `reprintReceipt` | 订单不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 807 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderQueryServiceImpl.java:330` | `reprintReceipt` | 仅支持重打已支付的订单小票 | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 808 | `backend/src/main/java/com/foodtraceability/service/impl/PosOrderQueryServiceImpl.java:349` | `reprintReceipt` | 重打小票失败:  | 未判定 | (需人工判定) |
| 809 | `backend/src/main/java/com/foodtraceability/service/impl/SysSettingServiceImpl.java:31` | `getSettingsByGroup` | 获取设置列表失败： | 未判定 | (需人工判定) |
| 810 | `backend/src/main/java/com/foodtraceability/service/impl/SysSettingServiceImpl.java:57` | `getSettingValue` | 获取设置值失败： | 未判定 | (需人工判定) |
| 811 | `backend/src/main/java/com/foodtraceability/service/impl/SysSettingServiceImpl.java:132` | `saveSetting` | 保存设置失败： | 未判定 | (需人工判定) |
| 812 | `backend/src/main/java/com/foodtraceability/service/impl/SysSettingServiceImpl.java:149` | `getSettingsBatch` | 批量获取设置失败： | 未判定 | (需人工判定) |
| 813 | `backend/src/main/java/com/foodtraceability/service/impl/SysSettingServiceImpl.java:176` | `initDefaultSettings` | 初始化默认设置失败： | 未判定 | (需人工判定) |
| 814 | `backend/src/main/java/com/foodtraceability/service/impl/TrayServiceImpl.java:130` | `bindOrder` | 托盘不存在:  | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 815 | `backend/src/main/java/com/foodtraceability/service/impl/TrayServiceImpl.java:133` | `bindOrder` | 托盘状态不允许绑定，当前状态:  | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 816 | `backend/src/main/java/com/foodtraceability/service/impl/TrayServiceImpl.java:160` | `bindOrder` | 订单不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 817 | `backend/src/main/java/com/foodtraceability/service/impl/TrayServiceImpl.java:203` | `manualTransition` | 托盘码不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 818 | `backend/src/main/java/com/foodtraceability/service/impl/TrayServiceImpl.java:210` | `manualTransition` | 不支持的目标状态:  | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 819 | `backend/src/main/java/com/foodtraceability/service/impl/TrayServiceImpl.java:216` | `manualTransition` | 托盘不存在:  | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 820 | `backend/src/main/java/com/foodtraceability/service/impl/TrayServiceImpl.java:224` | `manualTransition` | 不允许的状态跃迁:  | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 821 | `backend/src/main/java/com/foodtraceability/service/impl/TrayServiceImpl.java:272` | `scanKitchenIn` | 托盘不存在:  | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 822 | `backend/src/main/java/com/foodtraceability/service/impl/TrayServiceImpl.java:275` | `scanKitchenIn` | 托盘状态不允许后厨一次扫码，当前状态:  | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 823 | `backend/src/main/java/com/foodtraceability/service/impl/TrayServiceImpl.java:285` | `scanKitchenIn` | 扫码过于频繁，已忽略（防抖阈值  | 未判定 | (需人工判定) |
| 824 | `backend/src/main/java/com/foodtraceability/service/impl/TrayServiceImpl.java:321` | `scanKitchenOut` | 托盘不存在:  | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 825 | `backend/src/main/java/com/foodtraceability/service/impl/TrayServiceImpl.java:324` | `scanKitchenOut` | 托盘状态不允许后厨二次扫码，当前状态:  | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 826 | `backend/src/main/java/com/foodtraceability/service/impl/TrayServiceImpl.java:334` | `scanKitchenOut` | 扫码过于频繁，已忽略（防抖阈值  | 未判定 | (需人工判定) |
| 827 | `backend/src/main/java/com/foodtraceability/service/impl/TrayServiceImpl.java:400` | `scanServe` | 托盘不存在:  | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 828 | `backend/src/main/java/com/foodtraceability/service/impl/TrayServiceImpl.java:403` | `scanServe` | 托盘状态不允许取餐确认，当前状态:  | 状态非法(400) | ErrorCode.PARAM_ERROR (400) |
| 829 | `backend/src/main/java/com/foodtraceability/service/impl/TrayServiceImpl.java:413` | `scanServe` | 扫码过于频繁，已忽略（防抖阈值  | 未判定 | (需人工判定) |
| 830 | `backend/src/main/java/com/foodtraceability/service/impl/TrayServiceImpl.java:418` | `scanServe` | 托盘未绑定后厨订单 | 未判定 | (需人工判定) |
| 831 | `backend/src/main/java/com/foodtraceability/service/impl/TrayServiceImpl.java:422` | `scanServe` | 关联订单不存在 | 不存在(404) | ErrorCode.NOT_FOUND (404) |
| 832 | `backend/src/main/java/com/foodtraceability/service/impl/TsplGeneratorServiceImpl.java:468` | `validateLayout` | 布局配置不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 833 | `backend/src/main/java/com/foodtraceability/service/impl/TsplGeneratorServiceImpl.java:473` | `validateLayout` | 标签尺寸配置不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 834 | `backend/src/main/java/com/foodtraceability/service/impl/TsplGeneratorServiceImpl.java:477` | `validateLayout` | 标签宽度必须大于0 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 835 | `backend/src/main/java/com/foodtraceability/service/impl/TsplGeneratorServiceImpl.java:481` | `validateLayout` | 标签高度必须大于0 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 836 | `backend/src/main/java/com/foodtraceability/service/impl/TsplGeneratorServiceImpl.java:503` | `validateElement` | 元素位置配置不能为空 | 参数(400) | ErrorCode.PARAM_ERROR (400) |
| 837 | `backend/src/main/java/com/foodtraceability/service/wecom/impl/WeComServiceImpl.java:80` | `getJsApiConfig` | 获取JSAPI配置失败 | 未判定 | (需人工判定) |
| 838 | `backend/src/main/java/com/foodtraceability/service/wecom/impl/WeComServiceImpl.java:99` | `getUserByCode` | 企业微信授权失败:  | 未判定 | (需人工判定) |
| 839 | `backend/src/main/java/com/foodtraceability/service/wecom/impl/WeComServiceImpl.java:126` | `getUserByCode` | 企业微信登录失败 | 未判定 | (需人工判定) |
| 840 | `backend/src/main/java/com/foodtraceability/service/wecom/impl/WeComServiceImpl.java:155` | `sendTextMessage` | 发送消息失败 | 未判定 | (需人工判定) |
| 841 | `backend/src/main/java/com/foodtraceability/service/wecom/impl/WeComServiceImpl.java:184` | `sendTemplateCard` | 发送消息失败 | 未判定 | (需人工判定) |

合计 **841** 处。按语义分布：
  - 未判定：566
  - 不存在(404)：162
  - 参数(400)：69
  - 状态非法(400)：44
