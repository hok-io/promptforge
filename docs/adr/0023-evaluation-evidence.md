# ADR 0023：Skill 評估證據

Skill 評估是附著在 bundle revision 上的不可變紀錄。TEST 至少引用一個工作區內已存在的證據檔，保存其 SHA-256；MODEL_REVIEW 至少包含一項文字 finding。兩種紀錄都不執行腳本、不計算分數，也不改變人工審核狀態。評估可指向目前或舊的有效 SHA-256；讀取時依目前 bundle hash 顯示 stale，避免舊評估被誤認為現行證據。

紀錄以 `.promptforge/evaluations/<skillId>/<subjectHash>/<evalId>.json` 保存，ID 限小寫 slug，檔案不可覆寫。時間採 ISO-8601，findings 最多 20 項、每項最多 2000 字元。證據路徑由 WorkspaceFiles 限制在已登記專案內，並限制文字檔大小。metadata 與評估紀錄都不參與 skill revision。

這讓工作台能保留測試結果及模型審查依據，同時維持批准流程由人明確操作，且 reference 或 SKILL.md 改動會讓相關評估顯示過期。
