# 功能規格與驗收

這份描述目前支援的行為；設計取捨見 [ADR](../adr/README.md)，執行方式見 [README](../../README.md)。

## 目標與範圍

PromptForge 是本地任務文字準備工具。使用者提供任務，工具依文字匹配挑選專案規則，容許人工確認，並加入明確選擇的知識卡。工具不呼叫 LLM、不執行任務、不保證節省 token 或提高下游程式品質。

## CLI 契約

| 操作 | 預期行為 | 驗證位置 |
|---|---|---|
| 自動 frame | keywords 全部命中才選中規則；沒有命中時保留任務 | RuleSelectorTest、TaskFrameTest |
| 人工選規則 | 指定 ID 取代自動選擇；空選擇表示不加規則 | RuleSelectionCliTest |
| 排除規則 | 排除選中 ID；未知、重複、同時選中及排除均報錯 | RuleSelectionCliTest |
| 合併知識 | 明確指定最多 3 張已載入卡片，依指定順序附正文及來源 | KnowledgeFrameCliTest |
| 搜尋 | 本地 BM25，最多 3 項；空／過長查詢不接受 | Bm25IndexTest、QueryCliTest |
| 格式檢查 | 驗證規則與卡片可讀、格式及大小，不驗證內容正確性 | ValidateCliTest、LoaderLimitsTest |
| 衝突 | 選中互相衝突規則時失敗，不交出看似有效的結果 | RuleConflictTest、RuleConflictFieldTest |

任務輸出上限 6000 字元；不作精確 token 計數，超限回報錯誤。來源保留相對路徑。CLI exit code：0 成功、2 輸入錯誤、3 格式錯誤、4 I/O、5 超出 frame 限制、6 規則衝突。

## Web Prompt

1. 專案必須先登記於 `workbench-projects.json`，服務啟動時載入設定。
2. 文件包含 ID、名稱、任務、排序區塊、變數、規則選擇及知識來源。變數替換按字面處理；缺少變數報錯。
3. `ruleIds: null` 使用自動選擇；`ruleIds: []` 完全不選規則。
4. 預覽呼叫共用 Java 核心。只含區塊的模板亦可保存。
5. 比較差異顯示前後文字；保存攜帶讀取時的 revision。新檔以 `new` 表示。
6. revision 不符回傳 409，要求重載；網址 ID 與文件 ID 不符不得保存。
7. 保存至 `.promptforge/prompts/<id>/document.json`。介面不自動建立 Git commit。

證據：PromptDomainTest、PromptWorkflowTest、FileStoreTest、WebContractTest、BrowserWorkflowTest。

## Skill 與評估

匯入來源限登記專案的 `.promptforge/inbox/<source>`，需要有效 `SKILL.md`。正文及 references 一同計算版本 hash；未知 frontmatter 欄位保留在原文。匯入不執行 Skill 中的指令。

狀態順序為 IMPORTED → REVIEWED → APPROVED → ACTIVE。批准需先 review 目前 hash，啟用需先批准目前 hash；內容改變後舊批准失效。啟用只記錄狀態，不安裝到外部 agent。

只有目前已批准版本可匯出至 `.promptforge/exports/<target>`，已有目的地不覆蓋。匯入／匯出先建立 staging，完成後發布目錄。

評估 `TEST` 至少附一份已存在的專案內證據；`MODEL_REVIEW` 至少有一項 finding。保存證據 hash，記錄不代表工具執行過該測試。subjectHash 不同時顯示過期。評估不會自動批准 Skill。

證據：SkillParserTest、SkillWorkflowTest、EvaluationWorkflowTest、BrowserWorkflowTest。

## 本地邊界與失敗

- 只綁定 `127.0.0.1`；沒有多使用者登入或公開伺服器部署模式。
- 寫入需 CSRF token；提供 Origin 時必須同來源；HTTP request body 最多 64 KiB。
- 路徑限已登記專案內，拒絕符號連結和解析後越界路徑。
- WorkspaceFiles 單檔及一次目錄掃描總內容上限為 256 KiB；HTTP 上限可能更早觸發。不同 loader 有自己的更小限制，例如知識卡 8 KiB。
- 原子替換防止半份檔案；revision 鎖只在單一 JVM 內。外部程式並行改檔、跨檔交易不在保證範圍。
- workbench 用 FailureKind 表達錯誤，由 Web 映射成 422／404／409／500。無效 JSON 為 400；請求超限為 413；CSRF／來源拒絕為 403。

證據：WorkspaceBoundaryTest、LocalServerTest、HttpErrorsTest、WebContractTest。
