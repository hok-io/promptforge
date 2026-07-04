# ADR 0005 — 用一個本地指令取得 task frame

- 狀態：接受
- 日期：2026-09-22

## 問題

Java核心已能編譯 task frame，但使用者不能為每次任務手動寫 Java程式。需要一個可從 PowerShell及其他工具呼叫的穩定入口。

## 決定

`frame.ps1 -Task "..." -Project <目錄>` 編譯並執行 Java CLI；Java本身接受 `--project <目錄> --task <文字>` 或 `--stdin`。正常 stdout只有可複製的 frame；錯誤寫 stderr並用非零 exit code。無效參數／空任務為 2，規則格式錯誤為 3，檔案 I/O錯誤為 4。`--project`省略時用目前目錄。不開 HTTP server，不依賴 Spring Boot。

## 後果

Windows本機可以即用，也能從 Codex hook或其他程式呼叫。每次執行會重新編譯，啟動較慢；日後若真的頻繁使用，再引入 JAR／Maven及快取。

