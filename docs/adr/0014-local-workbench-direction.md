# ADR 0014：本地 Prompt / Skill 工作台方向

狀態：Proposed（等待使用者 review，未開始 Web 實作）。日期：2026-09-26。

## 原因

CLI 已提供規則 frame 與本地知識查詢。新需求是看清楚及編輯 prompt / skill 結構，保存評估證據與人手批准。單純 CLI 不方便處理這些互動。

## 提議

在完成原 31 commits 人手 review、必要基礎修正與資料契約後，加入 Spring Boot MVC 本地 Web adapter。普通 Java core 共用於 CLI 與 Web；`.promptforge` 檔案與 Git 是來源，不增加必要 LLM 或付費服務。

匯入內容預設草稿，評估與批准分開，批准綁定內容 hash。預覽表示準備交給 agent 的候選內容，不能聲稱觀察 agent 實際上下文。

## 代價與替代

需學習 Maven、Spring injection、Controller、template 與 HTTP 測試，並處理安全顯示、revision 衝突及原子保存。僅擴充 CLI 可保持簡單，但不符合可視化編輯用途；先採 MVC / Thymeleaf，避免同时引入另一套大型前端框架。

這不改寫既有 ADR 的歷史決策；原先無 Spring 的 core 繼續成立。逐步範圍與 gate 見 [工作台計劃](../plans/local-workbench.md)。
