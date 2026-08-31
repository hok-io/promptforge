# 目前版本的學習入口

先讀 [整理後的 Java 架構](../learning/read-the-clean-architecture.md)，按預覽、保存、Skill、錯誤處理四段實際操作。下面保留舊階段的 review 記錄；最新責任分工以這份學習入口為準。

# 按 commit review Java

Web 已按你之後「先實作好了」的要求落地，原 review gate 已撤回。新階段從 [Java Web 學習路線](../learning/java-web-tour.md) 和 [Web 逐 commit review](web-commit-review.md) 開始；實際測試結果另見 Web 驗證報告。你的原 review 仍可照常逐個完成。

後續已根據用途測評完成 [確認規則／合併知識與重測](phase-5-confirmed-prompts.md)。原 31 張卡仍對照原實作；新增 commit 另列在階段五，請按 Git 差異看當時程式。

Git 日期已依你的要求由 2026-09-01 起重新編排；本頁與 review 卡已更新 hash。原時間及新 hash 見 [對照紀錄](date-rewrite.md)。

整理日期：2026-09-26。原有實作基線：`0891eb9`，共 31 個 commit。這份文件和後續規劃是新文件，不代表你已批准原有實作。

## 依序閱讀

1. [逐個 commit review](commit-review.md)：31 個 commit各有用途、要看檔案、Java概念和 review問題。
2. [Web前的核心整理](../plans/pre-web-foundation.md)：區分既有缺口、新資料模型和 Web開始條件。
3. [本地 Prompt／Skill Workbench規劃](../plans/local-workbench.md)：新產品用途、畫面、資料、Spring Boot學習路線及細步實作。

初次整理本頁時只有 review 和規劃文件。後續 CLI 修正見上方階段五；Web 新實作見本頁開頭的新入口。原 31 個 commit 的 review 卡仍描述當時程式。

## 每次只 review一個 commit

在 `D:\PromptForge`執行：

```powershell
git log --reverse --oneline
git show --stat b7f984e
git show b7f984e
```

之後換成清單中下一個 hash。`git show`不會切換或修改工作目錄。看歷史版本檔案可用 `git show <hash>:<path>`；目前檔案可能已被後面的 commit修改，不能把它當成當時內容。

先回答四件事：這一步解決甚麼、資料怎樣經過 Java class、測試證明甚麼、你是否接受這個取捨。看不明白的地方先記錄，唔使假裝完成 review。可以逐個 hash問 Codex解釋，要求用原來任務例子逐行講。

部分 `test:` commit刻意不能編譯，部分能編譯但 assertion失敗。這是當時測試先行的 red狀態；不要要求每一個歷史 commit都可交付。後面 `feat:`／`fix:`才是對應 green版本。測試在實作 commit內亦有追加，不能把 Git歷史說成每一項 edge test都先寫完。

## 你的 review紀錄

每個 commit頁有四個欄位：`我理解的用途`、`不明白／不同意`、`我想自己改的練習`、`結果`。結果填「未開始／理解但待修／接受」。31個項目目前全部未開始；自動測試或 subagent review不替代你的 review。

## Web開始之前

- [ ] 31個原有 commit都有人 review結果。
- [ ] 高優先缺口已逐個小 commit修正並重新驗收。
- [ ] Java 21、Maven Wrapper、JUnit及可重現建置已 review。
- [ ] CLI和新 prompt／skill核心有共用 application services。
- [ ] 結構化 prompt及 skill讀取／儲存／預覽先有 CLI測試。
- [ ] 再根據 Web規劃開始 Spring Boot；不是讀完這份文件就自動開始。

目前可證明的是歷史本機測試紀錄：12個 test classes、skill檢查和 JAR smoke通過。這不是12個 JUnit test cases，也不是模型評估。GitHub Actions設定了 Windows／Ubuntu，但沒有遠端成功結果；Windows symlink建立測試曾跳過。詳細限制見 [階段驗收](phase-3-4-retrieval-release.md)。

## 已完成的 Web 實作，下一步由你 review

以上是原有 CLI 階段的 review 清單，並非目前 Web 的阻擋條件。Web 已按授權實作；從 [分段 Java／Spring 路線](../learning/spring-boot-stages.md) 開始，再逐個讀 [48 個 commit](web-commit-review.md)。自動測試沒有代替你的人手 review。
