# ADR 0017：任務文字經 UTF-8 stdin 傳遞

狀態：Accepted。實際觀察日 2026-09-26，

真人流程重測 F05 發現本機 Windows Java 15 的 native command-line 把「进」轉成「?」。設定 file.encoding／sun.jnu.encoding 仍無法保留。Java 程式收到參數前已損壞，Main.run 的單元測試不能重現此邊界。

frame.ps1 改用既有 --stdin，以 UTF-8 編碼傳任務，選擇 ID／來源仍作為參數。保存及還原 PowerShell OutputEncoding。Main 的 stdin 契約會移除結尾空白，不保證原有尾端空白；正文必須完整保留。smoke 增加非本機碼頁字元與 emoji。

用途 runner 的 task 模式使用相同 UTF-8 stdin，以驗證實際可用流程。直接 Java 15 --task／--query 的 Unicode 參數仍可能受本機碼頁限制，不能聲稱 PrintStream UTF-8 已修好输入。此步只修 task wrapper；query 路徑限制在 README 明示，不在這個 patch 增加新查詢模式。
