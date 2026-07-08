# Codex Skill — 實作驗收

- 自動檢查：`scripts/check-skill.ps1` 驗證 frontmatter、CLI路徑及入口檔案；GitHub Actions也會執行同一檢查。
- Skill本體很短，沒有複製專案規則；更新 `.promptforge/rules`不需改 SKILL.md。
- 已用 skill-creator 的 `quick_validate.py` 驗證，結果 `Skill is valid!`；也已複製到個人 `C:\Users\Hohokio\.codex\skills\prompt-frame`。
- 觸發範圍是「要求整理／預處理 prompt」，普通 coding任務不應自動轉成 frame；這需要最後以 Codex實際對話驗證，靜態測試無法證明。
- 待使用者／Codex實際驗收：使用 `$prompt-frame` 對付款重試、README及無關任務各試一次；確認跟 CLI輸出一致，且不自行執行任務。靜態驗證不能代替 Codex觸發測試。
