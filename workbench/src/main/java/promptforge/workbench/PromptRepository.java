package promptforge.workbench;

import java.util.List;

/** Port: application code can use another storage adapter without knowing JSON. */
public interface PromptRepository {
    PromptVersion load(String id);
    List<PromptVersion> list();
    ChangePreview diff(PromptDocument document, String expectedRevision);
    PromptVersion save(PromptDocument document, String expectedRevision);
}
