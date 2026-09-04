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
                .content("{\"revision\":\"new\",\"outline\":{\"steps\":[{\"id\":\"one\",\"title\":\"First\",\"selected\":true}]}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.outline.steps.length()").value(1));
        mvc.perform(get("/api/program")).andExpect(status().isOk()).andExpect(jsonPath("$.generated").isNotEmpty());
    }


    @Test void selectedStepsPersistAndFailedGenerationPreservesUsableVersion() throws Exception {
        ProgramController api = new ProgramController(root.toString());
        String four = """
            {"steps":[
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
        changed.withArray("steps").get(0).deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) changed.withArray("steps").get(0)).put("instructions", "Updated");
        ((com.fasterxml.jackson.databind.node.ObjectNode) changed.withArray("steps").get(2)).put("selected", false);
        var second = api.save(new ProgramController.Write(first.revision(), changed));
        assertTrue(second.generated().contains("Updated"));
        assertFalse(second.generated().contains("Third"));
        assertThrows(WorkbenchException.class, () -> api.save(new ProgramController.Write(first.revision(), changed)));
        ((com.fasterxml.jackson.databind.node.ObjectNode) changed.withArray("steps").get(0)).putArray("scripts").add("missing.ps1");
        assertThrows(WorkbenchException.class, () -> api.save(new ProgramController.Write(second.revision(), changed)));
        assertEquals(second.generated(), Files.readString(root.resolve(".agents/skills/PromptForge/SKILL.md")));
        assertEquals(second.revision(), api.get().revision());
        assertTrue(second.contextGenerated().contains("Do not execute the original task"));
    }
    private int countSelected(com.fasterxml.jackson.databind.JsonNode outline) {
        int count = 0; for (var step : outline.path("steps")) if (step.path("selected").asBoolean()) count++; return count;
    }
}
