package promptforge.workbench;

import promptforge.KnowledgeCard;
import promptforge.KnowledgeCardLoader;
import promptforge.KnowledgeQueryService;
import promptforge.Rule;
import promptforge.RuleFileLoader;
import promptforge.SearchHit;
import java.io.IOException;
import java.util.List;

/** Loads the registered project's sources for any interface, including HTTP. */
public final class ProjectContextService {
    private final ProjectRegistry projects;
    private final RuleFileLoader rules;
    private final KnowledgeCardLoader knowledge;
    private final KnowledgeQueryService search;

    public ProjectContextService(ProjectRegistry projects, RuleFileLoader rules,
            KnowledgeCardLoader knowledge, KnowledgeQueryService search) {
        this.projects = projects;
        this.rules = rules;
        this.knowledge = knowledge;
        this.search = search;
    }

    public List<Rule> rules(String projectId) throws IOException {
        return rules.load(projects.require(projectId).root());
    }

    public List<KnowledgeCard> knowledge(String projectId) throws IOException {
        return knowledge.load(projects.require(projectId).root());
    }

    public List<SearchHit> search(String projectId, String query) throws IOException {
        return search.search(projects.require(projectId).root(), query);
    }
}
