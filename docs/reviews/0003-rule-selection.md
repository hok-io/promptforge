# Rule selection — 實作驗收

- 本機檢查：`scripts/test.ps1`；RuleSelectorTest涵蓋命中、排除、大小寫、全部 keyword及輸入順序。
- 真人用途：修改付款重試只選 `payment.retry`；更新 README只選 `docs.readme`。
- 人讀檢查：沒有置信度分數或「AI判斷」說法；結果可由 task文字和 rule keywords直接解釋。
- 已知限制：同義詞會漏選，中文關鍵字可能在否定句出現；下一大階段的真實任務驗收需記錄誤選和漏選，不將此簡單匹配誇稱為語意檢索。
