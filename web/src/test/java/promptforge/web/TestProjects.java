package promptforge.web;

import java.nio.file.Files;
import java.nio.file.Path;

final class TestProjects {
    static Path create() {
        try {
            Path home = Files.createTempDirectory("promptforge-web-tests-");
            Path rules = Files.createDirectories(home.resolve("demo/.promptforge/rules"));
            Files.writeString(rules.resolve("payment.rule"), "id=payment.retry\nkeywords=付款,重試\ninstruction=保留冪等性 <script>alert(1)</script>\n");
            Path knowledge = Files.createDirectories(home.resolve("demo/.promptforge/knowledge/adr"));
            Files.writeString(knowledge.resolve("payment.md"), "# 付款 ADR\n\n先查詢交易狀態，不能重複扣款。");
            Path inbox = Files.createDirectories(home.resolve("demo/.promptforge/inbox/demo-skill/references"));
            Files.writeString(inbox.getParent().resolve("SKILL.md"), "---\nname: demo-skill\ndescription: 整理任務\n---\n\n# Instructions\n保留來源。\n");
            Files.writeString(inbox.resolve("policy.md"), "資料來自已登記專案。");
            Files.writeString(home.resolve("workbench-projects.json"), "[{\"id\":\"demo\",\"name\":\"Demo\",\"root\":\"demo\"}]");
            return home;
        } catch (java.io.IOException error) { throw new AssertionError(error); }
    }
    private TestProjects() { }
}
