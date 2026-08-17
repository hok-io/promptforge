# ADR 0024：本地 Spring MVC 接口

Web 綁 127.0.0.1、使用已登記 project ID；Controller 呼叫普通 Java services。Spring Security 保留 CSRF，GET /api/csrf 提供 token，mutating fetch 必須帶 header。拒絕未知 Host／跨來源寫入，HTTP body 上限 64 KiB（包括 chunked），資料作純文字顯示，沒有 CDN。

MockMvc 測每種路由與錯誤：400 壞 JSON、404 未登記專案／文件、409 stale revision、422 domain 錯誤、413 過大 body、403 CSRF／Host。另有真實 localhost 埠測試，不能只用mock假稱 server可用。

所有 skill 狀態／保存／匯出都需要 revision；Preview 不保存資料。服務沒有 HTTP 或 shell 呼叫，核心與 CLI 保持分層。
