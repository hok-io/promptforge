# ADR 0015：讓人確認與修正規則選擇

狀態：Accepted；實作日 2026-09-26，

測評發現同義詞漏選、否定句及文件任務誤選。先增加可確認的 CLI 選擇，不把關鍵字比對假装成理解任務。

新增 --list-rules 顯示 ID、keywords、指令與來源；--rules 以逗號分隔 ID 指定完整選擇，取代自動匹配，空字串代表不選；--exclude 排除指定 ID。未知／重複 ID、選中與排除同一 ID 回輸入錯誤，不默默忽略。選擇最多 20 項，順序按 ID 穩定排列。

沒有新選項時保留舊行為。已選規則仍檢查衝突及 frame 上限；list 模式和任務／查詢／驗證／選擇選項不能混用。普通 Java RuleSelection 負責選擇，Main 只連接 CLI 與 compiler。

替代：添加同義詞或否定句特例會引入新的誤選，且容易只修好測試集。明確選擇可避免使用者手動重抄來源，但仍需要人知道哪些規則適用，不能當成自動準確率提升。

PowerShell frame wrapper 提供 -Rules／-Exclude／-Knowledge，僅在參數確實傳入時轉交，保留「省略」與「明確空選擇」的差別。先用跨平台 smoke test 驗證實際 native argument 傳遞，再做 wrapper；CI 執行同一條命令。

本機 smoke 證實 PowerShell native 呼叫會丟掉空字串。因此新增 --no-rules，wrapper 把 -Rules '' 轉成這個旗標；空 Exclude／Knowledge 不轉交（沒有項目）。與 --rules 重複使用回輸入錯誤，不讓 native argument 行為改變使用者選擇。
