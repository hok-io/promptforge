package promptforge.workbench;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PromptDomainTest {
    @Test void orderedCustomBlocksAndLiteralVariables() {
        PromptDocument doc = new PromptDocument("draft", "付款任務", "修正付款", List.of(
                new PromptBlock("背景", "專案 ${project}"), new PromptBlock("驗收", "不能重複扣款")),
                Map.of("project", "${literal}"), List.of(), List.of(), List.of(), "template");
        String compiled = new PromptTemplateCompiler().compile(doc);
        assertTrue(compiled.indexOf("背景") < compiled.indexOf("驗收"));
        assertTrue(compiled.contains("專案 ${literal}"), "replacement does not recursively execute another template");
        assertFalse(compiled.contains("角色"), "no mandatory generic role");
    }
    @Test void missingVariableAndInvalidDocumentAreReadable() {
        PromptDocument doc = new PromptDocument("draft", "任務", "${missing}", List.of(), Map.of(), null, List.of(), List.of(), "frame");
        WorkbenchException error = assertThrows(WorkbenchException.class, () -> new PromptTemplateCompiler().compile(doc));
        assertEquals(FailureKind.INVALID_INPUT, error.kind());
        assertTrue(error.getMessage().contains("missing"));
        assertThrows(WorkbenchException.class, () -> new PromptDocument("../outside", "x", "x", List.of(), Map.of(), null, List.of(), List.of(), "frame"));
    }
    @Test void automaticAndExplicitEmptySelectionRemainDifferent() {
        PromptDocument auto = new PromptDocument("draft", "任務", "付款重試", List.of(), Map.of(), null, List.of(), List.of(), "frame");
        PromptDocument none = new PromptDocument("draft", "任務", "付款重試", List.of(), Map.of(), List.of(), List.of(), List.of(), "frame");
        assertNull(auto.ruleIds()); assertEquals(List.of(), none.ruleIds());
    }
}
