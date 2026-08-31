# 學習架構整理：驗收

實測日期：2026-09-26。Git 展示日期依要求放在 9/24 晚上，9/25–26 沒有新增提交。

## 實際完成

- SkillService 404 → 78 行；SkillBundleStore 168 行、SkillTransfer 132 行、SkillEvaluationService 102 行。目的為責任清晰，行數只作閱讀範圍參考。
- PromptService 提供操作方法，repository 變成 private 實作細節。
- ProjectContextService 接手規則、知識及搜尋，Controller 保留路由及 DTO 轉換。
- FailureKind 描述應用錯誤，HttpErrors 才决定 HTTP 狀態。
- 新增架構依賴檢查、錯誤映射測試、ADR 0027／0028，以及 [四段學習入口](../learning/read-the-clean-architecture.md)。既有 CI 自動發現新增 JUnit tests，不需要另外的工作流程。

## 證據

1. 第一輪 Java／HTTP：48 個結果通過，記錄 `build/cleanup-tests.log`。
2. 離線 `mvnw.cmd -o clean verify`：core 16、workbench 24、web 12，共 52 個 JUnit 結果，零失敗。Web 包含 4 個 Edge Playwright 流程、6 個 HTTP 契約、1 個真 socket 流程、1 個錯誤映射。記錄 `build/cleanup-final-verify.log`。
3. 打包 JAR 在 127.0.0.1:18081 啟動，GET projects 返回 3 個專案；付款專案 context 返回 2 條規則。測試完成已停止該程序；記錄 `build/cleanup-jar-smoke.log`。
4. 獨立 subagent 對拆分及呼叫路徑作靜態覆核，未提出具體行為回歸。這項檢查不取代測試或你的人手 review。

## 限制

四個舊 Windows symlink 子檢查因權限不足仍跳過，它們位於自訂 main 測試內，所以 JUnit 的 skipped 數字未反映這四項。Windows junction 測試有執行。沒有遠端 CI 執行結果；本次沒有新增性能或真人效率研究。

架構適合逐段學習，但不代表多檔更新具有交易性。JVM 鎖不能協調其他外部程序。前端仍是原生單檔 JavaScript；本輪重點是 Java 的責任分工。
