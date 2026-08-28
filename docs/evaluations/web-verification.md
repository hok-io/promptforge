# Web 實作驗證紀錄

實測：2026-09-26，Windows 11、專案 JDK 21.0.12.1、Maven Wrapper／Maven 3.9.11、Spring Boot 3.5.16、Playwright 1.55.0、已安裝 Edge。Git 展示日期按使用者要求為 2026-09-17–24，不當成實際測試日期。

## 已執行的測試

| 層 | JUnit 結果 | 實際證明 |
|---|---:|---|
| core | 16 pass，0 failure／error | 15 個舊 main 測試各作一個 dynamic result；另測 UTF-8、16 KiB 輸入及超限 |
| workbench | 22 pass，0 failure／error | domain、缺變數、檔案讀回、同時保存、外部改檔、registry、skill／評估／批准／匯出及路徑限制 |
| web | 11 pass，0 failure／error | 6 個 MockMvc、1 個真實 HttpClient server、4 個真 Edge browser workflow |
| 合計 | 49 pass | 是 JUnit 結果數，不是所有內部 assertion 的總數 |

JUnit XML 的 skipped 為 0；但舊 main runner 有 **4 個 Windows symlink 子檢查**因建立連結權限而印出 SKIP。不能把它們當作通過。另有實際 Windows junction 測試，驗證讀取、列檔和寫入拒絕專案外目標，且目標 sentinel 未改動；這項正常通過。

瀏覽器實際操作：搜尋 ADR、選規則和知識、Unicode 與變數、區塊排序、保存及重載、只有區塊的模板、缺變數、第二個編輯者的舊 revision、skill 匯入及編輯、未保存不得批准／匯出、review／批准／匯出、窄畫面控件。HTML 攻擊字串只顯示成文字，沒有 alert；攔截並確認沒有非本機請求。

第一次完整瀏覽器測試發現 async handler 在 await 後讀 event.currentTarget，導致保存按鈕不能恢復。保留失敗 log，0386b61 先捕捉 button 再 await，後續完整測試通過。這是實際操作測試找到的問題。

## 執行命令與證據

1. `scripts/build-web.ps1`：clean verify，core 16／workbench 22／web 11 全過，產生可執行 Web JAR。見 [完整 log](../../build/final-web-verify.log)。
2. 最後 CSS 側欄修正後，以 Maven `-o -pl web -am verify -Dtest=BrowserWorkflowTest -Dtests.required=false -Dsurefire.failIfNoSpecifiedTests=false` 重跑 4 個 browser tests 並重新打包。成功，沒有下載依賴。見 [離線 log](../../build/offline-browser-verify.log)。其餘 Java 程式未改，不再重複其他已通過測試。
3. `scripts/test.ps1`：15 個 CLI main test classes 通過，另有 class-clean safety smoke；frame、query、package 的過期 class marker 清理通過。
4. `scripts/check-skill.ps1`、`scripts/confirmed-prompt-smoke.ps1`、`scripts/package-smoke.ps1` 通過。見 build 下 skill-contract-check、confirmed-prompt-check、cli-jar-check logs。
5. 真正以 `java -jar web/target/promptforge-web-1.0-SNAPSHOT.jar --promptforge.home=D:\PromptForge --server.port=18080` 啟動。GET projects／payments context 成功：3 個登記專案、2 條付款規則、1 張知識卡。系統 listener 確认只綁 `127.0.0.1:18080`。

最新 Web JAR SHA-256：`624a49d000b492fb62f40b7103906e5bd919e563b1fdfb7c87f016fb345fb36c`。

原始 JUnit XML 在各 module 的 `target/surefire-reports`；截圖在 [prompt](../../build/browser-prompt.png) 和 [skill](../../build/browser-skill.png)。這些 build artifacts 留本地，沒有加入 Git 或上載。

## 實際限制

- 已跑本機 Windows／Edge；Linux／Chromium 只設定 CI，沒有 remote，未執行遠端 CI。沒有聲稱 Firefox、WebKit 或所有 OS 通過。
- 首次建置下載 Java/Maven 依賴；執行只有本地服務和資產，沒有 LLM、API key、CDN 或模型下載。離線 Maven 重測使用已存在的 dependency cache。
- 沒有真人時間／token 消耗對照研究，不聲稱效率提高固定比例；用途及取捨見 [用途測評](web-usefulness.md)。
- TEST 評估記錄驗證現有文字證據及 hash，不重新執行外部評估中的指令；模型意見不是人的批准。
- revision 鎖協調本 JVM，外部程序仍可能競速最後檢查；bundle 和 metadata 不是跨檔案資料庫交易。只支援有大小上限的 UTF-8 text skill bundle。

CI 原擬加入報告／截圖上載，但自動批准審查以未授權對外分享為由拒絕。已移除上載，只保留測試執行設定；沒有發佈 repo 或推送任何資料。
