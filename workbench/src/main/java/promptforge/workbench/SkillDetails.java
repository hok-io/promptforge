package promptforge.workbench;

import java.util.List;

public record SkillDetails(String id, String name, String description, String revision, String raw, String state,
                           String origin, List<SkillReference> references, List<EvaluationView> evaluations) {}
