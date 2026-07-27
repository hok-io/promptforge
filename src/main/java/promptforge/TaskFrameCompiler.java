package promptforge;

import java.util.List;

/** Adds only applicable project rules to the original task. */
public final class TaskFrameCompiler {
    public static final int MAX_FRAME_CHARACTERS = 6000;
    private final RuleSelector selector = new RuleSelector();
    private final RuleConflictChecker conflicts = new RuleConflictChecker();

    public TaskFrame compile(String task, List<Rule> availableRules) {
        return compileSelected(task, selector.select(task, availableRules));
    }

    /** Compiles a confirmed selection without applying keyword matching again. */
    public TaskFrame compileSelected(String task, List<Rule> selected) {
        if (task == null || task.isBlank()) {
            throw new IllegalArgumentException("task must not be blank");
        }
        if (task.length() > 4000) throw new IllegalArgumentException("task exceeds 4000 characters");
        conflicts.check(selected);
        if (selected.isEmpty()) return new TaskFrame(task, selected);
        StringBuilder text = new StringBuilder(task);
        text.append("\n\n專案規則：\n");
        for (Rule rule : selected) {
            text.append("- ").append(rule.instruction())
                    .append(" [").append(rule.id()).append("; ")
                    .append(rule.source()).append("]\n");
        }
        String rendered = text.toString().stripTrailing();
        if (rendered.length() > MAX_FRAME_CHARACTERS) {
            throw new FrameBudgetException("frame exceeds 6000 characters; narrow the rule keywords");
        }
        return new TaskFrame(rendered, selected);
    }
}
