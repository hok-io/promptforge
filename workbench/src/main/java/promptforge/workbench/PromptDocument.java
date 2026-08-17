package promptforge.workbench;

import java.util.List;
import java.util.Map;

/** Immutable editor document. Null ruleIds means automatic; an empty list means none. */
public record PromptDocument(String id, String name, String task, List<PromptBlock> blocks,
                             Map<String, String> variables, List<String> ruleIds, List<String> excludedRuleIds,
                             List<String> knowledgeSources, String mode) {
    public PromptDocument {
        WorkbenchException.id(id);
        WorkbenchException.require(name != null && !name.isBlank() && name.length() <= 100, "Prompt 名稱需要 1–100 字。");
        WorkbenchException.require(task != null && task.length() <= 4000, "任務最多 4000 字。");
        blocks = blocks == null ? List.of() : List.copyOf(blocks);
        WorkbenchException.require(blocks.size() <= 20, "最多 20 個區塊。");
        variables = variables == null ? Map.of() : Map.copyOf(variables);
        WorkbenchException.require(variables.size() <= 20, "最多 20 個變數。");
        for (var entry : variables.entrySet()) {
            WorkbenchException.require(entry.getKey().matches("[A-Za-z][A-Za-z0-9_]{0,39}") && entry.getValue().length() <= 2000, "變數名稱無效或值超過 2000 字。");
        }
        ruleIds = ruleIds == null ? null : List.copyOf(ruleIds);
        excludedRuleIds = excludedRuleIds == null ? List.of() : List.copyOf(excludedRuleIds);
        knowledgeSources = knowledgeSources == null ? List.of() : List.copyOf(knowledgeSources);
        WorkbenchException.require((ruleIds == null || ruleIds.size() <= 20) && excludedRuleIds.size() <= 20 && knowledgeSources.size() <= 3, "規則最多 20 項，知識最多 3 項。");
        WorkbenchException.require("frame".equals(mode) || "template".equals(mode), "請選 frame 或 template 模式。");
    }
}
