package promptforge.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import promptforge.workbench.WorkbenchException;
import java.nio.file.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class ProgramControllerTest {
    @TempDir Path root;
    private final ObjectMapper json = new ObjectMapper();

    @Test void webReadsAndSavesActualLocalOutline() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new ProgramController(root.toString())).build();
        mvc.perform(get("/api/program")).andExpect(status().isOk()).andExpect(jsonPath("$.revision").value("new"));
        mvc.perform(put("/api/program").contentType(MediaType.APPLICATION_JSON)
                .content("{\"revision\":\"new\",\"outline\":{\"description\":\"Test instructions\",\"steps\":[{\"id\":\"one\",\"title\":\"First\",\"selected\":true}]}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.outline.steps.length()").value(1))
                .andExpect(jsonPath("$.generatedCurrent").value(true));
        mvc.perform(get("/api/program")).andExpect(status().isOk()).andExpect(jsonPath("$.generated").isNotEmpty());
        mvc.perform(post("/api/program/preview").contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"Test instructions\",\"steps\":[{\"id\":\"next\",\"title\":\"Preview Only\",\"selected\":true}]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.generated").value(org.hamcrest.Matchers.containsString("Preview Only")));
        assertFalse(Files.readString(root.resolve(".agents/skills/PromptForge/SKILL.md")).contains("Preview Only"));
    }


    @Test void selectedStepsPersistAndFailedGenerationPreservesUsableVersion() throws Exception {
        ProgramController api = new ProgramController(root.toString());
        String four = """
            {"description":"Test instructions","steps":[
              {"id":"one","title":"First","selected":true,"instructions":"A"},
              {"id":"two","title":"Second","selected":false,"instructions":"B"},
              {"id":"three","title":"Third","selected":true,"instructions":"C"},
              {"id":"four","title":"Fourth","selected":false,"instructions":"D"}]}
            """;
        var first = api.save(new ProgramController.Write("new", json.readTree(four)));
        assertEquals(4, first.outline().path("steps").size());
        assertTrue(first.generated().contains("First"));
        assertTrue(first.generated().contains("Third"));
        assertFalse(first.generated().contains("Second"));
        assertFalse(first.generated().contains("Fourth"));
        assertEquals(4, new ProgramController(root.toString()).get().outline().path("steps").size());
        assertEquals(2, countSelected(new ProgramController(root.toString()).get().outline()));
        var changed = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(four);
        ((com.fasterxml.jackson.databind.node.ObjectNode) changed.withArray("steps").get(0)).put("instructions", "Updated");
        ((com.fasterxml.jackson.databind.node.ObjectNode) changed.withArray("steps").get(2)).put("selected", false);
        var caps = ((com.fasterxml.jackson.databind.node.ObjectNode) changed.withArray("steps").get(0)).putArray("capabilities");
        caps.addObject().put("type", "reference").put("path", "unused.md").put("selected", false);
        Files.writeString(root.resolve("used.md"), "needed");
        caps.addObject().put("type", "reference").put("path", "used.md").put("selected", true);
        var second = api.save(new ProgramController.Write(first.revision(), changed));
        assertTrue(second.generated().contains("Updated"));
        assertFalse(second.generated().contains("Third"));
        assertTrue(second.generated().contains("used.md"));
        assertFalse(second.generated().contains("unused.md"));
        assertThrows(WorkbenchException.class, () -> api.save(new ProgramController.Write(first.revision(), changed)));
        ((com.fasterxml.jackson.databind.node.ObjectNode) changed.withArray("steps").get(0)).putArray("scripts").add("missing.ps1");
        assertThrows(WorkbenchException.class, () -> api.save(new ProgramController.Write(second.revision(), changed)));
        assertEquals(second.generated(), Files.readString(root.resolve(".agents/skills/PromptForge/SKILL.md")));
        assertEquals(second.revision(), api.get().revision());
        assertFalse(Files.exists(root.resolve(".agents/skills/PromptForge-context/SKILL.md")));
    }
    private int countSelected(com.fasterxml.jackson.databind.JsonNode outline) {
        int count = 0; for (var step : outline.path("steps")) if (step.path("selected").asBoolean()) count++; return count;
    }
}
