package promptforge.web;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.nio.file.*;
import java.util.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BrowserWorkflowTest {
    static final Path HOME = TestProjects.create();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) { registry.add("promptforge.home", HOME::toString); }
    @LocalServerPort int port;
    @Test void createSkillAddScriptSaveReloadAndReadThroughMcp() throws Exception {
        try (Playwright playwright = Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true).setChannel(System.getenv().getOrDefault("PROMPTFORGE_BROWSER_CHANNEL", "chromium")));
             BrowserContext context = browser.newContext()) {
            context.grantPermissions(List.of("clipboard-read", "clipboard-write"));
            Page page = context.newPage();
            List<String> errors = new ArrayList<>(); page.onPageError(errors::add);
            page.onDialog(Dialog::accept);
            page.navigate("http://127.0.0.1:" + port + "/");
            assertThat(page.locator("#skillSelect")).hasValue("PromptForge");
            page.locator("nav a[href='#manage']").click();
            page.locator("#newSkill").click(); page.locator("#newSkillId").fill("text-check"); page.locator("#createSkill").click();
            assertThat(page.locator("#skillSelect")).hasValue("text-check");
            page.locator("#skillDescription").fill("檢查輸入文字的 Skill");
            page.getByLabel("名稱", new Page.GetByLabelOptions().setExact(true)).fill("Run local checker");
            page.getByLabel("指示", new Page.GetByLabelOptions().setExact(true)).fill("Run the selected script and report its actual stdout.");
            page.locator("#resources > summary").click(); page.locator("#resourcePath").fill("scripts/check.js"); page.locator("#resourceText").fill("console.log('SCRIPT_OK');\n"); page.locator("#saveResource").click();
            assertThat(page.locator("#resourceStatus")).containsText("檔案已保存"); page.locator("#attachResource").click();
            page.getByLabel("能力使用方法").fill("node .promptforge/resources/text-check/scripts/check.js");
            page.locator("#save").click(); assertThat(page.locator("#saveState")).hasText("已同步");
            page.locator("#output > summary").click(); assertThat(page.locator("#generated")).containsText("scripts/check.js");
            page.reload(); page.locator("#skillSelect").selectOption("text-check");
            page.locator("#output > summary").click(); assertThat(page.locator("#generated")).containsText("Run local checker");
            page.locator("#resources > summary").click();
            assertThat(page.locator("#resourceSelect")).containsText("scripts/check.js");
            page.locator("#resourceSelect").selectOption("scripts/check.js"); assertThat(page.locator("#resourceText")).hasValue("console.log('SCRIPT_OK');\n");
            page.locator("#addStep").click(); page.getByLabel("名稱", new Page.GetByLabelOptions().setExact(true)).fill("PRIVATE_UNSELECTED"); page.locator("#save").click();
            assertThat(page.locator("#saveState")).hasText("已同步"); assertThat(page.locator("#generated")).not().containsText("PRIVATE_UNSELECTED");
            Object result = page.evaluate("""
                async () => (await (await fetch('/mcp', {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({jsonrpc:'2.0',id:1,method:'tools/call',params:{name:'get_skill',arguments:{name:'text-check'}}})})).json()).result.content[0].text
                """);
            assertTrue(result.toString().contains("Run local checker")); assertFalse(result.toString().contains("PRIVATE_UNSELECTED"));
            assertFalse(result.toString().contains("Write a prompt with the original goal"));
            Path script = HOME.resolve(".promptforge/resources/text-check/scripts/check.js");
            Process run = new ProcessBuilder("node", script.toString()).redirectErrorStream(true).start();
            assertTrue(run.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)); assertEquals(0,run.exitValue()); assertEquals("SCRIPT_OK",new String(run.getInputStream().readAllBytes()).trim());
            page.locator("nav a[href='#agent']").click();
            assertThat(page.locator("#mcpState")).hasText("本地服務可達");
            page.locator("nav a[href='#verify']").click(); page.locator("#startVerification").click();
            assertThat(page.locator("#verificationState")).hasText("等候 AI 讀取");
            String prompt = page.locator("#agentPrompt").inputValue();
            var match = java.util.regex.Pattern.compile("verificationCode=\"([^\"]+)\"").matcher(prompt); assertTrue(match.find());
            String code = match.group(1);
            page.evaluate("""
              async code => { await fetch('/mcp',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({jsonrpc:'2.0',id:7,method:'tools/call',params:{name:'get_skill',arguments:{name:'text-check',verificationCode:code}}})}); }
              """,code);
            page.locator("#refreshVerification").click(); assertThat(page.locator("#verificationState")).hasText("讀取驗證通過");
            page.locator("nav a[href='#home']").click(); assertThat(page.locator("#acceptanceChecks")).not().isVisible();
            assertThat(page.locator("#taskInput")).isVisible();
            page.locator("#taskInput").fill("Check sample input"); page.locator("#copyTask").click();
            assertThat(page.locator("#taskFeedback")).containsText("已複製");
            String copied = page.evaluate("navigator.clipboard.readText()").toString(); assertTrue(copied.contains("text-check")); assertTrue(copied.contains("Check sample input"));
            page.locator("nav a[href='#manage']").click(); page.locator(".step-open").first().click(); page.getByLabel("名稱",new Page.GetByLabelOptions().setExact(true)).fill("UNSAVED_EDIT");
            page.locator("nav a[href='#home']").click(); assertThat(page.locator("#activeSteps")).not().containsText("UNSAVED_EDIT");
            page.goBack(); assertThat(page.locator("#manage")).isVisible(); page.goForward(); assertThat(page.locator("#home")).isVisible();
            for (int width : new int[]{375,768,1024,1440}) { page.setViewportSize(width,900); assertEquals(1,((Number)page.evaluate("document.documentElement.scrollWidth <= innerWidth ? 1 : 0")).intValue(),"overflow at " + width); }
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of(System.getProperty("java.io.tmpdir")).getParent().resolve("uiux-home-desktop.png")));
            page.setViewportSize(390,844); assertTrue(((Number)page.evaluate("document.documentElement.scrollWidth <= innerWidth ? 1 : 0")).intValue() == 1);
            assertTrue(errors.isEmpty(), errors.toString());
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of(System.getProperty("java.io.tmpdir")).getParent().resolve("skill-library-mobile.png")));
        }
    }
}
