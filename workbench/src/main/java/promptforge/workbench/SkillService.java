package promptforge.workbench;

import java.util.List;

/** Application entry point: coordinates bundle operations, approval and evaluation. */
public final class SkillService {
    private final SkillBundleStore bundles;
    private final SkillTransfer transfers;
    private final SkillEvaluationService evaluations;

    public SkillService(ProjectRegistry projects) {
        bundles = new SkillBundleStore(projects);
        transfers = new SkillTransfer(bundles);
        evaluations = new SkillEvaluationService(bundles);
    }

    public SkillDetails get(String projectId, String id) {
        return withEvaluations(projectId, bundles.get(projectId, id));
    }

    public List<SkillDetails> list(String projectId) {
        return bundles.list(projectId).stream().map(skill -> withEvaluations(projectId, skill)).toList();
    }

    public SkillDetails importSkill(String projectId, String id, String source) {
        return withEvaluations(projectId, transfers.importSkill(projectId, id, source));
    }

    public ChangePreview diff(String projectId, String id, String revision, String raw) {
        return bundles.diff(projectId, id, revision, raw);
    }

    public SkillDetails save(String projectId, String id, String revision, String raw) {
        return withEvaluations(projectId, bundles.save(projectId, id, revision, raw));
    }

    public String export(String projectId, String id, String revision, String target) {
        return transfers.export(projectId, id, revision, target);
    }

    public EvaluationView addEvaluation(String projectId, String id, EvaluationArtifact artifact) {
        return evaluations.addEvaluation(projectId, id, artifact);
    }

    public SkillDetails transition(String projectId, String id, String revision, String action) {
        SkillDetails current = get(projectId, id);
        bundles.requireCurrent(current, revision);
        WorkspaceFiles files = bundles.files(projectId);
        String path = bundles.locate(files, id);
        SkillMetadata meta = bundles.metadata(files, path, revision);
        SkillMetadata updated;
        switch (action) {
            case "review" -> updated = meta.review(revision);
            case "approve" -> {
                if (!revision.equals(meta.reviewedHash())) {
                    throw new WorkbenchException(FailureKind.CONFLICT, "必須先 review 目前版本。");
                }
                updated = meta.approve(revision);
            }
            case "activate" -> {
                if (!revision.equals(meta.approvedHash())) {
                    throw new WorkbenchException(FailureKind.CONFLICT, "必須先 approve 目前版本。");
                }
                updated = meta.activate(revision);
            }
            default -> throw new WorkbenchException(FailureKind.INVALID_INPUT, "不支援的狀態操作。");
        }
        // Only explicit human transitions advance the review and approval hashes.
        bundles.writeMetadata(files, path, updated);
        return get(projectId, id);
    }

    private SkillDetails withEvaluations(String projectId, SkillDetails skill) {
        return new SkillDetails(skill.id(), skill.name(), skill.description(), skill.revision(),
                skill.raw(), skill.state(), skill.origin(), skill.references(),
                evaluations.evaluations(bundles.files(projectId), skill.id(), skill.revision()));
    }
}
