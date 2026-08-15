# ADR 0021：檔案版本與保存

資料在專案 .promptforge，Git 由人管理。使用 content SHA-256 作 revision，建立檔案用 new。保存前比對版本，不符合回 409；先檢查大小，再寫同目錄 temp file，最後 atomic move，不支援 atomic 時拒絕，不降級為可能半寫的覆蓋。

WorkspaceFiles 拒絕絕對／越界路徑、符號連結及實際路徑離開 project。RevisionFileStore 使用同一程序內按檔案同步，避免同時 request 覆蓋。外部工具修改後舊版本 request 會被拒絕；不宣稱能防止另一個不合作程序在極短檢查／rename 間隙惡意寫入。

測試包括中文、外部改檔、stale revision、越界與過大寫入保留舊檔。保存前 diff 只是顯示候選，最後 PUT 仍重新驗證 revision。
