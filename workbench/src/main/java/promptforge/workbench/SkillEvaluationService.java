package promptforge.workbench;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/** Validates external evidence and persists evaluation records without granting approval. */
final class SkillEvaluationService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern HASH = Pattern.compile("[a-f0-9]{64}");
    private final SkillBundleStore bundles;
    SkillEvaluationService(SkillBundleStore bundles) { this.bundles = bundles; }
    public EvaluationView addEvaluation(String projectId, String id, EvaluationArtifact artifact) {
        WorkbenchException.id(id);
        WorkspaceFiles files = bundles.files(projectId);
        String skillPath = bundles.locate(files, id);
        WorkbenchException.require(artifact != null, "評估不可為空。");
        WorkbenchException.id(artifact.id());
        WorkbenchException.require(artifact.subjectHash() != null && HASH.matcher(artifact.subjectHash()).matches(),
                "subjectHash 必須是 SHA-256。");
        WorkbenchException.require(artifact.kind() != null, "評估 kind 無效。");
        WorkbenchException.require(artifact.evaluator() != null && !artifact.evaluator().isBlank(), "evaluator 不可空白。");
        WorkbenchException.require(artifact.rubricVersion() != null && !artifact.rubricVersion().isBlank(), "rubricVersion 不可空白。");
        try {
            Instant.parse(artifact.timestamp());
        } catch (Exception e) {
            throw new WorkbenchException(FailureKind.INVALID_INPUT, "timestamp 必須是 ISO-8601 時間。");
        }
        List<String> findings = artifact.findings() == null ? List.of() : List.copyOf(artifact.findings());
        WorkbenchException.require(findings.size() <= 20
                        && findings.stream().allMatch(f -> f != null && f.length() <= 2000),
                "findings 最多 20 項，每項最多 2000 字元。");
        List<EvaluationView.Evidence> evidence = new ArrayList<>();
        List<EvaluationArtifact.Evidence> inputEvidence = artifact.evidence() == null ? List.of() : artifact.evidence();
        WorkbenchException.require(inputEvidence.size() <= 20, "evidence 最多 20 項。");
        WorkbenchException.require(artifact.evaluator().length() <= 200
                        && (artifact.model() == null || artifact.model().length() <= 200)
                        && artifact.rubricVersion().length() <= 200,
                "evaluator、model、rubricVersion 最多 200 字元。");
        for (EvaluationArtifact.Evidence item : inputEvidence) {
            WorkbenchException.require(item != null && item.status() != null, "證據狀態無效。");
            String body = files.read(item.path());
            evidence.add(new EvaluationView.Evidence(item.path(), item.status(), RevisionFileStore.hash(body)));
        }
        if (artifact.kind() == EvaluationArtifact.Kind.TEST) {
            WorkbenchException.require(!evidence.isEmpty(), "TEST 評估至少需要一個既存證據檔。");
        }
        if (artifact.kind() == EvaluationArtifact.Kind.MODEL_REVIEW) {
            WorkbenchException.require(!findings.isEmpty(), "MODEL_REVIEW 至少需要一項 finding。");
        }
        boolean stale = !bundles.bundleHash(files, skillPath).equals(artifact.subjectHash());
        EvaluationView view = new EvaluationView(artifact.id(), artifact.subjectHash(), artifact.kind(),
                artifact.model(), artifact.evaluator(), artifact.timestamp(), artifact.rubricVersion(),
                findings, List.copyOf(evidence), stale);
        String rel = ".promptforge/evaluations/" + id + "/" + artifact.subjectHash() + "/" + artifact.id() + ".json";
        Path destination = files.resolve(rel);
        if (Files.exists(destination)) {
            throw new WorkbenchException(FailureKind.CONFLICT, "評估 ID 已存在，不能覆寫。");
        }
        try {
            Files.createDirectories(destination.getParent());
            new RevisionFileStore(files).save(rel, "new", JSON.writeValueAsString(view));
        } catch (WorkbenchException e) {
            if (e.kind() == FailureKind.CONFLICT) {
                throw new WorkbenchException(FailureKind.CONFLICT, "評估 ID 已存在，不能覆寫。");
            }
            throw e;
        } catch (IOException e) {
            throw new WorkbenchException(FailureKind.STORAGE_FAILURE, "無法保存評估。");
        }
        return view;
    }

    List<EvaluationView> evaluations(WorkspaceFiles files, String id, String currentHash) {
        String base = ".promptforge/evaluations/" + id;
        if (!Files.exists(files.resolve(base))) {
            return List.of();
        }
        List<String> records = files.filesUnder(base);
        WorkbenchException.require(records.size() <= 128, "每個 Skill 最多保存 128 筆評估。");
        try {
            List<EvaluationView> views = new ArrayList<>();
            for (String relative : records) {
                EvaluationView saved = JSON.readValue(files.read(relative), EvaluationView.class);
                boolean stale = !currentHash.equals(saved.subjectHash());
                views.add(new EvaluationView(saved.id(), saved.subjectHash(), saved.kind(), saved.model(),
                        saved.evaluator(), saved.timestamp(), saved.rubricVersion(), saved.findings(),
                        saved.evidence(), stale));
            }
            views.sort(Comparator.comparing(EvaluationView::timestamp).reversed());
            return List.copyOf(views);
        } catch (IOException e) {
            throw new WorkbenchException(FailureKind.STORAGE_FAILURE, "無法讀取評估紀錄。");
        }
    }
}
