package promptforge;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class KnowledgeQueryService {
    public List<SearchHit> search(Path project, String query) throws IOException {
        if (query == null || query.isBlank() || query.length() > 4000) throw new IllegalArgumentException("query must be 1-4000 characters");
        return new Bm25Index(new KnowledgeCardLoader().load(project)).search(query, 3);
    }
}
