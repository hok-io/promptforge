package promptforge.workbench;

import java.util.List;

public record EvaluationView(String id, String subjectHash, EvaluationArtifact.Kind kind, String model,
                             String evaluator, String timestamp, String rubricVersion, List<String> findings,
                             List<Evidence> evidence, boolean stale) {
    public record Evidence(String path, EvaluationArtifact.EvidenceStatus status, String sha256) {}
}
