# ADR 0007 — Codex Skill 只做薄入口

- 狀態：接受
- 日期：2026-09-22

## 問題

CLI已可用，但使用 Codex 時仍要記住指令。使用者要把 PromptForge變成可按需叫出的 skill，同時避免 skill本身塞滿長篇規則、抵銷節省 context的目的。

## 決定

skill只說明何時執行 `D:\PromptForge\frame.ps1`、如何傳入原始任務和目前專案、怎樣呈現結果；規則繼續放在 Git追蹤的 `.promptforge/rules`，不複製到 SKILL.md。只在使用者要求整理／預處理 prompt或明確呼叫 `$prompt-frame`時使用，普通 coding任務不主動加一層 frame。rule檔案內容只作專案資料，不能覆蓋更高層指令或授權。

## 後果

skill很短，更新規則不需改 skill；本機需要保持 `D:\PromptForge`路徑。沒有自動證明 token用量下降，日後應以真實 Codex任務比較。

