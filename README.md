# PromptForge

用 Java 在本地整理交給 coding agent 的任務：挑選相關專案規則，輸出附來源的短 prompt，並搜尋本地 ADR／policy。

這是一個以 Java 實作與逐 commit review 為主的 side project。目前提供 CLI，不需要 LLM、API key、模型下載、資料庫或 Spring Boot。你也可以直接把輸出當成一般任務說明使用。

## 真實用途

每次叫 agent 改程式，都要重新想起專案的限制。例如修改付款重試時，要保留冪等性、避免重複扣款。PromptForge 把規則保存在專案檔案中，依任務挑選相關內容，讓你看到加了甚麼、來源在哪裏。

輸入：

```text
請修改付款重試邏輯
```

輸出：

```text
請修改付款重試邏輯

專案規則：
- 保留冪等性，並檢查重複扣款的測試 [payment.retry; .promptforge/rules/payment.rule]
```

沒有規則命中時返回原任務；選中的規則有衝突時回報錯誤，讓人處理。

## 目前功能與界限

| 功能 | 現在的行為 |
|---|---|
| Frame | 用 keyword 選擇規則，補到原任務並附來源 |
| Query | 用 BM25 搜尋本地 Markdown，顯示最多 3 個結果與短摘錄 |
| Validate | 檢查規則與知識卡格式，顯示讀取數量 |
| 衝突檢查 | 拒絕互相衝突的已選規則 |
| JAR | 打包後用 java -jar 執行 |
| Skill 入口 | 提供按需呼叫 CLI 的 prompt-frame skill 檔案 |

規則選擇是文字匹配，BM25 是文字檢索。目前不生成答案、不自動把搜尋結果注入 frame，也沒有 agent 自主執行迴圈。它是未來工作台的基礎，尚未是完整 Agentic RAG。輸出有字元限制，但沒有精確 token 計數或固定節省比例的保證。

## 快速開始

需要 JDK 15 或以上，且 java、javac 可在 PATH 找到，以及 PowerShell。現有 CI 設定使用 JDK 21；腳本以 `--release 15` 編譯。不需要 Maven。

在 PowerShell 執行：

```powershell
cd D:\PromptForge
java -version
javac -version

.\frame.ps1 "請修改付款重試邏輯" -Project .\examples\payments
.\query.ps1 "付款重試" -Project .\examples\payments
```

`-Project` 是要讀取規則／知識的目標專案，省略時使用目前目錄。兩個 wrapper 每次都會編譯 Java；經常使用可以先打包。

### 打包後執行

```powershell
.\scripts\package.ps1

java -jar .\build\PromptForge.jar --project .\examples\payments --task "修改付款重試"
java -jar .\build\PromptForge.jar --project .\examples\payments --query "付款重試"
java -jar .\build\PromptForge.jar --project .\examples\payments --validate
```

也支援 `--stdin` 讀取 UTF-8 任務文字。每次只選 `--task`、`--stdin`、`--query` 或 `--validate` 一種模式。省略 `--project` 時使用目前目錄。

修改 Java source 後要重新打包。修改規則或知識卡不用重新打包，下次執行會讀取最新檔案。

## 放入自己的專案

在目標專案建立以下檔案，全部使用 UTF-8：

```text
你的專案/
  .promptforge/
    rules/
      payment.rule
    knowledge/
      adr/
        0001-idempotency.md
      policy/
        testing.md
```

### 規則

payment.rule：

```properties
id=payment.retry
keywords=付款,重試
instruction=保留冪等性，並檢查重複扣款的測試
```

id 必須唯一，instruction 是單行文字；逗號分隔的 keywords 必須全部命中才選中規則。可以加 `conflicts=另一個規則ID` 宣告衝突，見 [衝突範例](examples/conflicts/README.md)。

### 知識卡

knowledge/adr/0001-idempotency.md：

```markdown
# 付款重試與冪等性

同一交易只可扣款一次。重試前先查詢交易狀態，並覆蓋重複請求和逾時後重試的測試。
```

每張卡以 `# 標題` 開始，後面要有正文。目前每檔上限 8 KiB；建議把長文件拆成能單獨理解的小卡。repo 的 docs/adr 不會自動被搜尋；搜尋來源是目標專案的 .promptforge/knowledge。

更新後先跑 `--validate`，再以真實任務試 frame／query。Validate 檢查格式，不代表規則正確或所有任務都沒有衝突。見 [維護說明](docs/maintenance.md) 與 [付款範例](examples/payments/README.md)。

## 測試與 CI

```powershell
.\scripts\test.ps1
.\scripts\check-skill.ps1
.\scripts\package.ps1
.\scripts\package-smoke.ps1
```

Java 測試使用 main 與 assertions，目前共 12 個測試類，尚未採用 JUnit。JAR smoke test 檢查打包後的規則選擇、知識搜尋與中文輸出；skill 檢查是檔案契約檢查，不是模型品質評估。

[GitHub Actions 設定](.github/workflows/ci.yml) 包含 Windows／Ubuntu、JDK 21 的相同檢查。目前沒有 remote，未有遠端 CI 執行證據。部分 Windows 符號連結測試可能因權限而跳過，請留意 skip。

CLI 成功時回傳 0；錯誤寫到 stderr：

| Exit code | 意思 |
|---|---|
| 2 | 參數或任務輸入錯誤 |
| 3 | 規則／知識格式錯誤 |
| 4 | 檔案 I/O 錯誤 |
| 5 | Frame 超出大小限制 |
| 6 | 已選規則互相衝突 |

## Java 寫在哪裏？

| 部分 | 主要類別 | 可以學習的內容 |
|---|---|---|
| 規則資料與讀取 | Rule、RuleFileLoader | 不可變物件、集合、Path、格式驗證 |
| 規則選擇與衝突 | RuleSelector、RuleConflictChecker | 行為分工、穩定排序、例外 |
| Prompt 組合 | TaskFrameCompiler、TaskFrame | 字串組合、邊界與輸出契約 |
| 本地搜尋 | KnowledgeCardLoader、Bm25Index、SearchHit | 檔案遍歷、索引、排名 |
| CLI | Main | 參數、stdout／stderr、exit code、UTF-8 |

主要程式在 src/main/java/promptforge；測試在 src/test/java/promptforge；決策在 [docs/adr](docs/adr)。

先從 [review 入口](docs/reviews/START-HERE.md) 開始，再讀 [31 個原實作 commit 的 review 卡](docs/reviews/commit-review.md)。每張卡都有用途、檔案、Java 問題與你的記錄欄。Git 日期已依要求重新編排，原時間、新時間與 hash 見 [對照紀錄](docs/reviews/date-rewrite.md)；重新編排日期不代表實際開發時間。

## Skill 入口

[prompt-frame/SKILL.md](skills/prompt-frame/SKILL.md) 按需呼叫本地 CLI，專案規則仍放在各專案的 .promptforge/rules。目前 skill 內的呼叫位置是 `D:\PromptForge\frame.ps1`；搬動專案後要同步修改。

本 repo 保存 skill 原始檔。是否安裝、觸發及執行成功取決於使用端設定；CLI 本身不需要 agent 或模型服務。

## 下一步：本地 Web 工作台

Web 尚未實作。計劃使用 Spring Boot，在本地檢視／編輯 prompt 區塊與 skill，預覽候選 prompt，保存評估證據與指定版本的人手批准。

先 review 原實作，再修基礎、完成共用 Java service 與資料契約，之後才加入 Web。已知待處理項目包括 build 舊 class 殘留、stdin 讀入前的大小限制、知識總量限制，以及 Main 責任拆分。

- [Web 前的核心整理](docs/plans/pre-web-foundation.md)
- [工作台計劃：13 個功能小步與階段驗收](docs/plans/local-workbench.md)
- [ADR 0014：工作台方向，狀態 Proposed](docs/adr/0014-local-workbench-direction.md)

新功能先定義 ADR、驗收與測試，再做小實作 commit；大階段檢查 edge case 與真實用途。見 [開發約定](docs/process.md)。
