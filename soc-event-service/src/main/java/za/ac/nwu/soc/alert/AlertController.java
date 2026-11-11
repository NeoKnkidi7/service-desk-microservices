package za.ac.nwu.soc.alert;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/soc/alerts")
public class AlertController {

    private final AlertRepository repository;

    public AlertController(AlertRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Alert> getAlerts() {
        return repository.findTop100ByOrderByTimestampDesc();
    }

    @GetMapping("/{alertId}")
    public Alert getAlert(@PathVariable String alertId) {
        return repository.findById(alertId)
            .orElseThrow(() ->
                new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "Alert not found"
                )
            );
    }
}