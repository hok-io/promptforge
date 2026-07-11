package promptforge;

/** A short, source-linked Markdown document for local retrieval. */
public final class KnowledgeCard {
    private final String title;
    private final String body;
    private final String source;

    public KnowledgeCard(String title, String body, String source) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title must not be blank");
        if (body == null || body.isBlank()) throw new IllegalArgumentException("body must not be blank");
        if (source == null || source.isBlank()) throw new IllegalArgumentException("source must not be blank");
        this.title = title;
        this.body = body;
        this.source = source;
    }

    public String title() { return title; }
    public String body() { return body; }
    public String source() { return source; }
}
