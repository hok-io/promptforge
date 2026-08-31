# 範例專案

從 PromptForge repository 根目錄執行：

```powershell
.\frame.ps1 "請修改付款重試邏輯，保持既有測試通過" -Project .\examples\payments
.\frame.ps1 "更新 README" -Project .\examples\payments
.\frame.ps1 "整理會議筆記" -Project .\examples\payments
.\query.ps1 "付款重試" -Project .\examples\payments
```

第一個任務應只取得付款規則；第二個只取得 README規則；第三個原文返回。這些規則只係 demo，不會默認套用到其他專案。

第四個指令獨立查詢本地 ADR，顯示標題、來源及短摘錄；目前不會自動加入 task frame。
