# Web 階段逐 commit review

先從 [Java 路線](../learning/java-web-tour.md) 跑一次實際操作，再用 `git show <commit>` 讀修改。這些是你的 review 問題，不代表你已完成 review。Git 作者與提交日期按要求使用 2026-09-16；實際測試是 2026-09-26。

| Commit | 這小步解決甚麼 | 你要理解的 Java／實作問題 |
|---|---|---|
| aa1d1aa | 分 core／workbench／web 和 JUnit 建置 | 哪個 module 可以依賴 Spring？舊 main 測試如何接入 JUnit？ |
| 7b7050d | 先定 prompt 區塊及變數契約 | 哪些輸入要接受？字面 `$` 和反斜線為何需要測？ |
| 89b6121 | Maven Wrapper 和檔案保存測試 | Wrapper 省掉甚麼安裝？revision 為何能拒絕舊版本？ |
| 08bc50a | 先定 skill 的批准／匯出契約 | 修改正文和 reference 時，批准會怎樣？ |
| d534ee0 | 先定 loader 的大小及路徑限制 | 單檔上限為何不等於整個目錄上限？ |
| 60ffcfc | 固定欄位次序、限制知識卡、共同路徑檢查 | walk visitor 怎樣在第 101 張停止？為何要 real path？ |
| a214203 | 先定共用 preview 和輸入串流契約 | readNBytes 為何讀上限＋1？不合法 UTF-8 怎樣處理？ |
| 172a38c | PromptDocument、PromptBlock、編譯器 | record constructor 怎樣維持合法且不可變的物件？ |
| 4a0533f | 先定 HTTP 和 CSRF 契約 | MockMvc 能證明甚麼，不能證明甚麼？ |
| 44d9e8e | 註冊專案、WorkspaceFiles、RevisionFileStore | 誰負責路徑？誰負責保存？同步鎖的範圍是甚麼？ |
| e781bc3 | FrameService 和 JSON repository | Interface 使 service 少知道了甚麼？CLI 如何共用？ |
| 702b676 | 真實 HTTP 和同時保存測試 | CountDownLatch 如何令兩個編輯者競爭同一 revision？ |
| b708cef | Windows junction 和讀取上限測試 | Junction 與 symlink 有何測試差別？清理時為何只 unlink？ |
| c06cdb6 | 外部評估證據及完整 CI 設定 | TEST 和 MODEL_REVIEW 為何不同？設定 CI 不等於已執行。 |
| 22c8787 | 先定來源相連的 Web 搜尋 | 沒結果及空 query 為何是不同結果？ |
| e318a83 | Skill 原文、hash、狀態、評估和匯出 | 分別讀 import、save、transition、export，每次只追一個方法。元資料為何不進 bundle hash？ |
| 5c9471e | Spring 組裝、Controller、HTTP 錯誤及 filter | Bean 是普通 Java 物件嗎？HTTP body 和來源限制在哪一層？ |
| 532f0be | 先定真瀏覽器和乾淨 class 目錄 | 瀏覽器測試多驗證了甚麼？殘留 class 為何可令測試假綠？ |
| 4b1b488 | CLI 每次編譯先清理 | 清理前檢查絕對路徑的目的？為何不刪 source？ |
| e5ae8f3 | 先定 registry 壞資料契約 | 缺 root 可以偷偷解讀成目前目錄嗎？ |
| 8c12384 | 必須明確指定 root | 驗證應放在設定解析還是 HTTP 請求？ |
| 16b00c7 | 拒絕 dangling link、平台 path 比較 | Windows 和 Linux 的大小寫比較為何不同？ |
| 4f32f82 | 瀏覽器負例、排序及窄畫面 | Assertion 真能證明保存後資料仍相同嗎？ |
| c1f6f3a | 記下 UI 操作設計理由 | UI 和 domain 的責任如何分開？ |
| a4048ce | Prompt／skill 工作台畫面 | 請從一個 fetch 追到 Controller、service 和檔案。 |
| f3928fa | 先定只有區塊的模板 | 這是人的用途，還是實作自己規定的限制？ |
| 2579c4d | 區塊模板可保存、中文可讀 | 前端的限制是否比 domain 多了不必要條件？ |
| d602dc7 | 空草稿顯示指引 | 正常起始狀態應該當成錯誤嗎？ |
| b9b0dd0 | 修正 async 後按鈕不恢復 | event.currentTarget 為何不能等 await 後才讀？瀏覽器負例如何捉到？ |
| 79b94d7 | 建置、啟動腳本和真實匯入範例 | 建置和執行是兩件甚麼事？修改資料需要重新打包嗎？ |
| 6631d84 | 人用 README、私人設計及學習分開 | 使用说明、設計理由及實測證據各回答甚麼問題？ |
| 13bdd5a | 先定長模板的規則控件驗收 | 元素存在和人在 viewport 看得到有何不同？ |
| 6db31b9 | 桌面側欄固定，保持規則可見 | 這個畫面修正為何只需重跑 browser tests？ |

每個 commit 記：你的理解、最重要的測試、仍未證明的事、想改的寫法。先記下疑問，再開新的小修正，不修改舊碼來遮住原來學習過程。

e318a83 的 service 比其他小步大；分四次 review 它的不同方法即可，不用一次讀完。它還可以日後拆成 bundle repository、workflow 和 evaluation service，但先理解真實重複和依賴，才決定要不要再抽 class。
