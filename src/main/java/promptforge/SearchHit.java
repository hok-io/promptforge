package promptforge;

public final class SearchHit {
    private final KnowledgeCard card;
    private final double score;

    public SearchHit(KnowledgeCard card, double score) {
        this.card = card;
        this.score = score;
    }

    public KnowledgeCard card() { return card; }
    public double score() { return score; }
}
