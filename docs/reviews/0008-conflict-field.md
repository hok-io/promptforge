# Conflict metadata — 實作驗收

- 自動測試：規則檔可讀取兩個 conflict ID；清單不可由外部修改；自己衝突或重複 ID均報格式錯誤。
- Java練習：constructor overload保留舊呼叫方式，新增不可變 list並檢查集合唯一性。
- 下一小步：只在兩條規則同時命中時阻止 frame；本 commit尚未加入該編譯行為。
