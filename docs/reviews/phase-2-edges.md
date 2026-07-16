# 階段二驗收：邊界、衝突與 Skill

日期：2026-09-22。`scripts/test.ps1` 8個 test classes通過；`scripts/check-skill.ps1`通過；skill-creator `quick_validate.py`通過。GitHub Actions已配置，但無 remote，未曾在 GitHub runner執行。

## Edge cases

| 情況 | 結果 | 證據／限制 |
|---|---|---|
| 規則檔超過 8 KiB、instruction超過 500字 | 報格式錯誤 | RuleBoundaryTest |
| task超過 4000字、frame超過 6000字 | 報明確長度錯誤 | RuleBoundaryTest；字元數不是 token數 |
| 兩條命中規則互相衝突 | 不輸出 frame，exit 6，列兩個來源 | RuleConflictTest、CliTest、`examples/conflicts` |
| 只有一條命中、或任務無關 | 不阻止執行 | RuleConflictTest |
| symlink規則 | 程式拒絕 symlink | 本機無建立 symlink權限，測試跳過；CI另設 Ubuntu runner，該環境若不能建立 symlink會令測試失敗，仍待遠端運行 |

## 實際用途是否相合

付款重試、README及無關任務仍如階段一所見，只加相關規則或 BYPASS。已實際執行 `frame.ps1 '修改付款重試' -Project .\examples\conflicts`，收到 `Rule conflict`、兩個規則檔來源和 process exit 6；人可以從 Git diff處理衝突。Skill只在要求整理 prompt時調用 CLI，沒有把規則寫死在 skill。低成本 subagent獨立用付款任務測試 skill，CLI exit 0，輸出與直接呼叫一致；skill已按回饋要求在 frame外說明「尚未實作／測試」。

## 下一階段候選

優先觀察真實任務的誤命中／漏命中，再決定是否加 path scope、本地 ADR檢索或 BM25。現階段的 keyword匹配不等於完整 RAG；CI遠端運行、JDK 21/Maven/JUnit及 symlink測試仍未完成。
