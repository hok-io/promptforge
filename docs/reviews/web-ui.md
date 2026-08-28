# Web UI review

## 交付內容

- `web/src/main/resources/static/index.html` 提供繁體中文的本機工作台結構，分為提示詞與技能管理兩個主頁籤；桌面顯示專案規則／知識選擇側欄和右側預覽，窄螢幕改為單欄。
- `app.js` 以原生 JavaScript 呼叫 `/api` contract，先取得 CSRF token，所有寫入要求附上指定 header。專案切換、草稿編輯、提示詞差異確認、技能審核狀態和收件匣匯入／專案匯出均在 UI 中串接。
- `style.css` 使用本地系統字型與 CSS，沒有 CDN 或外部資產依賴。
- 窄螢幕下規則與知識選擇仍可在側欄捲動使用；新草稿和專案切換會重設變數、模式與選擇，已載入提示詞則保留 `excludedRuleIds`。
- 預覽需由使用者按「更新預覽」觸發；同一專案連續預覽時只呈現最後一次請求的回應。
- `BrowserWorkflowTest` 使用 Playwright 1.55.0 驗證真實瀏覽器中的提示詞儲存／重載及技能匯入／審核／核准／匯出，並阻擋和檢查非本機請求。可用 `PROMPTFORGE_BROWSER_CHANNEL=msedge` 或 `-Dpromptforge.browser.channel=msedge` 選擇已安裝的 Edge；預設使用 Chromium。測試截圖寫入 `build/browser-prompt.png` 與 `build/browser-skill.png`。瀏覽器設定參照 [Playwright Java 瀏覽器文件](https://playwright.dev/java/docs/browsers)。

## 安全與版本保護

- API 回傳內容一律寫入 `textContent`、表單值或新建 DOM 節點的文字，不用 `innerHTML`，也不把 Markdown 當 HTML 執行。
- 每個專案請求綁定切換世代；舊專案的晚到回應會被捨棄。切換或重新載入時若有未儲存草稿會先詢問。
- 提示詞／技能在 PUT 前先取得差異並開啟確認視窗；409 顯示版本衝突訊息，不會自動重試覆蓋。
- 匯入只接受 `.promptforge/inbox/<資料夾名稱>` 的名稱；匯出只接受單一目標名稱，並說明寫入 `.promptforge/exports/`、不會自動安裝。
- 錯誤訊息優先顯示伺服器 JSON `message` 與 HTTP 狀態碼。知識卡片最多選三張；規則可選自動或手動模式。

## 操作限制與驗收狀態

- UI 依據本機 `/api` contract 使用 context 的 `skills` 摘要和提示詞 PUT 回傳的 `{revision, document}` envelope。
- 新增提示詞由使用者輸入 slug ID 與名稱；diff 和 PUT 路徑及 `document.id` 使用相同 ID，版本為 `new`。ID 已存在時以 409 顯示衝突，UI 不會覆蓋既有內容。
- 評估表單以 JSON 範例建立 TEST 或 MODEL_REVIEW 記錄，範例初始 `subjectHash` 使用目前技能 revision；送出時保留表單中的 hash，並要求 64 位十六進位字串，讓舊版本評估仍可被記錄為 stale。使用者需提供專案內已存在的相對 evidence path，路徑有效性由伺服器驗證。
- 儲存技能只更新目前技能詳情與 revision，不重新載入專案 context，也不觸發提示詞預覽，因此不會清除提示詞草稿或 dirty 狀態。
- 技能狀態按服務回傳的 `IMPORTED`、`REVIEWED`、`APPROVED`、`ACTIVE` 流程顯示操作。匯入的技能 ID 可與收件匣資料夾名稱不同。

已執行 4 個 Playwright browser workflows，全部通過；完整 reactor 49 個 JUnit 結果通過。最後桌面側欄修正後亦以 Maven offline mode 重跑 browser tests 並重新打包。詳見 [實際驗證](../evaluations/web-verification.md)。中文文字已加大，桌面側欄固定在 viewport 內；窄画面保留原有捲動選擇。

