# 小赫兹项目文档中心

这里是当前代码的唯一技术文档入口。阅读时以代码和本目录为准；过去按 V0.2、V3、V4
拆分的阶段文档已经移除，避免把历史计划误当成当前实现。

## 推荐阅读顺序

1. [学习路线](学习路线.md)：明确学习顺序、练习方式和掌握标准。
2. [业务、范围与架构](01-业务范围与系统架构.md)：理解系统解决什么问题、边界在哪里。
3. [后端、数据与一致性](02-后端数据与一致性.md)：理解分层、事务、幂等、TTL 和并发。
4. [前端、OIDC 与接口安全](03-前端与OIDC安全.md)：理解 Vue 调用链和认证授权。
5. [Agent、LLM 与 RAG](04-Agent与RAG.md)：理解确定性业务与模型能力如何组合。
6. [测试、可观测与运维](05-测试可观测与运维.md)：理解如何证明系统可靠。
7. [多模态能力设计](06-多模态能力设计.md)：下一阶段的图片、语音、图文 RAG、生成与评测方案；内容是规划，不是当前实现。
8. [生产化运行与恢复手册](生产化运行与恢复手册.md)：实际部署和演练。
9. [工程化审计与上线清单](工程化审计与上线清单.md)：检查还有哪些外部工作未完成。

## 文档类型

| 类型 | 文档 | 用途 |
| --- | --- | --- |
| 当前设计 | `01`～`05` | 学习当前代码和工程决策 |
| 下一阶段设计 | `06` | 多模态能力的实施顺序与验收门禁，不代表已实现 |
| 操作手册 | 生产化运行与恢复手册 | 启动、备份、灰度、回滚 |
| 验收清单 | 工程化审计与上线清单 | 上线前自查 |
| 产品资料 | 根目录三份产品文档 | 理解需求来源与产品指标 |
| 运行时知识 | `backend/src/main/resources/knowledge` | RAG 实际检索语料，不是开发文档 |

## 以代码为准的关键入口

- 后端启动：`backend/src/main/java/com/xiaohezi/XiaoheziApplication.java`
- HTTP API：`backend/src/main/java/com/xiaohezi/api/AppController.java`
- 对话编排：`backend/src/main/java/com/xiaohezi/agent/AgentService.java`
- 业务持久化：`backend/src/main/java/com/xiaohezi/state/BusinessStateStore.java`
- RAG：`backend/src/main/java/com/xiaohezi/rag/RagService.java`
- 安全：`backend/src/main/java/com/xiaohezi/config/SecurityConfig.java`
- 前端入口：`frontend/src/App.vue`
- 容器编排：`infra/docker-compose.yml`
