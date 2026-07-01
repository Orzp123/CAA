# FE-B 任务报告

## Status: DONE

## Commits
- `5b48d04` feat(school): 第二身份联动过滤 + 编辑模式重置密码

## Test Summary
`npx tsc --noEmit` — 0 errors，0 warnings

## 变更内容

### 1. 第二身份联动（新建模式）
- 新增 `getSecondaryRoleOptions(accountType)` 纯函数：
  - `SCHOOL_ADMIN` → `["TEACHER", "ASSISTANT"]`
  - `TEACHER` / `STUDENT` → `["ASSISTANT"]`
- 新增 `handleAccountTypeChange`：切换账户类型时，若当前 `secondaryRole` 不在新合法集合内，自动清空为 `""`
- 第二身份 `<select>` 选项改为动态渲染（`getSecondaryRoleOptions` 驱动），配合 `SECONDARY_ROLE_LABELS` 映射中文标签

### 2. 编辑模式重置密码
- 新增独立 state `resetPassword: string`（不混入 `FormValues`，避免污染 update 请求）
- `useEffect` 在 account 切换时同步将 `resetPassword` 清空
- 编辑模式下在手机号字段下方渲染"重置密码"区域（带灰色背景卡片样式，仅 `isEdit` 时显示）
- `handleSubmit` 编辑分支：先 `accountApi.update`，再仅当 `resetPassword` 非空时调用 `accountApi.resetPassword(schoolId, account.id, resetPassword)`

## 验收清单
- [x] 新建：SCHOOL_ADMIN 可选 TEACHER 或 ASSISTANT
- [x] 新建：TEACHER/STUDENT 只能选 ASSISTANT
- [x] 新建：切换 accountType 后不合法 secondaryRole 自动清空
- [x] 编辑：显示重置密码输入框
- [x] 编辑：先 update，再 resetPassword（仅非空时）
- [x] `npx tsc --noEmit` 0 error
- [x] 无 any，无内联 style，无新依赖，只改 AccountFormDialog.tsx

## Concerns
无
