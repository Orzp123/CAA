# Task FE-B Brief: AccountFormDialog.tsx — 第二身份联动 + 编辑模式重置密码

## 任务目标

补全 `frontend/src/components/school/AccountFormDialog.tsx`：

1. **第二身份联动规则**：第二身份选项必须根据第一身份（accountType）动态过滤：
   - SCHOOL_ADMIN → 可选 TEACHER 或 ASSISTANT
   - TEACHER → 只能选 ASSISTANT
   - STUDENT → 只能选 ASSISTANT
   规则：当 accountType 改变时，若当前 secondaryRole 在新规则下不合法，自动清空 secondaryRole

2. **编辑模式重置密码**：编辑模式下新增"重置密码"区域，包含一个密码输入框（留空=使用系统默认策略），提交时若密码框有内容则额外调用 `accountApi.resetPassword(schoolId, account.id, password)`

## 现有文件状态

`frontend/src/components/school/AccountFormDialog.tsx`

当前已实现：
- 新建表单：loginName / name / accountType / secondaryRole / nickname / email / phone / password
- 编辑表单：loginName(只读) / name / nickname / email / phone（无密码重置）
- 第二身份选项**固定**为 TEACHER 和 ASSISTANT（未根据 accountType 联动）

## API

```ts
// 已在 schoolApi.ts 存在，直接使用
accountApi.resetPassword(schoolId: string, accountId: string, password: string | null): Promise<void>
// password 传 null = 使用系统默认密码策略
```

## 第二身份合法选项

```ts
function getSecondaryRoleOptions(accountType: AccountType): SecondaryRole[] {
  if (accountType === "SCHOOL_ADMIN") return ["TEACHER", "ASSISTANT"];
  return ["ASSISTANT"]; // TEACHER / STUDENT 均只能选 ASSISTANT
}
```

## 项目约束（强制）

- TypeScript strict，**禁止 any**
- **禁止内联 style**，只用 Tailwind CSS 4 class
- 不引入新依赖
- 只修改 `AccountFormDialog.tsx`，不动其他文件

## 验收标准

1. 新建模式：选 SCHOOL_ADMIN 时第二身份可选 TEACHER 或 ASSISTANT；选 TEACHER/STUDENT 时只能选 ASSISTANT
2. 新建模式：切换 accountType 后若 secondaryRole 不合法，自动清空为 ""
3. 编辑模式：显示"重置密码"输入框，留空=系统默认，有值=提交时调用 resetPassword
4. 编辑模式：重置密码与 update 信息分开调用（先 update，再 resetPassword if password非空）
5. `npx tsc --noEmit` 0 error

## 工作目录

`D:/agent/CAA/frontend`

## 报告文件

`D:/agent/CAA/.superpowers/sdd/briefs/FE-B-report.md`
