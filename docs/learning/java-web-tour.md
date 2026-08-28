# 用 Web 工作台學 Java

這份是你的本地 review 路線。先跑功能，再追一條操作如何穿過 Java；每次只 review 一個 commit，不需要一次學完整 Spring。

## 1. 一個物件代表甚麼

先看 `PromptDocument`、`PromptBlock` 和 `PromptDomainTest`。實際操作：加「背景」和「驗收」區塊，再用 `${feature}` 變數。

逐項理解：record 自動提供甚麼？compact constructor 何時執行？為何複製 List 和 Map？`ruleIds == null` 與空 List 分別表示甚麼？缺變數為何拋例外而不是猜值？

練習：先加測試，令重複區塊名稱回報錯誤。思考你的實際使用是否真的需要這條限制，才決定是否修改。

## 2. 一次保存如何避免覆蓋

看 `WorkspaceFiles`、`RevisionFileStore`、`FileStoreTest`。實際操作：兩個分頁開相同 prompt，第一頁保存後，第二頁保存應顯示衝突。

逐項理解：Path normalize 與 real path 有何不同？SHA-256 在此為何代表 revision？synchronized 保護甚麼？為何先寫臨時檔再 move？try-with-resources 如何關閉 stream？失敗時原檔是否保留？

限制：鎖協調本 JVM 的保存；外部程式仍可能在最後檢查與 move 之間寫入。不是跨程式資料庫交易。

## 3. Interface 的實際用途

看 `PromptRepository`、`JsonPromptRepository`、`PromptService`。先讀 interface，再看 JSON 實作，最後看呼叫者。

逐項理解：service 需要知道 JSON 格式嗎？repository 與 file store 分工是甚麼？diff 為何不能順便保存？CLI 和 Web 怎樣共用 `FrameService`，而不啟動另一個 Java 程序？

## 4. Spring Boot 只負責組裝和 HTTP

看 `PromptForgeWeb`、`WorkbenchConfiguration`、`WorkbenchController`、`HttpErrors`。操作 preview，再在瀏覽器 Network 看 POST 的 JSON 和回應。

逐項理解：main 怎樣啟動服務？@Bean 怎樣建立普通 Java 物件？constructor injection 怎樣提供依賴？record request 怎樣從 JSON 建立？422 和 409 對使用者各代表甚麼？Controller 為何不用自己讀檔？

先理解一條 preview 路由，再看保存；不用先背完 annotation。

## 5. Skill 的狀態與內容版本

看 `SkillService`、`SkillMetadata`、`EvaluationArtifact`。匯入範例，review、批准、啟用，修改正文後觀察狀態失效。

逐項理解：為何 hash 包括 references？狀態為何放 sidecar？模型意見為何不能等於人的批准？匯出為何只接受已批准版本？未知 frontmatter 如何保留？為何不能執行 skill 文字？

## 6. 不同測試各證明甚麼

| 測試 | 要你理解的問題 |
|---|---|
| Domain JUnit | 不啟動 Spring，業務規則是否成立？ |
| TempDir 檔案整合 | 真正寫入、重讀、衝突與越界是否正確？ |
| MockMvc | JSON 路由、CSRF 和 HTTP status 是否正確？ |
| HttpClient 真實 server | 真正的 localhost socket、cookie、chunked body 是否可用？ |
| Playwright 瀏覽器 | 人按按鈕後，畫面和保存結果是否相合？ |
| CLI/JAR smoke | 舊使用方式及打包後程式是否仍可用？ |

先故意改壞一條規則，確認對應測試失敗，再還原。不要只看綠色數字，也看測試的 fixture、操作和 assertion 是否能證明實際用途。

## 每個 commit 的 review 記錄

用 `git log --oneline` 按順序看小 commit。每個記四行：

1. 使用者原本遇到甚麼問題？
2. 哪個 Java 類別負責解決？
3. 哪個測試證明它？甚麼仍未證明？
4. 你會如何簡化，或下一步想練甚麼？

README 是使用說明；ADR 是設計理由；此文件和 review 是你的學習筆記。Web Git 日期按要求放在 9 月 17–24 日，實際驗證日期記在測試報告。

Web 的分日步驟見 [9 月 17–24 日學習路線](spring-boot-stages.md)。
