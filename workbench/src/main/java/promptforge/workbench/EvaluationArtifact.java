package promptforge.workbench;

import java.util.List;

public record EvaluationArtifact(String id, String subjectHash, Kind kind, String model, String evaluator,
                                 String timestamp, String rubricVersion, List<String> findings,
                                 List<Evidence> evidence) {
    public enum Kind { TEST, MODEL_REVIEW }
    public enum EvidenceStatus { PASS, FAIL, SKIP }
    public record Evidence(String path, EvidenceStatus status) {}
}
