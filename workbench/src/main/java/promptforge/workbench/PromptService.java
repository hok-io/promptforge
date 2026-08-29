package promptforge.workbench;

import java.util.List;

/** Prompt use cases; callers do not need to know the storage format. */
public final class PromptService {
    private final ProjectRegistry projects;
    private final PromptPreviewService previews;
    public PromptService(ProjectRegistry projects, PromptPreviewService previews) {
        this.projects = projects;
        this.previews = previews;
    }

    public String preview(String projectId, PromptDocument document) {
        return previews.preview(projects.require(projectId), document);
    }

    public List<PromptVersion> list(String projectId) {
        return repository(projectId).list();
    }

    public PromptVersion load(String projectId, String id) {
        return repository(projectId).load(id);
    }

    public ChangePreview diff(String projectId, PromptDocument document, String revision) {
        return repository(projectId).diff(document, revision);
    }

    public PromptVersion save(String projectId, PromptDocument document, String revision) {
        return repository(projectId).save(document, revision);
    }

    private PromptRepository repository(String projectId) {
        return new JsonPromptRepository(new WorkspaceFiles(projects.require(projectId).root()));
    }
}
