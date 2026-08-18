package promptforge.workbench;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

/** JSON file adapter; it implements the repository port rather than changing the domain. */
public final class JsonPromptRepository implements PromptRepository {
    private final WorkspaceFiles files;
    private final RevisionFileStore store;
    private final ObjectMapper json = new ObjectMapper();
    public JsonPromptRepository(WorkspaceFiles files) { this.files = files; this.store = new RevisionFileStore(files); }
    private String path(String id) { return ".promptforge/prompts/" + WorkbenchException.id(id) + "/document.json"; }

    @Override public PromptVersion load(String id) {
        VersionedText saved = store.read(path(id));
        try {
            PromptDocument document = json.readValue(saved.text(), PromptDocument.class);
            WorkbenchException.require(document.id().equals(id), "Prompt 內部 ID 與檔案位置不同。");
            return new PromptVersion(saved.revision(), document);
        } catch (java.io.IOException error) { throw new WorkbenchException(422, "Prompt JSON 格式錯誤：" + id); }
    }
    @Override public List<PromptVersion> list() {
        List<PromptVersion> result = new ArrayList<>();
        for (String file : files.filesUnder(".promptforge/prompts")) {
            if (file.endsWith("/document.json")) {
                String id = file.substring(".promptforge/prompts/".length(), file.length() - "/document.json".length());
                result.add(load(id));
            }
        }
        return List.copyOf(result);
    }
    private String serialize(PromptDocument document) {
        try { return json.writerWithDefaultPrettyPrinter().writeValueAsString(document) + "\n"; }
        catch (java.io.IOException error) { throw new WorkbenchException(500, "無法編譯 Prompt JSON。"); }
    }
    @Override public ChangePreview diff(PromptDocument document, String expectedRevision) {
        VersionedText old = store.readOrNew(path(document.id()));
        if (!old.revision().equals(expectedRevision)) throw new WorkbenchException(409, "Prompt 已改變，請重新載入。");
        String after = serialize(document);
        return new ChangePreview(old.text(), after, !old.text().equals(after));
    }
    @Override public PromptVersion save(PromptDocument document, String expectedRevision) {
        VersionedText saved = store.save(path(document.id()), expectedRevision, serialize(document));
        return new PromptVersion(saved.revision(), document);
    }
}
