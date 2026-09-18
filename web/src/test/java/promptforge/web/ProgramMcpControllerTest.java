package promptforge.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ProgramMcpControllerTest {
    @TempDir Path root;
    private final ObjectMapper json = new ObjectMapper();

    @Test void servesOnlyCurrentGeneratedSelectionThroughReadOnlyTools() throws Exception {
        ProgramController program = new ProgramController(root.toString());
        var source = json.readTree("""
                {"description":"Test instructions","steps":[{"id":"one","title":"Selected","selected":true},
                {"id":"two","title":"Unselected","selected":false}]}
                """);
        program.save(new ProgramController.Write("new", source));
        ProgramMcpController mcp = new ProgramMcpController(root.toString());
        assertEquals("2025-06-18", response(mcp, 1, "initialize", null).path("result").path("protocolVersion").asText());
        assertEquals(5, response(mcp, 2, "tools/list", null).path("result").path("tools").size());
        String generated = response(mcp, 3, "tools/call", "get_selected_skill").path("result").path("content").get(0).path("text").asText();
        assertTrue(generated.contains("Selected"));
        assertFalse(generated.contains("Unselected"));
        String status = response(mcp, 4, "tools/call", "get_program_status").path("result").path("content").get(0).path("text").asText();
        assertTrue(status.contains("Selected"));
        assertFalse(status.contains("Unselected"));
        var second = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(source.toString());
        ((com.fasterxml.jackson.databind.node.ObjectNode) second.withArray("steps").get(0)).put("title", "Updated");
        program.save(new ProgramController.Write(program.get().revision(), second));
        String updated = response(mcp, 5, "tools/call", "get_selected_skill").path("result").path("content").get(0).path("text").asText();
        assertTrue(updated.contains("Updated"));
        assertFalse(updated.contains("## 1. Selected"));
        assertTrue(Files.isRegularFile(root.resolve(".agents/skills/PromptForge/SKILL.md")));
    }

    private com.fasterxml.jackson.databind.JsonNode response(ProgramMcpController mcp, int id, String method, String name) {
        var request = json.createObjectNode().put("jsonrpc", "2.0").put("id", id).put("method", method);
        if (name != null) request.putObject("params").put("name", name);
        return json.valueToTree(mcp.handle(request).getBody());
    }
}
