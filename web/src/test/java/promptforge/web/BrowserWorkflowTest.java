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
        for (String name : List.of("PromptForge", "PromptForge-context", "ui-ux-pro-max")) {
            Path file = HOME.resolve(".agents/skills/" + name + "/SKILL.md"); Files.createDirectories(file.getParent());
            Files.writeString(file, "---\nname: " + name + "\ndescription: Local fixture\n---\n\n# Instructions\nRead input and report evidence.\n");
        }
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
            page.locator("#newSkill").click();
            assertThat(page.locator("#skillSelect")).isEnabled();
            page.locator("nav a[href='#verify']").click();
            page.locator("#skillSelect").selectOption("PromptForge-context");
            assertThat(page.locator("#verifyTitle")).hasText("測試 PromptForge-context");
            page.locator("nav a[href='#manage']").click();
            assertThat(page.locator("#newSkillForm")).not().isVisible();
            page.locator("#newSkill").click();
            page.locator("#skillSelect").selectOption("PromptForge");
            assertThat(page.locator("#editTitle")).hasText("PromptForge");
            assertThat(page.locator("#newSkillForm")).not().isVisible();
            page.locator("#newSkill").click(); page.locator("#newSkillId").fill("text-check"); page.locator("#createSkill").click();
            assertThat(page.locator("#skillSelect")).hasValue("text-check");
            assertThat(page.locator("#editTitle")).hasText("text-check");
            page.locator("#skillDescription").fill("檢查輸入文字的 Skill");
            assertThat(page.locator("#editor")).not().isVisible();
            page.locator("#manageNext").click();
            assertThat(page.locator("#editor")).containsText("直接寫 AI 要做的事");
            page.getByLabel("名稱", new Page.GetByLabelOptions().setExact(true)).fill("Run local checker");
            page.getByLabel("指示", new Page.GetByLabelOptions().setExact(true)).fill("Run the selected script and report its actual stdout.");
            page.locator("#resources > summary").click(); page.locator("#resourcePath").fill("scripts/check.js"); page.locator("#resourceText").fill("console.log('SCRIPT_OK');\n"); page.locator("#saveResource").click();
            assertThat(page.locator("#resourceStatus")).containsText("檔案已保存"); page.locator("#attachResource").click();
            context.clearCookies(); // Simulate an expired session or a local server restart, without reloading the draft.
            page.getByLabel("能力使用方法").fill("node .promptforge/resources/text-check/scripts/check.js");
            page.locator("[data-manage-pane='3']").click(); page.locator("#save").click(); assertThat(page.locator("#saveState")).hasText("已同步");
            assertThat(page.locator("#generated")).containsText("scripts/check.js");
            page.reload(); page.locator("#skillSelect").selectOption("text-check");
            assertThat(page.locator("#editTitle")).hasText("text-check");
            page.locator("[data-manage-pane='3']").click();
            assertThat(page.locator("#generated")).containsText("Run local checker");
            page.locator("[data-manage-pane='2']").click(); page.locator("#resources > summary").click();
            assertThat(page.locator("#resourceSelect")).containsText("scripts/check.js");
            page.locator("#resourceSelect").selectOption("scripts/check.js"); assertThat(page.locator("#resourceText")).hasValue("console.log('SCRIPT_OK');\n");
            page.locator("#addStep").click(); page.getByLabel("名稱", new Page.GetByLabelOptions().setExact(true)).fill("PRIVATE_UNSELECTED"); page.locator("[data-manage-pane='3']").click(); page.locator("#save").click();
            assertThat(page.locator("#saveState")).hasText("已同步"); assertThat(page.locator("#generated")).not().containsText("PRIVATE_UNSELECTED");
            Object result = page.evaluate("""
                async () => (await (await fetch('/mcp', {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({jsonrpc:'2.0',id:1,method:'tools/call',params:{name:'get_skill',arguments:{name:'text-check'}}})})).json()).result.content[0].text
                """);
            assertTrue(result.toString().contains("Run local checker")); assertFalse(result.toString().contains("PRIVATE_UNSELECTED"));
            assertFalse(result.toString().contains("Write a prompt with the original goal"));
            Path script = HOME.resolve(".promptforge/resources/text-check/scripts/check.js");
            Process run = new ProcessBuilder("node", script.toString()).redirectErrorStream(true).start();
            assertTrue(run.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)); assertEquals(0,run.exitValue()); assertEquals("SCRIPT_OK",new String(run.getInputStream().readAllBytes()).trim());
            for (String name : List.of("PromptForge-context", "ui-ux-pro-max")) {
                page.locator("#skillSelect").selectOption(name);
                assertThat(page.locator("#editTitle")).hasText(name);
                page.locator("#manageNext").click();
                assertThat(page.locator("#structuredEditor")).isVisible();
                assertThat(page.locator("#steps")).containsText("Instructions");
                page.locator("#addStep").click();
                page.getByLabel("名稱", new Page.GetByLabelOptions().setExact(true)).fill("Extra workflow step");
                page.getByLabel("指示", new Page.GetByLabelOptions().setExact(true)).fill("Edited through GUI.");
                page.getByLabel("Extra workflow step 提供給 Agent").check();
                page.locator("[data-manage-pane='3']").click(); page.locator("#save").click();
                assertThat(page.locator("#saveState")).hasText("已同步");
                String document = Files.readString(HOME.resolve(".agents/skills/" + name + "/SKILL.md"));
                assertTrue(document.contains("Read input and report evidence."));
                assertTrue(document.contains("Edited through GUI."));
                assertTrue(document.contains("Extra workflow step"));
            }
            String companion = Files.readString(HOME.resolve(".agents/skills/PromptForge-context/SKILL.md"));
            page.locator("#skillSelect").selectOption("PromptForge");
            assertThat(page.locator("#editTitle")).hasText("PromptForge");
            page.locator("#manageNext").click();
            assertThat(page.locator("#structuredEditor")).isVisible(); page.locator("[data-manage-pane='3']").click(); page.locator("#save").click();
            assertThat(page.locator("#saveState")).hasText("已同步");
            assertEquals(companion, Files.readString(HOME.resolve(".agents/skills/PromptForge-context/SKILL.md")));
            page.locator("#skillSelect").selectOption("text-check");
            assertThat(page.locator("#editTitle")).hasText("text-check"); page.locator("#manageNext").click();
            assertThat(page.locator("#structuredEditor")).isVisible();
            page.locator("nav a[href='#agent']").click();
            assertThat(page.locator("#mcpState")).hasText("本地服務可達");
            assertThat(page.locator("#skillBar")).not().isVisible();
            assertThat(page.locator("#connectionLibrary")).containsText("text-check");
            assertThat(page.locator("#connectionLibrary")).containsText("PromptForge-context");
            assertThat(page.locator("#agent")).containsText("一次連接，使用整個 Skill 庫");
            page.locator("#copyLibraryRead").click();
            assertTrue(page.evaluate("navigator.clipboard.readText()").toString().contains("list_skills"));
            Path added = HOME.resolve(".agents/skills/external-new/SKILL.md"); Files.createDirectories(added.getParent());
            Files.writeString(added,"---\nname: external-new\ndescription: Added outside the GUI\n---\n\n# External instructions\nRead new data.\n");
            page.locator("nav a[href='#home']").click();
            assertThat(page.locator("#skillCards")).containsText("Added outside the GUI");
            page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON, new Page.GetByRoleOptions().setName("使用 external-new").setExact(true)).click();
            assertThat(page.locator("#skillPurpose")).hasText("Added outside the GUI");
            assertThat(page.locator("#activeSteps")).containsText("External instructions");
            page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON, new Page.GetByRoleOptions().setName("使用 text-check").setExact(true)).click();
            assertThat(page.locator("#useTitle")).hasText("使用 text-check");

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
            page.locator("nav a[href='#manage']").click(); page.locator("[data-manage-pane='2']").click(); page.locator(".step-open").first().click(); page.getByLabel("名稱",new Page.GetByLabelOptions().setExact(true)).fill("UNSAVED_EDIT");
            page.locator("nav a[href='#home']").click(); assertThat(page.locator("#activeSteps")).not().containsText("UNSAVED_EDIT");
            page.goBack(); assertThat(page.locator("#manage")).isVisible(); page.goForward(); assertThat(page.locator("#home")).isVisible();
            for (int width : new int[]{375,768,1024,1440}) {
                page.setViewportSize(width,900);
                for (String route : List.of("home", "agent", "verify", "manage")) {
                    page.locator("nav a[href='#" + route + "']").click();
                    assertThat(page.locator("#" + route)).isVisible();
                    if (route.equals("manage")) {
                        for (int pane : new int[]{1,2,3}) {
                            page.locator("[data-manage-pane='" + pane + "']").click();
                            assertThat(page.locator("[data-manage-panel='" + pane + "']")).isVisible();
                            assertEquals(1,((Number)page.evaluate("document.documentElement.scrollWidth <= innerWidth ? 1 : 0")).intValue(),"manage pane " + pane + " overflow at " + width);
                        }
                    }
                    assertEquals(1,((Number)page.evaluate("document.documentElement.scrollWidth <= innerWidth ? 1 : 0")).intValue(),route + " overflow at " + width);
                }
            }
            page.locator("[data-manage-pane='2']").click();
            page.screenshot(new Page.ScreenshotOptions().setFullPage(true).setPath(Path.of("../build/manage-steps-desktop.png")));
            page.setViewportSize(375,900);
            page.screenshot(new Page.ScreenshotOptions().setFullPage(true).setPath(Path.of("../build/manage-steps-mobile.png")));
            page.locator("nav a[href='#home']").click();
            page.setViewportSize(1440,900);
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of(System.getProperty("java.io.tmpdir")).getParent().resolve("uiux-home-desktop.png")));
            page.setViewportSize(390,844); assertTrue(((Number)page.evaluate("document.documentElement.scrollWidth <= innerWidth ? 1 : 0")).intValue() == 1);
            assertTrue(errors.isEmpty(), errors.toString());
            page.screenshot(new Page.ScreenshotOptions().setPath(Path.of(System.getProperty("java.io.tmpdir")).getParent().resolve("skill-library-mobile.png")));
        }
    }
}
