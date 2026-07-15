# PromptForge

本地 Java CLI：為具體任務挑選專案規則，輸出短而可追溯的 task frame。無 LLM、API token 或 Spring Boot 依賴。

目前是逐步實作的教學型 side project。每個功能的 ADR、測試、實作和驗收紀錄都會分開提交，方便逐個 commit review Java 寫法。

## 即刻試用

需要 JDK 15 或以上、PowerShell；不需要 Maven、Spring Boot、LLM或 API key。

```powershell
cd D:\PromptForge
.\scripts\test.ps1
.\frame.ps1 "請修改付款重試邏輯，保持既有測試通過" -Project .\examples\payments
.\query.ps1 "付款重試" -Project .\examples\payments
```

自己專案只要建立 `.promptforge/rules/*.rule`。每檔包含 `id`、以逗號分隔的 `keywords`、單行 `instruction`；全部關鍵字都命中才加規則，沒有命中便原文返回。輸出的方括號包含規則 ID和來源檔案。詳見 [範例專案](examples/payments/README.md)。

目前仍是 CLI MVP：用可解釋的文字匹配，不宣稱理解語意；已有輸出長度上限和明確的規則衝突檢查。

`query.ps1`會用本地 BM25查 `.promptforge/knowledge/**/*.md` 的 ADR／policy等卡片，顯示來源和短摘錄；它不生成答案，也不自動把檢索結果塞進 prompt。查詢指令與整理 task的指令刻意分開。

如果唔想每次用 PowerShell script重新編譯，可以先打包一次：

```powershell
.\scripts\package.ps1
java -jar .\build\PromptForge.jar --project .\examples\payments --task "修改付款重試"
java -jar .\build\PromptForge.jar --project .\examples\payments --query "付款重試"
```

修改 Java source後要重新打包；修改 `.rule`或知識卡片不用重新打包。

## Codex Skill

[Skill入口](skills/prompt-frame/SKILL.md) 可複製到個人 Codex skills目錄。它只負責按需呼叫本地 CLI；規則仍保存在各專案的 `.promptforge/rules`。執行 `./scripts/check-skill.ps1` 可檢查本專案中的 skill檔案。
