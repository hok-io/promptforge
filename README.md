# PromptForge

2026 年 9 月重整：本地 Skill 管理程式。用 Web 編輯每個 Skill 的完整大綱，選擇步驟及能力，保存後只生成已選內容供 agent 使用。

## 開始使用

```powershell
./scripts/build-web.ps1
./scripts/start-web.ps1
```

打開 http://127.0.0.1:8081/ 。需要 Java 21；build 腳本會優先使用 project 內的 `.tools` runtime。完整建置會跑後端與瀏覽器測試。

1. 在「Skill 庫」選擇已有 Skill，或按「新增 Skill」。在管理頁的「1 用途」填用途、輸入種類和最後想收到的結果。
2. 在「2 步驟」點名稱，修改指示、排序和勾選。每個欄位有用途說明和例子；ReAct 等詳細設定可展開編輯。現有 Markdown Skill 則直接編輯指示內容。
3. 如需腳本或資料，在「腳本與參考資料」建立／編輯文字檔，保存並加入步驟；填使用命令與必要依賴。
4. 到「3 檢查並保存」確認選中的步驟及生成預覽，再按「保存並生成 Skill」。未選內容留在大綱，不交給 agent。生成失敗保留上一版，舊 revision 不會覆蓋新修改。
5. 在「連接 AI」複製 MCP 指令、重新開啟 agent 工作階段，再複製測試句，讓 agent 實際呼叫工具。

## MCP 與 Skill

```powershell
codex mcp add promptforge --url http://127.0.0.1:8081/mcp
codex mcp list
```

MCP 提供 `list_skills`、`get_skill(name)`、`get_selected_skill`、`get_context_skill`、`get_program_status`。它讀取保存版，不執行腳本。Web 的連線測試只證明服務可達，agent 必須實際呼叫工具才算接入成功。此 loopback URL 供同機客戶端使用，不能直接給雲端 agent 使用。

亦可在本 project 的 agent 工作階段使用 `$PromptForge`、`$PromptForge-context` 或 `$自訂名稱`。斜線入口是否顯示由客戶端決定，檔案改動後需重新發現。PromptForge 產生包含 ReAct 的任務 prompt；PromptForge-context 檢查實際 context，不執行原任務。自訂 Skill 則依自己的步驟工作。

管理資料在 `.promptforge/program.json`、`.promptforge/outlines/`；資源在 `.promptforge/resources/<name>/`；生成版在 `.agents/skills/<name>/SKILL.md`。不要手動同步修改生成檔。

## 本機資料與範本

管理大綱、資源及生成 Skill 都是工作區本機資料，不再納入新 Git 提交。2026 年 9 月以新 commit 停止追蹤，實體檔案保留；舊 commit 仍保留當時內容，沒有改寫歷史。新 checkout 可執行 `./scripts/init-local-data.ps1`，以 `templates/PromptForge-v1.json` 和 `templates/PromptForge-context-v1.json` 建立 prompt 組成及 context 檢查兩份初始大綱；已有檔案會保留，缺少的檔案才會初始化。`templates/outline-v1.json` 是建立其他 Skill 的通用範本。格式定義於 `templates/outline-v1.schema.json`，版本欄位為 `schemaVersion: "1.0"`；結構大綱與 Markdown 文件大綱均受支援。後端接受未帶版本欄位的舊大綱；明確標記不支援版本者會提示轉換。

`.agents/skills/` 也是本機安裝／生成目錄。若開發時需要 UI/UX Pro Max，請在本機安裝該 Skill 到 `.agents/skills/ui-ux-pro-max/`，並在工作階段開始前讀取其 `SKILL.md` 及所需參考資料；此目錄不會隨 checkout 提供或提交。

## 舊功能

使用者於 2026-09-30 批准刪除舊功能。手動拼 prompt、固定問卷、評估匯入、Skill 審核／批准、多專案管理及舊 CLI 已退役。舊資料、範例及 Git 歷史保留；舊頁面書籤轉到新 GUI。歷史設計文件描述既有實作，不能用作目前操作指引。詳見 [重整紀錄](docs/2026-09-program.md)。
