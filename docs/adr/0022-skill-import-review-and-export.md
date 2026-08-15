# ADR 0022：Skill 的匯入與版本狀態

匯入來源限於已登記專案 .promptforge/inbox/<name>，不接受任意路徑。保存原 SKILL.md 及文字 references／scripts，保留未知 frontmatter；工作台 metadata 放 sidecar。首版是 UTF-8 文字 bundle，最多 128 entries／256 KiB，不執行其中腳本，不處理二進位資產。

revision 是整個 skill 文字 bundle 的 hash，包含 references，而不只是 SKILL.md。Imported → Reviewed → Approved → Active 的人工事件綁定這個 hash；任何內容改變都使既有批准及 active 不再有效。模型評估不自動批准。Active 只表示本工作台選用，未安裝到任何 agent。

保存時先 diff，再檢查 bundle revision；export 僅批准的版本，輸出到同專案 exports/<name>，原子建立新資料夾，不能覆蓋，不默默安裝。匯入及匯出都先 stage，最後 rename；metadata 和 skill raw 是分開檔案，狀態以 hash 衍生，避免改內容但舊批准繼續生效。

測試先驗證未知欄位保留、批准順序、stale revision、改 reference 失效及 export 不覆蓋。HTTP／UI 另外測同一流程。
