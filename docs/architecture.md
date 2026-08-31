# 架構

| 模組 | 責任 | 主要入口 |
|---|---|---|
| core | 規則選擇、BM25、frame 組成、CLI | Main、FrameService、KnowledgeQueryService |
| workbench | Prompt／Skill 操作、版本、檔案儲存 | PromptService、SkillService、ProjectContextService |
| web | Spring 組裝、HTTP、CSRF、本地網頁 | WorkbenchConfiguration、WorkbenchController |

依賴方向為 web → workbench → core。core、workbench 不 import Spring；workbench 使用 Jackson 讀寫 JSON／YAML。根 POM 使用 Spring Boot 的依賴管理，不代表核心的執行需要 Spring。

## 兩條操作路徑

預覽：HTTP → PromptService → PromptPreviewService → PromptTemplateCompiler → FrameService。CLI 直接使用 FrameService，因此規則與 frame 行為共用。

保存：HTTP → PromptService → PromptRepository → JsonPromptRepository → RevisionFileStore → WorkspaceFiles。Repository 定義契約，JSON adapter 負責格式，file store 負責 revision 及替換，WorkspaceFiles 負責路徑邊界。

## Skill 的責任

SkillService 協調操作及批准流程；SkillBundleStore 讀寫內容、metadata 和 hash；SkillTransfer 管匯入／匯出 staging；SkillEvaluationService 管外部評估證據。後三者是 package-private 的內部合作類別，不作公開 API。評估與批准分開，批准綁定 bundle hash。

FailureKind 是應用錯誤種類；HttpErrors 才處理 HTTP 狀態。Controller 保留 request／response 轉換，專案來源讀取交給 ProjectContextService。

## 儲存及限制

以專案內 UTF-8 檔案作來源，沒有資料庫或背景 agent。每次操作重新讀取，因此規則更新不用重建；大量資料下沒有索引快取的性能保證。單檔原子替換及 JVM 鎖不等於跨檔、跨程序交易。

CLI 原始碼保留於根 `src/`，core Maven module 指向該目錄，維持原有 PowerShell 建置方式。Web 使用 Java 21；獨立 CLI 腳本以 Java 15 release 編譯。安全及行為邊界見 [功能規格](specs/behavior.md)，取捨見 [ADR](adr/README.md)。
