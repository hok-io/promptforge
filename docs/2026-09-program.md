# 2026 年 9 月：選擇式 Skill program

這是 2026 年 9 月重整新增的功能；下列「既有實作」不是此版本新功能，也不代表它們已成為 agent 入口。

## 使用

啟動本地 Web 後開啟 `/program.html`。管理來源是 repo 根目錄 `.promptforge/program.json`，包含完整步驟、能力與勾選狀態。新增、編輯、排序及勾選後按 Save；每步的可選能力可分別設定 script、reference 或 skill 的 repo 相對檔案路徑。生成器只驗證所選步驟所選能力的必要檔案，再更新 `.agents/skills/PromptForge/SKILL.md` 及 `.agents/skills/PromptForge-context/SKILL.md`。未勾選步驟與能力只留在管理來源。Web 下方顯示實際生成內容和重新載入提示。保存時攜帶 revision；另一分頁或外部編輯造成的舊 revision 會得到 409。依賴缺失會得到 422，上一份生成版保持可用。

Codex 的 repo scoped Skill 用 `$PromptForge` 與 `$PromptForge-context` 明確呼叫。若客戶端把 Skill 顯示為斜線項目，也可選 `/PromptForge` 與 `/PromptForge-context`；斜線顯示由客戶端決定。檔案變更後開新 agent 工作階段使其重新發現。`PromptForge-context` 只檢查 context，不執行原任務。

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

## 驗證範圍

`ProgramControllerTest` 覆蓋四步選兩步、重新讀取、修改再生成、版本衝突、缺依賴時保留舊版及 HTTP 讀寫。Skill 指令中的 ReAct 與 context 四類輸出經生成檔案檢查。格式及檔案存在不能證明 agent 實際會遵循指令；需在重新載入後用真實任務測試。
