package promptforge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Selects only rules whose complete keyword set appears in the task. */
public final class RuleSelector {
    public List<Rule> select(String task, List<Rule> rules) {
        Objects.requireNonNull(rules, "rules");
        if (task == null || task.isBlank()) return List.of();
        String normalizedTask = task.toLowerCase(Locale.ROOT);
        List<Rule> selected = new ArrayList<>();
        for (Rule rule : rules) {
            boolean matches = true;
            for (String keyword : rule.keywords()) {
                if (!normalizedTask.contains(keyword.toLowerCase(Locale.ROOT))) {
                    matches = false;
                    break;
                }
            }
            if (matches) selected.add(rule);
        }
        selected.sort(Comparator.comparing(Rule::id));
        return List.copyOf(selected);
    }
}
