# Local BM25 — 實作驗收

- 自動測試：中文「付款重試」返回相關 ADR，不返回無關 README；英文字母忽略大小寫；無交集回空；分數平手按來源排序、limit生效。
- Java練習：字詞切分、term frequency、document frequency、BM25分數、Comparator及不可變結果。
- 真實用途：可在沒有 LLM、embedding或網路的情況下找出短知識卡片和來源；不自行生成建議。
- 限制：字面重合不代表語意正確；下一步 CLI query要把排名和來源清楚呈現，不能把低相關卡片假裝成事實。
