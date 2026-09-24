package promptforge.web;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import promptforge.workbench.*;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class SkillLibraryTest {
 @TempDir Path root;
 @Test void emptyLibraryHasNoInventedSkillsAndMcpSeesNewSavesWithoutReconnect() throws Exception {
  var api = new SkillLibraryController(root.toString(), new ProgramController(root.toString()));
  var json = new ObjectMapper();
  var mcp = new ProgramMcpController(root.toString());
  assertTrue(api.list().isEmpty());
  assertEquals("[]", call(mcp,json,"list_skills",json.createObjectNode()).path("result").path("content").get(0).path("text").asText());
  for(String name : List.of("alpha", "beta")) {
   var outline = json.readTree("{\"description\":\"Test\",\"steps\":[{\"id\":\"one\",\"title\":\"Do work\",\"selected\":true}]}");
   var saved = api.save(name,new ProgramController.Write("new",outline));
   assertEquals(saved.generated(),call(mcp,json,"get_skill",json.createObjectNode().put("name",name)).path("result").path("content").get(0).path("text").asText());
  }
  assertEquals(List.of("alpha","beta"),api.list());
  assertTrue(call(mcp,json,"list_skills",json.createObjectNode()).path("result").path("content").get(0).path("text").asText().contains("beta"));
 }
 @Test void resourcesConflictAndMissingTransitiveDependenciesPreserveGeneration() throws Exception {
  var json=new ObjectMapper(); var api=new SkillLibraryController(root.toString(),new ProgramController(root.toString()));
  var file=api.write("demo",new SkillLibraryController.ResourceWrite("scripts/check.js","new","console.log('ok')"));
  assertThrows(WorkbenchException.class,()->api.write("demo",new SkillLibraryController.ResourceWrite("scripts/check.js","new","overwrite")));
  assertThrows(WorkbenchException.class,()->api.write("demo",new SkillLibraryController.ResourceWrite("scripts/../../outside","new","escape")));
  var outline=json.readTree("""
    {"description":"Review code","steps":[{"id":"one","title":"Review","selected":true,"capabilities":[{"type":"script","selected":true,"path":".promptforge/resources/demo/scripts/check.js","usage":"node check.js"}]}]}
    """);
  var saved=api.save("demo",new ProgramController.Write("new",outline));
  assertTrue(saved.generatedCurrent()); assertTrue(api.list().contains("demo")); assertFalse(saved.generated().contains("Write a prompt with"));
  var dep=root.resolve("helper/SKILL.md"); Files.createDirectories(dep.getParent()); Files.writeString(dep,"---\nname: helper\ndescription: helper\n---\n[required](missing.md)");
  var cap=(com.fasterxml.jackson.databind.node.ObjectNode)outline.path("steps").get(0).path("capabilities").get(0); cap.put("type","skill").put("path","helper/SKILL.md");
  assertThrows(WorkbenchException.class,()->api.save("demo",new ProgramController.Write(saved.revision(),outline)));
  assertEquals(saved.generated(),api.get("demo").generated());
  assertEquals(saved.revision(),api.get("demo").revision());
  var request=json.readTree("""
   {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"get_skill","arguments":{"name":"demo"}}}
   """);
  var response=json.valueToTree(new ProgramMcpController(root.toString()).handle(request).getBody());
  assertEquals(saved.generated(),response.path("result").path("content").get(0).path("text").asText());
 }

 @Test void listsAndReadsIndependentSkillsAndPreservesExternalUpdates() throws Exception {
  var json = new ObjectMapper();
  var api = new SkillLibraryController(root.toString(), new ProgramController(root.toString()));
  for (String name : List.of("PromptForge", "PromptForge-context", "ui-ux-pro-max")) writeSkill(name, "Original " + name);
  assertTrue(api.list().containsAll(List.of("PromptForge", "PromptForge-context", "ui-ux-pro-max")));

  var context = api.get("PromptForge-context");
  var editedContext = ((com.fasterxml.jackson.databind.node.ObjectNode) context.outline()).deepCopy();
  editedContext.put("document", editedContext.path("document").asText() + "\nContext-only edit.\n");
  var contextSaved = api.save("PromptForge-context", new ProgramController.Write(context.revision(), editedContext));
  String savedContext = Files.readString(root.resolve(".agents/skills/PromptForge-context/SKILL.md"));

  var main = json.readTree("""
   {"description":"Main instructions","steps":[{"id":"one","title":"Main step","selected":true}]}
   """);
  api.save("PromptForge", new ProgramController.Write(api.get("PromptForge").revision(), main));
  assertEquals(savedContext, Files.readString(root.resolve(".agents/skills/PromptForge-context/SKILL.md")));
  assertTrue(api.get("PromptForge-context").generatedCurrent());

  var external = savedContext + "\nExternal edit that must survive.\n";
  Files.writeString(root.resolve(".agents/skills/PromptForge-context/SKILL.md"), external);
  var stale = api.get("PromptForge-context");
  var staleEdit = ((com.fasterxml.jackson.databind.node.ObjectNode) stale.outline()).deepCopy();
  staleEdit.put("document", staleEdit.path("document").asText() + "\nMy edit.\n");
  assertThrows(WorkbenchException.class, () -> api.save("PromptForge-context", new ProgramController.Write(stale.revision(), staleEdit)));
  assertEquals(external, Files.readString(root.resolve(".agents/skills/PromptForge-context/SKILL.md")));

  var custom = json.readTree("""
   {"description":"Review code changes","steps":[{"id":"review","title":"Review","selected":true}]}
   """);
  var added = api.save("review-code", new ProgramController.Write("new", custom));
  assertTrue(api.list().contains("review-code"));
  var mcp = new ProgramMcpController(root.toString());
  var list = call(mcp, json, "list_skills", json.createObjectNode()).path("result").path("content").get(0).path("text").asText();
  assertTrue(list.contains("PromptForge-context"));
  assertTrue(list.contains("ui-ux-pro-max"));
  assertTrue(list.contains("review-code"));
  var args = json.createObjectNode().put("name", "review-code");
  var read = call(mcp, json, "get_skill", args).path("result").path("content").get(0).path("text").asText();
  assertEquals(added.generated(), read);
 }

 @Test void concurrentSavesWithTheSameRevisionHaveOneWinner() throws Exception {
  var api = new SkillLibraryController(root.toString(), new ProgramController(root.toString()));
  var json = new ObjectMapper();
  var first = json.readTree("{\"description\":\"First version\",\"steps\":[{\"id\":\"s\",\"title\":\"One\",\"selected\":true}]}");
  var second = json.readTree("{\"description\":\"Second version\",\"steps\":[{\"id\":\"s\",\"title\":\"Two\",\"selected\":true}]}");
  var revision = api.get("race").revision();
  var start = new java.util.concurrent.CountDownLatch(1);
  var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
  try {
   var a = pool.submit(() -> { start.await(); try { api.save("race", new ProgramController.Write(revision, first)); return "saved"; } catch (WorkbenchException error) { return "conflict"; } });
   var b = pool.submit(() -> { start.await(); try { api.save("race", new ProgramController.Write(revision, second)); return "saved"; } catch (WorkbenchException error) { return "conflict"; } });
   start.countDown();
   assertEquals(1, List.of(a.get(), b.get()).stream().filter("saved"::equals).count());
   assertEquals(1, List.of(a.get(), b.get()).stream().filter("conflict"::equals).count());
  } finally { pool.shutdownNow(); }
 }

 private void writeSkill(String name, String text) throws Exception {
  var path = root.resolve(".agents/skills/" + name + "/SKILL.md");
  Files.createDirectories(path.getParent());
  Files.writeString(path, "---\nname: " + name + "\ndescription: " + text + "\n---\n\n" + text + "\n");
 }
 private com.fasterxml.jackson.databind.JsonNode call(ProgramMcpController mcp, ObjectMapper json, String name, com.fasterxml.jackson.databind.JsonNode args) {
  var request = json.createObjectNode().put("jsonrpc", "2.0").put("id", 1).put("method", "tools/call");
  request.putObject("params").put("name", name).set("arguments", args);
  return json.valueToTree(mcp.handle(request).getBody());
 }
}
