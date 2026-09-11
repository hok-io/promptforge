package promptforge.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import promptforge.workbench.FailureKind;
import promptforge.workbench.WorkbenchException;
import java.util.Map;

@RestControllerAdvice
public class HttpErrors {
    @ExceptionHandler(WorkbenchException.class)
    ResponseEntity<?> application(WorkbenchException error) {
        return message(status(error.kind()), error.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> domain(RuntimeException error) { return message(HttpStatus.UNPROCESSABLE_ENTITY, error.getMessage()); }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<?> json(HttpMessageNotReadableException error) { return message(HttpStatus.BAD_REQUEST, "JSON 格式或欄位無效，請檢查輸入。"); }
    @ExceptionHandler(java.io.IOException.class)
    ResponseEntity<?> io(java.io.IOException error) { return message(HttpStatus.INTERNAL_SERVER_ERROR, "讀取專案檔案失敗。"); }

    private HttpStatus status(FailureKind kind) {
        return switch (kind) {
            case INVALID_INPUT -> HttpStatus.UNPROCESSABLE_ENTITY;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case STORAGE_FAILURE -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private ResponseEntity<?> message(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
