# 逐個 Git commit review

原實作範圍：8b0238f → a89db4b，共 31 個 commit。所有人手 review 狀態均未開始。今次新增的規劃文件另計，不會重寫原歷史。

## 怎樣讀一張卡

先執行 `git show --stat HASH`，再 `git show HASH`。檔名均相對於 repository；Java 檔位於 `src/main/java/promptforge/`，Test 位於 `src/test/java/promptforge/`。先用自己的話解釋用途，再看差異。不要 checkout 舊版本覆蓋正在 review 的工作目錄。

「預期失敗」表示測試先於實作加入，可能尚未編譯成功；不是聲稱每一項已有 assertion 紅燈證據。「實作」的歷史結果請對照對應 review 文件，不代表今天重新執行。完整 gate 見 [START-HERE](START-HERE.md)。

## 01 · 8b0238f · 建立測試入口

- **用途／要睇：** scripts/test.ps1、SmokeTest.java。
- **決策：** [ADR](../adr/0001-build-and-test.md)。類型：建置。
- **你要理解：** 理解 javac 編譯、java 執行與失敗 exit code；CI 設定存在不代表遠端已跑。
- **檢查方法：** `git show 8b0238f`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）
## 02 · 8739dac · 先定義規則檔契約

- **用途／要睇：** RuleFileTest.java。
- **決策：** [ADR](../adr/0002-rule-files.md)。類型：預期失敗。
- **你要理解：** 讀取正常、缺欄位、重複 ID 的案例；此時類別未完成，可能是編譯失敗。
- **檢查方法：** `git show 8739dac`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 03 · 65c5532 · 把檔案變成 Rule 物件

- **用途／要睇：** Rule.java、RuleFileLoader.java、RuleFormatException.java。
- **決策：** [ADR](../adr/0002-rule-files.md)。類型：實作。
- **你要理解：** 為何 Rule 保存來源？constructor 怎樣保護資料？注意此 commit 也修改測試。
- **檢查方法：** `git show 65c5532`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 04 · dd8ef4b · 先定義規則選擇

- **用途／要睇：** RuleSelectorTest.java。
- **決策：** [ADR](../adr/0003-rule-selection.md)。類型：預期失敗。
- **你要理解：** 全部 keyword 命中才選取；空結果及排序是否清楚？
- **檢查方法：** `git show dd8ef4b`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 05 · 7afabf3 · 挑選相關規則

- **用途／要睇：** RuleSelector.java。
- **決策：** [ADR](../adr/0003-rule-selection.md)。類型：實作。
- **你要理解：** 用集合與比較器保持結果穩定；關鍵字匹配不是語義理解。
- **檢查方法：** `git show 7afabf3`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 06 · 0b5cfd4 · 先定義短 frame

- **用途／要睇：** TaskFrameTest.java。
- **決策：** [ADR](../adr/0004-short-task-frame.md)。類型：預期失敗。
- **你要理解：** 有命中才補充指令；無命中保留原 task。
- **檢查方法：** `git show 0b5cfd4`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 07 · 36d57ea · 組合可追溯 prompt

- **用途／要睇：** TaskFrame.java、TaskFrameCompiler.java。
- **決策：** [ADR](../adr/0004-short-task-frame.md)。類型：實作。
- **你要理解：** 區分資料物件與組合行為；來源如何出現在輸出？
- **檢查方法：** `git show 36d57ea`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 08 · 37dd21a · 先定義 CLI 行為

- **用途／要睇：** CliTest.java。
- **決策：** [ADR](../adr/0005-cli-contract.md)。類型：預期失敗。
- **你要理解：** 參數、stdout、stderr、exit code 各有甚麼用途？
- **檢查方法：** `git show 37dd21a`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 09 · c2116b5 · 讓人真正用 CLI

- **用途／要睇：** Main.java、frame.ps1、examples/payments。
- **決策：** [ADR](../adr/0005-cli-contract.md)。類型：實作。
- **你要理解：** 跟一次 task 由參數到輸出的流程；Main 承擔多少責任？
- **檢查方法：** `git show c2116b5`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 10 · b107ad2 · 先定義輸入限制

- **用途／要睇：** RuleBoundaryTest.java。
- **決策：** [ADR](../adr/0006-rule-boundaries.md)。類型：預期失敗。
- **你要理解：** 大小、數量及路徑邊界；限制是否有單位？
- **檢查方法：** `git show b107ad2`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 11 · 371020d · 限制規則與輸出大小

- **用途／要睇：** RuleFileLoader.java、TaskFrameCompiler.java、FrameBudgetException.java。
- **決策：** [ADR](../adr/0006-rule-boundaries.md)。類型：實作。
- **你要理解：** bytes 與 characters 不同；例外應在哪一層處理？此處也補測試。
- **檢查方法：** `git show 371020d`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 12 · 9f2904e · 先檢查 skill 入口契約

- **用途／要睇：** scripts/check-skill.ps1、ci.yml。
- **決策：** [ADR](../adr/0007-codex-skill.md)。類型：結構測試。
- **你要理解：** 這是 skill 檔案檢查，不是模型品質測試，也不是 Java 行為測試。
- **檢查方法：** `git show 9f2904e`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 13 · 60fce31 · 提供按需使用 skill

- **用途／要睇：** skills/prompt-frame/SKILL.md。
- **決策：** [ADR](../adr/0007-codex-skill.md)。類型：實作。
- **你要理解：** skill 如何呼叫 Java CLI？不保證每次自動觸發或節省固定 token。
- **檢查方法：** `git show 60fce31`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 14 · 449f96d · 先定義衝突欄位

- **用途／要睇：** RuleConflictFieldTest.java。
- **決策：** [ADR](../adr/0008-rule-conflicts.md)。類型：預期失敗。
- **你要理解：** conflicts 缺省及有值時要有甚麼資料？
- **檢查方法：** `git show 449f96d`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 15 · 7ba70cb · 保存不可變衝突關係

- **用途／要睇：** Rule.java、RuleFileLoader.java。
- **決策：** [ADR](../adr/0008-rule-conflicts.md)。類型：實作。
- **你要理解：** defensive copy 如何防止外部修改集合？
- **檢查方法：** `git show 7ba70cb`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 16 · ef65bdf · 先定義衝突錯誤

- **用途／要睇：** RuleConflictTest.java、CliTest.java。
- **決策：** [ADR](../adr/0008-rule-conflicts.md)。類型：預期失敗。
- **你要理解：** 只對已選規則檢查；錯誤是否讓人知道兩條來源？
- **檢查方法：** `git show ef65bdf`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 17 · aebdbec · 拒絕互相衝突指令

- **用途／要睇：** RuleConflictChecker.java、RuleConflictException.java。
- **決策：** [ADR](../adr/0008-rule-conflicts.md)。類型：實作。
- **你要理解：** 檢查器與 compiler 如何合作？為何不自行決定哪條贏？
- **檢查方法：** `git show aebdbec`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 18 · 2b5c1fe · 記錄第二階段使用證據

- **用途／要睇：** phase-2-edges.md、examples/conflicts、SKILL.md。
- **決策：** [ADR](../adr/0007-codex-skill.md)。類型：文件及 skill。
- **你要理解：** 不是純文件 commit：也改了 skill；檢查記錄與實際行為是否相符。
- **檢查方法：** `git show 2b5c1fe`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 19 · 9fee286 · 先定義知識卡格式

- **用途／要睇：** KnowledgeCardTest.java。
- **決策：** [ADR](../adr/0009-knowledge-cards.md)。類型：預期失敗。
- **你要理解：** ADR、policy 如何保留路徑？壞資料如何回報？
- **檢查方法：** `git show 9fee286`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 20 · 36e259d · 讀取本地知識卡

- **用途／要睇：** KnowledgeCard.java、KnowledgeCardLoader.java。
- **決策：** [ADR](../adr/0009-knowledge-cards.md)。類型：實作。
- **你要理解：** Path、檔案遍歷與例外；單檔限制不等於總量限制。
- **檢查方法：** `git show 36e259d`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 21 · dd6f4b8 · 先定義搜尋排名

- **用途／要睇：** Bm25IndexTest.java。
- **決策：** [ADR](../adr/0010-local-bm25.md)。類型：預期失敗。
- **你要理解：** 排序、空查詢、相同分數及中文案例。
- **檢查方法：** `git show dd6f4b8`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 22 · 6af404d · 實作 BM25 搜尋

- **用途／要睇：** Bm25Index.java、SearchHit.java。
- **決策：** [ADR](../adr/0010-local-bm25.md)。類型：實作。
- **你要理解：** 索引、詞頻、分數及 tie-break；搜尋分數不是答案正確率。
- **檢查方法：** `git show 6af404d`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 23 · 7d913f3 · 先定義 query CLI

- **用途／要睇：** QueryCliTest.java。
- **決策：** [ADR](../adr/0011-knowledge-query.md)。類型：預期失敗。
- **你要理解：** 搜尋結果須有來源；沒結果應正常回應。
- **檢查方法：** `git show 7d913f3`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 24 · 7af24c2 · 讓人搜尋本地 ADR

- **用途／要睇：** Main.java、query.ps1、idempotency.md。
- **決策：** [ADR](../adr/0011-knowledge-query.md)。類型：實作。
- **你要理解：** 以 payment 問題跑例子；query 尚未自動注入 frame。
- **檢查方法：** `git show 7af24c2`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 25 · 4843915 · 先定義可執行 JAR

- **用途／要睇：** scripts/package-smoke.ps1。
- **決策：** [ADR](../adr/0012-executable-jar.md)。類型：包裝測試。
- **你要理解：** JAR 未存在時失敗；這不是 Java 單元測試。
- **檢查方法：** `git show 4843915`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 26 · ecaa97c · 打包 JAR 並試跑

- **用途／要睇：** scripts/package.ps1、package-smoke.ps1。
- **決策：** [ADR](../adr/0012-executable-jar.md)。類型：實作。
- **你要理解：** manifest Main-Class 的用途；舊 class 殘留是後續須修的風險。
- **檢查方法：** `git show ecaa97c`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 27 · b1cd80c · 定義驗證與跨平台 CI

- **用途／要睇：** ValidateCliTest.java、ci.yml、RuleBoundaryTest.java。
- **決策：** [ADR](../adr/0013-local-validation.md)。類型：預期失敗及 CI。
- **你要理解：** 此 commit 同時涉及 CI 與功能測試；往後拆開較易 review。Windows symlink skip 不等於通過。
- **檢查方法：** `git show b1cd80c`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 28 · 1631e4c · 提供整個專案驗證

- **用途／要睇：** Main.java、docs/maintenance.md。
- **決策：** [ADR](../adr/0013-local-validation.md)。類型：實作。
- **你要理解：** validate 與 frame 的不同；理解 exit code 如何供腳本使用。
- **檢查方法：** `git show 1631e4c`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 29 · 7eb8b3c · 先重現中文 JAR 輸出問題

- **用途／要睇：** scripts/package-smoke.ps1。
- **決策：** [ADR](../adr/0012-executable-jar.md)。類型：預期失敗。
- **你要理解：** 直接 java -jar 與 PowerShell wrapper 的 encoding 差別。
- **檢查方法：** `git show 7eb8b3c`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 30 · 01c738a · 明確使用 UTF-8 輸出

- **用途／要睇：** Main.java。
- **決策：** [ADR](../adr/0012-executable-jar.md)。類型：修正。
- **你要理解：** PrintStream encoding 怎樣影響 stdout/stderr？
- **檢查方法：** `git show 01c738a`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）

## 31 · a89db4b · 補邊界案例及交付紀錄

- **用途／要睇：** KnowledgeCardTest.java、phase-3-4-retrieval-release.md。
- **決策：** [ADR](../adr/0012-executable-jar.md)。類型：測試及文件。
- **你要理解：** 12 個 main 測試類不等於 12 個 JUnit case；區分本地證據與遠端 CI。
- **檢查方法：** `git show a89db4b`；逐項對照新增測試／文件與程式差異，不只看 commit 名稱。

你的記錄：

- 我理解的用途：
- 不明白／不同意：
- 我想自己改的練習：
- 結果：未開始（理解／需要解釋／需要修正）
