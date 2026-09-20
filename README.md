# 个人知识库

一个基于 Spring Boot 和 RAG 的个人知识库

用户可以录入文件、笔记和网页，并针对自己的资料进行带来源的问答。

## 功能

- 解析 TXT、Markdown、PDF 和 DOCX
- 录入文字笔记和公开网页
- 文本切分、向量化和 pgvector 存储
- 资料查询、修改、替换和删除
- 混合检索、模型重排、Agent 按需二次检索、无依据拒答和来源追踪
- 问答会话保存与刷新恢复
- 资料管理与知识问答页面

## 技术栈

- Java 17、Spring Boot、Maven
- PostgreSQL、pgvector、Spring Data JPA
- Spring AI
- HTML、CSS、JavaScript
- Docker Compose、GitHub Actions

## Docker 运行

需要先安装并启动 Docker Desktop。

```powershell
Copy-Item .env.example .env
```

编辑 `.env`，填写数据库密码和 `SILICONFLOW_API_KEY`，然后运行：

```powershell
docker compose up --build
```

打开 <http://localhost:8080>。数据库数据保存在 Docker volume 中，执行 `docker compose down` 后不会丢失。

## 本地运行

需要 Java 17、Maven 和 PostgreSQL。先创建数据库：

```sql
CREATE DATABASE personal_knowledge_base_rebuild;
```

然后连接到 `personal_knowledge_base_rebuild`，启用向量扩展：

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

复制并填写环境变量：

```powershell
Copy-Item .env.example .env
mvn spring-boot:run
```

打开 <http://localhost:8080>，健康检查地址为 <http://localhost:8080/api/health>。

## 测试和打包

```powershell
mvn verify
```

GitHub Actions 会在创建 PR 和更新 `main` 时自动执行相同检查。

## 主要接口

| 方法 | 地址 | 作用 |
| --- | --- | --- |
| `GET` | `/api/health` | 健康检查 |
| `POST` | `/api/documents` | 上传文件 |
| `POST` | `/api/documents/notes` | 创建笔记 |
| `POST` | `/api/documents/links` | 收藏网页 |
| `GET` | `/api/documents` | 查询资料列表 |
| `GET` | `/api/documents/{id}` | 查询资料详情 |
| `PUT` | `/api/documents/{id}` | 更新资料或替换文件 |
| `POST` | `/api/documents/{id}/retry` | 重新处理失败资料 |
| `DELETE` | `/api/documents/{id}` | 删除资料 |
| `POST` | `/api/chat` | 知识库问答 |
| `GET` | `/api/chat/{conversationId}` | 查询会话历史 |

## 设计文档

- [产品需求文档（Issue #1）](https://github.com/haiwangxing6666-a11y/personal-knowledge-base-rebuild/issues/1)
- [产品架构设计（Issue #2）](https://github.com/haiwangxing6666-a11y/personal-knowledge-base-rebuild/issues/2)
- [接口与页面模块（Issue #3）](https://github.com/haiwangxing6666-a11y/personal-knowledge-base-rebuild/issues/3)
- [资料库模块（Issue #55）](https://github.com/haiwangxing6666-a11y/personal-knowledge-base-rebuild/issues/55)
- [知识索引模块（Issue #60）](https://github.com/haiwangxing6666-a11y/personal-knowledge-base-rebuild/issues/60)
- [知识问答模块（Issue #67）](https://github.com/haiwangxing6666-a11y/personal-knowledge-base-rebuild/issues/67)
- [公共支持模块（Issue #24）](https://github.com/haiwangxing6666-a11y/personal-knowledge-base-rebuild/issues/24)

## 密钥安全

`.env` 已被 Git 忽略，不要把真实数据库密码或 API Key 写入代码、README 或 `.env.example`。

## License

本项目使用仓库中声明的开源许可证，详见 [LICENSE](LICENSE)。
