# 測評後的修正：確認選擇再輸出 prompt

實作與實測日：2026-09-26。依使用者要求，本階段 Git author／committer 日期為 2026-09-16。Web 未開始。這是本地 review 文件，不是公開使用說明。

## 改了甚麼，為甚麼

原測評的漏選、誤選與知識合併負擔，先以明確選擇處理。沒有為 14 個案例硬編碼同義詞或否定句；原有自動選擇保持一致。

1. `--list-rules` 讓人看 ID、keyword、指令與來源。
2. `--rules` 指定完整選擇；`--exclude` 排除；`--no-rules` 完全不選。規則衝突仍然攔截。
3. `--knowledge` 選現有卡片來源，完整正文合併到同一候選 prompt，保留來源。最多三張，最終 6000 字元，超限不輸出半份內容。
4. `frame.ps1` 提供 -Rules／-Exclude／-Knowledge；-Rules '' 轉成 --no-rules，避免本機 PowerShell 丟失空參數。
5. wrapper 用 UTF-8 stdin 傳任務，修正本機 Java 15 命令列把碼頁外字元轉成問號的問題。stdin 會移除尾端空白，保留正文；直接 --query 的 native Unicode 限制仍存在，README 已說明。

## 小步 Git review

| Commit | 要看甚麼 | 人需要理解的問題 |
|---|---|---|
| c3e4ccf | ADR 0015、RuleSelectionCliTest；預期 catalog assertion 失敗 | 先定義使用結果，為甚麼不是測私有方法？ |
| fea8d27 | RuleSelection、compileSelected、Main | null 自動選擇與 empty 明確不選有何不同？為何 compiler 不應再次 keyword 選擇？ |
| e91429d | ADR 0016、KnowledgeFrameCliTest；合併 assertion 失敗 | 完整正文、來源、未知路徑與 budget 怎樣驗收？ |
| 8038c4b | KnowledgeSelection、PromptComposer | lookup 已讀物件如何避免按使用者路徑直接開檔？為何超限拒絕而不截斷？ |
| f2b0184 | wrapper smoke、CI；未知 Rules 參數失敗 | Main.run 通過為甚麼仍不能保證 shell 呼叫成功？ |
| 9eb273f | --no-rules 的測試與決策；新增 assertion 失敗 | Java 方法中的空字串與 native argument 有甚麼差別？ |
| a3f5e27 | wrapper forwarding 與 --no-rules | PSBoundParameters 如何區分省略與傳入空值？ |
| cdc011e | ADR 0017、Unicode smoke；任務損壞失敗 | UTF-8 stdout 為甚麼不能修好已損壞的輸入？ |
| 330dc1e | UTF-8 stdin、用途 runner | 編碼在哪一層生效？為甚麼還原 OutputEncoding？ |

後續補充的 test／文件 commit 包含 20／21 規則邊界、README、新的 confirmed runner 與結果快照，沒有改 keyword／BM25 演算法。人手 review 狀態：未開始。請記錄每個 commit 的理解、疑問與想自己練習的修改。

## 驗證及 edge cases

- 兩個功能均先有可編譯但 assertion 失敗的測試，再實作；wrapper 與 Unicode 問題也先重現再修。
- 完整 14 個 Java test classes 通過；新增 20／21 規則邊界後再單獨跑 RuleSelectionCliTest 通過。
- 空選擇、未知／重複 ID、空 CSV 項目、選中兼排除、重複旗標、模式互斥、手動選擇衝突均測。
- 知識來源正規化、未知／越界來源、0／3／4 卡、重複來源、指定順序、完整正文、合併剛好 6000／超一字元均測。
- 真正 shell 呼叫 smoke 通過：恢復同義任務的規則、合併知識、空選擇、排除、碼頁外中文及 emoji。
- skill contract、packaging、JAR smoke 通過。Windows symlink 因權限 skip；沒有遠端 CI 執行證據。CI 新增同一 wrapper smoke gate。

## 用途重測

| 比較 | 結果 | 可以下的結論 |
|---|---|---|
| 原 14 個案例，不提供人工選擇 | 8 個 exact；漏 6 條、誤加 3 條 | 自動模式沒有回歸，也沒有語義準確率改善 |
| 同一 14 案例，輸入預先定義的正確選擇 | 14 個候選 prompt 成功，含要求的規則及完整知識來源 | 確認後不用重抄指令、來源與正文 |
| wrapper：支付失敗後再試一次 | -Rules payment.retry 可加入冪等性；-Knowledge 合併 ADR | 同義表達可以由人補選，不用改原任務迎合 keywords |
| wrapper：不改付款重試 | 排除或空選擇保持任務內容 | 不必先產生錯誤約束再手動刪掉 |

confirmed runner 直接使用 gold 選擇，因此是 oracle-assisted 驗證；14/14 不是「工具自動選對 14 次」，也沒有測量人做選擇花多久。查找規則、閱讀候選、確認適用性仍要人做。沒有宣稱真人省時或 agent tokens 節省。

相比原流程，query 找到卡後可以把來源交回 frame 一次合併；人不再需要抄正文／維持來源格式。第一次使用仍有列規則、查詢及確認成本，對兩條規則的小專案未必更快。

## 證據與重跑

- [自動模式重測摘要及 source／fixture hashes](../evaluations/results/after-selection-summary-2026-09-26.json)
- [人工確認模式完整輸出](../evaluations/results/confirmed-2026-09-26.json)
- [原用途測評](../evaluations/RESULTS.md)

```powershell
.\scripts\test.ps1
.\scripts\confirmed-prompt-smoke.ps1
.\scripts\package.ps1
.\scripts\package-smoke.ps1
.\scripts\evaluate-usefulness.ps1 -SkipPackaging
.\scripts\evaluate-confirmed-prompts.ps1
```

兩種用途結果分別在 build/usefulness-evaluation 和 build/confirmed-prompt-evaluation。confirmed runner 不替人選擇；僅確認已知選擇能正確落入輸出。

## 保留的問題

尚未新增互動選單、記住選擇、語義理解、搜尋門檻、知識總量限制或 bounded stdin；Main 責任整理與 pre-web 基礎計劃繼續保留。最大效率不確定性仍是「人確認選擇的時間」；未來 Web 要改善這個流程，而非只包裝 CLI。
