# Web 階段逐 commit review

先讀 [分段學習路線](../learning/spring-boot-stages.md)，再用 `git show <commit>` 逐步看修改。48 個實作／測試提交及文件整理分配到 2026-09-17–24；日期是展示與學習安排，實測日期仍是 2026-09-26。測試先行提交可能暫時失敗；靜態版面提交尚未串接 API。

| 日期／時間 | Commit | 這小步解決甚麼 | 你要理解的 Java／實作問題 |
|---|---|---|---|
| 09-17 09:12 | 18d45d6 | 分 core／workbench／web 和 JUnit 建置 | 哪個 module 可以依賴 Spring？舊 main 測試如何接入 JUnit？ |
| 09-17 09:35 | b7b8ef6 | 先定 prompt 區塊及變數契約 | 哪些輸入要接受？字面 `$` 和反斜線為何需要測？ |
| 09-17 09:58 | 6a77cda | Maven Wrapper 和檔案保存測試 | Wrapper 省掉甚麼安裝？revision 為何能拒絕舊版本？ |
| 09-17 10:21 | 8627e51 | 先定 skill 的批准／匯出契約 | 修改正文和 reference 時，批准會怎樣？ |
| 09-17 20:08 | a9c2602 | 先定 loader 的大小及路徑限制 | 單檔上限為何不等於整個目錄上限？ |
| 09-17 20:31 | 0f93184 | 固定欄位次序、限制知識卡、共同路徑檢查 | walk visitor 怎樣在第 101 張停止？為何要 real path？ |
| 09-17 20:54 | 08f543b | 先定共用 preview 和輸入串流契約 | readNBytes 為何讀上限＋1？不合法 UTF-8 怎樣處理？ |
| 09-17 21:17 | a342043 | PromptDocument、PromptBlock、編譯器 | record constructor 怎樣維持合法且不可變的物件？ |
| 09-18 09:12 | 9a650d1 | 先定 HTTP 和 CSRF 契約 | MockMvc 能證明甚麼，不能證明甚麼？ |
| 09-18 09:35 | b966fc2 | 註冊專案、WorkspaceFiles、RevisionFileStore | 誰負責路徑？誰負責保存？同步鎖的範圍是甚麼？ |
| 09-18 09:58 | cfe56f9 | FrameService 和 JSON repository | Interface 使 service 少知道了甚麼？CLI 如何共用？ |
| 09-18 10:21 | 1e306ba | 真實 HTTP 和同時保存測試 | CountDownLatch 如何令兩個編輯者競爭同一 revision？ |
| 09-18 10:44 | b68d27a | Windows junction 和讀取上限測試 | Junction 與 symlink 有何測試差別？清理時為何只 unlink？ |
| 09-18 20:08 | 9481c16 | 外部評估證據及完整 CI 設定 | TEST 和 MODEL_REVIEW 為何不同？設定 CI 不等於已執行。 |
| 09-18 20:31 | be997bf | 先定來源相連的 Web 搜尋 | 沒結果及空 query 為何是不同結果？ |
| 09-18 20:54 | b824c16 | Skill 資料模型及評估記錄 | record 如何限制合法狀態？資料模型為何不依賴 Spring？ |
| 09-18 21:17 | 187a0ba | 讀取 Skill 原文、references、版本 hash | 哪些檔案進入 hash？元資料為何不進入？ |
| 09-19 09:12 | f6b620e | 以暫存目錄匯入 Skill | 匯入失敗時如何避免留下半個 bundle？ |
| 09-19 09:35 | e6674e9 | 比較及保存 Skill 修改 | revision 如何阻止覆蓋別人的修改？ |
| 09-19 09:58 | 4012146 | review、approve、activate 及匯出 | 批准綁定哪個版本？匯出為何不直接覆蓋外部資料？ |
| 09-19 10:21 | 2a19652 | 記錄外部評估證據 | 測試證據與人的批准有何分別？ |
| 09-19 10:44 | 8721a9b | 啟動 Spring、組裝 Bean、限制 localhost | constructor injection 如何組裝普通 Java 物件？filter 保護甚麼？ |
| 09-19 20:08 | e7c207d | 專案列表、context 及 CSRF API | Controller 如何把資料交給服務？CSRF token 從哪裡來？ |
| 09-19 20:31 | 98620f3 | Prompt 預覽及知識搜尋 API | request record 如何連接既有 Java 核心？ |
| 09-19 20:54 | 94150d3 | Prompt 載入、diff 及保存 API | 409 衝突與無效輸入分別由誰處理？ |
| 09-19 21:17 | 953b810 | Skill 檢視及匯入 API | HTTP 路由為何不自行解析檔案？ |
| 09-19 21:40 | c639ae2 | Skill 編輯及狀態轉移 API | 服務中的批准條件如何透過 HTTP 呈現？ |
| 09-20 09:12 | 926428c | Skill 評估及匯出 API | API 為何不能把評估自動變成批准？ |
| 09-20 09:35 | 6306b07 | 先定真瀏覽器和乾淨 class 目錄 | 瀏覽器測試多驗證了甚麼？殘留 class 為何可令測試假綠？ |
| 09-20 09:58 | c54b2d6 | CLI 每次編譯先清理 | 清理前檢查絕對路徑的目的？為何不刪 source？ |
| 09-20 10:21 | 8207bce | 先定 registry 壞資料契約 | 缺 root 可以偷偷解讀成目前目錄嗎？ |
| 09-20 10:44 | e46ba15 | 必須明確指定 root | 驗證應放在設定解析還是 HTTP 請求？ |
| 09-20 20:08 | 9d2a7b9 | 拒絕 dangling link、平台 path 比較 | Windows 和 Linux 的大小寫比較為何不同？ |
| 09-20 20:31 | 6f0592e | 瀏覽器負例、排序及窄畫面 | Assertion 真能證明保存後資料仍相同嗎？ |
| 09-20 20:54 | 7baa8df | 記下 UI 操作設計理由 | UI 和 domain 的責任如何分開？ |
| 09-20 21:17 | e45f232 | 靜態版面與表單（尚未串接） | HTML/CSS 提供哪些控件？此步尚不能操作 API。 |
| 09-20 21:40 | 45d09a1 | 串接 Prompt 搜尋、預覽、diff 及保存 | 從 fetch 追到 Controller、repository 和檔案。 |
| 09-21 09:12 | 3e96839 | 串接 Skill 編輯、評估及批准 | 畫面狀態如何跟隨服務回應？文字如何安全顯示？ |
| 09-21 09:35 | dd93d30 | 先定只有區塊的模板 | 這是人的用途，還是實作自己規定的限制？ |
| 09-21 09:58 | c0ab584 | 區塊模板可保存、中文可讀 | 前端的限制是否比 domain 多了不必要條件？ |
| 09-21 10:21 | c9fd436 | 空草稿顯示指引 | 正常起始狀態應該當成錯誤嗎？ |
| 09-21 20:08 | 0386b61 | 修正 async 後按鈕不恢復 | event.currentTarget 為何不能等 await 後才讀？瀏覽器負例如何捉到？ |
| 09-21 20:31 | fbd66bd | 建置、啟動腳本和真實匯入範例 | 建置和執行是兩件甚麼事？修改資料需要重新打包嗎？ |
| 09-21 20:54 | 04fc964 | 人用 README、私人設計及學習分開 | 使用说明、設計理由及實測證據各回答甚麼問題？ |
| 09-21 21:17 | 2d8b1f2 | 先定長模板的規則控件驗收 | 元素存在和人在 viewport 看得到有何不同？ |
| 09-22 09:12 | 8c303f3 | 桌面側欄固定，保持規則可見 | 這個畫面修正為何只需重跑 browser tests？ |
| 09-22 20:08 | fb57dc4 | 記錄實際驗收結果及限制 | 哪些結果有實測證據？哪些仍未驗證？ |
| 09-23 09:12 | 287a17b | README 操作名稱與畫面一致 | 新使用者能否跟說明完成第一次操作？ |

每個 commit 記：你的理解、最重要的測試、仍未證明的事、想自己改的寫法。歷史拆分未改動最終程式；人手 review 仍由你完成。備份及對照見 [歷史整理記錄](web-history-rewrite.md)。
