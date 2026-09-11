package promptforge.web;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import promptforge.workbench.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class SkillLibraryTest {
 @TempDir Path root;
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
}
