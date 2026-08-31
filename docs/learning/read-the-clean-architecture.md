# 從一個操作學 Java

這份是你的本地學習入口。先用目前版本操作，再沿一條呼叫路徑讀程式。每次只回答「這個物件接收甚麼、負責甚麼、回傳甚麼」。

## 先認清三個模組

| 模組 | 用人的語言說 | 先讀甚麼 |
|---|---|---|
| core | 選規則、找知識、組成文字 | `src/main/java/promptforge/FrameService.java` |
| workbench | 管理可編輯的 Prompt、Skill 及檔案版本 | `workbench/src/main/java/promptforge/workbench/PromptService.java` |
| web | 接收瀏覽器操作，轉交 Java 服務 | `web/src/main/java/promptforge/web/WorkbenchController.java` |

依賴是 web → workbench → core。core 和 workbench 不 import Spring。`WorkbenchConfiguration` 用 `@Bean` 將普通 Java 物件交給 Spring 管理；constructor injection 是把物件需要的合作伙伴傳進建構子。

下面簡寫的 Java 檔名都在對應模組的 `src/main/java/promptforge/<模組>`；core 保留原有 `src/main/java/promptforge` 路徑，讓 CLI 腳本繼續使用。測試在相應的 `src/test/java`。

## 第一步：預覽 Prompt

1. 在畫面輸入任務及區塊，再按預覽。
2. 讀 `WorkbenchController.preview`：HTTP JSON 轉成 `PromptDocument`。
3. 讀 `PromptService.preview`：用專案 ID 找到已註冊專案。
4. 讀 `PromptPreviewService.preview`：先呼叫 `PromptTemplateCompiler` 代入變數，再呼叫 core 的 `FrameService`。
5. 讀 `PromptWorkflowTest`：找出怎樣證明共用核心的結果一致。

學習重點：record 保存資料、service 協調操作、方法參數如何一路傳遞。

自己驗收：能說明「變數未填」由哪個類別發現，錯誤如何返回畫面。

## 第二步：保存 Prompt

1. 讀 `WorkbenchController.savePrompt`：先確認網址與文件 ID 相同。
2. 讀 `PromptService.save`：取得該專案的 repository，呼叫保存。
3. 讀 `PromptRepository`：它只規定 load/list/diff/save。
4. 讀 `JsonPromptRepository.save`：文件轉 JSON，再交給 `RevisionFileStore`。
5. 讀 `RevisionFileStore.save`：核對 revision、写暫存檔、原子替換。
6. 對照 `FileStoreTest` 的兩個編輯者案例。

學習重點：interface 是契約；JSON 是一個實作；呼叫端不必知道檔案格式。`PromptService` 不再把 repository 交出去。

自己驗收：兩個分頁同時讀取，先保存第一份，再保存第二份，能解釋為何後者會衝突。

## 第三步：Skill 的四個角色

| 類別 | 責任 | 先讀的方法 |
|---|---|---|
| SkillService | 對外操作入口、批准流程、組合回應 | `transition`、`withEvaluations` |
| SkillBundleStore | Skill 原文、references、hash、版本保存 | `get`、`save`、`bundleHash` |
| SkillTransfer | 匯入／匯出暫存與完成後發布 | `importSkill`、`export` |
| SkillEvaluationService | 驗證及保存外部評估證據 | `addEvaluation`、`evaluations` |

1. 匯入一個範例 Skill，從 `SkillService.importSkill` 追到 `SkillTransfer`。
2. 看匯入怎樣先放暫存目錄，成功後才移至目的地。
3. 只讀 `transition`，逐個追 review → approve → activate。
4. 修改內容，再看 hash 為甚麼令舊批准失效。
5. 最後讀評估流程：它記錄證據，沒有授予批准的操作。

學習重點：composition（物件合作），以及 package-private（內部合作類別不成為公開 API）。沒有為每個類別增加介面；需要替換的 Prompt 儲存才保留 repository 契約。

自己驗收：能指出「修改檔案格式」與「改批准條件」應該分別改哪個類別。對照 `SkillWorkflowTest`、`EvaluationWorkflowTest`。

## 第四步：錯誤如何變成 HTTP 回應

1. `FailureKind` 定義 INVALID_INPUT、NOT_FOUND、CONFLICT、STORAGE_FAILURE。
2. workbench 丟出帶有種類的 `WorkbenchException`。
3. `HttpErrors` 將種類對應到 422、404、409、500。
4. `HttpErrorsTest` 使用真正的 MockMvc 路由驗證對應。

學習重點：Java 應用只描述發生甚麼事，由 Web 決定怎樣向瀏覽器表達。將來 CLI 使用同一個服務，也可以用文字或 exit code 表達同一個錯誤。

## 怎樣判斷這次整理有沒有用

- 找一項用途時，能找到一個操作入口。
- 修改批准條件時，不必先看 JSON 評估保存。
- HTTP Controller 不建立 loader、不直接取得 Prompt repository。
- 每個類別的合作對象可以從欄位／建構子讀出來。
- `LearningArchitectureTest` 防止核心引入 Web framework，亦防止 PromptService 再暴露 repository。

目前仍是本機檔案工具。RevisionFileStore 的鎖在單個 JVM 內，不是跨程序交易；多檔 Skill 更新也不是資料庫交易。這些是要理解的邊界，不能因為分成多個類別便當成已解決。

每段 review 寫四行：用途、輸入／輸出、測試證據、我想自己修改的地方。原始逐 commit 歷史仍可讀；這份描述的是整理後的目前版本。
