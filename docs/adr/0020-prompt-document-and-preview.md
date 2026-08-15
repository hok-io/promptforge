# ADR 0020：結構化 Prompt 與普通 Java 預覽

PromptDocument 用 Java record 表達不可變資料；包含自訂有序區塊、variables 與明確選擇。frame 模式保留短任務，template 模式依序加入非空區塊，沒有必填角色。變數只作單次文字替換，缺值回可理解的 422 錯誤，不執行或遞迴展開。

普通 Java PromptPreviewService 呼叫共享 FrameService，CLI 與 Web 都直接用同一核心，不透過 shell。測試先驗證順序、缺值、字面替換及 auto／empty 的差別，再測與 CLI 的同資料一致性。Domain 不知道 HTTP；WorkbenchException 保存應用錯誤分類，HTTP adapter 才映射狀態碼。

代價：結構化編輯要由人選合適區塊，沒有語義補全。預览仍是候選 prompt，不代表 agent 實際上下文。
