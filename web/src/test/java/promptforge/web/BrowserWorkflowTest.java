package promptforge.web;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BrowserWorkflowTest {
    private static final Path HOME = TestProjects.create();
    private static final List<String> blockedRequests = new CopyOnWriteArrayList<>();
    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;
    private Page page;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("promptforge.home", HOME::toString);
    }

    @LocalServerPort
    int port;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create(new Playwright.CreateOptions().setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
        String channel = setting("promptforge.browser.channel", "PROMPTFORGE_BROWSER_CHANNEL", "chromium");
        boolean headless = Boolean.parseBoolean(setting("promptforge.browser.headless", "PROMPTFORGE_BROWSER_HEADLESS", "true"));
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(headless);
        if (!channel.isBlank() && !"chromium".equalsIgnoreCase(channel)) options.setChannel(channel);
        browser = playwright.chromium().launch(options);
    }

    private static String setting(String property, String environment, String fallback) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) value = System.getenv(environment);
        return value == null || value.isBlank() ? fallback : value;
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @AfterEach
    void closePage() {
        if (context != null) context.close();
    }

    @Test
    void promptAndSkillFlowsWorkInTheRealBrowserWithoutExternalRequests() throws Exception {
        blockedRequests.clear();
        context = browser.newContext();
        page = context.newPage();
        page.route("**/*", route -> routeLocalOnly(route));
        page.onDialog(dialog -> dialog.accept());
        page.navigate("http://127.0.0.1:" + port + "/");
        page.locator("#connectionState.online").waitFor();
        page.locator("#projectSelect").selectOption("demo");
        assertThat(page.locator("#projectName")).hasText("Demo");
        page.locator("#knowledgeQuery").fill("付款");
        page.locator("#searchKnowledgeButton").click();
        assertThat(page.locator("#knowledgeSearchResults")).containsText("付款 ADR");
        assertThat(page.locator("#knowledgeSearchResults")).containsText(".promptforge/knowledge/adr/payment.md");
        page.locator("#knowledgeSearchResults .knowledge-hit").click();
        assertTrue(page.locator("#knowledgeList input").first().isChecked());

        page.locator("#promptName").fill("支付流程 🙂");
        page.locator("#promptId").fill("browser-flow");
        page.locator("#taskInput").fill("支付失敗時先查詢狀態，再安全重試 🙂");
        page.locator("#variablesInput").fill("{\"audience\":\"工程團隊\",\"tone\":\"清楚\"}");
        page.locator("#modeSelect").selectOption("template");
        page.locator("#rulesList input[name='ruleMode'][value='manual']").check();
        page.locator("#rulesList .rule-options input").first().check();
        page.locator("#knowledgeList input").first().check();
        var blocks = page.locator("#blocksList .block-row");
        blocks.nth(0).locator(".block-name").fill("目標");
        blocks.nth(0).locator("textarea").fill("處理付款結果，不可重複扣款");
        page.locator("#addBlockButton").click();
        blocks = page.locator("#blocksList .block-row");
        blocks.nth(2).locator(".block-name").fill("回覆格式");
        blocks.nth(2).locator("textarea").fill("以繁體中文列出檢查步驟");
        page.locator("#refreshPreviewButton").click();
        assertThat(page.locator("#previewOutput")).containsText("支付失敗時先查詢狀態，再安全重試 🙂");
        Files.createDirectories(Path.of("build"));
        page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("build/browser-prompt.png").toAbsolutePath()));

        page.locator("#savePromptButton").click();
        page.locator("#diffDialog").waitFor();
        page.locator("#confirmSaveButton").click();
        page.locator("#diffDialog").waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));
        assertThat(page.locator("#toast")).containsText("提示詞已儲存");

        page.reload();
        page.locator("#connectionState.online").waitFor();
        page.locator("#promptSelect").selectOption("browser-flow");
        assertThat(page.locator("#promptName")).hasValue("支付流程 🙂");
        assertThat(page.locator("#promptId")).hasValue("browser-flow");
        assertThat(page.locator("#taskInput")).hasValue("支付失敗時先查詢狀態，再安全重試 🙂");
        assertThat(page.locator("#modeSelect")).hasValue("template");
        assertTrue(page.locator("#variablesInput").inputValue().contains("工程團隊"));
        assertEquals(3, page.locator("#blocksList .block-row").count());
        assertThat(page.locator("#previewOutput")).containsText("支付失敗時先查詢狀態，再安全重試 🙂");

        page.locator("#skillsView").waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
        page.locator(".side-tab[data-tab='skills']").click();
        page.locator("#importSkillButton").click();
        page.locator("#importId").fill("browser-skill");
        page.locator("#importSource").fill("demo-skill");
        page.locator("#confirmImportButton").click();
        assertThat(page.locator("#skillState")).hasText("IMPORTED");

        page.locator("#skillActionButton").click();
        assertThat(page.locator("#skillState")).hasText("REVIEWED");
        page.locator("#skillActionButton").click();
        assertThat(page.locator("#skillState")).hasText("APPROVED");
        page.screenshot(new Page.ScreenshotOptions().setPath(Path.of("build/browser-skill.png").toAbsolutePath()));

        page.locator("#exportSkillButton").click();
        page.locator("#exportTarget").fill("browser-copy");
        page.locator("#confirmExportButton").click();
        assertThat(page.locator("#toast")).containsText(".promptforge/exports/browser-copy");
        assertTrue(Files.exists(HOME.resolve("demo/.promptforge/exports/browser-copy/SKILL.md")));
        assertTrue(blockedRequests.isEmpty(), "Unexpected external requests: " + blockedRequests);
    }

    private void routeLocalOnly(Route route) {
        URI uri = URI.create(route.request().url());
        if ("http".equals(uri.getScheme()) && "127.0.0.1".equals(uri.getHost()) && uri.getPort() == port) {
            route.resume();
        } else {
            blockedRequests.add(route.request().url());
            route.abort();
        }
    }
}
