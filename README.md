# PromptForge

用 Java 在本地整理交給 coding agent 的任務：挑選相關專案規則，輸出附來源的短 prompt，並搜尋本地 ADR／policy。

所有規則選擇與知識搜尋都在本地執行，不需要 LLM、API key 或模型下載。輸出可以複製給 coding agent，也可以當成一般任務說明。

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

適合反覆使用相同專案約束的任務。規則按文字選擇，可能漏掉同義表達，也可能在「不改付款重試」這類否定句誤選；使用前請檢查輸出。是否比人手整理更快，取決於資料量、重複頻率與修正成本。

## 目前功能與界限

| 功能 | 現在的行為 |
|---|---|
| Frame | 自動建議規則，或使用你明確選擇／排除的規則，附上來源 |
| 規則清單 | 顯示 ID、keywords、指令與來源，方便確認選擇 |
| 知識合併 | 把你指定的最多 3 張知識卡正文與來源加入候選 prompt |
| Query | 用 BM25 搜尋本地 Markdown，顯示最多 3 個結果與短摘錄 |
| Validate | 檢查規則與知識卡格式，顯示讀取數量 |
| 衝突檢查 | 拒絕互相衝突的已選規則 |
| JAR | 打包後用 java -jar 執行 |
| Skill 入口 | 提供按需呼叫 CLI 的 prompt-frame skill 檔案 |
| 本地 Web 工作台 | 選規則與知識卡、編排 prompt 區塊及變數，預覽和比較後保存 |
| Skill 管理 | 匯入、查看正文與 references、修改、review／批准／啟用、匯出指定版本 |
| 評估紀錄 | 匯入外部測試或模型意見及本地證據，內容更新後標示過期 |

規則選擇是文字匹配，BM25 是文字檢索。目前不生成答案、不自動把搜尋結果注入 frame，也沒有 agent 自主執行迴圈。Web 與 CLI 使用同一套 Java 規則處理。輸出有字元限制，但沒有精確 token 計數或固定節省比例的保證。

## 本地 Web：第一次使用

需要 JDK 21、PowerShell 及 Edge（完整瀏覽器測試）。Maven Wrapper 首次建置會下載依賴；建好後執行不需要網絡。若 `.tools` 有專案自己的 JDK，腳本會優先使用。

```powershell
.\scripts\build-web.ps1
.\scripts\start-web.ps1
```

開啟 [本地工作台](http://127.0.0.1:8080)。伺服器只綁定本機；終端按 Ctrl+C 停止。8080 被佔用時可用 `-Port 8081`。

先用 Payments 範例試一次完整操作：

1. 輸入「支付失敗後再試一次」，切換手動選規則，選 `payment.retry`。
2. 選付款冪等性 ADR。預覽應包含任務、規則、卡片正文和來源。
3. 加入「驗收」區塊，寫重複請求及逾時重試測試；完整模板模式可用 `${feature}` 及 JSON 變數，例如 `{"feature":"付款重試"}`。
4. 輸入名稱和 ID，先比較差異，再確認保存。重新載入可讀回同一文件；另一分頁保存舊版本會顯示衝突。
5. Skill 匯入來源填 `prompt-helper`，新 ID 填 `payment-helper`。來源來自 Payments 的 `.promptforge/inbox/prompt-helper`，不接受任意電腦路徑。
6. 檢查正文及 references，標記已 review，再批准。只有目前已批准內容可以匯出；改正文或 reference 後舊批准失效。

「啟用」只記錄工作台使用的版本，不會安裝到 Codex。匯出到目標專案 `.promptforge/exports/<名稱>`，不覆蓋已有目錄。你自行檢查後使用。

評估頁可貼入外部評估 JSON：`kind` 為 `TEST` 或 `MODEL_REVIEW`，附 `subjectHash`、來源、時間、rubric、發現及證據路徑。TEST 的證據必須是專案內已存在檔案，會記錄 SHA-256；記錄不代表工作台重新執行該測試，也不會自動批准 skill。介面提供欄位提示。

自己的專案加到根目錄 `workbench-projects.json`：每項有唯一 `id`、顯示 `name` 和專案 `root` 路徑。重啟服務後讀取。保存的是專案內 UTF-8 檔案，可以自行用 Git 檢查變更；工作台不自動 commit。

## 快速開始

需要 JDK 15 或以上，且 java、javac 可在 PATH 找到，以及 PowerShell。現有 CI 設定使用 JDK 21；腳本以 `--release 15` 編譯。不需要 Maven。

在專案根目錄開啟 PowerShell，執行：

```powershell
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

也支援 `--stdin` 讀取 UTF-8 任務文字。每次只選 `--task`、`--stdin`、`--query`、`--validate` 或 `--list-rules` 一種模式。省略 `--project` 時使用目前目錄。

`frame.ps1` 透過 UTF-8 stdin 傳遞任務，保留正文並移除尾端空白。本機 Windows Java 15 的直接 `--task`／`--query` 參數可能損壞碼頁外字元；任務請用 wrapper。Query 尚未有 stdin 模式，遇到這種情況可先用能正確傳遞的關鍵字搜尋。

修改 Java source 後要重新打包。修改規則或知識卡不用重新打包，下次執行會讀取最新檔案。

### 確認規則並合併知識

自動匹配不能理解同義表達或否定。先查看規則和搜尋結果，再明確指定需要的內容：

```powershell
java -jar .\build\PromptForge.jar --project .\examples\payments --list-rules
.\query.ps1 "付款重試" -Project .\examples\payments

.\frame.ps1 "支付失敗後再試一次" -Project .\examples\payments -Rules "payment.retry" -Knowledge ".promptforge/knowledge/adr/0001-idempotency.md"
```

`-Rules` 取代自動選擇，逗號分隔多個 ID；不是把指定 ID 加到自動結果。`-Exclude` 從選擇中排除 ID。未知或重複 ID、同時選中及排除同一 ID 都會回報錯誤。

```powershell
.\frame.ps1 "這次不改付款重試，只整理圖片" -Project .\examples\payments -Exclude "payment.retry"
.\frame.ps1 "修改 API 文件標點，不改程式" -Project .\examples\payments -Rules ''
```

`-Rules ''` 表示完全不選規則；省略 `-Rules` 才使用自動匹配。直接呼叫 JAR 可用 `--rules payment.retry`、`--exclude payment.retry`、`--knowledge 來源路徑`；不選規則用 `--no-rules`，避免 shell 丟失空字串。

知識來源只可指定專案知識卡已存在的相對路徑，逗號分隔最多 3 張，按指定順序加入完整正文，不會自動加入 query 的其他結果。最終 prompt 上限 6000 字元，超限回報錯誤，不默默截斷；請選較少或較小的卡片。逗號分隔格式不支援來源檔名本身含逗號。規則與知識選項只可配合任務模式使用。

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

每張卡以 `# 標題` 開始，後面要有正文。目前每檔上限 8 KiB；建議把長文件拆成能單獨理解的小卡。只會搜尋目標專案的 .promptforge/knowledge，不會自動搜尋其他目錄。

更新後先跑 `--validate`，再以真實任務試 frame／query。Validate 檢查格式，不代表規則正確或所有任務都沒有衝突。可先參考 [付款範例](examples/payments/README.md)。

## 試跑現有功能

先打包 JAR，再跑以下案例：

| 情境 | 指令中的模式與輸入 | 預期結果 |
|---|---|---|
| 選中規則 | payments 專案，task「修改付款重試」 | 原任務後附 payment.retry 指令與來源 |
| 沒有命中 | payments 專案，task「整理圖片」 | 原任務文字 |
| 搜尋 ADR | payments 專案，query「付款重試」 | 付款冪等性卡片、來源與摘錄 |
| 沒有搜尋結果 | payments 專案，query「火星種植」 | 顯示沒有相關本地資料 |
| 檔案格式驗證 | payments 專案，validate | 顯示規則與知識卡數量 |
| 規則衝突 | conflicts 專案，task「修改付款重試」 | stderr 顯示衝突，exit code 6 |

```powershell
java -jar .\build\PromptForge.jar --project .\examples\payments --task "整理圖片"
java -jar .\build\PromptForge.jar --project .\examples\payments --query "火星種植"
java -jar .\build\PromptForge.jar --project .\examples\conflicts --task "修改付款重試"
$LASTEXITCODE
```

最後一個案例故意觸發衝突，預期 exit code 為 6。

## 自動化測試與 CI

```powershell
.\scripts\test.ps1
.\scripts\check-skill.ps1
.\scripts\confirmed-prompt-smoke.ps1
.\scripts\package.ps1
.\scripts\package-smoke.ps1
```

CLI 保留 15 個 main／assertion 測試，亦以 JUnit 接入 Maven。Web 建置執行 domain、真實檔案整合、MockMvc API、真實 localhost HTTP 及 Playwright 瀏覽器測試。Confirmed prompt smoke 檢查 wrapper 的規則／知識選擇及空選擇；JAR smoke 檢查打包後規則、搜尋和中文輸出。Skill 入口檢查是檔案契約，不是模型品質評估。

完整 Web 測試使用 `scripts/build-web.ps1`。只有想先建置而暫不驗證瀏覽器時使用 `-SkipBrowser`；這不等於完整測試通過。其他作業系統可用 JDK 21、`./mvnw verify`，先安裝 Playwright Chromium。

[GitHub Actions 設定](.github/workflows/ci.yml) 包含 Windows／Ubuntu、JDK 21 的相同檢查。workflow 設定本身不代表已通過遠端 CI。部分 Windows 符號連結測試可能因權限而跳過，請留意 skip。

CLI 成功時回傳 0；錯誤寫到 stderr：

| Exit code | 意思 |
|---|---|
| 2 | 參數或任務輸入錯誤 |
| 3 | 規則／知識格式錯誤 |
| 4 | 檔案 I/O 錯誤 |
| 5 | Frame 超出大小限制 |
| 6 | 已選規則互相衝突 |

## Skill 入口

[prompt-frame/SKILL.md](skills/prompt-frame/SKILL.md) 按需呼叫本地 CLI，專案規則仍放在各專案的 .promptforge/rules。目前 skill 內的呼叫位置是 `D:\PromptForge\frame.ps1`；搬動專案後要同步修改。

本 repo 保存 skill 原始檔。是否安裝、觸發及執行成功取決於使用端設定；CLI 本身不需要 agent 或模型服務。
