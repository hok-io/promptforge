# ADR 0009 — 把 ADR與政策讀作可引用的本地卡片

- 狀態：接受
- 日期：2026-09-22

## 問題

規則檔適合短 instruction；完整 ADR、agent behavior和 policy仍需要有來源的本地檢索。直接把全文加入 task frame會太長，也會使相關性不明。

## 決定

在 `.promptforge/knowledge/`下按 `adr/`、`behavior/`、`policy/`、`practice/`等子目錄放 UTF-8 Markdown；每檔第一個非空行必須是 `# 標題`，餘下是內文。第一版一檔一卡，最多 8 KiB，禁止 symlink檔案；結果記錄相對來源。loader只讀取資料，不把 Markdown當作指令執行。檢索與是否附加到 frame是後續獨立決策。

## 後果

Git diff可審、來源清楚；長 ADR需拆卡片或日後加入 chunking。暫時不支援 front matter或版本欄位，避免過早增加格式負擔。

