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
| Frame | 用 keyword 選擇規則，補到原任務並附來源 |
| Query | 用 BM25 搜尋本地 Markdown，顯示最多 3 個結果與短摘錄 |
| Validate | 檢查規則與知識卡格式，顯示讀取數量 |
| 衝突檢查 | 拒絕互相衝突的已選規則 |
| JAR | 打包後用 java -jar 執行 |
| Skill 入口 | 提供按需呼叫 CLI 的 prompt-frame skill 檔案 |

規則選擇是文字匹配，BM25 是文字檢索。目前不生成答案、不自動把搜尋結果注入 frame，也沒有 agent 自主執行迴圈。目前提供 CLI，沒有 Web 介面。輸出有字元限制，但沒有精確 token 計數或固定節省比例的保證。

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
.\scripts\package.ps1
.\scripts\package-smoke.ps1
```

Java 測試使用 main 與 assertions，目前共 12 個測試類，尚未採用 JUnit。JAR smoke test 檢查打包後的規則選擇、知識搜尋與中文輸出；skill 檢查是檔案契約檢查，不是模型品質評估。

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
