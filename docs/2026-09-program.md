# 2026 年 9 月：選擇式 Skill program

這是 2026 年 9 月重整新增的功能；下列「既有實作」不是此版本新功能，也不代表它們已成為 agent 入口。

## 使用

啟動本地 Web 後開啟 `/` 或 `/program.html`（預設埠 8081）。管理來源是 repo 根目錄 `.promptforge/program.json`，包含完整步驟、能力與勾選狀態。新增、編輯、排序及勾選後按 Save；每步的可選能力可分別設定 script、reference 或 skill 的 repo 相對檔案路徑。生成器只驗證所選步驟所選能力的必要檔案，再更新 `.agents/skills/PromptForge/SKILL.md` 及 `.agents/skills/PromptForge-context/SKILL.md`。未勾選步驟與能力只留在管理來源。Web 顯示保存版與預覽版、同步狀態及重新載入提示。保存時攜帶 revision；另一分頁或外部編輯造成的舊 revision 會得到 409。依賴缺失會得到 422，上一份生成版保持可用。

Codex 的 repo scoped Skill 用 `$PromptForge` 與 `$PromptForge-context` 明確呼叫。若客戶端把 Skill 顯示為斜線項目，也可選 `/PromptForge` 與 `/PromptForge-context`；斜線顯示由客戶端決定。檔案變更後開新 agent 工作階段使其重新發現。`PromptForge-context` 只檢查 context，不執行原任務。

## 本地 MCP

Web 另提供 `http://127.0.0.1:8081/mcp`，只綁定 loopback，提供 `get_selected_skill`、`get_context_skill`、`get_program_status` 三個唯讀工具。工具讀取**已保存的生成檔**，不回傳完整管理大綱，不修改資料。Web 頁面可測試 MCP 回傳與生成版是否一致。先啟動 Web，再執行：

```powershell
codex mcp add promptforge --url http://127.0.0.1:8081/mcp
codex mcp list
```

Codex 需開新工作階段才會取得新 MCP 設定；MCP 端點在 Web 停止後不可用。MCP 的即時讀取與 repo Skill 的啟用是兩條入口；使用者在 Web 的選擇仍是唯一來源。2026-09-28 的實際 Codex CLI 呼叫紀錄顯示 `promptforge/get_selected_skill` 與 `get_context_skill` 完成：前者含已選的「理解與界定任務」且不含未選的「補查專案資料」。另用一個無專案資料的任務測試 `$PromptForge`，得到包含判斷、行動、觀察、調整與完成／受阻條件的 prompt；交給 `$PromptForge-context` 後，agent 區分已讀資料、可讀但未讀資源、缺少資料及目前受阻範圍，沒有執行原任務。

## 既有實作去留盤點，待使用者決定

| 項目 | 現有作用 | 與新目標關係 | 刪除影響 | 共用依賴 |
|---|---|---|---|---|
| 手動拼 prompt（`PromptService`、`PromptTemplateCompiler`、舊工作台） | 人手編排區塊、規則與知識卡並保存 prompt | 與新大綱生成流程平行，非生成來源 | 舊 prompt 編輯、預覽及既有檔案操作入口失效 | `ProjectRegistry`、`WorkspaceFiles`、規則與知識載入器被其他舊功能共用 |
| 固定釐清問卷（`ClarificationService`、`guide.html`） | 保存問題與輸出格式、逐題或直接組 prompt | 新 Skill 由 agent 判斷何時追問，不依賴問卷 | 既有釐清頁與 profile 不再可用 | `ProjectRegistry`、`RevisionFileStore`、Web CSRF 共用 |
| 評估匯入（`SkillEvaluationService`） | 記錄外部測試／模型意見與證據 | 新生成流程不讀它 | 既有評估紀錄無法從 Web 匯入／查看 | `SkillService`、`WorkspaceFiles` 共用 |
| review／批准／啟用／匯出（`SkillService`） | 管理舊 Skill bundle 狀態與匯出 | 不控制新 repo scoped 入口 | 舊匯入 Skill 的審核與匯出操作失效 | `SkillBundleStore`、`SkillParser`、Web context 共用 |
| 多專案登記（`workbench-projects.json`、`ProjectRegistry`） | 提供舊工作台選擇範例與已登記專案 | 新 program 固定保存在 PromptForge repo | 舊規則、知識、prompt 與 Skill 工作台失去專案入口 | 大部分舊 Web service 共用 |
| CLI `frame.ps1`、`query.ps1` 與 `skills/prompt-frame` | 文字規則選擇及本地檢索 | 可獨立使用；新 program 未自動引用 | CLI 使用者與舊 Skill 受影響 | core Java 規則、知識檔案共用 |

此盤點沒有執行刪除。既有未提交變更也未納入這次新實作 commit。

Web 的 `/legacy.html` 也顯示這份清單，並保留 `/workbench.html` 與 `/guide.html` 的舊入口，供使用者決定去留。

## 驗證範圍

`ProgramControllerTest` 覆蓋四步選兩步、重新讀取、修改再生成、版本衝突、缺依賴時保留舊版及 HTTP 讀寫。Skill 指令中的 ReAct 與 context 四類輸出經生成檔案檢查。格式及檔案存在不能證明 agent 實際會遵循指令；需在重新載入後用真實任務測試。

## 2026-09-30 後續：Skill library、資源管理與清理

本節取代上面的「待使用者決定」狀態；上文記錄前一階段的既有行為。

使用者已批准移除舊功能。刪除手動 prompt、固定問卷、評估匯入、舊 Skill 審核／批准／匯出、多專案及舊 CLI 的專用程式與測試。保留共用的 WorkspaceFiles、RevisionFileStore、錯誤類型及其邊界／衝突測試。移除 core 模組，workbench 模組目前只提供共用檔案儲存。舊資料、examples、workbench-projects.json 和歷史設計文件保留但不再被程式讀取；Git 舊 commit 未重寫。刪除前將包括未提交變更在內的舊檔案備份至 `.private/before-2026-09-30-cleanup/`。

新 GUI 可建立多個獨立 Skill（同一個 project），各有用途、步驟與勾選；新增 `GET /api/skills`、`GET/PUT /api/skills/{name}`、preview 及 files/file API。來源位於 `.promptforge/outlines/<name>.json`，PromptForge 的舊來源路徑保留。自訂 Skill 不會被強制生成為 prompt 整理流程。

GUI 可保存 UTF-8 文字腳本、references 與文字 assets，帶 revision 衝突保護；可加入步驟並描述使用方式、執行命令及 requires 檔案依賴。生成器驗證已選依賴、被引用 Skill 的 Markdown 本地檔案連結與生成依賴，循環或缺檔報錯。動態 import、套件及 runtime 不會由靜態路徑檢查自動安裝／證實，須在步驟描述並由 agent 實際檢查。腳本不由 Web 或 MCP 自動執行。資源保存立即更新本地檔案，已引用該路徑的 Skill 下次讀取會看到新版；資源不是不可變快照。

MCP 新增 list_skills 和 get_skill(name)。連接頁提供網址、CLI 命令、TOML 設定、複製按鈕及實際 agent 測試句，明確區分服務連線與 agent 接入。指令依據：https://developers.openai.com/learn/docs-mcp 。

### 2026-09-30 實際驗證

- `mvnw.cmd verify`：13 項測試通過，包含真實 Edge 瀏覽器新增自訂 Skill、建立 JavaScript、加入步驟、保存／重開、未選內容排除、MCP 指定 Skill 回傳；測試腳本用 Node 實際執行並輸出 `SCRIPT_OK`。
- 衝突、越界、四步選兩步、缺少直接與間接依賴、失敗保留上一份版本均有測試。
- 新版服務啟動後，本對話實際呼叫 `get_selected_skill` 及 `get_context_skill` 成功。
- 新 Codex CLI 工作階段實際完成 `promptforge/list_skills`、`promptforge/get_skill`，列出兩個入口，回報目前已選的兩步。結果存於本機 `build/mcp-library-agent-result.txt`。這次新工具驗證只讀取 Skill，沒有執行原任務。
- 新 GUI 的實際本地資料仍為四步、選兩步；舊頁面重導到新 GUI。自訂 Skill／腳本測試資料留在隔離的測試目錄，沒有新增到使用者正式大綱。
- 本次測試不代表所有使用者腳本都可執行，也不驗證其他 MCP 客戶端或雲端連接。二進位素材上傳未實作；目前資源編輯器只處理 UTF-8 文字。
