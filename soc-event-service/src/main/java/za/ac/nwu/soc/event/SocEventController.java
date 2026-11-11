
package za.ac.nwu.soc.event;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/soc/events")
public class SocEventController {

    private final SocEventRepository repository;

    public SocEventController(SocEventRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SocEvent ingest(@RequestBody SocEvent event) {
        if (event.getServiceName() == null
                || event.getServiceName().isBlank()
                || event.getEventType() == null
                || event.getEventType().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "serviceName and eventType are required"
            );
        }

        if (event.getEventId() == null
                || event.getEventId().isBlank()) {
            event.setEventId("evt-" + UUID.randomUUID());
        }

        if (event.getTimestamp() == null) {
            event.setTimestamp(LocalDateTime.now());
        }

        return repository.save(event);
    }

    @GetMapping
    public List<SocEvent> list(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String severity) {

        if (service != null && !service.isBlank()) {
            return repository
                .findByServiceNameOrderByTimestampDesc(service)
                .stream()
                .filter(e -> severity == null
                    || severity.equalsIgnoreCase(e.getSeverity()))
                .limit(100)
                .toList();
        }

        if (severity != null && !severity.isBlank()) {
            return repository
                .findBySeverityOrderByTimestampDesc(severity)
                .stream()
                .limit(100)
                .toList();
        }

        return repository.findTop100ByOrderByTimestampDesc();
    }

    @GetMapping("/user/{userId}")
    public List<SocEvent> byUser(@PathVariable String userId) {
        return repository
            .findByUserIdOrderByTimestampDesc(userId)
            .stream()
            .limit(100)
            .toList();
    }
}
