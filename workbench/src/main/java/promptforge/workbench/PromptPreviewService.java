package promptforge.workbench;

import promptforge.FrameService;
import java.io.IOException;

public final class PromptPreviewService {
    private final FrameService frames;
    public PromptPreviewService(FrameService frames) { this.frames = frames; }
    public String preview(Project project, PromptDocument document) {
        String task = new PromptTemplateCompiler().compile(document);
        try {
            return frames.prepare(project.root(), task, document.ruleIds() == null ? null : String.join(",", document.ruleIds()),
                    String.join(",", document.excludedRuleIds()), String.join(",", document.knowledgeSources()));
        } catch (IOException error) { throw new WorkbenchException(FailureKind.STORAGE_FAILURE, "讀取專案資料失敗。"); }
        catch (IllegalArgumentException error) { throw new WorkbenchException(FailureKind.INVALID_INPUT, error.getMessage()); }
    }
}
