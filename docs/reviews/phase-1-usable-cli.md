# 階段一驗收：本地 CLI 是否真有用

日期：2026-09-22。運行環境：Windows PowerShell、JDK 15；`scripts/test.ps1` 5個 test classes通過。CI workflow已配置，但尚未連接 GitHub remote，因此沒有遠端執行結果。

## 三個真實操作

| 任務 | 期望 | 實際 |
|---|---|---|
| 修改付款重試，保持測試 | 只帶付款規則、保留原話 | 通過；附 `payment.retry` 和來源 |
| 更新 README | 只帶文件規則 | 通過；附 `docs.readme` 和來源 |
| 整理會議筆記 | 不要亂加規則 | 通過；原句返回 |

舊 spike對第一個任務輸出七段模板，並把未指定輸出格式列為缺漏；此版本輸出兩小段，沒有推測不存在的任務資料。這是可用性的改善，不等於已證明實際 Codex token用量下降。

## Edge cases與下一步

- 已測：空任務、錯誤規則、重複 ID、缺欄位、未知欄位、大小寫、排序、stdin、中文。
- 待補：symlink規則可否讀到專案外、含空白路徑、過長 instruction、多條互相矛盾的規則、中文同義詞漏選。下一階段先按實際風險處理 symlink和長輸出，再加衝突診斷。
- Java可讀性：目前每個 class只有一個責任；`RuleFileLoader`只讀檔，`RuleSelector`只挑規則，`TaskFrameCompiler`只組結果，`Main`只管 CLI。最後由使用者 review這些分工和寫法。
