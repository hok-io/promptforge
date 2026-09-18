package promptforge.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class PortableOutlineTemplateTest {
    @TempDir Path workspace;
    private final ObjectMapper json = new ObjectMapper();

    @Test void trackedTemplateUsesVersionedFormatAndCompilesWithTheBackend() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.isRegularFile(root.resolve("templates/outline-v1.json"))) root = root.getParent();
        assertNotNull(root, "repository template should be available to tests");
        JsonNode schema = json.readTree(root.resolve("templates/outline-v1.schema.json").toFile());
        assertEquals("https://json-schema.org/draft/2020-12/schema", schema.path("$schema").asText());
        assertEquals("1.0", schema.path("$defs").path("structured").path("properties").path("schemaVersion").path("const").asText());
        JsonNode generic = json.readTree(root.resolve("templates/outline-v1.json").toFile());
        JsonNode prompt = json.readTree(root.resolve("templates/PromptForge-v1.json").toFile());
        JsonNode context = json.readTree(root.resolve("templates/PromptForge-context-v1.json").toFile());
        for (JsonNode outline : new JsonNode[] { generic, prompt, context }) {
            assertEquals("1.0", outline.path("schemaVersion").asText());
            assertTrue(outline.path("steps").size() >= 1);
            assertTrue(outline.path("steps").get(0).path("selected").asBoolean());
        }
        String promptSkill = new ProgramController(workspace.toString(), "PromptForge").preview(prompt).generated();
        String contextSkill = new ProgramController(workspace.toString(), "PromptForge-context").preview(context).generated();
        assertTrue(new ProgramController(workspace.toString(), "generic").preview(generic).generated().contains("Understand the task"));
        assertTrue(promptSkill.contains("evidence-driven ReAct loop"));
        assertTrue(promptSkill.contains("PromptForge-context"));
        assertTrue(contextSkill.contains("Do not perform the task actions"));
        assertTrue(contextSkill.contains("Do not execute the original task"));
        JsonNode unsupported = generic.deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) unsupported).put("schemaVersion", "2.0");
        assertThrows(RuntimeException.class, () -> new ProgramController(workspace.toString()).preview(unsupported));
    }
}
