package promptforge.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import promptforge.workbench.RevisionFileStore;
import promptforge.workbench.WorkspaceFiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Local, stateless, read-only MCP endpoint. It exposes generated artifacts only. */
@RestController
public final class ProgramMcpController {
    private static final String MAIN = ".agents/skills/PromptForge/SKILL.md";
    private static final String CONTEXT = ".agents/skills/PromptForge-context/SKILL.md";
    private static final String OUTLINE = ".promptforge/program.json";
    private final WorkspaceFiles files;
    private final ObjectMapper json = new ObjectMapper();
    private final ConnectionVerification verification;

    public ProgramMcpController(String home) { this(home, new ConnectionVerification(home)); }
    @org.springframework.beans.factory.annotation.Autowired
    public ProgramMcpController(@Value("${promptforge.home:.}") String home, ConnectionVerification verification) {
        this.verification = verification;
        files = new WorkspaceFiles(Path.of(home));
    }

    @PostMapping(path = "/mcp", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> handle(@RequestBody JsonNode request) {
        if (!request.isObject() || !"2.0".equals(request.path("jsonrpc").asText()))
            return ResponseEntity.ok(error(request.path("id"), -32600, "Invalid JSON-RPC request"));
        String method = request.path("method").asText("");
        if (method.startsWith("notifications/")) return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        JsonNode id = request.path("id");
        if (id.isMissingNode() || id.isNull()) return ResponseEntity.ok(error(id, -32600, "Request id required"));
        Object result;
        switch (method) {
            case "initialize" -> result = Map.of(
                    "protocolVersion", "2025-06-18",
                    "capabilities", Map.of("tools", Map.of()),
                    "serverInfo", Map.of("name", "promptforge-local", "version", "1.0"));
            case "ping" -> result = Map.of();
            case "tools/list" -> result = Map.of("tools", List.of(
                    tool("list_skills", "List local Skills available to read."),
                    Map.of("name", "get_skill", "description", "Read a generated Skill by name. Follow its project root and dependency paths; this tool does not execute scripts.",
                            "inputSchema", Map.of("type", "object", "properties", Map.of("name", Map.of("type", "string"), "verificationCode", Map.of("type", "string", "description", "Optional code supplied by the Web connection check. Records a short-lived receipt of this read.")), "required", List.of("name"), "additionalProperties", false),
                            "annotations", Map.of("readOnlyHint", true)),
                    tool("get_selected_skill", "Read the exact currently generated PromptForge Skill. Only selected steps and dependencies are present."),
                    tool("get_context_skill", "Read the currently generated PromptForge-context Skill for checking context without executing the task."),
                    tool("get_program_status", "Read generation status, selected step names, and revisions without exposing the full management outline.")));
            case "tools/call" -> {
                String name = request.path("params").path("name").asText("");
                result = switch (name) {
                    case "list_skills" -> skillList();
                    case "get_skill" -> namedSkill(request.path("params").path("arguments"));
                    case "get_selected_skill" -> readTool(MAIN);
                    case "get_context_skill" -> readTool(CONTEXT);
                    case "get_program_status" -> statusTool();
                    default -> null;
                };
                if (result == null) return ResponseEntity.ok(error(id, -32602, "Unknown tool: " + name));
            }
            default -> { return ResponseEntity.ok(error(id, -32601, "Method not found: " + method)); }
        }
        return ResponseEntity.ok(Map.of("jsonrpc", "2.0", "id", id, "result", result));
    }

    private Map<String, Object> skillList() {
        try {
            var names = SkillCatalog.names(files);
            return toolText(json.writeValueAsString(names));
        } catch (Exception error) { return toolError(error.getMessage()); }
    }
    private Map<String, Object> namedSkill(JsonNode arguments) {
        String name = arguments.path("name").asText("");
        try { ProgramController.validateName(name); }
        catch (RuntimeException error) { return toolError(error.getMessage()); }
        if (!SkillCatalog.names(files).contains(name)) return toolError("找不到本機 Skill：" + name);
        Map<String,Object> result = readTool(".agents/skills/" + name + "/SKILL.md");
        if (!result.containsKey("isError") && arguments.has("verificationCode")) {
            @SuppressWarnings("unchecked") var content = (List<Map<String,String>>) result.get("content");
            verification.record(arguments.path("verificationCode").asText(), name, content.get(0).get("text"));
        }
        return result;
    }

    private static Map<String, Object> tool(String name, String description) {
        return Map.of("name", name, "description", description,
                "inputSchema", Map.of("type", "object", "properties", Map.of(), "additionalProperties", false),
                "annotations", Map.of("readOnlyHint", true));
    }

    private Map<String, Object> readTool(String path) {
        if (!Files.isRegularFile(files.resolve(path))) return toolError("尚未生成：" + path);
        return toolText(files.read(path));
    }

    private Map<String, Object> statusTool() {
        boolean main = Files.isRegularFile(files.resolve(MAIN));
        boolean context = Files.isRegularFile(files.resolve(CONTEXT));
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("generated", main && context);
        status.put("skillReloadRequiredAfterSave", true);
        status.put("mcpReadsLatestSavedVersion", true);
        status.put("outlineRevision", new RevisionFileStore(files).readOrNew(OUTLINE).revision());
        status.put("selectedSkillRevision", main ? RevisionFileStore.hash(files.read(MAIN)) : null);
        status.put("contextSkillRevision", context ? RevisionFileStore.hash(files.read(CONTEXT)) : null);
        try {
            JsonNode outline = json.readTree(new RevisionFileStore(files).readOrNew(OUTLINE).text());
            status.put("selectedSteps", outline.path("steps").isArray() ?
                    java.util.stream.StreamSupport.stream(outline.path("steps").spliterator(), false)
                            .filter(step -> step.path("selected").asBoolean(false))
                            .map(step -> step.path("title").asText()).toList() : List.of());
        } catch (Exception error) { status.put("selectedSteps", List.of()); }
        try { return toolText(json.writeValueAsString(status)); }
        catch (Exception error) { return toolError("無法讀取狀態。"); }
    }

    private static Map<String, Object> toolText(String text) {
        return Map.of("content", List.of(Map.of("type", "text", "text", text)));
    }
    private static Map<String, Object> toolError(String text) {
        return Map.of("isError", true, "content", List.of(Map.of("type", "text", "text", text)));
    }
    private static Map<String, Object> error(JsonNode id, int code, String message) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("jsonrpc", "2.0");
        envelope.put("id", id.isMissingNode() ? null : id);
        envelope.put("error", Map.of("code", code, "message", message));
        return envelope;
    }
}
