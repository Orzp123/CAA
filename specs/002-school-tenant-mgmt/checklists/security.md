# 安全需求质量检查清单：学校租户管理与账户管理

**用途**：验证规格中安全相关需求的完整性、清晰度与可测量性（进入 /speckit-plan 前）
**创建日期**：2026-06-25
**深度**：深度（覆盖所有场景类含恢复/回滚路径）
**功能**: [spec.md](../spec.md)

## 密码安全需求

- [ ] CHK001 - 初始密码"仅展示一次"是否量化了展示方式（弹窗/下载/复制按钮）以及展示窗口关闭后的不可恢复性？[Clarity, Spec §1.1]
- [ ] CHK002 - 是否定义了初始密码的强度要求（长度、字符类型）？[Gap, Completeness]
- [ ] CHK003 - 是否定义了初始密码的生成算法要求（密码学安全随机）？[Gap, Completeness]
- [ ] CHK004 - "不存储明文"这一安全要求是否指定了哈希算法或合规标准（如 bcrypt/Argon2）？[Clarity, Spec §成功标准]
- [ ] CHK005 - 重置密码后旧密码是否立即失效的需求是否明确定义？[Gap, Exception Flow]
- [ ] CHK006 - 批量导入账户时初始密码的生成方式是否有安全需求（是否与单账户创建一致）？[Consistency, Spec §2.5]

## 租户隔离需求

- [ ] CHK007 - "跨租户数据完全隔离"是否量化了具体隔离层次（API 层/数据层/存储层）？[Clarity, Spec §成功标准]
- [ ] CHK008 - 学校管理员越权访问其他租户数据的拒绝行为是否有明确定义（返回 403 还是 404）？[Gap, Completeness]
- [ ] CHK009 - SYSTEM_ADMIN 操作学校数据时是否有访问记录/审计要求？[Gap, Non-Functional]
- [ ] CHK010 - 是否定义了租户隔离的验证方式（如请求中 tenantId 校验机制）？[Gap, Assumption]

## 停用学校的安全需求

- [ ] CHK011 - 停用学校后"无法再登录"是否定义了具体实现约束（是否需要 Token 黑名单，还是纯依赖 Token 自然过期）？[Clarity, Spec §边界与约束]
- [ ] CHK012 - Token 自然过期期间（停用后到过期前）该校用户可继续操作的范围是否有明确限制需求？[Gap, Exception Flow]
- [ ] CHK013 - 停用学校后是否需要强制刷新 Token 的需求是否被明确排除或包含？[Gap, Ambiguity]

## 会话安全需求

- [ ] CHK014 - 是否定义了 Token 有效期的要求（时长/刷新策略）？[Gap, Completeness]
- [ ] CHK015 - 密码重置后现有会话的处理方式（是否立即失效）是否明确定义？[Gap, Exception Flow]

## 文件上传安全需求

- [ ] CHK016 - Logo 上传的格式约束是否定义了服务端 MIME 类型校验（非仅后缀名校验）需求？[Clarity, Spec §边界与约束]
- [ ] CHK017 - Logo 上传超限时的拒绝行为（错误码/提示文案）是否明确定义？[Clarity, Spec §边界与约束]
- [ ] CHK018 - 批量导入 Excel/CSV 文件是否有安全需求（恶意宏、文件注入防护）？[Gap, Completeness]

## 权限边界需求

- [ ] CHK019 - SCHOOL_ADMIN 可将账户类型提升为另一个 SCHOOL_ADMIN 的权限边界是否有约束需求？[Gap, Exception Flow]
- [ ] CHK020 - "不可删除自身账户"约束在单条删除（§2.4）与批量删除（§2.5）两处规格中是否一致？[Consistency, Spec §2.4, §2.5]
- [ ] CHK021 - SYSTEM_ADMIN 创建学校时默认管理员账户的权限初始化规则是否定义？[Gap, Completeness, Spec §1.1]

## 恢复与回滚场景

- [ ] CHK022 - 批量导入服务端异常时已成功创建的部分账户的数据一致性需求是否定义？[Gap, Recovery]
- [ ] CHK023 - 学校创建失败后自动创建的默认管理员账户是否有回滚需求（避免孤儿账户）？[Gap, Recovery, Spec §1.1]
