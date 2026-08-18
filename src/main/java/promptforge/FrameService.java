package promptforge;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Shared application operation: CLI and Web use the same Java objects. */
public final class FrameService {
    public String prepare(Path project, String task, String explicitRules, String excludedRules, String knowledgeSources) throws IOException {
        List<Rule> available = new RuleFileLoader().load(project);
        List<Rule> selected = new RuleSelection().select(task, available, explicitRules, excludedRules);
        TaskFrame frame = new TaskFrameCompiler().compileSelected(task, selected);
        List<KnowledgeCard> knowledge = knowledgeSources == null ? List.of()
                : new KnowledgeSelection().select(new KnowledgeCardLoader().load(project), knowledgeSources);
        return new PromptComposer().compose(frame, knowledge);
    }
}
