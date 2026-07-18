# Executable JAR — 實作驗收

- `scripts/package.ps1`用 JDK內建工具編譯主程式、設定 Main-Class，產生 `build/PromptForge.jar`。
- `scripts/package-smoke.ps1`以 `java -jar`跑付款任務，檢查 packaged版本真的讀到規則。
- 追加中文 smoke：直接 `java -jar`，不加 `-Dfile.encoding`；必須能看到完整中文規則和 ADR標題。獨立 review找到的 Windows亂碼已用 red/green commit修正。
- 人實際使用可直接調 `java -jar`，無需 Spring Boot或 `.o`。JAR不入 Git，避免 source與 binary不同步。
- CI workflow在測試和 skill檢查後才打包、smoke test；遠端 CI仍需 push到 GitHub後驗證。
