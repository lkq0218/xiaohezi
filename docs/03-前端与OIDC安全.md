# 前端与 OIDC 安全

## 前端结构

- `main.ts`：挂载 Vue。
- `App.vue`：登录页、会话状态、消息列表和业务卡片。
- `api.ts`：统一 Fetch、错误解析和 API DTO。
- `auth.ts`：demo/OIDC 双模式认证、Token 刷新和退出。
- `style.css`：页面样式。

前端只负责交互和展示。它的 maxlength、按钮禁用和登录状态都不能替代后端校验。

## 两种认证模式

### demo

用于没有 Docker 的本地学习。`/api/auth/login` 返回环境变量配置的演示 Token，后端过滤器
建立 `ROLE_USER`。不要在共享或生产环境使用。

### oidc

容器默认使用 Keycloak：

```text
浏览器 → Keycloak 登录 → Authorization Code + PKCE
       → Access Token → Spring Resource Server 校验 JWT
```

后端验证签名、issuer 和有效期，并把 Keycloak realm roles 映射为 Spring 角色。OIDC 模式下
旧登录 API 被禁用；ADMIN 才能访问学习接口和指标接口。

## 资源级权限

角色只回答“你能使用哪类功能”，资源所有者校验回答“这条数据是不是你的”。
`BusinessStateStore.conversation(id, userId)` 同时使用 ID 和当前认证用户过滤，因此知道别人的
conversationId 也不能读取或修改。

## 常见安全问题

- 不把 Access Token 放到日志或 URL。
- 不把真实密码写入 realm JSON；`provision-keycloak.ps1` 从 `.env` 注入。
- CORS 来源通过环境变量配置。
- 用户输入不能决定任意外部 URL；并发教学接口只允许本机 HTTP Host。
- 前端 XSS 风险通过 Vue 文本插值降低，仍不应随意使用 `v-html`。

