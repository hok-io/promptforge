# Loader limits — 實作驗收

- 自動測試：101 張卡拒絕；100 張、各小於 8 KiB 但合計超過 256 KiB 的卡拒絕；missing rule 欄位固定先報 `keywords`；原有 8 KiB 單檔上限維持。
- 路徑邊界：兩個 loader 檢查專案路徑的祖先、設定目錄、巢狀子目錄與內容檔；檢查 symbolic link，並以 real path 防止內容逃出專案根目錄。
- 驗證：`javac --release 15` targeted compile 及 `LoaderLimitsTest` 通過。Windows 執行環境無建立 symbolic link 權限，因此三個 symlink 情境由測試明確 skip；Java NIO 對實際 Windows junction 的 `isSymbolicLink` 行為需在有 junction fixture 的 Windows 環境另外確認，real-path containment 仍會攔截已遍歷到的越界路徑。
- Java 練習：NIO real-path containment、目錄遍歷與容量限制、把固定順序寫成 List。
- 邊界：byte 上限指檔案系統大小總和，不代表 Unicode 字元數；總量錯誤會拒絕整個集合，不回傳局部卡片。
