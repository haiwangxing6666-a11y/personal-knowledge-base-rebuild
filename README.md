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

## 核心流程

```text
上传资料 → 后台解析与验重 → 文本切分 → 建立索引 → 资料变为 READY

用户提问 → 混合检索 → 模型重排 → Agent 判断证据
        → 必要时改写问题并再次检索 → 回答或拒答 → 保存会话
```

## 技术栈

- Java 17、Spring Boot、Maven
- PostgreSQL、pgvector、Spring Data JPA
- Spring AI
- Vue 3、Vite、HTML、CSS、JavaScript
- Docker Compose、GitHub Actions

## Docker 运行

需要先安装并启动 Docker Desktop。

Windows PowerShell：

```powershell
Copy-Item .env.example .env
```

Linux、macOS 或 WSL：

```bash
cp .env.example .env
```

编辑 `.env`，填写数据库密码和 `SILICONFLOW_API_KEY`，然后运行：

```powershell
docker compose up --build
```

打开 <http://localhost:8080>。数据库保存在 `postgres-data`，上传的原文件保存在 `document-files`；执行 `docker compose down` 后数据不会丢失。

## 本地运行

需要 Java 17、Maven、Node.js 24 和 PostgreSQL。先创建数据库：

```sql
CREATE DATABASE personal_knowledge_base_rebuild;
```

然后连接到 `personal_knowledge_base_rebuild`，启用向量扩展：

```sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
```

复制环境变量文件。Windows PowerShell：

```powershell
Copy-Item .env.example .env
```

Linux、macOS 或 WSL：

```bash
cp .env.example .env
```

填写 `.env` 后，构建前端并启动后端：

```powershell
cd frontend
npm ci
npm run build
cd ..
mvn spring-boot:run
```

打开 <http://localhost:8080>，健康检查地址为 <http://localhost:8080/api/health>。

## 测试和打包

```powershell
cd frontend
npm ci
npm run build
cd ..
mvn verify
```

前端开发时也可以在 `frontend` 目录运行 `npm run dev`，然后访问 <http://localhost:5173>；Vite 会把 `/api` 请求转发到 8080 端口的 Spring Boot。GitHub Actions 会在创建 PR 和更新 `main` 时自动构建前端并执行后端测试。

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
- [资料库模块（Issue #55）](https://github.com/haiwangxing6666-a11y/personal-knowledge-base-rebuild/issues/55)
- [知识索引模块（Issue #60）](https://github.com/haiwangxing6666-a11y/personal-knowledge-base-rebuild/issues/60)
- [知识问答模块（Issue #67）](https://github.com/haiwangxing6666-a11y/personal-knowledge-base-rebuild/issues/67)
- [Vue 前端（Issue #74）](https://github.com/haiwangxing6666-a11y/personal-knowledge-base-rebuild/issues/74)
- [工程支持（Issue #24）](https://github.com/haiwangxing6666-a11y/personal-knowledge-base-rebuild/issues/24)

## 密钥安全

`.env` 已被 Git 忽略，不要把真实数据库密码或 API Key 写入代码、README 或 `.env.example`。

## License

本项目使用仓库中声明的开源许可证，详见 [LICENSE](LICENSE)。
