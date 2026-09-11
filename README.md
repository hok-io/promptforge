# PromptForge

2026 年 9 月重整：本地 Skill 管理程式。用 Web 編輯每個 Skill 的完整大綱，選擇步驟及能力，保存後只生成已選內容供 agent 使用。

## 開始使用

```powershell
./scripts/build-web.ps1
./scripts/start-web.ps1
```

打開 http://127.0.0.1:8081/ 。需要 Java 21；build 腳本會優先使用 project 內的 `.tools` runtime。完整建置會跑後端與瀏覽器測試。

1. 在「我的 Skill」選擇已有 Skill，或按「建立新 Skill」，填 ID 與用途。
2. 編輯步驟，排序、勾選。每步有目的、輸入、判斷、行動、觀察、失敗調整、輸出及停止條件。
3. 如需腳本或資料，在「腳本與參考資料」建立／編輯文字檔，保存並加入步驟；填使用命令與必要依賴。
4. 按「保存並生成 Skill」。未選內容留在大綱，不交給 agent。生成失敗保留上一版，舊 revision 不會覆蓋新修改。
5. 在「連接 AI」複製 MCP 指令、重新開啟 agent 工作階段，再複製測試句，讓 agent 實際呼叫工具。

## MCP 與 Skill

```powershell
codex mcp add promptforge --url http://127.0.0.1:8081/mcp
codex mcp list
```

MCP 提供 `list_skills`、`get_skill(name)`、`get_selected_skill`、`get_context_skill`、`get_program_status`。它讀取保存版，不執行腳本。Web 的連線測試只證明服務可達，agent 必須實際呼叫工具才算接入成功。此 loopback URL 供同機客戶端使用，不能直接給雲端 agent 使用。

亦可在本 project 的 agent 工作階段使用 `$PromptForge`、`$PromptForge-context` 或 `$自訂名稱`。斜線入口是否顯示由客戶端決定，檔案改動後需重新發現。PromptForge 產生包含 ReAct 的任務 prompt；PromptForge-context 檢查實際 context，不執行原任務。自訂 Skill 則依自己的步驟工作。

管理資料在 `.promptforge/program.json`、`.promptforge/outlines/`；資源在 `.promptforge/resources/<name>/`；生成版在 `.agents/skills/<name>/SKILL.md`。不要手動同步修改生成檔。

## 舊功能

使用者於 2026-09-30 批准刪除舊功能。手動拼 prompt、固定問卷、評估匯入、Skill 審核／批准、多專案管理及舊 CLI 已退役。舊資料、範例及 Git 歷史保留；舊頁面書籤轉到新 GUI。歷史設計文件描述既有實作，不能用作目前操作指引。詳見 [重整紀錄](docs/2026-09-program.md)。
