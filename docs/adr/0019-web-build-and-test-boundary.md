# ADR 0019：Web 建置與測試邊界

狀態 Accepted。使用者要求先實作 Web，review 改為交付後逐 commit 學 Java；不再要求填完原 review 才實作。沿用 9 月 16 日 Git 展示日期，實作實測日期另記。

Java 21、Spring Boot 3.5.16、Maven 3.9.11。core 保留純 Java 和原 CLI；workbench 放普通 Java domain／application service 及檔案 adapter；web 只接 Spring MVC、HTTP、CSRF 和本地 assets。沒有付費模型、資料庫或遠端 agent。

為保持學習負擔，採 Spring MVC 配本地普通 HTML／CSS／JavaScript；這取代原提議 Thymeleaf，不引入 React。畫面用 JSON 呼叫 Java service，能清楚觀察 Controller → Service → Repository。所有內容安全顯示成文字。

每功能先 JUnit 契約，檔案 temp-directory 整合測試，HTTP MockMvc／真實埠測試，最後 browser 操作與離線啟動。原 main 測試先用 JUnit dynamic tests 接入，不把它們假稱全部已改寫成細粒度 JUnit；後續學習可逐類轉換。遠端 CI 未跑不宣稱通過。

版本來源：[Spring 3.5 系統需求](https://docs.spring.io/spring-boot/3.5/system-requirements.html)、[Apache Maven Wrapper](https://maven.apache.org/tools/wrapper/)。JDK／Maven 下载在專案 .tools，校驗官方 checksum，不改全域系統。
