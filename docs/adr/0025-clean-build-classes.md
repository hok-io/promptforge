# ADR 0025：每次編譯先重建 class 目錄

狀態：Accepted。實際觀察日 2026-09-26，

CLI、測試與可執行 JAR 都先呼叫 `scripts/prepare-classes.ps1`，清除各自的輸出目錄再編譯。這避免舊 `.class` 殘留讓已刪除或改名的型別繼續出現在執行結果中。測試 JVM 的 `java.io.tmpdir` 固定在 `build/test-tmp`，臨時測試檔不散落到系統暫存路徑。

清理器只接受位於絕對 `build` 根目錄之下的輸出目錄。遞迴刪除前逐層檢查輸出路徑及其祖先的 ReparsePoint 屬性，遇到 symbolic link、junction 或其他可識別的 reparse point 即拒絕。通過檢查後以 LiteralPath 移除舊目錄，再建立空目錄供 javac 使用。

`prepare-classes-smoke.ps1` 會先在 classes 與 package-classes 放入過期 marker，證明共用 build 清理流程會刪除它；並將輸出目錄替換為指向另一個 build 暫存目錄的 junction（Windows）或 symbolic link（其他平台），確認清理器拒絕操作且外部 sentinel 保留。smoke 清理 link 時只移除 link 本身，遞迴清理前先確認目標仍在 build 根目錄之下。此檢查縮小了路徑誤刪風險，但檢查與刪除之間仍存在一般檔案系統的競態窗口。
