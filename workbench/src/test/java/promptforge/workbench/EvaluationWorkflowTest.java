package promptforge.workbench;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EvaluationWorkflowTest {
    @TempDir Path root;
    private SkillService service() { return new SkillService(new ProjectRegistry(List.of(new Project("demo", "Demo", root)))); }
    private void inbox() throws Exception {
        Path inbox = Files.createDirectories(root.resolve(".promptforge/inbox/sample"));
        Files.writeString(inbox.resolve("SKILL.md"), "---\nname: sample\ndescription: Sample\n---\n# Sample\n");
        Files.writeString(inbox.resolve("check.txt"), "evidence");
    }

    @Test void testEvaluationStoresEvidenceDigestAndBecomesStaleAfterEdit() throws Exception {
        inbox(); SkillService service = service();
        SkillDetails skill = service.importSkill("demo", "sample", "sample");
        EvaluationArtifact artifact = new EvaluationArtifact("test-1", skill.revision(), EvaluationArtifact.Kind.TEST,
                null, "reviewer", Instant.parse("2026-09-26T02:00:00Z").toString(), "rubric-v1",
                List.of("Works as expected"), List.of(new EvaluationArtifact.Evidence(".promptforge/inbox/sample/check.txt", EvaluationArtifact.EvidenceStatus.PASS)));
        EvaluationView result = service.addEvaluation("demo", "sample", artifact);
        assertFalse(result.stale());
        assertEquals(RevisionFileStore.hash("evidence"), result.evidence().get(0).sha256());
        assertEquals(1, service.get("demo", "sample").evaluations().size());
        service.save("demo", "sample", skill.revision(), skill.raw() + "Changed\n");
        assertTrue(service.get("demo", "sample").evaluations().get(0).stale());
    }

    @Test void modelReviewNeedsFindingAndEvaluationDoesNotApprove() throws Exception {
        inbox(); SkillService service = service();
        SkillDetails skill = service.importSkill("demo", "sample", "sample");
        EvaluationArtifact invalid = new EvaluationArtifact("review-1", skill.revision(), EvaluationArtifact.Kind.MODEL_REVIEW,
                null, "reviewer", Instant.now().toString(), "rubric-v1", List.of(), List.of());
        assertThrows(WorkbenchException.class, () -> service.addEvaluation("demo", "sample", invalid));
        EvaluationArtifact valid = new EvaluationArtifact("review-1", skill.revision(), EvaluationArtifact.Kind.MODEL_REVIEW,
                null, "reviewer", Instant.now().toString(), "rubric-v1", List.of("Needs clearer constraints"), List.of());
        service.addEvaluation("demo", "sample", valid);
        assertEquals("IMPORTED", service.get("demo", "sample").state());
        assertEquals(409, assertThrows(WorkbenchException.class, () -> service.transition("demo", "sample", skill.revision(), "approve")).status());
    }

    @Test void staleRevisionsConflictAndLocalSkillsAreListed() throws Exception {
        inbox(); SkillService service = service();
        Path local = Files.createDirectories(root.resolve(".promptforge/skills/local/local-one"));
        Files.writeString(local.resolve("SKILL.md"), "---\nname: local-one\ndescription: Local\n---\n# Local\n");
        assertEquals("local", service.get("demo", "local-one").origin());
        assertTrue(service.list("demo").stream().anyMatch(s -> s.id().equals("local-one")));
        assertEquals(409, assertThrows(WorkbenchException.class,
                () -> service.importSkill("demo", "local-one", "sample")).status());
        SkillDetails imported = service.importSkill("demo", "sample", "sample");
        service.save("demo", "sample", imported.revision(), imported.raw() + "changed\n");
        assertEquals(409, assertThrows(WorkbenchException.class, () -> service.diff("demo", "sample", imported.revision(), "new")).status());
        assertEquals(409, assertThrows(WorkbenchException.class, () -> service.transition("demo", "sample", imported.revision(), "review")).status());
        assertEquals(409, assertThrows(WorkbenchException.class, () -> service.export("demo", "sample", imported.revision(), "stale")).status());
    }

    @Test void evaluationEvidenceIsLimitedAndRecordsCannotBeOverwritten() throws Exception {
        inbox(); SkillService service = service(); SkillDetails skill = service.importSkill("demo", "sample", "sample");
        var evidence = List.of(new EvaluationArtifact.Evidence(".promptforge/inbox/sample/check.txt", EvaluationArtifact.EvidenceStatus.PASS));
        EvaluationArtifact artifact = new EvaluationArtifact("test-one", skill.revision(), EvaluationArtifact.Kind.TEST,
                "model", "reviewer", Instant.now().toString(), "v1", List.of(), evidence);
        service.addEvaluation("demo", "sample", artifact);
        assertEquals(409, assertThrows(WorkbenchException.class, () -> service.addEvaluation("demo", "sample", artifact)).status());
        var tooMany = java.util.stream.IntStream.range(0, 21).mapToObj(i -> evidence.get(0)).toList();
        EvaluationArtifact invalid = new EvaluationArtifact("test-two", skill.revision(), EvaluationArtifact.Kind.TEST,
                "model", "reviewer", Instant.now().toString(), "v1", List.of(), tooMany);
        assertThrows(WorkbenchException.class, () -> service.addEvaluation("demo", "sample", invalid));
    }
}
