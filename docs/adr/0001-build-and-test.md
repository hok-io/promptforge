# ADR 0001 — 先用本機 JDK 建立可驗證的 Java 核心

- 狀態：接受
- 日期：2026-09-22

## 問題

本機只有 JDK 15，沒有 Maven；專案目標是練 Java 並先得到可用的本地 CLI。若開工先等框架和 build tool，便無法先驗證業務行為。

## 決定

第一階段只用標準 Java 15 語法，`scripts/test.ps1` 負責編譯主程式和測試，GitHub Actions 用 JDK 21在 Windows及 Ubuntu跑同一支 script。每個功能先提交 ADR 和會失敗的測試，再提交最小實作和驗收紀錄。將來引入 Maven/JUnit 時，需另寫 ADR 並保持現有行為測試。

## 後果

本機無需額外安裝或付費；可直接練 class、interface、集合、NIO、例外和演算法。這個輕量測試 runner 沒有 JUnit 的報告功能；CI workflow 只有在連接 GitHub 並 push 後才會真正執行，現階段以本機同一命令驗證。
