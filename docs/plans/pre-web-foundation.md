# Web前先做好甚麼

狀態：2026-09-26 已實作必要基礎並接入 Web；原基線 `0891eb9`。使用者之後要求「先實作好了」，所以 A review 改為交付後學習，不再是啟動 gate。

落地範圍：B01–B04、Maven/JDK21、共用 FrameService／KnowledgeQueryService、Project／Prompt／Skill／Evaluation 普通 Java service，以及本地 Web。C02 先以 JUnit dynamic tests 包裝 15 個 main 測試，尚未逐個改寫。ProjectValidationService 和完整 typed diagnostics 尚未抽出；現有 validate 保留 CLI 行為，這兩項作後續學習重構，不影響這版 Web 操作。

## A：先 review原有31個 commit

依 [review入口](../reviews/START-HERE.md)逐個檢查。不 squash，也不一邊教原碼一邊大幅重構。2026-09-26 依你的要求重寫 commit 日期，內容與次序保留，對照見 [日期紀錄](../reviews/date-rewrite.md)。你可以先把疑問記下，修正放在後面的新 commit，才容易比較前後寫法。

## B：既有核心的修正

每項依序做 ADR／測試→看預期失敗→最小修正→驗證→人讀輸出。測試 commit可 red；可交付階段末必須 green。

| 小步 | 問題與產物 | 先做的驗證 | 完成證據 |
|---|---|---|---|
| B01 | 整理輸出資料夾；修正刪除source後舊 `.class`仍可進JAR | 放入舊 class標記後重新打包，確認不存在；清理前檢查實際路徑在build內 | fresh build與repeat build只含現有主程式；不刪source |
| B02 | 固定缺欄位錯誤次序，讓人每次看到同一原因 | 同一非法rule多次驗證結果一致 | 錯誤含檔案及欄位；正例不變 |
| B03 | stdin在讀入時限制bytes，避免 `readAllBytes()`先接收無上限內容 | 超額串流、中文UTF-8、剛好上限 | 清楚拒絕超額；不破壞多位元字元 |
| B04 | 限制知識卡總數／總bytes；建立共同project path邊界 | 超額目錄、symlink、junction支援情況、專案外path | CLI具明確限制和平台測試紀錄；不聲稱能攔所有OS技巧 |
| B05 | 保留付款、README、無關、衝突、中文JAR作行為基線 | 一條統一驗證命令及機器可讀結果 | 記錄pass/fail/skip；12個class不冒稱12個case |

這些是現在代码讀取／建置缺口；不是Web功能。其他如同義詞、Lucene、requires／supersedes graph及多步Agentic retrieval留待實際需求，不是Web啟動先修。

## C：建置與Java架構先升級

Spring Boot Web階段採Java 21；現有Java 15語法暫可沿用。不要同一commit同時升JDK、改資料格式和重寫UI。

| 小步 | Java工作 | ADR與先做測試 | 驗收 |
|---|---|---|---|
| C01 | JDK21環境檢查、Maven Wrapper、最小pom | 固定版本、fresh checkout建置契約 | 不要求系統預裝Maven；依賴首次需下載，完成後本機運行離線 |
| C02 | 把現有main測試逐類移到JUnit，先保留舊runner對照 | 正常／失敗行為不變；先移Rule類，再Selector，再CLI／檢索 | Maven產JUnit報告；不要一次改所有測試 |
| C03 | 抽 `FrameService`，接受task及project context，返回資料 | 相同fixture與舊CLI一致 | Main只轉參數和錯誤；Service沒有HTTP概念 |
| C04 | 抽 `KnowledgeQueryService`、`ProjectValidationService` | query/validate結果與旧CLI一致 | Web之後可直接呼叫，不啟動CLI subprocess |
| C05 | 分離human renderer及typed diagnostics | domain結果與文字格式分開驗證 | 退出碼只屬CLI，HTTP code之後由Web轉譯 |
| C06 | 只在真實adapter需要時分core／cli模組 | 依賴方向、JAR smoke | core不依賴Spring；新增Web module時才接Spring |

正式遠端CI綠燈需GitHub repository及push。沒有remote時以同一命令本機驗證並記錄限制；不可把workflow存在當成CI成功。建立或發布remote另按使用者要求處理。

## D：新設計所需的核心，也要先在CLI成立

以下新能力已在 workbench 的普通 Java 類別和 JUnit 中實作，再由 Web adapter 呼叫。它們不依賴 Spring，但第一版沒有新增對應的結構化 CLI 子命令；原 CLI 的 frame/query 共用核心。

| 小步 | 最小新能力 | 先做case | 真人驗收 |
|---|---|---|---|
| D01 | `ProjectId`與註冊project目錄；保留 `.promptforge` | 重複ID、失效路徑、未註冊project | 看得出今次操作哪個project |
| D02 | `PromptDocument`含有序blocks、名稱、variables | block順序、空optional block、重複名稱 | 可以省略role，保留用户原話；不是固定七段模板 |
| D03 | prompt編譯預覽與明確變數替換 | 缺變數、字面保留、無遞迴模板執行 | CLI預覽與將來Web共用同一結果 |
| D04 | skill唯讀清單／inspector，讀SKILL.md與references | 缺frontmatter、缺reference、未知欄位、外部skill | 列出名稱、用途、來源、聲明依賴；不把聲明當已授權 |
| D05 | skill／prompt保存：版本hash、預覽diff、atomic save | 檔案外部被改、寫入失敗、path escape | 舊revision保存回衝突，不覆蓋其他工具的改動 |
| D06 | skill評估artifact：版本hash、評估者、findings、evidence、test結果 | model未知、沒有proof、artifact已過期 | 可保存外部Agent的review，不自動呼叫LLM／造分數 |
| D07 | imported／reviewed／approved／active狀態及人工操作 | 評估不等於approval、改內容後評估stale | 本地匯入後不啟用；批准綁定具體hash |

每小步都有自己的ADR、測試先行commit、實作commit和人可讀例子。D階段不用Spring就能驗證；Web只把已成立的操作顯示成畫面。

## 可以開始Web的條件

B必要缺口、C建置／共用services及D資料契約先成立，再接 Web。Evaluation／approval 使用真正的檔案與 hash，不以畫面假資料冒稱功能。

各大階段保留正反例及 edge cases。你逐 commit review 的學習入口見 [Java Web 路線](../learning/java-web-tour.md)；原計劃的等待 review 指示已由使用者撤回。
