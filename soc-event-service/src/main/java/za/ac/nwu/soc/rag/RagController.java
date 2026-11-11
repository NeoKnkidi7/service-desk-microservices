
package za.ac.nwu.soc.rag;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> askPost(
            @RequestBody Map<String, String> request) {

        String question = request == null
            ? null : request.get("question");

        return ResponseEntity.ok(ragService.ask(question));
    }

    @GetMapping("/ask")
    public ResponseEntity<Map<String, Object>> askGet(
            @RequestParam("question") String question) {

        return ResponseEntity.ok(ragService.ask(question));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(
            IllegalArgumentException exception) {

        return Map.of("error", exception.getMessage());
    }
}
