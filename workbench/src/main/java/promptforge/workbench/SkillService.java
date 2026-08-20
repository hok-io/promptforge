package promptforge.workbench;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public final class SkillService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern HASH = Pattern.compile("[a-f0-9]{64}");
    private final ProjectRegistry projects;
    public SkillService(ProjectRegistry projects) {
        this.projects = projects;
    }

    public SkillDetails importSkill(String projectId, String id, String sourceInboxName) {
        WorkbenchException.id(id);
        WorkbenchException.id(sourceInboxName);
        WorkspaceFiles files = files(projectId);
        String source = ".promptforge/inbox/" + sourceInboxName;
        List<String> sourceFiles = files.filesUnder(source);
        WorkbenchException.require(sourceFiles.contains(source + "/SKILL.md"), "來源資料夾需要 SKILL.md。");
        if (Files.exists(files.resolve(".promptforge/skills/local/" + id + "/SKILL.md"))) {
            throw new WorkbenchException(409, "Skill ID 已存在，不能匯入重複項目。");
        }
        String dest = ".promptforge/skills/imports/" + id;
        Path destination = files.resolve(dest);
        if (Files.exists(destination)) throw new WorkbenchException(409, "Skill 已存在，不能覆寫。");
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
                throw new WorkbenchException(409, "Skill 已存在，不能覆寫。");
            }
            Files.move(stage, destination, StandardCopyOption.ATOMIC_MOVE);
            stage = null;
            String hash = bundleHash(files, dest);
            writeMetadata(files, dest, SkillMetadata.imported(hash));
            return get(projectId, id);
        } catch (WorkbenchException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new WorkbenchException(500, "無法匯入 Skill。");
        } finally {
            if (stage != null) safeDeleteStage(files, stagingRoot, stage);
        }
    }

    public SkillDetails get(String projectId, String id) {
        WorkbenchException.id(id);
        WorkspaceFiles files = files(projectId);
        String path = locate(files, id);
        return details(files, id, path);
    }

    public List<SkillDetails> list(String projectId) {
        WorkspaceFiles files = files(projectId);
        Path root = files.resolve(".promptforge/skills");
        if (!Files.exists(root)) return List.of();
        List<SkillDetails> out = new ArrayList<>();
        String importedRoot = ".promptforge/skills/imports";
        for (Path item : children(files, importedRoot)) {
            String id = item.getFileName().toString();
            String relative = importedRoot + "/" + id;
            files.resolve(relative);
            if (Files.isDirectory(item) && Files.exists(files.resolve(relative + "/SKILL.md"))) {
                out.add(details(files, id, relative));
            }
        }
        String localRoot = ".promptforge/skills/local";
        for (Path item : children(files, localRoot)) {
            String id = item.getFileName().toString();
            String relative = localRoot + "/" + id;
            files.resolve(relative);
            if (Files.isDirectory(item) && Files.exists(files.resolve(relative + "/SKILL.md"))) {
                out.add(details(files, id, relative));
            }
        }
        WorkbenchException.require(out.size() <= 100, "每個專案最多列出 100 個 Skills。");
        out.sort(Comparator.comparing(SkillDetails::id));
        return List.copyOf(out);
    }

    public ChangePreview diff(String projectId, String id, String revision, String raw) {
        SkillDetails current = get(projectId, id);
        requireCurrent(current, revision);
        return new ChangePreview(current.raw(), raw, !current.raw().equals(raw));
    }

    public SkillDetails save(String projectId, String id, String revision, String raw) {
        WorkbenchException.id(id);
        SkillDetails current = get(projectId, id);
        requireCurrent(current, revision);
        SkillParser.parse(raw);
        WorkspaceFiles files = files(projectId);
        String path = locate(files, id);
        String before = bundleHash(files, path);
        if (!before.equals(revision)) throw new WorkbenchException(409, "Skill reference 已改變，請重新載入。");
        new RevisionFileStore(files).save(path + "/SKILL.md", RevisionFileStore.hash(current.raw()), raw);
        String after = bundleHash(files, path);
        writeMetadata(files, path, SkillMetadata.imported(after));
        return details(files, id, path);
    }

    public SkillDetails transition(String projectId, String id, String revision, String action) {
        SkillDetails current = get(projectId, id);
        requireCurrent(current, revision);
        WorkspaceFiles files = files(projectId);
        String path = locate(files, id);
        SkillMetadata meta = metadata(files, path, revision);
        SkillMetadata updated;
        switch (action) {
            case "review" -> updated = meta.review(revision);
            case "approve" -> {
                if (!revision.equals(meta.reviewedHash())) {
                    throw new WorkbenchException(409, "必須先 review 目前版本。");
                }
                updated = meta.approve(revision);
            }
            case "activate" -> {
                if (!revision.equals(meta.approvedHash())) {
                    throw new WorkbenchException(409, "必須先 approve 目前版本。");
                }
                updated = meta.activate(revision);
            }
            default -> throw new WorkbenchException(422, "不支援的狀態操作。");
        }
        // Only explicit human transitions advance the review and approval hashes.
        writeMetadata(files, path, updated);
        return details(files, id, path);
    }

    public String export(String projectId, String id, String revision, String target) {
        WorkbenchException.id(target);
        SkillDetails current = get(projectId, id);
        requireCurrent(current, revision);
        WorkspaceFiles files = files(projectId);
        String source = locate(files, id);
        Path destination = files.resolve(".promptforge/exports/" + target);
        if (!revision.equals(metadata(files, source, revision).approvedHash())) throw new WorkbenchException(409, "只能匯出已批准的目前版本。");
        WorkbenchException.require(destination.startsWith(files.resolve(".promptforge/exports")), "匯出路徑無效。");
        if (Files.exists(destination)) {
            throw new WorkbenchException(409, "匯出目的地已存在，不能覆寫。");
        }
        Path stageRoot = files.resolve(".promptforge/staging");
        Path stage = null;
        try {
            Files.createDirectories(stageRoot);
            stage = Files.createTempDirectory(stageRoot, "export-");
            for (String file : skillFiles(files, source)) {
                String relative = file.substring(source.length() + 1);
                Path stagedFile = stage.resolve(relative).normalize();
                WorkbenchException.require(stagedFile.startsWith(stage), "bundle 路徑無效。");
                Files.createDirectories(stagedFile.getParent());
                Files.copy(files.resolve(file), stagedFile);
            }
            // Recheck after staging so a concurrent edit cannot export an unapproved bundle.
            if (!bundleHash(files, source).equals(revision)) {
                throw new WorkbenchException(409, "匯出期間 Skill 已改變，未輸出內容。");
            }
            Files.createDirectories(destination.getParent());
            if (Files.exists(destination)) {
                throw new WorkbenchException(409, "匯出目的地已存在，不能覆寫。");
            }
            Files.move(stage, destination, StandardCopyOption.ATOMIC_MOVE);
            stage = null;
            return files.root().relativize(destination).toString().replace('\\', '/');
        } catch (WorkbenchException e) {
            throw e;
        } catch (IOException e) {
            throw new WorkbenchException(500, "無法匯出 Skill。");
        } finally {
            if (stage != null) safeDeleteStage(files, stageRoot, stage);
        }
    }

    public EvaluationView addEvaluation(String projectId, String id, EvaluationArtifact artifact) {
        WorkbenchException.id(id);
        WorkspaceFiles files = files(projectId);
        String skillPath = locate(files, id);
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
            throw new WorkbenchException(422, "timestamp 必須是 ISO-8601 時間。");
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
        boolean stale = !bundleHash(files, skillPath).equals(artifact.subjectHash());
        EvaluationView view = new EvaluationView(artifact.id(), artifact.subjectHash(), artifact.kind(),
                artifact.model(), artifact.evaluator(), artifact.timestamp(), artifact.rubricVersion(),
                findings, List.copyOf(evidence), stale);
        String rel = ".promptforge/evaluations/" + id + "/" + artifact.subjectHash() + "/" + artifact.id() + ".json";
        Path destination = files.resolve(rel);
        if (Files.exists(destination)) {
            throw new WorkbenchException(409, "評估 ID 已存在，不能覆寫。");
        }
        try {
            Files.createDirectories(destination.getParent());
            new RevisionFileStore(files).save(rel, "new", JSON.writeValueAsString(view));
        } catch (WorkbenchException e) {
            if (e.status() == 409) {
                throw new WorkbenchException(409, "評估 ID 已存在，不能覆寫。");
            }
            throw e;
        } catch (IOException e) {
            throw new WorkbenchException(500, "無法保存評估。");
        }
        return view;
    }

    private SkillDetails details(WorkspaceFiles files, String id, String path) {
        String raw = files.read(path + "/SKILL.md");
        SkillParser.Header header = SkillParser.parse(raw);
        String hash = bundleHash(files, path);
        SkillMetadata metadata = metadata(files, path, hash);
        List<SkillReference> references = new ArrayList<>();
        for (String item : skillFiles(files, path)) {
            if (!item.equals(path + "/SKILL.md")) {
                references.add(new SkillReference(item.substring(path.length() + 1), files.read(item)));
            }
        }
        String origin = path.contains("/imports/") ? "import" : "local";
        return new SkillDetails(id, header.name(), header.description(), hash, raw, metadata.state(hash),
                origin, List.copyOf(references), evaluations(files, id, hash));
    }

    private List<EvaluationView> evaluations(WorkspaceFiles files, String id, String currentHash) {
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
            throw new WorkbenchException(500, "無法讀取評估紀錄。");
        }
    }

    private String bundleHash(WorkspaceFiles files, String path) {
        StringBuilder material = new StringBuilder();
        for (String file : skillFiles(files, path)) {
            String relative = file.substring(path.length() + 1);
            String content = files.read(file);
            material.append(relative.length()).append(':').append(relative)
                    .append(':').append(content.getBytes(StandardCharsets.UTF_8).length)
                    .append(':').append(content).append('\n');
        }
        return RevisionFileStore.hash(material.toString());
    }

    private List<String> skillFiles(WorkspaceFiles files, String path) {
        return files.filesUnder(path).stream()
                .filter(file -> !file.equals(path + "/.promptforge-metadata.json"))
                .toList();
    }

    private SkillMetadata metadata(WorkspaceFiles files, String path, String current) {
        String relative = path + "/.promptforge-metadata.json";
        Path metadataPath = files.resolve(relative);
        if (!Files.exists(metadataPath)) {
            return SkillMetadata.imported(current);
        }
        try {
            SkillMetadata stored = JSON.readValue(files.read(relative), SkillMetadata.class);
            return current.equals(stored.bundleHash()) ? stored : SkillMetadata.imported(current);
        } catch (IOException e) {
            return SkillMetadata.imported(current);
        }
    }

    private void writeMetadata(WorkspaceFiles files, String path, SkillMetadata value) {
        String relative = path + "/.promptforge-metadata.json";
        try {
            Files.createDirectories(files.resolve(path));
            VersionedText previous = new RevisionFileStore(files).readOrNew(relative);
            new RevisionFileStore(files).save(relative, previous.revision(), JSON.writeValueAsString(value));
        } catch (IOException e) {
            throw new WorkbenchException(500, "無法保存 Skill 狀態。");
        }
    }

    private String locate(WorkspaceFiles files, String id) {
        String imported = ".promptforge/skills/imports/" + id;
        if (Files.exists(files.resolve(imported + "/SKILL.md"))) {
            return imported;
        }
        String local = ".promptforge/skills/local/" + id;
        if (Files.exists(files.resolve(local + "/SKILL.md"))) {
            return local;
        }
        throw new WorkbenchException(404, "找不到 Skill：" + id);
    }

    private WorkspaceFiles files(String projectId) {
        return new WorkspaceFiles(projects.require(projectId).root());
    }

    private List<Path> children(WorkspaceFiles files, String relative) {
        Path directory = files.resolve(relative);
        if (!Files.exists(directory)) {
            return List.of();
        }
        WorkbenchException.require(Files.isDirectory(directory), "Skill 清單路徑需要是資料夾。");
        try (var paths = Files.list(directory)) {
            List<Path> entries = paths.limit(101).toList();
            WorkbenchException.require(entries.size() <= 100, "Skill 資料夾最多 100 個項目。");
            return entries;
        } catch (IOException e) {
            throw new WorkbenchException(500, "無法列出 Skills。");
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

    private void requireCurrent(SkillDetails details, String revision) {
        if (revision == null || !details.revision().equals(revision)) {
            throw new WorkbenchException(409, "Skill 已改變，請重新載入。");
        }
    }
}
