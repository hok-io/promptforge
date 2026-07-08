# PromptForge

本地 Java CLI：為具體任務挑選專案規則，輸出短而可追溯的 task frame。無 LLM、API token 或 Spring Boot 依賴。

目前是逐步實作的教學型 side project。每個功能的 ADR、測試、實作和驗收紀錄都會分開提交，方便逐個 commit review Java 寫法。

## 即刻試用

需要 JDK 15 或以上、PowerShell；不需要 Maven、Spring Boot、LLM或 API key。

```powershell
cd D:\PromptForge
.\scripts\test.ps1
.\frame.ps1 "請修改付款重試邏輯，保持既有測試通過" -Project .\examples\payments
```

自己專案只要建立 `.promptforge/rules/*.rule`。每檔包含 `id`、以逗號分隔的 `keywords`、單行 `instruction`；全部關鍵字都命中才加規則，沒有命中便原文返回。輸出的方括號包含規則 ID和來源檔案。詳見 [範例專案](examples/payments/README.md)。

目前仍是 CLI MVP：用可解釋的文字匹配，不宣稱理解語意；已有輸出長度上限，規則衝突檢查列在下一階段。

## Codex Skill

[Skill入口](skills/prompt-frame/SKILL.md) 可複製到個人 Codex skills目錄。它只負責按需呼叫本地 CLI；規則仍保存在各專案的 `.promptforge/rules`。執行 `./scripts/check-skill.ps1` 可檢查本專案中的 skill檔案。
