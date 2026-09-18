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
        if (name == null || !((name.equals("PromptForge") || name.equals("PromptForge-context")) || (name.matches("[a-z][a-z0-9-]{0,62}") && !name.equals("promptforge") && !name.equals("promptforge-context"))))
            throw new WorkbenchException(FailureKind.INVALID_INPUT, "Skill ID 請用小寫英文、數字、連字號，最多 63 字元。");
    }

    @GetMapping
    public synchronized View get() {
        VersionedText source = new RevisionFileStore(files).readOrNew(OUTLINE);
        JsonNode outline = source.text().isBlank() ? blankOutline() : parse(source.text());
        String generated = exists(SKILL) ? files.read(SKILL) : "";
        if (source.revision().equals("new") && !generated.isBlank()) {
            ObjectNode imported = blankOutline(); imported.put("document", generated);
            imported.put("description", descriptionFrom(generated));
            outline = imported;
            source = new VersionedText("external:" + RevisionFileStore.hash(generated), "");
        }
        if (outline.has("document")) ((ObjectNode)outline).put("description", descriptionFrom(outline.path("document").asText()));
        boolean current = false;
        String status;
        if (generated.isBlank()) status = "尚未生成；保存後需重新載入 agent。";
        else {
            try {
                current = generated.equals(compile(outline));
                status = current ? "生成版與管理大綱一致；agent 工作階段更新後需重新載入。" : "管理大綱與生成版不同步；請重新保存。";
            } catch (RuntimeException error) { status = "管理大綱有錯誤，生成版仍保留：" + error.getMessage(); }
        }
        return new View(source.revision(), outline, generated, status,
                exists(SKILL), current);
    }

    @PostMapping("/preview")
    public Preview preview(@RequestBody JsonNode outline) {
        return new Preview(compile(outline));
    }

    @PutMapping
    public synchronized View save(@RequestBody Write write) {
        if (write == null || write.outline() == null || write.revision() == null)
            throw new WorkbenchException(FailureKind.INVALID_INPUT, "缺少大綱或 revision。");
        RevisionFileStore store = new RevisionFileStore(files);
        if (!get().revision().equals(write.revision()))
            throw new WorkbenchException(FailureKind.CONFLICT, "大綱已有較新修改，請重新載入。");
        JsonNode currentSource = get().outline();
        String oldMain = exists(SKILL) ? files.read(SKILL) : null;
        String expectedArtifact = currentSource.path("_generatedRevision").asText("");
        if (!write.outline().isObject()) throw new WorkbenchException(FailureKind.INVALID_INPUT, "Skill 來源需要 JSON 物件。");
        if (!write.outline().has("schemaVersion")) ((ObjectNode)write.outline()).put("schemaVersion", "1.0");
        String main = compile(write.outline());
        if (oldMain != null && !expectedArtifact.isEmpty()
                && !RevisionFileStore.hash(oldMain).equals(expectedArtifact)
                && !main.equals(oldMain))
            throw new WorkbenchException(FailureKind.CONFLICT, "Skill 檔案已有外部修改。請把輸出區顯示的新版內容合併到管理大綱或 Markdown，再保存。");
        if (oldMain != null && expectedArtifact.isEmpty() && !compile(currentSource).equals(oldMain)
                && !main.equals(oldMain))
            throw new WorkbenchException(FailureKind.CONFLICT, "現有 Skill 與管理來源不同。請把輸出區顯示的新版內容合併到管理來源，再保存。");
        if (oldMain != null && expectedArtifact.isEmpty() && currentSource.has("document")
                && !currentSource.path("document").asText().equals(oldMain) && !main.equals(oldMain))
            throw new WorkbenchException(FailureKind.CONFLICT, "Skill 檔案已有外部修改。請把輸出區顯示的新版內容合併到管理 Markdown，再保存。");
        ((ObjectNode)write.outline()).put("_generatedRevision", RevisionFileStore.hash(main));
        String source;
        try { source = json.writerWithDefaultPrettyPrinter().writeValueAsString(write.outline()).replace("\r\n", "\n") + "\n"; }
        catch (IOException error) { throw new WorkbenchException(FailureKind.INVALID_INPUT, "無法序列化大綱。"); }
        if (source.getBytes(StandardCharsets.UTF_8).length > WorkspaceFiles.MAX_BYTES)
            throw new WorkbenchException(FailureKind.INVALID_INPUT, "大綱超過 256 KiB。");
        // Validate and stage every output before replacing any live file.
        Path sourceTmp = null, mainTmp = null;
        Path sourcePath = files.resolve(OUTLINE), mainPath = files.resolve(SKILL);
        String oldSource = exists(OUTLINE) ? files.read(OUTLINE) : null;
        boolean artifactMoved = false, sourceMoved = false;
        try {
            sourceTmp = stage(sourcePath, source);
            mainTmp = stage(mainPath, main);
            if (!get().revision().equals(write.revision()))
                throw new WorkbenchException(FailureKind.CONFLICT, "大綱保存前已變動，沒有覆蓋。");
            String latestMain = exists(SKILL) ? files.read(SKILL) : null;
            if (!Objects.equals(oldMain, latestMain))
                throw new WorkbenchException(FailureKind.CONFLICT, "Skill 檔案保存期間被外部修改；沒有覆蓋。");
            move(mainTmp, mainPath); mainTmp = null; artifactMoved = true;
            move(sourceTmp, sourcePath); sourceTmp = null; sourceMoved = true;
        } catch (Exception error) {
            if (artifactMoved) restore(mainPath, oldMain);
            if (sourceMoved) restore(sourcePath, oldSource);
            if (error instanceof WorkbenchException domain) throw domain;
            throw new WorkbenchException(FailureKind.STORAGE_FAILURE, "生成或保存失敗；已保留上一份可用版本：" + error.getMessage());
        } finally {
            delete(sourceTmp); delete(mainTmp);
        }
        return get();
    }

    private String compile(JsonNode outline) {
        if (outline.has("schemaVersion") && !"1.0".equals(outline.path("schemaVersion").asText()))
            throw new WorkbenchException(FailureKind.INVALID_INPUT, "不支援的大綱 schemaVersion；目前支援 1.0。請先轉換大綱格式。");
        if (outline.has("document")) {
            String document = outline.path("document").asText();
            String normalized = document.replace("\r\n", "\n");
            if (!normalized.startsWith("---\n") || !normalized.contains("\ndescription:") || !normalized.contains("\nname: " + name + "\n"))
                throw new WorkbenchException(FailureKind.INVALID_INPUT, "Markdown 必須保留 frontmatter name: " + name + " 及 description。");
            if (document.getBytes(StandardCharsets.UTF_8).length > WorkspaceFiles.MAX_BYTES)
                throw new WorkbenchException(FailureKind.INVALID_INPUT, "Skill 超過 256 KiB。");
            validateDependencyContent(document);
            return document;
        }
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
        String description = outline.path("description").asText("").trim();
        if (description.isEmpty()) throw new WorkbenchException(FailureKind.INVALID_INPUT, "請填寫 Skill 用途與使用時機。");
        StringBuilder out = new StringBuilder("---\nname: " + name + "\ndescription: " + yamlQuote(description) + "\n---\n\n# " + name + "\n\n");
        out.append("Project root: `").append(files.root().toString().replace('\\', '/')).append("`. Resolve dependency paths against this root.\n\n");
        String input = outline.path("input").asText("");
        if (!input.isBlank()) out.append("Input: ").append(input).append("\n\n");
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
        String result = outline.path("result").asText("");
        if (!result.isBlank()) out.append("## Required result\n\n").append(result).append("\n");
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

    private void validateDependencyContent(String body) {
        var links = java.util.regex.Pattern.compile("\\[[^\\]]*\\]\\(([^)\\s]+)\\)").matcher(body);
        while (links.find()) {
            String target = links.group(1).split("#", 2)[0];
            if (target.isEmpty() || target.matches("[a-zA-Z][a-zA-Z0-9+.-]*:.*")) continue;
            Path resolved = files.resolve(SKILL).getParent().resolve(target).normalize();
            validateDependency(files.root().relativize(resolved).toString().replace('\\', '/'), target.endsWith("SKILL.md"), new HashSet<>());
        }
    }

    private static String required(JsonNode node, String key) {
        String value = node.path(key).asText("").trim();
        if (value.isEmpty() || value.contains("\n") || value.contains("\r"))
            throw new WorkbenchException(FailureKind.INVALID_INPUT, "步驟缺少有效 " + key + "。");
        return value;
    }
    private String descriptionFrom(String markdown) {
        var matcher = java.util.regex.Pattern.compile("(?m)^description:\\s*(.+?)\\s*$").matcher(markdown.replace("\r\n", "\n"));
        if (!matcher.find()) return name;
        String value = matcher.group(1);
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            try { return json.readValue(value, String.class); }
            catch (IOException ignored) { return value.substring(1, value.length() - 1); }
        }
        if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) return value.substring(1, value.length() - 1).replace("''", "'");
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
    public record View(String revision, JsonNode outline, String generated,
                       String status, boolean discoverableFilesPresent, boolean generatedCurrent) { }
    public record Preview(String generated) { }
}
