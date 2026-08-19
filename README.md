# 小赫兹电力服务智能体

一个用于学习全栈、Agent、RAG 和工程化交付的个人作品。系统支持账单查询与解释、电力知识问答、模拟缴费、模拟报修、转人工和多轮槽位补全。所有业务和数据均为演示，不连接真实电网、支付或客服系统。

## 从这里开始

- 第一次学习项目：阅读 [文档中心](docs/README.md) 和 [学习路线](docs/学习路线.md)。
- 想立即运行：按下面的“本地开发”操作。
- 想学习部署：阅读 [生产化运行与恢复手册](docs/生产化运行与恢复手册.md)。
- 想知道还缺什么：阅读 [工程化审计与上线清单](docs/工程化审计与上线清单.md)。
- 想扩展图片、语音、图文 RAG 和图像生成：阅读 [多模态能力设计](docs/06-多模态能力设计.md)。该文档是路线图，不代表功能已实现。

## 技术栈

- 前端：Vue 3、TypeScript、Vite、Vitest、Keycloak JS。
- 后端：Java 21、Spring Boot、Spring Security、JPA、Flyway、Actuator。
- AI：Gemini GenerateContent/Embedding、结构化切片、关键词与向量检索、RRF。
- 基础设施：MySQL、Redis、RocketMQ、Keycloak、Prometheus、Docker Compose。
- 交付：GitHub Actions、Dependabot、Trivy、备份恢复和灰度回滚脚本。

## 目录

```text
backend/                  Spring Boot API、Agent、RAG、持久化和测试
frontend/                 Vue 应用、API 客户端、OIDC 和前端测试
infra/                    Compose、Keycloak、Prometheus 与告警规则
scripts/                  身份初始化、备份、恢复、候选发布和回滚
docs/                     当前技术文档与学习路线
.github/                  CI 和依赖更新配置
产品分析.md               产品背景扩展阅读
产品指标与数据看板方案.md  产品衡量方式扩展阅读
多轮澄清PRD.md            对话体验扩展阅读
```

## 本地开发

要求：Java 21、Maven 3.9+、Node.js 20+。

后端默认使用 H2、demo 认证和可降级 AI 配置：

```powershell
cd backend
mvn spring-boot:run
```

另开终端启动前端：

```powershell
cd frontend
npm ci
npm run dev
```

访问 `http://localhost:5173`，本地演示账号默认为 `demo / demo123`。这些默认值仅用于本机
学习；共享环境必须通过环境变量覆盖。

配置 Gemini 时设置 `GEMINI_ENABLED=true` 和 `GEMINI_API_KEY`。不配置时系统使用规则、
关键词检索和安全模板降级，不会伪装成真实模型调用。

## 测试与构建

```powershell
cd backend
mvn clean test

cd ../frontend
npm ci
npm test
npm run build
```

健康检查：`GET /actuator/health/readiness`。响应、响应头和日志使用同一个 `traceId`。

## Docker/OIDC 模式

```powershell
Copy-Item .env.example .env
# 修改 .env 中全部 replace-with-... 值
cd infra
docker compose up --build -d
cd ..
.\scripts\provision-keycloak.ps1
```

访问 `http://localhost:8080`，通过 Keycloak Authorization Code + PKCE 登录。数据库结构只由
Flyway 管理；不要修改已经执行过的迁移文件。

停止服务：

```powershell
cd infra
docker compose down
```

该命令保留数据卷；加入 `-v` 会删除数据卷，不应在没有备份时使用。

## 当前边界

- 账单主数据仍为内置合成样例；会话、幂等、确认动作和业务回执已持久化。
- 缴费和报修只生成模拟回执。
- 缴费和报修的关键判断已抽象到轻量业务规则层，接口响应会携带 `decisionTrace`，展示命中规则、规则版本、决策原因和执行结果。
- 当前规则为代码内置 v1 版本；规则配置后台、Replay 验证和业务决策指标属于后续演进项。
- Compose 是单机学习基线，不是跨区域高可用架构。
- Keycloak 可替换为企业 OIDC，但本项目没有真实企业租户。
- Docker 演练需要在安装 Docker 的机器上完成。
