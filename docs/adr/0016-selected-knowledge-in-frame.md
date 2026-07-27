# ADR 0016：合併人選定的知識卡

狀態：Accepted。實作日 2026-09-26；Git 展示日期為 9 月 16 日。

測評顯示 query 找得到必要來源，但額外結果增加篩選，且使用者還須手動合併。新增 --knowledge，使用逗號分隔的專案相對來源，例如 .promptforge/knowledge/adr/idempotency.md。

只從既有 KnowledgeCardLoader 已讀入的卡片中精確選取，不依輸入路徑直接開檔；最多三張、按指定順序，拒絕未知／重複／空來源（整個空字串代表不選）。來源可用 Windows 分隔符，會正規化為斜線。逗號分隔格式不支援檔名本身含逗號。

合併完整正文、標題與來源；不默默截斷或自動取 query top3。規則加知識合計仍最多 6000 字元，超限回 exit 5、stdout 空，讓人縮小選擇。沒有知識選項時保留既有結果。只有 task／stdin 模式能用此選項。

KnowledgeSelection 負責選已讀卡片，PromptComposer 負責輸出與最終 budget。不改 BM25 分數，不宣稱自动檢索變準，也不宣稱人不用理解知識內容。可讓同一個命令輸出完整候選 prompt，避免手動抄寫來源及正文。
