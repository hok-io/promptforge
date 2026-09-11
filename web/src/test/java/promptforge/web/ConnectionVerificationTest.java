package promptforge.web;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class ConnectionVerificationTest {
    @TempDir Path root;
    @Test void onlyMatchingTokenReadVerifiesTheExactVersion() throws Exception {
        Path skill = root.resolve(".agents/skills/PromptForge/SKILL.md"); Files.createDirectories(skill.getParent()); Files.writeString(skill,"version one");
        var proof = new ConnectionVerification(root.toString());
        var mcp = new ProgramMcpController(root.toString(),proof);
        String code = proof.create("PromptForge").get("code").toString();
        var json = new ObjectMapper();
        var request = json.readTree("""
            {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"get_skill","arguments":{"name":"PromptForge"}}}
            """);
        mcp.handle(request); assertEquals("waiting",proof.status(code).get("state"));
        var args = (com.fasterxml.jackson.databind.node.ObjectNode) request.path("params").path("arguments");
        args.put("verificationCode","wrong"); mcp.handle(request); assertEquals("waiting",proof.status(code).get("state"));
        args.put("verificationCode",code); mcp.handle(request); assertEquals("verified",proof.status(code).get("state"));
        assertNotNull(proof.status(code).get("readAt"));
        Files.writeString(skill,"version two"); assertEquals("stale",proof.status(code).get("state"));
        var restarted = new ConnectionVerification(root.toString()); assertThrows(RuntimeException.class,()->restarted.status(code));
    }
    @Test void installedDevelopmentSkillsDoNotBreakManagedMcpListing() throws Exception {
        Path installed = root.resolve(".agents/skills/ui-ux-pro-max/data/large.csv"); Files.createDirectories(installed.getParent()); Files.writeString(installed,"x".repeat(300000));
        Path generated = root.resolve(".agents/skills/PromptForge/SKILL.md"); Files.createDirectories(generated.getParent()); Files.writeString(generated,"generated");
        var json = new ObjectMapper();
        var request = json.readTree("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"list_skills\"}}");
        var result = json.valueToTree(new ProgramMcpController(root.toString()).handle(request).getBody()).path("result");
        assertFalse(result.has("isError")); assertEquals("[\"PromptForge\"]",result.path("content").get(0).path("text").asText());
    }
}
