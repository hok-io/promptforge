# Clean build classes — 實作驗收

- `scripts/prepare-classes.ps1` 要求絕對 BuildRoot／TargetPath，僅允許清理 build 根目錄的子目錄；遞迴刪除前檢查路徑及祖先是否為 ReparsePoint，然後用 LiteralPath 刪除並重建輸出目錄。
- `frame.ps1`、`query.ps1`、`scripts/test.ps1` 和 `scripts/package.ps1` 都使用共用清理器；test JVM 的 `java.io.tmpdir` 指向 `build/test-tmp`。
- `scripts/prepare-classes-smoke.ps1` 在 classes 與 package-classes 注入過期 class marker 並確認清除，再建立 build 外部的 junction（Windows）或 symbolic link（其他平台），確認清理被拒絕且目標 sentinel 沒有改變。
- 驗收：PowerShell safety smoke 通過；`scripts/test.ps1` 顯示 `PASS: 15 test classes`；JAR 建置與 `scripts/package-smoke.ps1` 通過；frame、query、package 三個入口在 stale marker smoke 中都清除了舊 class。
- 限制：ReparsePoint 保護依檔案系統屬性回報；一般使用者在檢查與刪除間仍可能競速替換路徑。
