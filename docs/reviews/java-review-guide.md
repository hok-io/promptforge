# 最後怎樣逐步 review Java

先用 `git log --oneline --reverse` 看順序。每個 `test:` commit在相鄰 `feat:` commit之前，會看到測試由 red變 green。建議每次只看一組 diff，不一次讀完整專案。

| Git步驟 | 讀哪個 Java class | 要理解的問題 | 自己可改的小練習 |
|---|---|---|---|
| Rule files | `Rule`、`RuleFileLoader` | 為何要複製 list？格式錯誤為何不能靜默忽略？ | 增加一個非法 ID測試 |
| Selection | `RuleSelector` | all-keywords為何可預測？排序為何影響可重現性？ | 增加大小寫／中文任務例子 |
| Frame | `TaskFrameCompiler` | 選擇、檢查、呈現如何分工？BYPASS何時成立？ | 改輸出清單標題而保持測試通過 |
| CLI | `Main` | stdout與stderr分開有何用途？退出碼怎樣被其他工具使用？ | 測試未知參數 |
| Boundaries | `RuleBoundaryTest`、`FrameBudgetException` | 大小限制如何防止意外長 prompt？為何字元不是 token？ | 測剛好 500字 instruction |
| Conflicts | `RuleConflictChecker` | Map讓查找從反覆遍歷變成甚麼成本？為何不自動揀一條？ | 加反方向衝突測試 |
| Knowledge | `KnowledgeCardLoader` | 為何一檔一卡、來源用相對路徑？讀檔時怎樣報錯？ | 測一張沒有正文的卡片 |
| Retrieval | `Bm25Index` | TF、DF、IDF和排序各處理甚麼？中文雙字組有何限制？ | 加一張相似但不相關的卡片，看排名 |
| Release | `scripts/package.ps1`、`Main` | `.java`如何變成 executable JAR？為何規則更新不用重包？ | 用 `java -jar`跑自己的專案 |

Review時先回答「這個 class接受甚麼、保證甚麼、錯誤怎樣回報」，再看語法細節。可以逐個 commit問 Codex解釋或要求指出替代寫法；不要把整份程式一次交給模型重寫，否則會失去自己練 Java的機會。
# 完整逐 commit review 入口

本文件是概念導讀；請從 [START-HERE](START-HERE.md) 及 [31 張 commit review 卡](commit-review.md) 逐個檢查。Web 實作等待你完成 review，計劃見 [工作台設計](../plans/local-workbench.md)。
