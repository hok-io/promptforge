package promptforge.web;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import promptforge.workbench.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Local authoring library. Generated artifacts are never edited through this API. */
@RestController
@RequestMapping("/api/skills")
public final class SkillLibraryController {
    private final String home;
    private final WorkspaceFiles files;
    private final Map<String, ProgramController> programs = new ConcurrentHashMap<>();
    public SkillLibraryController(@Value("${promptforge.home:.}") String home, ProgramController main) {
        this.home = home;
        files = new WorkspaceFiles(Path.of(home));
        programs.put("PromptForge", main);
    }
    ProgramController program(String name) {
        ProgramController.validateName(name);
        return programs.computeIfAbsent(name, key -> new ProgramController(home, key));
    }
    @GetMapping public List<String> list() {
        List<String> names = new ArrayList<>(List.of("PromptForge"));
        for (String path : files.filesUnder(".promptforge/outlines")) {
            String filename = Path.of(path).getFileName().toString();
            if (filename.endsWith(".json")) names.add(filename.substring(0, filename.length() - 5));
        }
        return names.stream().distinct().sorted().toList();
    }
    @GetMapping("/{name}") public ProgramController.View get(@PathVariable String name) { return program(name).get(); }
    @PutMapping("/{name}") public ProgramController.View save(@PathVariable String name, @RequestBody ProgramController.Write write) {
        if (program(name).get().revision().equals("new") && Files.exists(files.resolve(".agents/skills/" + name + "/SKILL.md")))
            throw new WorkbenchException(FailureKind.CONFLICT, "同名 Skill 已存在，請使用另一個 ID，避免覆蓋外部 Skill。");
        return program(name).save(write);
    }
    @PostMapping("/{name}/preview") public ProgramController.Preview preview(@PathVariable String name, @RequestBody JsonNode outline) { return program(name).preview(outline); }
    @GetMapping("/{name}/files") public List<String> resources(@PathVariable String name) {
        ProgramController.validateName(name);
        return files.filesUnder(".promptforge/resources/" + name);
    }
    private String resourcePath(String name, String path) {
        ProgramController.validateName(name);
        if (path == null || !path.matches("(scripts|references|assets)/[a-zA-Z0-9][a-zA-Z0-9._/-]*") || path.contains(".."))
            throw new WorkbenchException(FailureKind.INVALID_INPUT, "資源路徑須為 scripts/、references/ 或 assets/ 下的檔案。");
        return ".promptforge/resources/" + name + "/" + path;
    }
    @GetMapping("/{name}/file") public VersionedText read(@PathVariable String name, @RequestParam String path) {
        return new RevisionFileStore(files).readOrNew(resourcePath(name, path));
    }
    @PutMapping("/{name}/file") public VersionedText write(@PathVariable String name, @RequestBody ResourceWrite write) {
        return new RevisionFileStore(files).save(resourcePath(name, write.path()), write.revision(), write.text());
    }
    public record ResourceWrite(String path, String revision, String text) { }
}
