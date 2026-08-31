# 設計決策索引

ADR 解釋「為何這樣做」；目前功能以 [規格](../specs/behavior.md) 為準。歷史決策可由較新的 ADR 修正，不應把早期提案當成目前行為。

| 主題 | 決策 |
|---|---|
| CLI 基礎與規則 | 0001–0006：Java 核心、檔案格式、選擇及邊界 |
| Skill 入口與衝突 | 0007–0008：按需呼叫 CLI、規則衝突 |
| 檢索與發佈 | 0009–0013：知識卡、BM25、query、JAR、validate |
| 人工確認 | 0015–0018：選規則、加入知識、UTF-8、loader 限制 |
| Web 方向 | [0014](0014-local-workbench-direction.md) 的早期提案，由 [0019](0019-web-build-and-test-boundary.md) 確定實作方式 |
| Prompt 保存 | [0020](0020-prompt-document-and-preview.md)、[0021](0021-revision-safe-file-storage.md) |
| Skill 與證據 | [0022](0022-skill-import-review-and-export.md)、[0023](0023-evaluation-evidence.md) |
| 本地 HTTP 與 UI | [0024](0024-local-http-and-csrf.md)、[0025](0025-clean-build-classes.md)、[0026](0026-local-workbench-ui.md) |
| 應用層邊界 | [0027](0027-application-errors.md)、[0028](0028-learning-service-boundaries.md) |

Skill 路徑的可移植性見 [0029](0029-portable-skill-entry.md)。
