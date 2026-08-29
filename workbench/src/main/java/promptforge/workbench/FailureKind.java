package promptforge.workbench;

/** Meaning of an application failure, independent of its HTTP representation. */
public enum FailureKind {
    INVALID_INPUT,
    NOT_FOUND,
    CONFLICT,
    STORAGE_FAILURE
}
