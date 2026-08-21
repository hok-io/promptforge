package promptforge.web;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import promptforge.*;
import promptforge.workbench.WorkbenchException;
import java.util.Map;

@RestControllerAdvice
public class HttpErrors {
    @ExceptionHandler(WorkbenchException.class) ResponseEntity<?> application(WorkbenchException error) { return message(error.status(), error.getMessage()); }
    @ExceptionHandler({RuleFormatException.class, KnowledgeFormatException.class, RuleConflictException.class, FrameBudgetException.class, IllegalArgumentException.class})
    ResponseEntity<?> domain(RuntimeException error) { return message(422, error.getMessage()); }
    @ExceptionHandler(HttpMessageNotReadableException.class) ResponseEntity<?> json(HttpMessageNotReadableException error) { return message(400, "JSON 格式或欄位無效，請檢查輸入。"); }
    @ExceptionHandler(java.io.IOException.class) ResponseEntity<?> io(java.io.IOException error) { return message(500, "讀取專案檔案失敗。"); }
    private ResponseEntity<?> message(int status, String message) { return ResponseEntity.status(status).body(Map.of("message", message)); }
}
