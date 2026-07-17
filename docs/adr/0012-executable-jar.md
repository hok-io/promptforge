# ADR 0012 — 打包可直接執行的 JAR

- 狀態：接受
- 日期：2026-09-22

## 問題

`frame.ps1`和 `query.ps1`每次都重新編譯；日常用 CLI時啟動不夠方便。使用者亦曾問 Java是否要先包 `.o`。需要一個可移動、可執行的 Java artifact。

## 決定

用 JDK內建 `javac`及 `jar`建立 `build/PromptForge.jar`，manifest指定 `promptforge.Main`。本機與 CI先跑測試，再執行 `scripts/package.ps1`，最後**不加任何 encoding參數**用 `java -jar`對中文範例做 smoke test。JAR的標準輸出／錯誤採 UTF-8。JAR是建置產物，不加入 Git；source、ADR、測試和打包指令全部加入 Git。暫不引入 Maven，只因這階段沒有外部 Java依賴。

## 後果

建置一次後，使用時只需相容的 Java runtime；開發時改了 Java source要重新打包。將來引入 Lucene等 library時，另寫 ADR轉 Maven或其他 build tool，不靠手工拼 classpath。
