package promptforge.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.Path;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WebContractTest {
    private static final Path HOME = TestProjects.create();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) { registry.add("promptforge.home", HOME::toString); }
    @Autowired MockMvc mvc;
    static final String DOCUMENT = """
        {"id":"draft","name":"付款任務","task":"支付失敗再試 🙂","blocks":[],"variables":{},
         "ruleIds":["payment.retry"],"excludedRuleIds":[],"knowledgeSources":[".promptforge/knowledge/adr/payment.md"],"mode":"frame"}
        """;

    @Test void staticAssetsAndRegisteredProjects() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/app.js")).andExpect(status().isOk());
        mvc.perform(get("/api/projects")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value("demo"));
        mvc.perform(get("/api/projects/unknown/context")).andExpect(status().isNotFound());
        mvc.perform(get("/api/projects/demo/context")).andExpect(jsonPath("$.rules[0].source").value(".promptforge/rules/payment.rule"));
    }
    @Test void previewIsSharedUnicodeAndRequiresCsrf() throws Exception {
        mvc.perform(post("/api/projects/demo/preview").contentType("application/json").content(DOCUMENT)).andExpect(status().isForbidden());
        mvc.perform(post("/api/projects/demo/preview").with(csrf()).contentType("application/json").content(DOCUMENT))
                .andExpect(status().isOk()).andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("支付失敗再試 🙂")))
                .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("先查詢交易狀態")));
        mvc.perform(post("/api/projects/demo/preview").with(csrf()).contentType("application/json").content(DOCUMENT.replace("payment.retry", "missing.id"))).andExpect(status().isUnprocessableEntity());
    }
    @Test void diffDoesNotSaveAndStaleWriteReturnsConflict() throws Exception {
        String request = "{\"revision\":\"new\",\"document\":" + DOCUMENT + "}";
        mvc.perform(post("/api/projects/demo/prompts/draft/diff").with(csrf()).contentType("application/json").content(request)).andExpect(status().isOk()).andExpect(jsonPath("$.before").value(""));
        mvc.perform(get("/api/projects/demo/prompts/draft")).andExpect(status().isNotFound());
        mvc.perform(put("/api/projects/demo/prompts/draft").with(csrf()).contentType("application/json").content(request)).andExpect(status().isOk());
        mvc.perform(put("/api/projects/demo/prompts/draft").with(csrf()).contentType("application/json").content(request)).andExpect(status().isConflict());
    }
    @Test void localBoundaryAndRequestSizeAreEnforced() throws Exception {
        mvc.perform(get("/api/projects").header("Host", "evil.example")).andExpect(status().isForbidden());
        mvc.perform(post("/api/projects/demo/preview").with(csrf()).contentType("application/json").content("x".repeat(70_000))).andExpect(status().isPayloadTooLarge());
    }
    @Test void skillImportReviewExportAndStaleSaveUseSameHttpContracts() throws Exception {
        String imported = mvc.perform(post("/api/projects/demo/skills/import").with(csrf()).contentType("application/json")
                .content("{\"id\":\"http-skill\",\"source\":\"demo-skill\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("IMPORTED")).andReturn().getResponse().getContentAsString();
        String revision = new com.fasterxml.jackson.databind.ObjectMapper().readTree(imported).path("revision").asText();
        String payload = "{\"revision\":\"" + revision + "\",\"action\":\"approve\"}";
        mvc.perform(post("/api/projects/demo/skills/http-skill/state").with(csrf()).contentType("application/json").content(payload)).andExpect(status().isConflict());
        mvc.perform(post("/api/projects/demo/skills/http-skill/state").with(csrf()).contentType("application/json").content(payload.replace("approve", "review"))).andExpect(jsonPath("$.state").value("REVIEWED"));
        mvc.perform(post("/api/projects/demo/skills/http-skill/state").with(csrf()).contentType("application/json").content(payload)).andExpect(jsonPath("$.state").value("APPROVED"));
        String export = "{\"revision\":\"" + revision + "\",\"target\":\"http-copy\"}";
        mvc.perform(post("/api/projects/demo/skills/http-skill/export").with(csrf()).contentType("application/json").content(export)).andExpect(status().isOk());
        mvc.perform(post("/api/projects/demo/skills/http-skill/export").with(csrf()).contentType("application/json").content(export)).andExpect(status().isConflict());
        mvc.perform(post("/api/projects/demo/skills/http-skill/evaluations").with(csrf()).contentType("application/json")
                .content("{\"id\":\"opinion\",\"subjectHash\":\"" + revision + "\",\"kind\":\"MODEL_REVIEW\",\"model\":null,\"evaluator\":\"human-import\",\"timestamp\":\"2026-09-26T00:00:00Z\",\"rubricVersion\":\"v1\",\"findings\":[\"保留來源\"],\"evidence\":[]}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/projects/demo/skills/http-skill")).andExpect(jsonPath("$.evaluations[0].stale").value(false));
        mvc.perform(post("/api/projects/demo/skills/import").with(csrf()).contentType("application/json").content("{\"id\":\"escape\",\"source\":\"../outside\"}")).andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/projects/demo/preview").with(csrf()).contentType("application/json").content("not json")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/projects/demo/preview").with(csrf()).header("Origin", "https://evil.example").contentType("application/json").content(DOCUMENT)).andExpect(status().isForbidden());
    }
}
