
package za.ac.nwu.soc.graph;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import za.ac.nwu.soc.alert.Alert;
import za.ac.nwu.soc.alert.AlertRepository;
import za.ac.nwu.soc.event.SocEvent;
import za.ac.nwu.soc.event.SocEventRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GraphSyncService {

    private final Driver driver;
    private final AlertRepository alertRepository;
    private final SocEventRepository eventRepository;

    public GraphSyncService(
            Driver driver,
            AlertRepository alertRepository,
            SocEventRepository eventRepository) {
        this.driver = driver;
        this.alertRepository = alertRepository;
        this.eventRepository = eventRepository;
    }

    public Map<String, Object> synchronize() {
        int eventsSynced = 0;
        int alertsSynced = 0;

        try (Session session = driver.session()) {
            session.run("""
                CREATE CONSTRAINT alert_id IF NOT EXISTS
                FOR (n:Alert) REQUIRE n.alertId IS UNIQUE
                """).consume();

            session.run("""
                CREATE CONSTRAINT event_id IF NOT EXISTS
                FOR (n:Event) REQUIRE n.eventId IS UNIQUE
                """).consume();

            session.run("""
                CREATE CONSTRAINT service_name IF NOT EXISTS
                FOR (n:Service) REQUIRE n.name IS UNIQUE
                """).consume();

            session.run("""
                CREATE CONSTRAINT user_id IF NOT EXISTS
                FOR (n:User) REQUIRE n.userId IS UNIQUE
                """).consume();

            session.run("""
                CREATE CONSTRAINT threat_name IF NOT EXISTS
                FOR (n:Threat) REQUIRE n.name IS UNIQUE
                """).consume();

            session.run("""
                CREATE CONSTRAINT control_name IF NOT EXISTS
                FOR (n:Control) REQUIRE n.name IS UNIQUE
                """).consume();

            session.run("""
                CREATE CONSTRAINT endpoint_path IF NOT EXISTS
                FOR (n:Endpoint) REQUIRE n.path IS UNIQUE
                """).consume();

            List<SocEvent> events =
                eventRepository.findTop1000ByOrderByTimestampDesc();

            Map<String, SocEvent> eventsById = new HashMap<>();

            for (SocEvent event : events) {
                if (event.getEventId() == null) {
                    continue;
                }

                eventsById.put(event.getEventId(), event);
                syncEvent(session, event);
                eventsSynced++;
            }

            List<Alert> alerts = alertRepository.findAll();

            for (Alert alert : alerts) {
                syncAlert(session, alert);

                if (alert.getRelatedEvents() != null) {
                    for (String eventId : alert.getRelatedEvents()) {
                        if (eventsById.containsKey(eventId)) {
                            session.run("""
                                MATCH (a:Alert {alertId: $alertId})
                                MATCH (e:Event {eventId: $eventId})
                                MERGE (a)-[:TRIGGERED_BY]->(e)
                                """, Map.of(
                                    "alertId", alert.getAlertId(),
                                    "eventId", eventId
                                )).consume();
                        }
                    }
                }

                syncThreatAndControls(session, alert);
                alertsSynced++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("eventsSynced", eventsSynced);
        result.put("alertsSynced", alertsSynced);
        result.put("message",
            "PostgreSQL SOC events and alerts synchronized to Neo4j.");
        return result;
    }

    private void syncEvent(Session session, SocEvent event) {
        Map<String, Object> props = new HashMap<>();
        props.put("eventId", event.getEventId());
        put(props, "timestamp", event.getTimestamp());
        put(props, "serviceName", event.getServiceName());
        put(props, "eventType", event.getEventType());
        put(props, "severity", event.getSeverity());
        put(props, "userId", event.getUserId());
        put(props, "sourceIp", event.getSourceIp());
        put(props, "endpoint", event.getEndpoint());
        put(props, "httpMethod", event.getHttpMethod());
        put(props, "statusCode", event.getStatusCode());
        put(props, "message", event.getMessage());
        put(props, "correlationId", event.getCorrelationId());

        session.run("""
            MERGE (e:Event {eventId: $eventId})
            SET e += $props
            """, Map.of(
                "eventId", event.getEventId(),
                "props", props
            )).consume();

        if (hasText(event.getServiceName())) {
            session.run("""
                MERGE (s:Service {name: $service})
                MERGE (e:Event {eventId: $eventId})
                MERGE (e)-[:OBSERVED_IN]->(s)
                """, Map.of(
                    "service", event.getServiceName(),
                    "eventId", event.getEventId()
                )).consume();
        }

        if (hasText(event.getUserId())) {
            session.run("""
                MERGE (u:User {userId: $userId})
                MERGE (e:Event {eventId: $eventId})
                MERGE (e)-[:ASSOCIATED_WITH]->(u)
                """, Map.of(
                    "userId", event.getUserId(),
                    "eventId", event.getEventId()
                )).consume();
        }

        if (hasText(event.getEndpoint())) {
            session.run("""
                MERGE (p:Endpoint {path: $path})
                MERGE (e:Event {eventId: $eventId})
                MERGE (e)-[:TARGETS]->(p)
                """, Map.of(
                    "path", event.getEndpoint(),
                    "eventId", event.getEventId()
                )).consume();
        }
    }

    private void syncAlert(Session session, Alert alert) {
        Map<String, Object> props = new HashMap<>();
        props.put("alertId", alert.getAlertId());
        put(props, "timestamp", alert.getTimestamp());
        put(props, "ruleName", alert.getRuleName());
        put(props, "severity", alert.getSeverity());
        put(props, "status", alert.getStatus());
        put(props, "description", alert.getDescription());
        put(props, "affectedService", alert.getAffectedService());
        put(props, "affectedUser", alert.getAffectedUser());
        put(props, "affectedEntity", alert.getAffectedEntity());
        put(props, "recommendedAction", alert.getRecommendedAction());

        session.run("""
            MERGE (a:Alert {alertId: $alertId})
            SET a += $props
            """, Map.of(
                "alertId", alert.getAlertId(),
                "props", props
            )).consume();

        if (hasText(alert.getAffectedService())) {
            session.run("""
                MERGE (s:Service {name: $service})
                MERGE (a:Alert {alertId: $alertId})
                MERGE (a)-[:AFFECTS]->(s)
                """, Map.of(
                    "service", alert.getAffectedService(),
                    "alertId", alert.getAlertId()
                )).consume();
        }

        if (hasText(alert.getAffectedUser())) {
            session.run("""
                MERGE (u:User {userId: $userId})
                MERGE (a:Alert {alertId: $alertId})
                MERGE (a)-[:ASSOCIATED_WITH]->(u)
                """, Map.of(
                    "userId", alert.getAffectedUser(),
                    "alertId", alert.getAlertId()
                )).consume();
        }
    }

    private void syncThreatAndControls(
            Session session, Alert alert) {

        String rule = alert.getRuleName() == null
            ? "" : alert.getRuleName();

        String threat;
        List<String> controls = new ArrayList<>();

        switch (rule) {
            case "Credential Attack" -> {
                threat = "Credential Attack Pattern";
                controls.add("Authentication Rate Limiting");
                controls.add("Multi-Factor Authentication");
                controls.add("Failed Login Monitoring");
            }
            case "Repeated Access Denials" -> {
                threat = "Repeated Access Denial Pattern";
                controls.add("Access Control Review");
                controls.add("Authorization Audit Logging");
            }
            case "Service Error Spike" -> {
                threat = "Service Availability or Reliability Issue";
                controls.add("Service Health Monitoring");
                controls.add("Dependency Health Checks");
                controls.add("Error Rate Alerting");
            }
            default -> {
                threat = "Unclassified Detection";
                controls.add("Security Event Review");
            }
        }

        session.run("""
            MERGE (t:Threat {name: $threat})
            MERGE (a:Alert {alertId: $alertId})
            MERGE (a)-[:INDICATES]->(t)
            """, Map.of(
                "threat", threat,
                "alertId", alert.getAlertId()
            )).consume();

        for (String control : controls) {
            session.run("""
                MERGE (c:Control {name: $control})
                MERGE (t:Threat {name: $threat})
                MERGE (t)-[:MITIGATED_BY]->(c)
                """, Map.of(
                    "control", control,
                    "threat", threat
                )).consume();
        }
    }

    private void put(
            Map<String, Object> props,
            String key,
            Object value) {
        if (value != null) {
            props.put(key, value instanceof java.time.temporal.TemporalAccessor
                ? value.toString() : value);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
