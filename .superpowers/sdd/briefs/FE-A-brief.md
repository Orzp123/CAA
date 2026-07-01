# Task FE-A Brief: SchoolForm.tsx — Logo上传 + 运营位增删

## 任务目标

补全 `frontend/src/components/school/SchoolForm.tsx`，添加两块缺失功能：
1. **Logo 上传**：调用 `schoolApi.getLogoUploadUrl` 获取预签名 URL，用 PUT 直传 MinIO，成功后将 `logoPath` 存入表单品牌信息
2. **运营位管理**：支持增删最多 10 条运营位（PromotionalSlot），每条包含：标题、图片URL、链接URL、展示位置（3个枚举）、排序号；提交时将 slots 数组传入 create/update 请求

## 现有文件状态

文件路径：`frontend/src/components/school/SchoolForm.tsx`

现有代码已完成：基本信息、品牌信息、权益套餐选择+权限联动、新建/编辑 submit。
**缺失：`slots` 硬编码为 `[]`，Logo 上传未实现，`brand.logoUrl` 未包含在表单中。**

## API

```ts
// schoolApi.getLogoUploadUrl(schoolId) — 仅编辑模式可用
interface LogoUploadUrlResponse {
  uploadUrl: string;   // MinIO 预签名 PUT URL
  logoPath: string;    // 上传成功后存入 brand.logoUrl
  expiresIn: number;
}
// 上传：fetch(uploadUrl, { method: 'PUT', body: file })

type SlotPosition = "HOME_TOP_BANNER" | "HOME_SIDEBAR" | "LOGIN_BANNER";
interface PromotionalSlot {
  title: string;
  imageUrl: string;
  linkUrl: string;
  position: SlotPosition;
  sortOrder: number;
}
```

## 方案决策

- Logo 上传**仅编辑模式**显示（新建时无 schoolId，无法获取预签名 URL）
- `sortOrder` 自动按数组索引赋值（1-based）
- Logo 上传独立于表单提交：选文件后立即触发 PUT 直传，成功后 `logoUrl` 存入 `values.logoUrl`

## 项目约束（强制）

- TypeScript strict，**禁止 any**
- **禁止内联 style**，只用 Tailwind CSS 4 class
- 不引入新第三方依赖
- 运营位最多 10 条，超出禁用"添加"按钮

## 验收标准

1. 编辑模式：Logo 上传区可见，选文件后自动上传，上传中显示"上传中…"，成功显示文件名
2. 运营位：从 `d.slots` 回填；可添加（≤10）、删除；每条4字段可编辑
3. 提交时 `slots` 正确传入（含 sortOrder），`brand.logoUrl` 正确传入（若已上传）
4. `npx tsc --noEmit` 0 error

## 工作目录

`D:/agent/CAA/frontend`

## 报告文件路径

`D:/agent/CAA/.superpowers/sdd/briefs/FE-A-report.md`
