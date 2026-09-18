package promptforge.web;

import promptforge.workbench.*;
import java.nio.file.*;
import java.util.*;

/** Shallow discovery: large bundled data files are not read to list Skills. */
final class SkillCatalog {
    static List<String> names(WorkspaceFiles files) {
        Set<String> names = new TreeSet<>();
        Path directory = files.resolve(".agents/skills");
        if (Files.isDirectory(directory)) try (var children = Files.list(directory)) {
            for (Path child : children.toList()) {
                String name = child.getFileName().toString();
                if (name.equals("PromptForge") || name.equals("PromptForge-context") || name.matches("[a-z][a-z0-9-]{0,62}")) {
                    if (Files.isRegularFile(files.resolve(".agents/skills/" + name + "/SKILL.md"))) names.add(name);
                }
            }
        } catch (java.io.IOException error) { throw new WorkbenchException(FailureKind.STORAGE_FAILURE, "無法讀取 Skill 庫。"); }
        return List.copyOf(names);
    }
}
