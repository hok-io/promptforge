package promptforge.workbench;

public final class PromptService {
    private final ProjectRegistry projects;
    private final PromptPreviewService previews;
    public PromptService(ProjectRegistry projects, PromptPreviewService previews) { this.projects = projects; this.previews = previews; }
    public String preview(String projectId, PromptDocument document) { return previews.preview(projects.require(projectId), document); }
    public PromptRepository repository(String projectId) { return new JsonPromptRepository(new WorkspaceFiles(projects.require(projectId).root())); }
}
