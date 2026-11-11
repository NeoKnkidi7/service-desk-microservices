
package za.ac.nwu.soc.graph;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/soc/graph")
public class GraphSyncController {

    private final GraphSyncService graphSyncService;

    public GraphSyncController(GraphSyncService graphSyncService) {
        this.graphSyncService = graphSyncService;
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> synchronize() {
        return ResponseEntity.ok(graphSyncService.synchronize());
    }
}
