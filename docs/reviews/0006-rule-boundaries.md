# 規則邊界 — 實作驗收

- 自動測試：大於 8 KiB的檔案、501字 instruction、4001字 task、超過 6000字的 frame。symlink規則測試在這部 Windows機因建立 symlink缺少權限而跳過；程式明確檢查 symlink，但這條測試仍需在有權限的 CI環境執行。
- 真人用途：避免誤把整份 ADR或很長的 instruction塞入 Codex task；報錯時要求縮小規則，而不靜默刪掉內容。
- 審查重點：限制用 bytes／Java char衡量，不是模型 token；此版本只保證輸出有明確上界。
- 未解決：具有修改規則權限的人仍能改出不恰當 instruction，需靠 Git diff和使用者 review；這不是 sandbox。
