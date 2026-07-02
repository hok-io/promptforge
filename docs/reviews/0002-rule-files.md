# Rule files — 實作驗收

- 本機檢查：`scripts/test.ps1`，JDK 15，RuleFileTest + SmokeTest 通過。
- 真人用途：一條 `payment.retry` 規則可放在 `.promptforge/rules/payment.rule`，以普通文字和 Git diff 更新；loader回傳相對來源，之後的輸出可指出規則檔位置。
- Edge cases：缺 instruction、未知 key、同檔重複 key、跨檔重複 ID、空 keyword、不可變 keyword list都有測試。
- 人讀檢查：錯誤指向規則檔、欄位或 ID；沒有靜默跳過格式錯誤。
- 未做：matching、CLI、symlink規則、超大檔案限制。這些由下一功能或大階段檢查處理。
