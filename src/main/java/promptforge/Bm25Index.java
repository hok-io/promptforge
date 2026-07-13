package promptforge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small, deterministic lexical index; no model or network call. */
public final class Bm25Index {
    private static final Pattern TOKEN = Pattern.compile("[a-zA-Z][a-zA-Z0-9_-]*|[\\p{IsHan}]+");
    private final List<KnowledgeCard> cards;
    private final List<Map<String, Integer>> frequencies = new ArrayList<>();
    private final Map<String, Integer> documentFrequency = new HashMap<>();
    private final List<Integer> lengths = new ArrayList<>();
    private final double averageLength;

    public Bm25Index(List<KnowledgeCard> cards) {
        this.cards = List.copyOf(cards);
        int totalLength = 0;
        for (KnowledgeCard card : this.cards) {
            Map<String, Integer> frequency = new HashMap<>();
            List<String> tokens = tokenize(card.title() + " " + card.body());
            for (String token : tokens) frequency.merge(token, 1, Integer::sum);
            frequencies.add(frequency);
            lengths.add(tokens.size());
            totalLength += tokens.size();
            for (String token : frequency.keySet()) {
                documentFrequency.merge(token, 1, Integer::sum);
            }
        }
        averageLength = this.cards.isEmpty() ? 1.0 : (double) totalLength / this.cards.size();
    }

    public List<SearchHit> search(String query, int limit) {
        if (query == null || query.isBlank() || limit <= 0) return List.of();
        Set<String> terms = new HashSet<>(tokenize(query));
        List<SearchHit> hits = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            double score = 0.0;
            for (String term : terms) {
                int tf = frequencies.get(i).getOrDefault(term, 0);
                if (tf == 0) continue;
                int df = documentFrequency.get(term);
                double idf = Math.log(1.0 + (cards.size() - df + 0.5) / (df + 0.5));
                double norm = 0.25 + 0.75 * lengths.get(i) / averageLength;
                score += idf * tf * 2.2 / (tf + 1.2 * norm);
                if (cards.get(i).title().toLowerCase(Locale.ROOT).contains(term)) score += idf * 0.8;
            }
            if (score > 0.0) hits.add(new SearchHit(cards.get(i), score));
        }
        hits.sort(Comparator.comparingDouble(SearchHit::score).reversed()
                .thenComparing(hit -> hit.card().source()));
        return List.copyOf(hits.subList(0, Math.min(limit, hits.size())));
    }

    static List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(input.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String word = matcher.group();
            int[] points = word.codePoints().toArray();
            if (points.length > 2 && Character.UnicodeScript.of(points[0]) == Character.UnicodeScript.HAN) {
                for (int i = 0; i < points.length - 1; i++) {
                    tokens.add(new String(points, i, 2));
                }
            } else {
                tokens.add(word);
            }
        }
        return tokens;
    }
}
