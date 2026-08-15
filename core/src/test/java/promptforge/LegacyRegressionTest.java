package promptforge;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import java.util.stream.Stream;

/** Keeps each existing executable contract visible as a separate JUnit result. */
class LegacyRegressionTest {
    @TestFactory
    Stream<DynamicTest> existingContracts() {
        return Stream.of("Bm25IndexTest", "CliTest", "KnowledgeCardTest", "KnowledgeFrameCliTest", "QueryCliTest",
                "RuleBoundaryTest", "RuleConflictFieldTest", "RuleConflictTest", "RuleFileTest", "RuleSelectionCliTest",
                "RuleSelectorTest", "SmokeTest", "TaskFrameTest", "ValidateCliTest")
                .map(name -> DynamicTest.dynamicTest(name, () -> {
                    Class<?> test = Class.forName("promptforge." + name);
                    try { test.getMethod("main", String[].class).invoke(null, (Object) new String[0]); }
                    catch (java.lang.reflect.InvocationTargetException failure) { throw failure.getCause(); }
                }));
    }
}
