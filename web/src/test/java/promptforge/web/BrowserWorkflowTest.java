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
    private static final List<String> unexpectedAlerts = new CopyOnWriteArrayList<>();
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
        blockedRequests.clear(); unexpectedAlerts.clear();
        context = browser.newContext();
        page = context.newPage();
        page.route("**/*", route -> routeLocalOnly(route));
        page.onDialog(dialog -> { if ("alert".equals(dialog.type())) { unexpectedAlerts.add(dialog.message()); dialog.dismiss(); } else dialog.accept(); });
        page.navigate("http://127.0.0.1:" + port + "/");
        page.locator("#connectionState.online").waitFor();
        page.locator("#projectSelect").selectOption("demo");
        assertThat(page.locator("#projectName")).hasText("Demo");
        assertThat(page.locator("#rulesList")).containsText("<script>alert(1)</script>");
        page.locator("#knowledgeQuery").fill("付款");
        page.locator("#searchKnowledgeButton").click();
        assertThat(page.locator("#knowledgeSearchResults")).containsText("付款 ADR");
        assertThat(page.locator("#knowledgeSearchResults")).containsText(".promptforge/knowledge/adr/payment.md");
        page.locator("#knowledgeSearchResults .knowledge-hit").click();
        assertTrue(page.locator("#knowledgeList input").first().isChecked());

        page.locator("#promptName").fill("支付流程 🙂");
        page.locator("#promptId").fill("browser-flow");
        page.locator("#taskInput").fill("請由${audience}在付款失敗時先查詢狀態，再安全重試 🙂");
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
        blocks.nth(2).locator(".block-actions button[aria-label='上移區塊']").click();
        blocks = page.locator("#blocksList .block-row");
        assertEquals("回覆格式", blocks.nth(1).locator(".block-name").inputValue());
        page.locator("#refreshPreviewButton").click();
        assertThat(page.locator("#previewOutput")).containsText("工程團隊在付款失敗時先查詢狀態，再安全重試 🙂");
        page.locator("#rulesList .rule-options input").first().scrollIntoViewIfNeeded();
        assertThat(page.locator("#rulesList .rule-options input").first()).isInViewport();
        screenshotAtRootBuild("browser-prompt.png");

        saveAndConfirmPrompt();
        assertThat(page.locator("#toast")).containsText("提示詞已儲存");

        page.reload();
        page.locator("#connectionState.online").waitFor();
        page.locator("#promptSelect").selectOption("browser-flow");
        assertThat(page.locator("#promptName")).hasValue("支付流程 🙂");
        assertThat(page.locator("#promptId")).hasValue("browser-flow");
        assertThat(page.locator("#taskInput")).hasValue("請由${audience}在付款失敗時先查詢狀態，再安全重試 🙂");
        assertThat(page.locator("#modeSelect")).hasValue("template");
        assertTrue(page.locator("#variablesInput").inputValue().contains("工程團隊"));
        assertEquals(3, page.locator("#blocksList .block-row").count());
        assertThat(page.locator("#blocksList .block-row .block-name").nth(1)).hasValue("回覆格式");
        assertThat(page.locator("#previewOutput")).containsText("工程團隊在付款失敗時先查詢狀態，再安全重試 🙂");

        page.locator(".side-tab[data-tab='skills']").click();
        page.locator("#importSkillButton").click();
        page.locator("#importId").fill("browser-skill");
        page.locator("#importSource").fill("demo-skill");
        page.locator("#confirmImportButton").click();
        assertThat(page.locator("#skillState")).hasText("IMPORTED");

        page.locator("#skillRaw").fill(page.locator("#skillRaw").inputValue() + "\n\n## Local edit\nKeep the source context.");
        page.locator("#skillActionButton").click();
        assertThat(page.locator("#toast")).containsText("先儲存技能編輯");
        assertThat(page.locator("#skillState")).hasText("IMPORTED");
        page.locator("#exportSkillButton").click();
        page.locator("#exportTarget").fill("too-early");
        page.locator("#confirmExportButton").click();
        assertThat(page.locator("#toast")).containsText("先儲存技能編輯");
        page.locator("#exportDialog .dialog-actions button[value='cancel']").click();

        page.locator("#saveSkillButton").click();
        page.locator("#diffDialog").waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE));
        page.locator("#confirmSaveButton").click();
        page.locator("#diffDialog").waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));
        assertThat(page.locator("#skillState")).hasText("IMPORTED");
        page.locator("#exportSkillButton").click();
        page.locator("#exportTarget").fill("too-early");
        page.locator("#confirmExportButton").click();
        assertThat(page.locator("#toast")).containsText("請先核准");
        page.locator("#exportDialog .dialog-actions button[value='cancel']").click();

        page.locator("#skillActionButton").click();
        assertThat(page.locator("#skillState")).hasText("REVIEWED");
        page.locator("#skillActionButton").click();
        assertThat(page.locator("#skillState")).hasText("APPROVED");
        screenshotAtRootBuild("browser-skill.png");

        page.locator("#exportSkillButton").click();
        page.locator("#exportTarget").fill("browser-copy");
        page.locator("#confirmExportButton").click();
        assertThat(page.locator("#toast")).containsText(".promptforge/exports/browser-copy");
        assertTrue(Files.exists(HOME.resolve("demo/.promptforge/exports/browser-copy/SKILL.md")));
        assertTrue(blockedRequests.isEmpty(), "Unexpected external requests: " + blockedRequests);
        assertTrue(unexpectedAlerts.isEmpty(), "Unexpected JavaScript alert dialogs: " + unexpectedAlerts);
        page.setViewportSize(390, 844);
        assertThat(page.locator("#knowledgeQuery")).isVisible();
        assertThat(page.locator("#rulesList .rule-options input").first()).isVisible();
    }

    @Test
    void missingTemplateVariableIsShownInPreview() {
        openProject();
        page.locator("#modeSelect").selectOption("template");
        page.locator("#taskInput").fill("請回覆 ${missingValue}");
        page.locator("#refreshPreviewButton").click();
        assertThat(page.locator("#previewOutput")).containsText("缺少變數：missingValue");
        assertTrue(blockedRequests.isEmpty(), "Unexpected external requests: " + blockedRequests);
    }

    @Test
    void templateWithOnlyBlocksCanBeSavedAndReloaded() {
        openProject();
        page.locator("#modeSelect").selectOption("template");
        page.locator("#promptId").fill("blocks-only");
        page.locator("#promptName").fill("只有區塊的模板");
        page.locator("#blocksList .block-row textarea").first().fill("根據專案規則整理任務。");
        page.locator("#refreshPreviewButton").click();
        assertThat(page.locator("#previewOutput")).containsText("根據專案規則整理任務。");
        saveAndConfirmPrompt();
        page.reload();
        page.locator("#connectionState.online").waitFor();
        page.locator("#promptSelect").selectOption("blocks-only");
        assertThat(page.locator("#taskInput")).hasValue("");
        assertThat(page.locator("#previewOutput")).containsText("根據專案規則整理任務。");
    }

    @Test
    void stalePromptRevisionIsRejectedWithoutOverwritingTheNewerVersion() {
        openProject();
        page.locator("#promptName").fill("版本衝突保護");
        page.locator("#promptId").fill("stale-ui");
        page.locator("#taskInput").fill("目前版本");
        page.locator("#variablesInput").fill("{}");
        saveAndConfirmPrompt();
        page.locator("#taskInput").fill("本機草稿的變更");

        Object result = page.evaluate("""
                async () => {
                  const current = await (await fetch('/api/projects/demo/prompts/stale-ui')).json();
                  const csrf = await (await fetch('/api/csrf')).json();
                  current.document.task = '另一個編輯者已保存';
                  const response = await fetch('/api/projects/demo/prompts/stale-ui', {
                    method: 'PUT',
                    headers: {'Content-Type': 'application/json', [csrf.headerName]: csrf.token},
                    body: JSON.stringify({revision: current.revision, document: current.document})
                  });
                  return response.status;
                }
                """);
        assertEquals(200, ((Number) result).intValue());

        page.locator("#savePromptButton").click();
        assertThat(page.locator("#toast")).containsText("409");
        assertTrue(!page.locator("#diffDialog").isVisible(), "A stale diff should not show a save confirmation.");
        page.locator("#promptSelect").selectOption("");
        page.locator("#promptSelect").selectOption("stale-ui");
        assertThat(page.locator("#taskInput")).hasValue("另一個編輯者已保存");
        assertTrue(blockedRequests.isEmpty(), "Unexpected external requests: " + blockedRequests);
    }

    private void openProject() {
        blockedRequests.clear(); unexpectedAlerts.clear();
        context = browser.newContext(); page = context.newPage();
        page.route("**/*", route -> routeLocalOnly(route));
        page.onDialog(dialog -> { if ("alert".equals(dialog.type())) { unexpectedAlerts.add(dialog.message()); dialog.dismiss(); } else dialog.accept(); });
        page.navigate("http://127.0.0.1:" + port + "/");
        page.locator("#connectionState.online").waitFor();
    }

    private void saveAndConfirmPrompt() {
        page.locator("#savePromptButton").click();
        page.locator("#diffDialog").waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE));
        page.locator("#confirmSaveButton").click();
        page.locator("#diffDialog").waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));
    }

    private void screenshotAtRootBuild(String filename) throws Exception {
        Path rootBuild = Path.of(System.getProperty("java.io.tmpdir")).getParent();
        Files.createDirectories(rootBuild);
        page.screenshot(new Page.ScreenshotOptions().setPath(rootBuild.resolve(filename)));
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
