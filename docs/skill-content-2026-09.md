# 2026 年 9 月：Skill 內容管理修正

## 既有問題與本次修正

之前 GUI 只列管理大綱，漏掉已有的 context 和開發 Skill；context 指示寫在 Java 裡並由 PromptForge 保存流程重寫。本次改成共用磁碟發現機制，各 Skill 獨立保存。

- Skill 庫和選單列出本地 `.agents/skills/*/SKILL.md`；MCP `list_skills` 使用相同發現邏輯。
- 結構化大綱仍只生成勾選步驟。用途、輸入、整體輸出要求及驗收內容由管理資料提供；PromptForge 原有要求已搬到 `.promptforge/program.json`。
- 現有 Markdown Skill 可直接查看、使用及編輯。首次保存時，管理來源建立在 `.promptforge/outlines/<name>.json`，保留完整 Markdown 和原有支援檔案位置。
- context 不再由 PromptForge 保存流程覆寫。GUI 的固定 context 區塊移除，改為選擇該 Skill 並把完整 prompt 貼進任務輸入。
- 新 Skill 保存生成後，MCP 下次 `list_skills` / `get_skill` 即取得它；既有 AI 對話需再次呼叫工具。
- 外部新修改會回報衝突。重新讀取後可在輸出區查看磁碟版本，把內容合併回管理來源再保存。

## 如何使用

1. Skill 庫選「使用」；填任務或輸入資料，複製指令到已連接 MCP 的 AI。
2. 選「編輯」；大綱模式提供步驟選擇，Markdown 模式提供完整指示文件。保存後各自更新。
3. 新增 Skill：填 ID、用途、步驟並保存生成。
4. 腳本可透過現有檔案區保存；大綱可加入能力，Markdown Skill 需在指示中引用檔案相對路徑。已安裝 Skill 的既有支援檔案保持原位，不會自動搬移。

本次只完成內容功能；視覺重設等待使用者選擇開源樣本。Luna 實作核心，主 agent 收尾測試與部署。

## 實際驗收

- 使用專案 bundled Maven/JDK 離線執行 `mvn -o -q test`：17 個測試通過（workbench 6、web 11），無失敗或略過。Wrapper 在此環境的 PowerShell `.Target[0]` 失敗，未修改 wrapper。
- 真實 Edge 測試覆蓋 context / 已安裝 Markdown 的編輯保存、PromptForge 保存不覆蓋 context、新增 Skill 與腳本、選擇生成、重開、MCP 讀取，以及並行／外部修改衝突。
- 實際 localhost:8081 GUI 已顯示三個 Skill。三者 API 均回報 generatedCurrent=true，原有生成內容未變。
- 2026-09-30 02:00:56（UTC+8），真實 Codex agent 經 MCP 列出三個 Skill，成功讀取 PromptForge-context 並確認不執行原任務；網頁收到版本 `83b0a3932c5a` 的驗證回執。
- 此次驗證讀取與管理功能，不宣稱所有任務輸出都已驗收。視覺樣式尚未重整。
