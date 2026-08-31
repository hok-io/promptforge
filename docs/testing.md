# 建置及測試

在 repository 根目錄執行。Web 需要 JDK 21。第一次 Maven 建置和瀏覽器安裝需要網路；`.tools`、依賴快取、瀏覽器及編譯產物不隨原始碼提供。

## Windows

安裝 JDK 21 並設定 JAVA_HOME／PATH；完整測試需要 Edge。

```powershell
.\scripts\test.ps1
.\scripts\check-skill.ps1
.\scripts\confirmed-prompt-smoke.ps1
.\scripts\package.ps1
.\scripts\package-smoke.ps1
.\scripts\build-web.ps1
.\scripts\start-web.ps1
```

## Linux／其他 Maven 環境

Java 測試不依賴 PowerShell。先建立瀏覽器安裝器所需模組，再安裝 Chromium：

```sh
mkdir -p build/test-tmp
./mvnw -DskipTests install
./mvnw -pl web exec:java -Dexec.classpathScope=test -Dexec.mainClass=com.microsoft.playwright.CLI '-Dexec.args=install --with-deps chromium'
./mvnw clean verify
java -jar web/target/promptforge-web-1.0-SNAPSHOT.jar --promptforge.home=. --server.port=8080
```

系統瀏覽器依賴安裝可能需要管理權限。PowerShell CLI 腳本在 Linux 另需 pwsh。CI 的完整命令見 [workflow](../.github/workflows/ci.yml)。離線 `-o` 僅適用依賴和瀏覽器已安裝的環境。

## 驗證層次

| 層次 | 要證明甚麼 |
|---|---|
| 核心測試 | 規則、搜尋、輸出契約 |
| 檔案整合 | 真實暫存目錄、revision、路徑限制 |
| MockMvc | API、輸入錯誤、CSRF、HTTP 映射 |
| 真 socket | 啟動後的實際 HTTP 行為 |
| Playwright | Prompt／Skill 操作、保存、衝突、畫面可用性 |
| JAR smoke | 打包產物能被執行 |

目前開發環境在 2026-09-26 完成 `clean verify`：52 個 JUnit 結果通過，包括 4 個 Edge 瀏覽器流程。15 個舊 main/assertion 類別以 dynamic tests 接入；不是 15 個細粒度單元測試。4 個舊 Windows symlink 子檢查因權限跳過，JUnit skipped 數不反映它們；junction 檢查有執行。遠端 CI 尚未執行，Ubuntu 成功結果未獲驗證。

合成用途測評見 [協議](evaluations/PROTOCOL.md)。沒有真人計時、模型品質評估或 token 節省比例的證據。
