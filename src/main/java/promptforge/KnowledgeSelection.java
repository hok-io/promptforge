package promptforge;

import java.util.ArrayList;
import java.util.List;

/** Resolves confirmed source names only against already-loaded project cards. */
public final class KnowledgeSelection {
    public List<KnowledgeCard> select(List<KnowledgeCard> available, String csv) {
        if (csv.isEmpty()) return List.of();
        String[] sources = csv.split(",", -1);
        if (sources.length > 3) throw new IllegalArgumentException("select at most 3 knowledge cards");
        List<String> seen = new ArrayList<>();
        List<KnowledgeCard> selected = new ArrayList<>();
        for (String part : sources) {
            String source = part.trim().replace('\\', '/');
            if (source.isEmpty() || seen.contains(source)) throw new IllegalArgumentException("empty or duplicate knowledge source: " + source);
            KnowledgeCard match = null;
            for (KnowledgeCard card : available) if (card.source().equals(source)) { match = card; break; }
            if (match == null) throw new IllegalArgumentException("unknown knowledge source: " + source);
            seen.add(source);
            selected.add(match);
        }
        return List.copyOf(selected);
    }
}
