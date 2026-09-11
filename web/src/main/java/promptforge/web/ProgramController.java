package promptforge.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import promptforge.workbench.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** The outline is the source; repo-scoped agent skills are generated artifacts. */
@RestController
@RequestMapping("/api/program")
public final class ProgramController {
    private final WorkspaceFiles files;
    private final ObjectMapper json = new ObjectMapper();
    private final String OUTLINE;
    private final String SKILL;
    private final String name;
    private static final String CONTEXT = ".agents/skills/PromptForge-context/SKILL.md";

    @org.springframework.beans.factory.annotation.Autowired
    public ProgramController(@Value("${promptforge.home:.}") String home) { this(home, "PromptForge"); }

    ProgramController(String home, String name) {
        validateName(name);
        this.name = name;
        OUTLINE = name.equals("PromptForge") ? ".promptforge/program.json" : ".promptforge/outlines/" + name + ".json";
        SKILL = ".agents/skills/" + name + "/SKILL.md";
        files = new WorkspaceFiles(Path.of(home));
    }

    static void validateName(String name) {
        if (name == null || !(name.equals("PromptForge") || (name.matches("[a-z][a-z0-9-]{0,62}") && !name.equals("promptforge") && !name.equals("promptforge-context"))))
            throw new WorkbenchException(FailureKind.INVALID_INPUT, "Skill ID 請用小寫英文、數字、連字號，最多 63 字元。");
    }

    @GetMapping
    public synchronized View get() {
        VersionedText source = new RevisionFileStore(files).readOrNew(OUTLINE);
        JsonNode outline = source.text().isBlank() ? blankOutline() : parse(source.text());
        String generated = exists(SKILL) ? files.read(SKILL) : "";
        String context = exists(CONTEXT) ? files.read(CONTEXT) : "";
        boolean current = false;
        String status;
        if (generated.isBlank() || (name.equals("PromptForge") && context.isBlank())) status = "尚未生成；保存後需重新載入 agent。";
        else {
            try {
                current = generated.equals(compile(outline)) && (!name.equals("PromptForge") || context.equals(contextSkill()));
                status = current ? "生成版與管理大綱一致；agent 工作階段更新後需重新載入。" : "管理大綱與生成版不同步；請重新保存。";
            } catch (RuntimeException error) { status = "管理大綱有錯誤，生成版仍保留：" + error.getMessage(); }
        }
        return new View(source.revision(), outline, generated, context, status,
                exists(SKILL) && (!name.equals("PromptForge") || exists(CONTEXT)), current);
    }

    @PostMapping("/preview")
    public Preview preview(@RequestBody JsonNode outline) {
        return new Preview(compile(outline), contextSkill());
    }

    @PutMapping
    public synchronized View save(@RequestBody Write write) {
        if (write == null || write.outline() == null || write.revision() == null)
            throw new WorkbenchException(FailureKind.INVALID_INPUT, "缺少大綱或 revision。");
        RevisionFileStore store = new RevisionFileStore(files);
        if (!store.readOrNew(OUTLINE).revision().equals(write.revision()))
            throw new WorkbenchException(FailureKind.CONFLICT, "大綱已有較新修改，請重新載入。");
        String source;
        try { source = json.writerWithDefaultPrettyPrinter().writeValueAsString(write.outline()).replace("\r\n", "\n") + "\n"; }
        catch (IOException error) { throw new WorkbenchException(FailureKind.INVALID_INPUT, "無法序列化大綱。"); }
        String main = compile(write.outline());
        String context = contextSkill();
        if (source.getBytes(StandardCharsets.UTF_8).length > WorkspaceFiles.MAX_BYTES)
            throw new WorkbenchException(FailureKind.INVALID_INPUT, "大綱超過 256 KiB。");
        // Validate and stage every output before replacing any live file.
        Path sourceTmp = null, mainTmp = null, contextTmp = null;
        Path sourcePath = files.resolve(OUTLINE), mainPath = files.resolve(SKILL), contextPath = files.resolve(CONTEXT);
        String oldMain = exists(SKILL) ? files.read(SKILL) : null;
        String oldContext = exists(CONTEXT) ? files.read(CONTEXT) : null;
        try {
            sourceTmp = stage(sourcePath, source);
            mainTmp = stage(mainPath, main);
            if (name.equals("PromptForge")) contextTmp = stage(contextPath, context);
            if (!store.readOrNew(OUTLINE).revision().equals(write.revision()))
                throw new WorkbenchException(FailureKind.CONFLICT, "大綱保存前已變動，沒有覆蓋。");
            move(mainTmp, mainPath); mainTmp = null;
            if (contextTmp != null) { move(contextTmp, contextPath); contextTmp = null; }
            move(sourceTmp, sourcePath); sourceTmp = null;
        } catch (Exception error) {
            restore(mainPath, oldMain);
            if (name.equals("PromptForge")) restore(contextPath, oldContext);
            if (error instanceof WorkbenchException domain) throw domain;
            throw new WorkbenchException(FailureKind.STORAGE_FAILURE, "生成或保存失敗；已保留上一份可用版本：" + error.getMessage());
        } finally {
            delete(sourceTmp); delete(mainTmp); delete(contextTmp);
        }
        return get();
    }

    private String compile(JsonNode outline) {
        if (!outline.isObject() || !outline.path("steps").isArray())
            throw new WorkbenchException(FailureKind.INVALID_INPUT, "大綱需要 steps 陣列。");
        Set<String> ids = new HashSet<>();
        List<JsonNode> selected = new ArrayList<>();
        for (JsonNode step : outline.path("steps")) {
            String id = required(step, "id");
            if (!id.matches("[a-z][a-z0-9-]{0,63}") || !ids.add(id))
                throw new WorkbenchException(FailureKind.INVALID_INPUT, "步驟 ID 無效或重複：" + id);
            required(step, "title");
            if (step.path("selected").asBoolean(false)) selected.add(step);
        }
        if (selected.isEmpty()) throw new WorkbenchException(FailureKind.INVALID_INPUT, "至少選擇一個步驟。");
        String description = outline.path("description").asText("Turn a user's task into an executable prompt using the selected local PromptForge outline.").trim();
        if (description.isEmpty()) throw new WorkbenchException(FailureKind.INVALID_INPUT, "請填寫 Skill 用途與使用時機。");
        StringBuilder out = new StringBuilder("---\nname: " + name + "\ndescription: " + yamlQuote(description) + "\n---\n\n# " + name + "\n\n");
        out.append("Project root: `").append(files.root().toString().replace('\\', '/')).append("`. Resolve dependency paths against this root.\n\n");
        if (name.equals("PromptForge")) out.append("Input: the user's original task. Preserve goal, scope, constraints, expected result, and acceptance checks.\n\n");
        int number = 1;
        for (JsonNode step : selected) {
            out.append("## ").append(number++).append(". ").append(required(step, "title")).append("\n\n");
            for (String field : List.of("purpose", "instructions", "input", "decision", "actions", "observation", "adjustment", "output", "stop")) {
                String value = step.path(field).asText("").trim();
                if (!value.isEmpty()) out.append("- ").append(field).append(": ").append(value).append("\n");
            }
            for (String field : List.of("scripts", "references", "skills")) {
                JsonNode deps = step.path(field);
                if (!deps.isMissingNode() && !deps.isArray()) throw new WorkbenchException(FailureKind.INVALID_INPUT, field + " 必須是陣列。");
                for (JsonNode dep : deps) {
                    if (!dep.isTextual()) throw new WorkbenchException(FailureKind.INVALID_INPUT, field + " 需要路徑字串。");
                    String path = dep.asText();
                    validateDependency(path, field.equals("skills"), new HashSet<>());
                    out.append("- ").append(field).append(": `").append(path).append("` (read when needed)\n");
                }
            }
            JsonNode capabilities = step.path("capabilities");
            if (!capabilities.isMissingNode() && !capabilities.isArray())
                throw new WorkbenchException(FailureKind.INVALID_INPUT, "capabilities 必須是陣列。");
            for (JsonNode capability : capabilities) {
                if (!capability.isObject()) throw new WorkbenchException(FailureKind.INVALID_INPUT, "能力設定必須是物件。");
                String type = required(capability, "type");
                if (!List.of("script", "reference", "skill", "asset").contains(type))
                    throw new WorkbenchException(FailureKind.INVALID_INPUT, "未知能力類型：" + type);
                String path = capability.path("path").asText("").trim();
                if (capability.path("selected").asBoolean(false)) {
                    if (path.isEmpty()) throw new WorkbenchException(FailureKind.INVALID_INPUT, "已選能力缺少路徑。");
                    validateDependency(path, type.equals("skill"), new HashSet<>());
                    out.append("- ").append(type).append(": `").append(path).append("` (read when needed)\n");
                    String usage = capability.path("usage").asText("").trim();
                    if (!usage.isEmpty()) out.append("  Usage: ").append(usage).append("\n");
                    JsonNode requires = capability.path("requires");
                    if (!requires.isMissingNode() && !requires.isArray()) throw new WorkbenchException(FailureKind.INVALID_INPUT, "requires 必須是路徑陣列。");
                    for (JsonNode dependency : requires) {
                        if (!dependency.isTextual()) throw new WorkbenchException(FailureKind.INVALID_INPUT, "依賴必須是路徑文字。");
                        validateDependency(dependency.asText(), dependency.asText().endsWith("/SKILL.md"), new HashSet<>());
                        out.append("  Required file: `").append(dependency.asText()).append("`\n");
                    }
                }
            }
            out.append("\n");
        }
        if (name.equals("PromptForge")) out.append("## Required result\n\nWrite a prompt with the original goal, scope, constraints, expected output and acceptance method. Include an executable ReAct loop: judge the next step from current evidence; act; observe actual results; adjust, gather more context or retry; decide completion or blockage. Run only steps needed for this task. Ask or research when essential information is missing. Show a brief decision summary, action results and verification evidence; never request hidden chain of thought. The prompt must be usable directly as input to $PromptForge-context.\n");
        else out.append("\nUse the selected steps as needed for the user's task. Report concise decisions, action results and verification evidence. Observe results, adjust on failure, and stop on the defined completion or blockage conditions. Do not disclose hidden chain of thought.\n");
        return out.toString();
    }

    private String yamlQuote(String text) {
        try { return json.writeValueAsString(text); }
        catch (IOException error) { throw new WorkbenchException(FailureKind.INVALID_INPUT, "Skill 描述無效。"); }
    }

    private void validateDependency(String path, boolean skill, Set<Path> visiting) {
        Path resolved = files.resolve(path);
        if (!Files.isRegularFile(resolved)) throw new WorkbenchException(FailureKind.INVALID_INPUT, "缺少必要依賴：" + path);
        if (!skill) return;
        if (resolved.equals(files.resolve(SKILL)) || !visiting.add(resolved))
            throw new WorkbenchException(FailureKind.INVALID_INPUT, "Skill 依賴形成循環：" + path);
        String body = files.read(path);
        if (!body.startsWith("---") || !body.contains("\nname:") || !body.contains("\ndescription:"))
            throw new WorkbenchException(FailureKind.INVALID_INPUT, "Skill 需要 name 與 description：" + path);
        var generated = java.util.regex.Pattern.compile("(?m)^(?:- (?:script|reference|skill|asset|scripts|references|skills):|  Required file:) `([^`]+)`").matcher(body);
        while (generated.find()) validateDependency(generated.group(1), generated.group(1).endsWith("/SKILL.md"), visiting);
        var links = java.util.regex.Pattern.compile("\\[[^\\]]*\\]\\(([^)\\s]+)\\)").matcher(body);
        while (links.find()) {
            String target = links.group(1).split("#", 2)[0];
            if (target.isEmpty() || target.matches("[a-zA-Z][a-zA-Z0-9+.-]*:.*")) continue;
            Path local = resolved.getParent().resolve(target).normalize();
            String relative = files.root().relativize(local).toString().replace('\\', '/');
            validateDependency(relative, relative.endsWith("/SKILL.md"), visiting);
        }
        visiting.remove(resolved);
    }

    private static String contextSkill() {
        return "---\nname: PromptForge-context\ndescription: Check actual available context for a prompt produced by PromptForge. Invoke as $PromptForge-context (or /PromptForge-context when available).\n---\n\n# PromptForge context check\n\nInput: the complete prompt from PromptForge. Analyze which context the task requires. Inspect the current environment and read relevant accessible files, tools and resources when needed. Reassess after retrieval or clarification. Do not execute the original task.\n\nReport separately: (1) context actually read and understood, with evidence; (2) accessible resources not yet read; (3) necessary missing or inaccessible data; (4) scope currently feasible and scope blocked with reasons. Tool availability is not acquired data. A complete prompt is not proof of sufficient context. Give concise decision, action and verification summaries without hidden chain of thought.\n";
    }

    private static String required(JsonNode node, String key) {
        String value = node.path(key).asText("").trim();
        if (value.isEmpty() || value.contains("\n") || value.contains("\r"))
            throw new WorkbenchException(FailureKind.INVALID_INPUT, "步驟缺少有效 " + key + "。");
        return value;
    }
    private JsonNode parse(String text) {
        try { return json.readTree(text); }
        catch (IOException error) { throw new WorkbenchException(FailureKind.INVALID_INPUT, "大綱 JSON 無法讀取。"); }
    }
    private ObjectNode blankOutline() { ObjectNode value = json.createObjectNode(); value.putArray("steps"); return value; }
    private boolean exists(String path) { return Files.isRegularFile(files.resolve(path)); }
    private static Path stage(Path target, String text) throws IOException {
        Files.createDirectories(target.getParent());
        Path temp = Files.createTempFile(target.getParent(), ".program-", ".tmp");
        Files.writeString(temp, text, StandardCharsets.UTF_8);
        return temp;
    }
    private static void move(Path from, Path to) throws IOException { Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
    private static void delete(Path path) { if (path != null) try { Files.deleteIfExists(path); } catch (IOException ignored) { } }
    private static void restore(Path path, String old) {
        try { if (old == null) Files.deleteIfExists(path); else move(stage(path, old), path); }
        catch (IOException ignored) { /* original failure is reported */ }
    }
    public record Write(String revision, JsonNode outline) { }
    public record View(String revision, JsonNode outline, String generated, String contextGenerated,
                       String status, boolean discoverableFilesPresent, boolean generatedCurrent) { }
    public record Preview(String generated, String contextGenerated) { }
}
