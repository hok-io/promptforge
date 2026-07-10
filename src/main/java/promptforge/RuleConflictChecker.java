package promptforge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Refuses to silently choose between two selected, incompatible rules. */
public final class RuleConflictChecker {
    public void check(List<Rule> selected) {
        Map<String, Rule> byId = new HashMap<>();
        for (Rule rule : selected) byId.put(rule.id(), rule);
        for (Rule rule : selected) {
            for (String otherId : rule.conflicts()) {
                Rule other = byId.get(otherId);
                if (other != null) {
                    throw new RuleConflictException("conflicting rules: " + rule.id() + " (" + rule.source()
                            + ") and " + other.id() + " (" + other.source() + ")");
                }
            }
        }
    }
}
