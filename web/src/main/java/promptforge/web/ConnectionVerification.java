package promptforge.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import promptforge.workbench.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/** Short-lived receipts prove a token-bearing MCP read, not task correctness or client identity. */
@Service
public final class ConnectionVerification {
    private final WorkspaceFiles files;
    private final Map<String, Receipt> pending = new LinkedHashMap<>();
    public ConnectionVerification(@Value("${promptforge.home:.}") String home) { files = new WorkspaceFiles(Path.of(home)); }
    private record Receipt(String skill, String revision, Instant expires, Instant readAt) { }
    public synchronized Map<String,Object> create(String skill) {
        ProgramController.validateName(skill);
        String revision = RevisionFileStore.hash(files.read(".agents/skills/" + skill + "/SKILL.md"));
        pending.entrySet().removeIf(entry -> entry.getValue().expires().isBefore(Instant.now()));
        if (pending.size() >= 64) pending.remove(pending.keySet().iterator().next());
        String code = UUID.randomUUID().toString();
        pending.put(code, new Receipt(skill, revision, Instant.now().plusSeconds(900), null));
        return status(code);
    }
    public synchronized void record(String code, String skill, String content) {
        Receipt receipt = pending.get(code);
        if (receipt == null || receipt.expires().isBefore(Instant.now()) || !receipt.skill().equals(skill)
            || !receipt.revision().equals(RevisionFileStore.hash(content))) return;
        pending.put(code, new Receipt(skill, receipt.revision(), receipt.expires(), Instant.now()));
    }
    public synchronized Map<String,Object> status(String code) {
        Receipt receipt = pending.get(code);
        if (receipt == null) throw new WorkbenchException(FailureKind.NOT_FOUND, "驗證已失效，請重新產生測試句。");
        String current;
        try { current = RevisionFileStore.hash(files.read(".agents/skills/" + receipt.skill() + "/SKILL.md")); }
        catch (RuntimeException error) { current = "missing"; }
        String state = receipt.expires().isBefore(Instant.now()) ? "expired" : !current.equals(receipt.revision()) ? "stale" : receipt.readAt() == null ? "waiting" : "verified";
        Map<String,Object> value = new LinkedHashMap<>();
        value.put("code", code); value.put("skill", receipt.skill()); value.put("revision", receipt.revision()); value.put("state", state);
        value.put("expiresAt", receipt.expires().toString()); value.put("readAt", receipt.readAt() == null ? null : receipt.readAt().toString());
        return value;
    }
}
