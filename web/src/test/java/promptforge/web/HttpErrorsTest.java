package promptforge.web;

import org.junit.jupiter.api.Test;
import promptforge.workbench.FailureKind;
import promptforge.workbench.WorkbenchException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class HttpErrorsTest {
    @Test void applicationFailureKindsMapToHttpStatuses() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new FailureController())
                .setControllerAdvice(new HttpErrors()).build();
        mvc.perform(get("/invalid")).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/missing")).andExpect(status().isNotFound());
        mvc.perform(get("/conflict")).andExpect(status().isConflict());
        mvc.perform(get("/storage")).andExpect(status().isInternalServerError());
    }

    @org.springframework.web.bind.annotation.RestController
    static class FailureController {
        @org.springframework.web.bind.annotation.GetMapping("/invalid")
        void invalid() { throw new WorkbenchException(FailureKind.INVALID_INPUT, "invalid"); }
        @org.springframework.web.bind.annotation.GetMapping("/missing")
        void missing() { throw new WorkbenchException(FailureKind.NOT_FOUND, "missing"); }
        @org.springframework.web.bind.annotation.GetMapping("/conflict")
        void conflict() { throw new WorkbenchException(FailureKind.CONFLICT, "conflict"); }
        @org.springframework.web.bind.annotation.GetMapping("/storage")
        void storage() { throw new WorkbenchException(FailureKind.STORAGE_FAILURE, "storage"); }
    }
}
