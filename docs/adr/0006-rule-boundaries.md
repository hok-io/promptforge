# ADR 0006 — 限制規則檔的讀取邊界與大小

- 狀態：接受
- 日期：2026-09-22

## 問題

CLI會讀取指定專案中的規則。若 `.rule` 是連到專案外的 symbolic link，或規則檔過大，程式可能把不預期的內容放進 task frame，亦可能浪費 context。

## 決定

不接受 symlink規則目錄及 symlink規則檔；每個規則檔最多 8 KiB，單條 instruction最多 500字元。超過限制即報錯，不能靜默截斷。輸入 task最多 4000字元。這些只是保守的字元／byte邊界，**不宣稱是精確 token數**。

## 後果

輸出上限更容易預測，可信來源仍以專案內受 Git管理的文字檔為主。這不能阻止有權修改規則的人寫出惡意 instruction；使用者仍要 review rule diff。若未來需要長篇 ADR，應檢索小段 evidence而非把整份檔案作 instruction。

