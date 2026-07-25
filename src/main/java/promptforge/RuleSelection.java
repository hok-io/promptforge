package promptforge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Human overrides of lexical suggestions; never infers missing task intent. */
public final class RuleSelection {
    public List<Rule> select(String task, List<Rule> available, String explicit, String excluded) {
        List<String> includeIds = explicit == null ? null : parseIds(explicit);
        List<String> excludeIds = excluded == null ? List.of() : parseIds(excluded);
        Set<String> known = new HashSet<>();
        for (Rule rule : available) known.add(rule.id());
        if (includeIds != null) validateKnown(includeIds, known);
        validateKnown(excludeIds, known);
        if (includeIds != null) {
            for (String id : includeIds) {
                if (excludeIds.contains(id)) throw new IllegalArgumentException("rule both selected and excluded: " + id);
            }
        }
        List<Rule> selected = new ArrayList<>();
        List<Rule> candidates = includeIds == null ? new RuleSelector().select(task, available) : available;
        for (Rule rule : candidates) {
            if ((includeIds == null || includeIds.contains(rule.id())) && !excludeIds.contains(rule.id())) selected.add(rule);
        }
        selected.sort(Comparator.comparing(Rule::id));
        return List.copyOf(selected);
    }

    private List<String> parseIds(String csv) {
        if (csv.isEmpty()) return List.of();
        String[] parts = csv.split(",", -1);
        if (parts.length > 20) throw new IllegalArgumentException("select at most 20 rule IDs");
        List<String> ids = new ArrayList<>();
        for (String part : parts) {
            String id = part.trim();
            if (id.isEmpty() || ids.contains(id)) throw new IllegalArgumentException("empty or duplicate rule ID: " + id);
            ids.add(id);
        }
        return List.copyOf(ids);
    }

    private void validateKnown(List<String> ids, Set<String> known) {
        for (String id : ids) if (!known.contains(id)) throw new IllegalArgumentException("unknown rule ID: " + id);
    }
}
