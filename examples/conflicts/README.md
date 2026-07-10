# 衝突示範

```powershell
cd D:\PromptForge
.\frame.ps1 '修改付款重試' -Project .\examples\conflicts
```

應顯示兩條衝突規則的 ID和來源，並以 exit code 6結束；不產生 prompt。這讓人先修改受 Git追蹤的規則，再重試。

