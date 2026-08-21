package promptforge.web;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import promptforge.*;
import promptforge.workbench.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class WorkbenchController {
    private final ProjectRegistry projects;
    private final PromptService prompts;
    private final SkillService skills;
    public WorkbenchController(ProjectRegistry projects, PromptService prompts, SkillService skills) {
        this.projects = projects; this.prompts = prompts; this.skills = skills;
    }
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @GetMapping("/projects")
    public List<ProjectView> projects() {
        return projects.all().stream().map(p -> new ProjectView(p.id(), p.name())).toList();
    }

    @GetMapping("/projects/{project}/context")
    public Context context(@PathVariable String project) throws IOException {
        Project selected = projects.require(project);
        var rules = new RuleFileLoader().load(selected.root()).stream().map(r -> new RuleView(r.id(), r.keywords(), r.instruction(), r.source())).toList();
        var knowledge = new KnowledgeCardLoader().load(selected.root()).stream().map(k -> new KnowledgeView(k.title(), k.body(), k.source())).toList();
        var documents = prompts.repository(project).list().stream().map(p -> new PromptSummary(p.document().id(), p.document().name(), p.revision())).toList();
        var summaries = skills.list(project).stream().map(s -> new SkillSummary(s.id(), s.name(), s.description(), s.revision(), s.state(), s.origin())).toList();
        return new Context(rules, knowledge, documents, summaries);
    }

    @GetMapping("/projects/{project}/knowledge/search")
    public List<SearchView> search(@PathVariable String project, @RequestParam String q) throws IOException {
        Project selected = projects.require(project);
        return new KnowledgeQueryService().search(selected.root(), q).stream().map(hit -> {
            String body = hit.card().body().replaceAll("\\s+", " ").trim();
            int count = body.codePointCount(0, body.length());
            String excerpt = count <= 180 ? body : body.substring(0, body.offsetByCodePoints(0, 177)) + "...";
            return new SearchView(hit.card().title(), hit.card().source(), hit.score(), excerpt);
        }).toList();
    }

    @PostMapping("/projects/{project}/preview")
    public Map<String, String> preview(@PathVariable String project, @RequestBody PromptDocument document) {
        return Map.of("text", prompts.preview(project, document));
    }

    private void checkId(String id, PromptDocument document) { WorkbenchException.require(document != null && id.equals(document.id()), "網址與 Prompt ID 不同。"); }
    public record ProjectView(String id, String name) { }
    public record RuleView(String id, List<String> keywords, String instruction, String source) { }
    public record KnowledgeView(String title, String body, String source) { }
    public record SearchView(String title, String source, double score, String excerpt) { }
    public record PromptSummary(String id, String name, String revision) { }
    public record SkillSummary(String id, String name, String description, String revision, String state, String origin) { }
    public record Context(List<RuleView> rules, List<KnowledgeView> knowledge, List<PromptSummary> prompts, List<SkillSummary> skills) { }
    public record PromptWrite(String revision, PromptDocument document) { }
    public record SkillWrite(String revision, String raw) { }
    public record ImportSkill(String id, String source) { }
    public record Transition(String revision, String action) { }
    public record ExportSkill(String revision, String target) { }
}
