# 0028：應用服務的責任分工

狀態：Accepted。

SkillService 同時處理批准、檔案複製和評估 JSON，使儲存格式與業務流程的變更互相牽連。將它拆成 SkillService（操作入口）、SkillBundleStore（bundle 讀寫與版本）、SkillTransfer（匯入／匯出）、SkillEvaluationService（評估驗證與保存）。共用 WorkspaceFiles 和 RevisionFileStore，不新增只有一個實作且沒有替換需求的介面。

PromptService 提供 load/list/diff/save，不將 repository 暴露給 HTTP。ProjectContextService 提供 context 和搜尋；Controller 保留 HTTP 路由及顯示 DTO 轉換。Spring configuration 集中組裝物件。

先保留既有 SkillWorkflowTest、EvaluationWorkflowTest、PromptWorkflowTest、WebContractTest 的行為契約，再補模組依賴及服務入口的架構檢查。完成後跑完整測試及瀏覽器流程。重構不增加新功能，不承諾跨程序檔案交易。
