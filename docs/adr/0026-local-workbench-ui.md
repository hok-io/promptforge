# ADR 0026：把操作和版本狀態顯示給人

本地 HTML／CSS／JavaScript 呼叫 Java JSON API。不用另一套前端框架，讓業務流程集中在 Java 的 domain、service、repository 和 Controller。

Prompt 頁提供手動／自動規則、知識搜尋與選擇、有序區塊、變數、保存前比較。任務框架只用任務文字；提示詞範本才加入區塊。預覽由按鈕觸發，不在每次打字時發請求。同專案晚到的預覽只可顯示最後一次；切換專案後捨棄原專案回應。新草稿重設模式、變數和選擇。

Skill 頁保留原文及 references，狀態綁版本。未保存的正文不能進行批准、啟用、匯出或新增評估，避免人看著草稿卻操作舊版。保存後更新狀態及過期評估。匯入和匯出是明確的專案內名稱，啟用不代表安裝。

所有外部文字用 textContent 或表單值顯示，不執行 Markdown／HTML。來源與評估資料都可看見；來源較長時換行。窄畫面仍保留規則與知識選擇。自訂 ID 与 Java slug 限制一致：小寫字母開頭、小寫英數字及短橫線、最多 40 字。

Playwright Java 測真瀏覽器的成功流程、缺變數、過期保存、HTML 文字及窄畫面選擇。測試擋住外部請求，截圖只留本地 build 目錄。瀏覽器測試工具可使用已安裝 Edge 或指定版 Chromium，見 [官方瀏覽器文件](https://playwright.dev/java/docs/browsers)。
