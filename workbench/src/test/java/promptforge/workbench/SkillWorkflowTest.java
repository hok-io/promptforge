package promptforge.workbench;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SkillWorkflowTest {
    @TempDir Path root;
    private SkillService service() { return new SkillService(new ProjectRegistry(List.of(new Project("demo", "Demo", root)))); }
    private void inbox() throws Exception {
        Path inbox = Files.createDirectories(root.resolve(".promptforge/inbox/prompt-frame/references"));
        Files.writeString(inbox.getParent().resolve("SKILL.md"), "---\nname: prompt-frame\ndescription: 整理任務\ncustom: 保留未知欄位\n---\n\n# Prompt\n參考 references/policy.md\n");
        Files.writeString(inbox.resolve("policy.md"), "# Policy\n保留來源。");
    }
    @Test void importedIsNotApprovedAndEditsInvalidateReview() throws Exception {
        inbox(); SkillService service = service();
        SkillDetails imported = service.importSkill("demo", "prompt-frame", "prompt-frame");
        assertEquals("IMPORTED", imported.state()); assertEquals(1, imported.references().size());
        assertTrue(imported.raw().contains("custom: 保留未知欄位"));
        assertEquals(409, assertThrows(WorkbenchException.class, () -> service.transition("demo", "prompt-frame", imported.revision(), "approve")).status());
        SkillDetails reviewed = service.transition("demo", "prompt-frame", imported.revision(), "review");
        assertEquals("REVIEWED", reviewed.state());
        SkillDetails approved = service.transition("demo", "prompt-frame", reviewed.revision(), "approve");
        assertEquals("APPROVED", approved.state());
        assertEquals("ACTIVE", service.transition("demo", "prompt-frame", approved.revision(), "activate").state());
        SkillDetails edited = service.save("demo", "prompt-frame", approved.revision(), approved.raw() + "\n新指令\n");
        assertEquals("IMPORTED", edited.state());
        assertEquals(409, assertThrows(WorkbenchException.class, () -> service.save("demo", "prompt-frame", approved.revision(), "old")).status());
    }
    @Test void referenceChangesAlsoInvalidateApprovalAndExportNeverOverwrites() throws Exception {
        inbox(); SkillService service = service();
        SkillDetails skill = service.importSkill("demo", "prompt-frame", "prompt-frame");
        service.transition("demo", "prompt-frame", skill.revision(), "review");
        service.transition("demo", "prompt-frame", skill.revision(), "approve");
        String exported = service.export("demo", "prompt-frame", skill.revision(), "review-copy");
        assertTrue(Files.exists(root.resolve(exported).resolve("SKILL.md")));
        assertEquals(409, assertThrows(WorkbenchException.class, () -> service.export("demo", "prompt-frame", skill.revision(), "review-copy")).status());
        Files.writeString(root.resolve(".promptforge/skills/imports/prompt-frame/references/policy.md"), "外部更新");
        assertEquals("IMPORTED", service.get("demo", "prompt-frame").state());
        assertThrows(WorkbenchException.class, () -> service.importSkill("demo", "other", "../outside"));
    }
}
