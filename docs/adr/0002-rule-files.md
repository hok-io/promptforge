# ADR 0002 — 一條規則一個可讀文字檔

- 狀態：接受
- 日期：2026-09-22

## 問題

使用者要在 Git 中更新專案規則，也要看得出輸出來自哪個檔案。第一版若先加入 YAML parser、schema和索引，會增加安裝和理解成本。

## 決定

使用 `.promptforge/rules/*.rule` 的 UTF-8 文字檔，每行 `key=value`；允許空行和 `#` 註解。必填 `id`、`keywords`、`instruction`。`keywords` 用逗號分隔，下一功能定義 matching 語意。未知欄位、重複欄位、空值、重複 ID 一律報清楚錯誤；不猜測或靜默忽略。source由檔案相對路徑產生，方便追溯。

## 後果

人能用普通編輯器修改、Git diff容易審查；一條 instruction暫限單行，未來需要多行才另寫 ADR擴展。這是規則來源格式，不將檔案內容視為可執行指令。

