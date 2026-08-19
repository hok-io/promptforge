package promptforge.workbench;

public record SkillMetadata(String bundleHash, String reviewedHash, String approvedHash, String activeHash) {
    static SkillMetadata imported(String hash) { return new SkillMetadata(hash, null, null, null); }
    String state(String current) {
        if (!current.equals(bundleHash)) return "IMPORTED";
        if (current.equals(approvedHash) && current.equals(activeHash)) return "ACTIVE";
        if (current.equals(approvedHash)) return "APPROVED";
        if (current.equals(reviewedHash)) return "REVIEWED";
        return "IMPORTED";
    }
    SkillMetadata review(String hash) { return new SkillMetadata(hash, hash, null, null); }
    SkillMetadata approve(String hash) { return new SkillMetadata(hash, reviewedHash, hash, null); }
    SkillMetadata activate(String hash) { return new SkillMetadata(hash, reviewedHash, approvedHash, hash); }
}
