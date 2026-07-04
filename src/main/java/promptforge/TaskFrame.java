package promptforge;

import java.util.List;

/** The selected rules and the exact text that can be copied into an agent. */
public final class TaskFrame {
    private final String text;
    private final List<Rule> rules;

    public TaskFrame(String text, List<Rule> rules) {
        this.text = text;
        this.rules = List.copyOf(rules);
    }

    public String text() { return text; }
    public List<Rule> rules() { return rules; }
    public boolean bypassed() { return rules.isEmpty(); }
}
