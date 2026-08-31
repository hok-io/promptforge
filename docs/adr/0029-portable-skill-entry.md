# 0029：可設定的 Skill 入口

狀態：Accepted。取代 0007 的固定安裝路徑。

公開原始碼可被放在任意目錄。Skill 優先讀 PROMPTFORGE_HOME；若仍在 repository 內，可由自身位置找到根目錄。獨立複製安裝的 Skill 在沒有設定且找不到 CLI 時，要求提供 checkout 位置，不猜測某部電腦的磁碟路徑。

這只改變尋找 CLI 的方式。任務仍透過既有 frame.ps1，目標專案是獨立的 -Project 參數。檔案契約檢查拒絕硬編碼磁碟路徑；實際 wrapper smoke 驗證移動後仍能執行。
