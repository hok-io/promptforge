# ADR 0018：Loader 資料大小與路徑邊界

狀態：Accepted。實際觀察日 2026-09-26，

Knowledge loader 最多接受 100 張 Markdown 卡片，卡片檔案的 UTF-8 原始 bytes 合計最多 256 KiB。每個 rule 與 knowledge 檔仍各自最多 8 KiB。累計按檔案系統回報的大小計算；超出任一限制就停止並回報格式錯誤。限制在載入全部內容前執行，避免把不受限制的資料集合交給後續檢索。

兩個 loader 共用 `ProjectPaths`：檢查 project root 到檔案系統根的路徑元件，拒絕可識別的 symbolic link，並在存取設定目錄及內容檔時確認 real path 位於 project root real path 之下。這涵蓋 `.promptforge`、rules/knowledge 子目錄、卡片巢狀目錄及候選檔。Java NIO 對 Windows junction 的分類依檔案系統提供的 `isSymbolicLink`／`toRealPath` 行為；即使 junction 未被辨識成 symbolic link，real-path 越界仍會拒絕。若 junction 位於指定 project root 之前，解析後的 root 會作為此載入呼叫的專案根界線。

Rule 必填欄位的錯誤順序固定為 `id`、`keywords`、`instruction`，讓同一份無效檔案每次都先指出相同欄位。

限制是載入邊界，不是語意驗證；卡片內容仍由使用者維護。超過總量時 loader 不會部分載入。
