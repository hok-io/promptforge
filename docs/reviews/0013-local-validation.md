# Local validation — 實作驗收

- 自動測試：一條 rule和一張 card顯示各自數量；`--validate`不能與 task混用；無效 card報檔名和 exit 3。
- 真人用途：已用打包 JAR對付款範例執行 `--validate`，結果 `PASS: 2 rules, 1 knowledge cards`。Agent修改 rule／ADR後，可在 Git review前用同一 JAR檢查所有來源格式，不需猜一個會命中該規則的任務。
- 邊界：它不檢查語意正確性，也不自動批准或啟用修改。要另用正反例 task檢查實際輸出。
