# 階段三、四驗收：檢索、打包、更新流程

日期：2026-09-22。本機 Windows JDK 15跑 `scripts/test.ps1`，12個 test classes通過；`scripts/check-skill.ps1`通過；`scripts/package.ps1`與 `scripts/package-smoke.ps1`通過。GitHub Actions設定 Windows及 Ubuntu + JDK 21，尚無 GitHub remote，因此**遠端 CI未實際執行**。

## 真實應用

| 操作 | 實際結果 |
|---|---|
| `frame.ps1 '請修改付款重試邏輯，保持既有測試通過'` | 原文加付款規則及來源，沒有七段通用模板 |
| `query.ps1 '付款重試'` | 本地 ADR標題、來源和短摘錄 |
| `query.ps1 '天氣預報'` | 明確回沒有相關本地資料 |
| `java -jar build/PromptForge.jar --project examples/payments --validate` | `PASS: 2 rules, 1 knowledge cards` |
| `java -jar ... --task/--query` | 打包 smoke test通過 |
| 直接 `java -jar`查中文 ADR | 最初發現亂碼；補 red test後修正，現在完整顯示中文標題與摘錄 |

## Edge cases與誠實限制

- 已測：中文／英文查詢、無結果、排名平手、limit、缺標題／正文、超大卡片、格式錯誤、衝突、長 task/frame。
- 知識卡片只做字面 BM25排名，**不等於理解 ADR意思**；檢索結果獨立顯示，未自動塞入 prompt。無 LLM、embedding、API token或網路依賴。
- 目前尚未做多步 Agentic retrieval loop、MCP adapter、Lucene adapter、Maven/JUnit、JDK 21語法升級、真實 token節省 benchmark。這些都是後續里程碑，不應將現有 CLI說成已完成全部規劃。
- Windows本機因 symlink權限無法測建立 symlink；CI Ubuntu job將強制測這條路徑，但要 push後才知道遠端結果。
- CI使用的 [checkout v7](https://github.com/actions/checkout)與 [setup-java v6](https://github.com/actions/setup-java)均已核對官方 repository；這只能確認版本存在，不能代替本專案實際 CI運行。

## 人看輸出是否合理

付款任務、README、無關任務、衝突任務和 ADR查詢已分開驗收。輸出只附資料來源，不自行假定測試已跑、程式已改、政策已獲批准。這符合「先整理 prompt、讓人 review」的用途；若日後實際 Codex操作發現命中率不足，先收集誤選／漏選任務，再決定增加 path scope或更複雜檢索。
