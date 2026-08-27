# 本地 Prompt / Skill 工作台計劃

狀態：已按使用者「先實作好了」指示開始實作，原 review gate 改為交付後逐 commit 學習。實作採 ADR 0019 的普通 HTML／JS + Spring MVC；驗證結果另見測試報告，不以此計劃代替通過證據。

## 真實用途

你開一個專案，睇到它的規則、ADR、prompt 與 skill。準備工作時，選適合的內容，調整 prompt，右邊即時睇編譯結果。修改前後有 diff，保存成檔案，之後可以自己用 Git review。另一個 agent 可以提供評估檔，但不能因此自動取得你的批准。

例子：你想叫 Codex 修改付款功能。工作台讓你找到 idempotency ADR，挑選相關規則，填入任務與驗收條件，輸出一段有來源的 task frame。你自行交給 agent。右邊是「候選 prompt 預覽」，不是 agent 實際收到的完整上下文；工具無法觀察隱藏或其他來源的上下文。

| 部分 | 現在已有 | 新設計 |
|---|---|---|
| 規則 | keyword 選擇、衝突檢查、frame | 可見來源、手動選擇及預覽；修改規則仍編輯本地檔案 |
| 知識 | Markdown 卡、BM25、query | 選擇知識引用，不自動塞全部內容 |
| Prompt | task 文字 | 可排序的結構區塊與變數 |
| Skill | 一個 CLI skill | 檢視、匯入、編輯、評估紀錄 |
| 狀態 | Git 檔案 | 指定版本的 review／批准／啟用 |
| Web | 無 | 本地 Spring MVC 工作台 |

## 先把資料契約做成 Java

實作主要物件為 Project、PromptDocument、SkillDetails、SkillMetadata 和 EvaluationArtifact。啟用狀態保存在 SkillMetadata，綁定 bundle hash。驗證與操作放在普通 Java service；Spring Controller 接收 JSON 請求與回傳資料。CLI 與 Web 共用 FrameService，不用 shell 呼叫 CLI。

依賴方向：CLI / Web → application service → domain；service 透過 repository interface 讀寫本地檔案，file adapter 實作 interface。core 不依賴 Spring、HTTP 或畫面。先以清晰 package 分層，確有需要才拆 Maven module。

PromptDocument 有有序區塊，例如背景、目標、流程、限制、輸出、驗收條件；可刪除、改名及自訂，沒有強制每個 prompt 都要 role。變數只做明確的文字替換，不執行程式；缺值顯示錯誤，不猜內容。保留兩種輸出：短 task frame、完整模板。預覽展示來源及省略項，不宣稱 token 精確計數或保證節省比例。

Skill 保留原本 SKILL.md、frontmatter、未知欄位與 references。名稱／描述等可辨識欄位供畫面展示；goal、tools、examples 等沒有資料就標示未知，不能自行創造為必填格式。工作台自己的 provenance、狀態與評估放 sidecar，避免破壞 skill 相容性。不把完整 skill 一律附加到 prompt。

EvaluationArtifact 記錄內容 hash、評估來源、模型及版本（未知可空）、時間、rubric 版本、發現與證據檔案。區分「模型意見」與「可重跑測試結果」。沒有證據不能製造分數。可以手動匯入 agent 產生的評估，工具本身沒有必要呼叫付費 API 或本地 LLM。內容更新後，舊評估顯示過期。

Imported → Reviewed → Approved → Active 是不同事件，綁定內容 hash。人手批准與模型評估分開；修改內容不能繼承舊批准。Active 只表示工作台選用的版本，不表示已安裝或被 Codex 觸發。安裝／匯出需顯示具體位置與差異，不能默默修改全域 skill。

## 本地檔案是來源

沿用 `.promptforge`，不直接搬成附件提議的其他目錄名。舊 rules / knowledge 保持可讀。

```text
.promptforge/
  rules/*.rule
  knowledge/**/*.md
  prompts/<id>/document.json
  skills/local/<id>/SKILL.md
  skills/local/<id>/references/...
  skills/local/<id>/.promptforge-metadata.json
  skills/imports/<id>/...          # 未批准草稿
  evaluations/<skillId>/<hash>/<evaluationId>.json
  # 啟用版本放在各 skill sidecar；不用獨立 activation.json
```

第一版不需要資料庫。專案 registry 只保存已登記專案 ID 與目錄，HTTP 接口使用 ID，不能接收任意檔案路徑。每次保存檢查舊 revision/hash，變更則回報衝突，先預覽 diff，再原子寫入。Git commit 仍由人執行；自動 commit 不是第一版功能。

## 畫面與 Spring Boot

左邊：已登記專案及 prompt / skill 清單。中間：區塊或原始 Markdown 編輯器。右邊：候選 prompt、來源、缺變數、衝突提示。Skill 詳情頁展示用途、原文、references、來源、評估、批准版本，不假裝已知 agent 如何實際使用它。

採用 JDK 21、Maven Wrapper 3.3.4／Maven 3.9.11、Spring Boot 3.5.16 MVC + 普通 HTML／JS（ADR 0019 取代 Thymeleaf）。先學 Controller → service → repository，一次只引入一個 Spring 概念。Spring 管理物件組裝，規則處理仍是普通 Java OOP。[Spring 3.5 系統需求](https://docs.spring.io/spring-boot/3.5/system-requirements.html)。

伺服器只綁定 127.0.0.1。CSS、JS、字型都本地提供，運行不需 CDN；首次取得建置依賴可能要上網。檔案讀寫限制在登記專案內，拒絕 traversal／越界連結；Markdown 必須安全顯示，保留 CSRF 保護、輸入限制與 revision 檢查。匯入資料不能執行 shell 或 agent 指令。

## 每個 Web 小步怎樣做

每列是一個功能單位：先 ADR／驗收／測試 commit，再小實作 commit，必要修正再獨立 commit。實際檔案及通過結果以各 ADR、Java 學習路線和驗證報告為準。W01–W13 已有實作；預覽目前用明確「更新預覽」按鈕，沒有每次打字都送請求。

| 步驟 | 實作內容 | 先寫的測試／驗收 | Java 學習重點 |
|---|---|---|---|
| W01 | 鎖版本、Web adapter、localhost 啟動 | 啟動及綁定位址，core 無 Spring 依賴 | Maven dependency、入口 |
| W02 | 唯讀專案清單 | 空 registry、壞資料、未知 ID | constructor injection、Controller |
| W03 | 顯示規則與知識來源 | 沒資料、中文、危險 HTML | DTO、純文字 DOM |
| W04 | 呼叫既有 frame / query service | CLI 與 Web 同輸入同結果 | 共用 service、MockMvc |
| W05 | 唯讀 prompt 區塊預覽 | 自訂／缺區塊、排序、缺變數 | domain model、驗證 |
| W06 | 編輯草稿與按鈕更新預覽 | 未保存不改檔、取消確認 | request DTO、表單 |
| W07 | revision-safe 保存 prompt | 同時修改回衝突、寫失敗保留舊檔 | repository、原子寫入 |
| W08 | 唯讀 skill inspector | 未知 frontmatter、缺 references、中文 | parser 與保留原文 |
| W09 | 本地資料夾匯入草稿 | 重複 ID、越界／大檔、未批准不啟用 | provenance、狀態 |
| W10 | skill diff 與保存 | 未知欄位保存、舊批准失效 | hash、版本檢查 |
| W11 | 匯入／顯示評估紀錄 | 壞 schema、缺證據、hash 過期 | artifact 驗證 |
| W12 | 人手 review／批准指定版本 | 未 review 不批准、變更後失效 | 狀態轉移 |
| W13 | 選用／匯出指定版本 | 明確目標、衝突不覆蓋、可取消 | application use case |

實際 JSON 路由使用 `/api/projects/{id}/prompts`、`/skills`、`/knowledge/search`；評估放在指定 skill 的 `/evaluations`。壞 JSON 400、找不到 404、版本衝突 409、內容驗證 422；畫面顯示原因。正式接口契約在 Controller、ADR 0024 與測試中。

## 大階段驗收

1. W01–W04 唯讀：以 payments、conflicts 兩個專案操作；檢查空資料、中文、衝突、路徑限制；斷網仍可使用。確保顯示結果與 CLI 一致。
2. W05–W07 Prompt：以真實付款修改任務建立 prompt，編輯、取消、保存、重開；另一程序改檔時不得覆蓋。確認結果真能直接交給 agent，而不只是漂亮表單。
3. W08–W10 Skill：匯入真實 skill，保留原文／references，修改後舊批准失效；未批准草稿不能被默默安裝。
4. W11–W13 評估與選用：匯入外部評估，展示測試證據／模型意見／過期版本；由人選擇版本並 review 匯出 diff。

CI 每功能先新增合適測試：core 單元測試、temp directory 檔案整合測試、Web MockMvc，階段末本地離線 UI smoke。測試報告分 pass / fail / skip。沒有執行遠端 CI 就不宣稱 CI 通過；建立或推送遠端 repo 另依你的指示。

## 開始條件與暫緩內容

使用者已明確要求先實作 Web，因此不再等待原 31 commits review。必要基礎修正、共用 service、資料契約和測試先落地，再接 Web。交付後按 [Java 學習路線](../learning/java-web-tour.md) 逐 commit review。

第一版不做共享 marketplace、雲端帳號、任意 agent 執行、MCP server、語義 embedding 或 LLM 自動評分。這些不是 Java 能力的必要證明。先以清楚的 domain、service、檔案一致性、錯誤處理與可驗證測試證明實作能力。
