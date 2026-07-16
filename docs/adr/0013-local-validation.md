# ADR 0013 — 更新規則後可獨立驗證

- 狀態：接受
- 日期：2026-09-22

## 問題

人或 Agent修改 `.rule`及知識卡片後，不應靠剛好跑某個 task才發現格式錯誤；也要讓使用者在 Git review前能快速得到明確結果。

## 決定

CLI加入 `--validate --project <目錄>`，讀取全部規則與知識卡片，通過時顯示數量；格式錯誤仍用 exit 3並顯示來源。它只驗格式與已知邊界，不聲稱能判斷規則內容正確；更新後還要用實際 task做 frame/query驗收。`--validate`與 `--task`、`--query`、`--stdin`互斥。

## 後果

Agent可以在 branch中提出規則修改並先驗證；是否接受更改由使用者透過 Git diff決定，沒有自動 promotion或隱形更新。

