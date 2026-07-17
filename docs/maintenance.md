# 更新專案規則與知識卡片

專案可以把 `.promptforge/rules/*.rule`、`.promptforge/knowledge/**/*.md`放進自己的 Git repository。Agent可以在工作 branch提出修改，但不要直接把改動視作已獲批准。使用者最後 review Git diff和任務輸出。

每次更新保持細步：

1. 改一條 rule或一張 ADR／behavior／policy卡片；看 diff，確認沒有不相關或敏感內容。
2. 在 PromptForge專案跑 `./scripts/test.ps1`，再跑 `./scripts/package.ps1`。這些檢查 Java程式本身。
3. 用 `java -jar D:\PromptForge\build\PromptForge.jar --project <你的專案> --validate`檢查所有規則與卡片格式。
4. 用 `--task`重跑一個應命中的任務和一個不應命中的任務；必要時用 `--query`檢查 ADR來源。
5. 記錄預期與實際差異，讓人 review後才合併 Git commit／PR。

`--validate`只查格式、大小與可讀性；它不能保證 instruction合理，也不能代替對真實任務的驗收。Git merge和 Codex skill安裝仍由使用者控制。

