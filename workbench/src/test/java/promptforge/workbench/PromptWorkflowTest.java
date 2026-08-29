package promptforge.workbench;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import promptforge.FrameService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PromptWorkflowTest {
    @TempDir Path root;
    private PromptDocument document(String task) {
        return new PromptDocument("demo", "Demo", task, List.of(), Map.of(), List.of("payment.retry"), List.of(), List.of(), "frame");
    }
    @Test void cliAndWorkbenchSharePreviewAndSaveWithoutOverwrite() throws Exception {
        Path rules = Files.createDirectories(root.resolve(".promptforge/rules"));
        Files.writeString(rules.resolve("payment.rule"), "id=payment.retry\nkeywords=付款,重試\ninstruction=保留冪等性\n");
        Project project = new Project("demo", "Demo", root);
        FrameService core = new FrameService();
        PromptPreviewService preview = new PromptPreviewService(core);
        assertEquals(core.prepare(root, "支付失敗", "payment.retry", "", ""), preview.preview(project, document("支付失敗")));
        PromptRepository repository = new JsonPromptRepository(new WorkspaceFiles(root));
        ChangePreview diff = repository.diff(document("支付失敗"), "new");
        assertTrue(diff.changed()); assertEquals("", diff.before());
        assertFalse(Files.exists(root.resolve(".promptforge/prompts/demo/document.json")), "diff must not save");
        PromptVersion first = repository.save(document("支付失敗"), "new");
        assertEquals("支付失敗", repository.load("demo").document().task());
        repository.save(document("支付失敗，確認狀態"), first.revision());
        assertEquals(FailureKind.CONFLICT, assertThrows(WorkbenchException.class, () -> repository.save(document("old"), first.revision())).kind());
        assertEquals(1, repository.list().size());
    }
}
