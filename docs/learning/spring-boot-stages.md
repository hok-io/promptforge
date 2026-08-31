目前版本已整理架構：先讀 [從一個操作學 Java](read-the-clean-architecture.md)。下面日期分布記錄的是重構前階段；其後新增的小步見 [review 清單](../reviews/web-commit-review.md)。

# Spring Boot Web：分段學習路線（提交集中於 9 月 17–21 日）

這份路線把 PromptForge 從 Java 基礎一路拆到可在瀏覽器操作的本機 Spring Boot 工作台。每個學習段落只追一個可操作的主題：先做使用者操作，再沿 Java 呼叫路徑讀類別與測試。段落順序表示學習安排，實際提交日期以逐 commit 清單為準，不表示該日每個歷史提交都已通過完整測試。測試先行的提交可能在功能尚未完成時暫時失敗；把它當作待實作的契約，讀到後續階段再確認如何滿足。

實際操作以目前完整版本為準；歷史 commit 用來讀差異，不要求每個測試先行版本都能啟動。提交分布：9/17 8 個、9/18 9 個、9/19 10 個、9/20 10 個、9/21 8 個；9/22 2 個、9/23 1 個、9/24 1 個文件整理。9/25–26 沒有提交。時間只用早上及晚上，下午留空。完整 hash、時間、用途及 review 問題見 [逐 commit 清單](../reviews/web-commit-review.md)。Java 類別位於 `src/main/java/promptforge`、`workbench/src/main/java/promptforge/workbench`、`web/src/main/java/promptforge/web`；測試位於各模組對應的 `src/test/java`。

## 第 1 段：Maven、JDK 與模組地圖

**目的與前置：**建立可理解的 Java 專案地圖。先知道終端機、資料夾、`.java` 檔和測試各自是甚麼；不需先會 Spring。

1. 看根目錄 `pom.xml`、`mvnw`／`mvnw.cmd`、`src/main/java`、`src/test/java`、`workbench`、`web`。
2. 在 `pom.xml` 找 JDK release、Maven modules 和依賴；理解 Maven Wrapper 讓專案用指定 Maven 執行。
3. 由 `src/main/java/promptforge/Main.java` 追到 `FrameService`，辨認 `main`、物件建立與方法呼叫。
4. 看 `SmokeTest.java` 和 `CliTest.java`，區分生產程式與測試程式；認識多模組如何分工。

**你要做：**用 `frame.ps1` 啟動 CLI（Maven Wrapper 是建置工具），輸入一個任務，查看輸出；再從 `Main` 找到輸出形成的位置。

**具體檢查：**JDK 版本符合根 `pom.xml`，CLI smoke 操作可走完。**回顧：**`web` 模組為甚麼可以依賴 `workbench` 和核心程式？

## 第 2 段：載入限制與測試契約

**目的與前置：**學會把「能讀檔」變成有界、可預期的讀檔；承接 Maven 測試與 Java 例外概念。

1. 讀 `RuleFileLoader.java`、`KnowledgeCardLoader.java`，找出它們如何把檔案轉為 `Rule`、`KnowledgeCard`。
2. 追 `ProjectPaths.java` 的路徑正規化／越界檢查，以及 .rule 欄位／Markdown 格式錯誤如何回報。
3. 看 `LoaderLimitsTest.java`、`RuleBoundaryTest.java`，把輸入大小、數量、路徑邊界對應到 assertion。
4. 看 `RuleFormatException`、`KnowledgeFormatException`，分清格式不合法和一般程式錯誤。

**你要做：**使用範例專案讀取規則與知識；再試一個格式錯誤或超出界線的輸入，觀察明確錯誤。

**具體檢查：**合法資料可載入，超限或越界資料不會被默默接受。**回顧：**每個載入限制保護了哪一項實際資源或使用者預期？

## 第 3 段：不可變 Prompt 與變數

**目的與前置：**用不可變資料模型安全表示草稿，理解 Java record、集合複製及輸入驗證。

1. 讀 `workbench/src/main/java/promptforge/workbench/PromptBlock.java`、`PromptDocument.java` 和 `PromptDomainTest.java`。
2. 觀察 record 的欄位、建構子與 compact constructor；追 `List.copyOf`、`Map.copyOf` 如何避免外部修改。
3. 用 `PromptTemplateCompiler.java` 看 `${feature}` 等變數如何被辨認、替換；缺值時如何指出問題。
4. 比較 `ruleIds == null`（自動選規則）與空清單（不選規則），再看 `PromptPreviewService` 如何把文件轉為預覽。

**你要做：**建立含「背景」與「驗收」區塊的 Prompt，填入變數並預覽；試著省略必填變數。

**具體檢查：**預覽代入正確，缺變數或無效欄位有可理解的錯誤。**回顧：**複製清單和 Map 保護了誰免受哪種意外修改？

## 第 4 段：Registry、Repository 與 FrameService

**目的與前置：**理解服務如何協調資料來源與領域功能，讓 CLI 和 Web 共用同一套 Java 邏輯。

1. 讀 `ProjectRegistry.java`、`Project.java`：看專案如何載入、查找和驗證。
2. 讀 `PromptRepository.java`、`JsonPromptRepository.java`，比較介面契約與 JSON 檔案實作。
3. 追 `PromptService.java`、`PromptPreviewService.java` 到核心 `FrameService.java`；服務傳入資料，核心負責組成 frame。
4. 看 `PromptWorkflowTest.java` 和 `ProjectRegistryTest.java` 的臨時專案 fixture，辨認 service、repository 和測試替身／真實檔案的分工。

**你要做：**從註冊專案載入一個 Prompt，預覽它，再看保存後如何從 repository 重讀。

**具體檢查：**CLI 與 Web 呼叫同一個 `FrameService`，repository 以專案為界。**回顧：**若未來把 JSON 改成另一種儲存方式，哪一層最應該吸收改動？

## 第 5 段：並行保存、路徑邊界與 HTTP 測試

**目的與前置：**理解兩個分頁同時編輯時如何防止覆蓋，並知道服務端測試能證明甚麼。

1. 讀 `WorkspaceFiles.java`、`RevisionFileStore.java`、`VersionedText.java`，看路徑解析、SHA-256 revision、暫存檔與原子 move。
2. 追 `PromptRepository.diff`／`save` 和 `FileStoreTest.java`：revision 不合時回報衝突，而非覆蓋新內容。
3. 看 `WorkspaceBoundaryTest.java`，追相對路徑如何被限制在已註冊專案內。
4. 對照 `WebContractTest.java` 的 MockMvc 與 `LocalServerTest.java` 的真實 localhost socket：前者驗路由契約，後者驗真實 HTTP 連線／cookie 等行為。

**你要做：**兩份草稿載入同一版本，先保存一份，再試著保存另一份；觀察差異檢視與衝突回應。

**具體檢查：**陳舊 revision 不會靜默覆蓋；專案外路徑被拒絕。**回顧：**JVM 內的鎖能否阻止另一個程式同時改檔？

## 第 6 段：Skill 是普通 Java 管理的資料

**目的與前置：**把 Skill 當成可讀、可匯入、可審核的文字資料；運用前幾天的檔案邊界、revision 與服務概念。

1. 讀 `SkillParser.java`、`SkillMetadata.java`、`SkillReference.java`：了解 frontmatter、未知欄位保留及 references。
2. 追 `SkillService.importSkill`、`get`、`save`，再看 `WorkspaceFiles` 如何限制匯入位置和檔案數量／大小。
3. 看 `SkillWorkflowTest.java`：依序匯入、檢視、編輯、送出差異、保存、review、approve、activate。
4. 讀 `EvaluationArtifact.java`、`EvaluationView.java` 和 `EvaluationWorkflowTest.java`，比較人的批准與評估記錄；確認 skill 文字不會被當程式執行。

**你要做：**匯入範例 Skill，查看內容與 references，編輯並檢視變更，走一次狀態流程；再改正文確認舊狀態失效。

**具體檢查：**狀態綁定目前內容 revision，評估留有固定記錄。**回顧：**為甚麼模型評語或測試記錄不等於人的批准？

## 第 7 段：Spring 啟動、專案檢視、預覽與 Skill API

**目的與前置：**把 Spring 當作啟動與組裝 HTTP 的框架；Java 領域類別仍可獨立理解與測試。

1. 讀 `PromptForgeWeb.java`、`WorkbenchConfiguration.java`：追 `main`、`@Configuration`、`@Bean` 與 constructor injection。
2. 讀 `WorkbenchController.java` 中 `/projects`、`context`、`knowledge/search`、`preview` 路由，對應 request／response record。
3. 追 Prompt diff/save 路由至 `PromptRepository`；追 Skill import、diff、save、state、evaluations、export 至 `SkillService`。
4. 看 `HttpErrors.java`、`LocalRequestFilter.java` 與 `application.properties`，理解錯誤狀態、localhost 限制與 CSRF token/header。
5. 用瀏覽器或 HTTP client 取得 CSRF token，載入專案 context、搜尋知識、預覽並保存 Prompt，接著試 Skill API。

**具體檢查：**使用者操作可在 API 找到對應路由，409 表示版本衝突，422 表示輸入無效；寫入請求帶 CSRF header。**回顧：**Controller 為甚麼不自己解析檔案、計算 revision？

## 第 8 段：修補邊界並讀測試

**目的與前置：**練習從失敗案例看出資料邊界，而不是只把 happy path 當完成。

1. 回看 `PromptDomainTest`、`FileStoreTest`、`WorkspaceBoundaryTest`、`SkillParserTest`、`EvaluationWorkflowTest` 的測試名稱、fixture 和 assertion。
2. 以真實操作檢查 Prompt ID 與 URL 一致、未知路徑、錯誤 revision、無效 evidence path、Skill bundle 超限等邊界。
3. 將測試失敗對到負責類別：record 驗證、`WorkspaceFiles`、repository 或 service；避免把規則散落在 Controller。
4. 對照 9/18–23 的契約與後續實作，記下先寫測試但當時尚未實作的案例；不要把歷史上的紅色測試誤寫成已通過。

**你要做：**挑一個既有測試，先解釋它證明甚麼、沒證明甚麼，再沿呼叫路徑找到實作。

**具體檢查：**可指出一個具體輸入、預期狀態／結果和負責類別。**回顧：**這項限制對應到使用者可能遇到的哪種錯誤？

## 第 9 段：靜態介面與 API 串接

**目的與前置：**理解原生 HTML、CSS、JavaScript 如何呼叫已定義的 Java API，不把介面程式和伺服器責任混在一起。

1. 讀 `web/src/main/resources/static/index.html`、`style.css`，定位 Prompt 與 Skill 工作區、表單、側欄及窄螢幕版面。
2. 讀 `app.js` 的 API helper 和 CSRF 流程；把 `fetch` URL 對照 `WorkbenchController` 路由。
3. 追專案切換、context 載入、規則／知識選擇與預覽；找出畫面如何呈現 loading、錯誤和 response。
4. 追 Prompt 草稿、diff 確認、revision 保存；再看 Skill 匯入、review／approve、評估與匯出流程。
5. 看 `BrowserWorkflowTest.java` 的瀏覽器操作與 assertion，辨認它能驗證畫面、真實 API、保存重載哪些部分。

**你要做：**在瀏覽器切換專案、調整選擇、更新預覽、保存並重載 Prompt；再完成一次 Skill 匯入和審核操作。

**具體檢查：**瀏覽器操作穿過 Java API，畫面狀態與伺服器回應相符。**回顧：**diff 確認和 409 衝突各保護哪一種情況？

## 第 10 段：實際瀏覽器修正、驗收與打包

**目的與前置：**從真實瀏覽器操作發現並修正問題，最後驗收可交付的本機套件。

1. 重現操作痛點：桌面側欄、窄畫面選擇、切換專案時未保存草稿、舊請求晚到、錯誤訊息或文字安全。
2. 對照 `index.html`／`style.css`／`app.js` 與 `BrowserWorkflowTest.java`，釐清問題屬於畫面、API 使用還是 Java 契約。
3. 檢視修補是否維持 `textContent` 顯示、CSRF、revision 確認、專案路徑限制與 localhost 綁定。
4. 查看 `scripts/test.ps1`、`scripts/build-web.ps1`、`scripts/package.ps1` 和實際驗證紀錄；區分單元、檔案整合、MockMvc、真實 HTTP、Playwright、JAR smoke 各自覆蓋範圍。
5. 以瀏覽器走完 Prompt 保存／重載及 Skill 匯入／核准／匯出，再啟動打包結果做 smoke 操作。

**具體檢查：**記下當次執行的測試與打包結果，不把歷史 commit 的結果當成今天重跑；確認套件能在本機啟動且只對 localhost 提供服務。**回顧：**哪個端到端測試最接近你真正的使用方式？它還漏掉甚麼？

## 每段 review 的四個問題

1. 使用者做了甚麼操作，原本遇到甚麼問題？
2. 哪個 Java 類別與方法負責這段行為？
3. 哪項測試提供證據，仍有哪些情況未涵蓋？
4. 我能否不看答案，從輸入一路追到畫面或檔案結果？
