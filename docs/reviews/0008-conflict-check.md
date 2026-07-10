# Conflict check — 實作驗收

- 自動測試：同時命中的兩條不相容規則報兩個 ID及來源；CLI回 exit 6、stdout為空；只有一條命中或任務無關時不阻止。
- 真人用途：若付款規則同時要求「立即重試」與「先檢查狀態」，工具不把兩者默默交給 Agent自行猜；人先在 Git規則檔解決。
- Java練習：用 `Map<String, Rule>` 建立 selected規則查找，再逐條檢查衝突邊；時間複雜度約 O(R + E)，R為命中規則數，E為它們聲明的衝突數。
- 未做：requires、supersedes、cycle；此功能只處理明確宣告的同時命中衝突。
