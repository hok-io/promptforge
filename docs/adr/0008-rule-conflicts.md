# ADR 0008 — 規則可以明確聲明衝突

- 狀態：接受
- 日期：2026-09-22

## 問題

兩條規則可能同時命中任務，但要求互相矛盾。若直接把兩者放進 prompt，Codex只能自行猜。這也是值得親手練 Java domain model和集合檢查的真實情況。

## 決定

規則檔可選填 `conflicts=另一規則ID,...`，表示這兩條規則不能同時進入同一 frame；單向聲明已足夠，檢查時視為雙向。`conflicts`保存在不可變 list，空 ID、重複 ID和自己衝突屬格式錯誤。若兩條都命中，編譯器報兩個 ID及來源，不輸出矛盾 frame。未命中的規則不造成衝突。

## 後果

人可以在 Git diff中審查衝突關係；目前不做自動優先級／supersedes，避免靜默選擇。之後若需要 requires／supersedes graph，另寫 ADR和 cycle測試。

