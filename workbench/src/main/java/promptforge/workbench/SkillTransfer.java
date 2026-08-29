package promptforge.workbench;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;

/** Stages imports and exports before publishing a complete directory. */
final class SkillTransfer {
    private final SkillBundleStore bundles;
    SkillTransfer(SkillBundleStore bundles) { this.bundles = bundles; }
    public SkillDetails importSkill(String projectId, String id, String sourceInboxName) {
        WorkbenchException.id(id);
        WorkbenchException.id(sourceInboxName);
        WorkspaceFiles files = bundles.files(projectId);
        String source = ".promptforge/inbox/" + sourceInboxName;
        List<String> sourceFiles = files.filesUnder(source);
        WorkbenchException.require(sourceFiles.contains(source + "/SKILL.md"), "來源資料夾需要 SKILL.md。");
        if (Files.exists(files.resolve(".promptforge/skills/local/" + id + "/SKILL.md"))) {
            throw new WorkbenchException(FailureKind.CONFLICT, "Skill ID 已存在，不能匯入重複項目。");
        }
        String dest = ".promptforge/skills/imports/" + id;
        Path destination = files.resolve(dest);
        if (Files.exists(destination)) throw new WorkbenchException(FailureKind.CONFLICT, "Skill 已存在，不能覆寫。");
        Path stagingRoot = files.resolve(".promptforge/staging");
        Path stage = null;
        try {
            Files.createDirectories(stagingRoot);
            stage = Files.createTempDirectory(stagingRoot, "skill-");
            for (String sourceFile : sourceFiles) {
                String content = files.read(sourceFile);
                String rel = sourceFile.substring(source.length() + 1);
                Path target = stage.resolve(rel).normalize();
                WorkbenchException.require(target.startsWith(stage), "來源檔名無效。");
                Files.createDirectories(target.getParent());
                Files.writeString(target, content, StandardCharsets.UTF_8);
            }
            String raw = Files.readString(stage.resolve("SKILL.md"), StandardCharsets.UTF_8);
            SkillParser.parse(raw);
            Files.createDirectories(destination.getParent());
            if (Files.exists(destination)) {
                throw new WorkbenchException(FailureKind.CONFLICT, "Skill 已存在，不能覆寫。");
            }
            Files.move(stage, destination, StandardCopyOption.ATOMIC_MOVE);
            stage = null;
            String hash = bundles.bundleHash(files, dest);
            bundles.writeMetadata(files, dest, SkillMetadata.imported(hash));
            return bundles.get(projectId, id);
        } catch (WorkbenchException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new WorkbenchException(FailureKind.STORAGE_FAILURE, "無法匯入 Skill。");
        } finally {
            if (stage != null) safeDeleteStage(files, stagingRoot, stage);
        }
    }

    public String export(String projectId, String id, String revision, String target) {
        WorkbenchException.id(target);
        SkillDetails current = bundles.get(projectId, id);
        bundles.requireCurrent(current, revision);
        WorkspaceFiles files = bundles.files(projectId);
        String source = bundles.locate(files, id);
        Path destination = files.resolve(".promptforge/exports/" + target);
        if (!revision.equals(bundles.metadata(files, source, revision).approvedHash())) throw new WorkbenchException(FailureKind.CONFLICT, "只能匯出已批准的目前版本。");
        WorkbenchException.require(destination.startsWith(files.resolve(".promptforge/exports")), "匯出路徑無效。");
        if (Files.exists(destination)) {
            throw new WorkbenchException(FailureKind.CONFLICT, "匯出目的地已存在，不能覆寫。");
        }
        Path stageRoot = files.resolve(".promptforge/staging");
        Path stage = null;
        try {
            Files.createDirectories(stageRoot);
            stage = Files.createTempDirectory(stageRoot, "export-");
            for (String file : bundles.skillFiles(files, source)) {
                String relative = file.substring(source.length() + 1);
                Path stagedFile = stage.resolve(relative).normalize();
                WorkbenchException.require(stagedFile.startsWith(stage), "bundle 路徑無效。");
                Files.createDirectories(stagedFile.getParent());
                Files.copy(files.resolve(file), stagedFile);
            }
            // Recheck after staging so a concurrent edit cannot export an unapproved bundle.
            if (!bundles.bundleHash(files, source).equals(revision)) {
                throw new WorkbenchException(FailureKind.CONFLICT, "匯出期間 Skill 已改變，未輸出內容。");
            }
            Files.createDirectories(destination.getParent());
            if (Files.exists(destination)) {
                throw new WorkbenchException(FailureKind.CONFLICT, "匯出目的地已存在，不能覆寫。");
            }
            Files.move(stage, destination, StandardCopyOption.ATOMIC_MOVE);
            stage = null;
            return files.root().relativize(destination).toString().replace('\\', '/');
        } catch (WorkbenchException e) {
            throw e;
        } catch (IOException e) {
            throw new WorkbenchException(FailureKind.STORAGE_FAILURE, "無法匯出 Skill。");
        } finally {
            if (stage != null) safeDeleteStage(files, stageRoot, stage);
        }
    }

    private void safeDeleteStage(WorkspaceFiles files, Path stageRoot, Path stage) {
        Path allowedRoot = stageRoot.toAbsolutePath().normalize();
        Path target = stage.toAbsolutePath().normalize();
        if (!target.startsWith(allowedRoot) || target.equals(allowedRoot)) {
            return;
        }
        try {
            if (!Files.exists(target) || Files.isSymbolicLink(target)) {
                return;
            }
            try (var walk = Files.walk(target)) {
                List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
                for (Path path : paths) {
                    if (Files.isSymbolicLink(path)) {
                        continue;
                    }
                    String relative = files.root().relativize(path).toString().replace('\\', '/');
                    if (!files.resolve(relative).startsWith(allowedRoot)) {
                        return;
                    }
                    Files.deleteIfExists(path);
                }
            }
        } catch (IOException | WorkbenchException ignored) {
            // Keep any path that cannot be verified inside the project's staging directory.
        }
    }
}
