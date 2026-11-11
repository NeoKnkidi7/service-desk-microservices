package za.ac.nwu.soc.rag;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class RagService {

    private final Driver driver;

    public RagService(Driver driver) {
        this.driver = driver;
    }

    private static final String EVIDENCE_QUERY = """
        MATCH (a:Alert)
        OPTIONAL MATCH (a)-[:AFFECTS]->(s:Service)
        OPTIONAL MATCH (a)-[:ASSOCIATED_WITH]->(u:User)
        OPTIONAL MATCH (a)-[:INDICATES]->(t:Threat)
        OPTIONAL MATCH (t)-[:MITIGATED_BY]->(c:Control)
        OPTIONAL MATCH (a)-[:TRIGGERED_BY]->(e:Event)
        RETURN
            a.alertId AS alertId,
            a.ruleName AS ruleName,
            a.severity AS severity,
            a.status AS status,
            a.timestamp AS timestamp,
            a.description AS description,
            a.affectedEntity AS affectedEntity,
            a.recommendedAction AS recommendedAction,
            collect(DISTINCT s.name) AS services,
            collect(DISTINCT u.userId) AS users,
            collect(DISTINCT t.name) AS threats,
            collect(DISTINCT c.name) AS controls,
            collect(DISTINCT CASE WHEN e IS NULL THEN null ELSE {
                eventId: e.eventId,
                eventType: e.eventType,
                serviceName: e.serviceName,
                userId: e.userId,
                sourceIp: e.sourceIp,
                endpoint: e.endpoint,
                httpMethod: e.httpMethod,
                statusCode: e.statusCode,
                message: e.message,
                timestamp: e.timestamp
            } END) AS events
        ORDER BY a.timestamp DESC
        """;

    public Map<String, Object> ask(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                "Please provide a non-empty question."
            );
        }

        String normalized = question.toLowerCase(Locale.ROOT).trim();
        List<Map<String, Object>> candidates = new ArrayList<>();

        try (Session session = driver.session()) {
            List<Record> records = session.run(EVIDENCE_QUERY).list();

            for (Record record : records) {
                Map<String, Object> item = toAlertMap(record);
                item.put("_score", score(normalized, item));
                candidates.add(item);
            }
        }

        candidates.sort((left, right) ->
            Integer.compare(
                (Integer) right.get("_score"),
                (Integer) left.get("_score")
            )
        );

        List<Map<String, Object>> selected =
            selectEvidence(normalized, candidates);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("question", question);
        response.put("retrievalMethod", "neo4j");
        response.put("evidenceCount", selected.size());

        if (selected.isEmpty()) {
            response.put(
                "answer",
                "I could not find a sufficiently relevant alert in the "
                    + "current Neo4j evidence. Try mentioning a detection "
                    + "rule, service name, alert ID, event type, or status code."
            );
            response.put("evidence", List.of());
            response.put("recommendedControls", List.of());
            response.put("affectedServices", List.of());
            response.put("relatedUsers", List.of());
            response.put("confidence", "insufficient-evidence");
            response.put(
                "limitations",
                "No sufficiently relevant alert was retrieved. This does "
                    + "not prove that no relevant event exists elsewhere."
            );
            return response;
        }

        List<Map<String, Object>> publicEvidence = new ArrayList<>();
        List<String> allControls = new ArrayList<>();
        List<String> allServices = new ArrayList<>();
        List<String> allUsers = new ArrayList<>();

        for (Map<String, Object> item : selected) {
            Map<String, Object> clean = new LinkedHashMap<>(item);
            clean.remove("_score");
            publicEvidence.add(clean);

            addUnique(allControls, item.get("controls"));
            addUnique(allServices, item.get("services"));
            addUnique(allUsers, item.get("users"));
        }

        Map<String, Object> primary = selected.get(0);
        int primaryScore = (Integer) primary.get("_score");

        response.put("answer", buildAnswer(normalized, primary));
        response.put("evidence", publicEvidence);
        response.put("recommendedControls", allControls);
        response.put("affectedServices", allServices);
        response.put("relatedUsers", allUsers);
        response.put(
            "confidence",
            primaryScore >= 5
                ? "strong-keyword-match"
                : "limited-keyword-match"
        );
        response.put(
            "limitations",
            "This response retrieves existing graph evidence and applies "
                + "keyword matching. It is not a generative language model, "
                + "and a detection alert is not proof of a confirmed attack."
        );

        return response;
    }

    private Map<String, Object> toAlertMap(Record record) {
        Map<String, Object> item = new LinkedHashMap<>();

        item.put("alertId", stringValue(record.get("alertId")));
        item.put("ruleName", stringValue(record.get("ruleName")));
        item.put("severity", stringValue(record.get("severity")));
        item.put("status", stringValue(record.get("status")));
        item.put("timestamp", stringValue(record.get("timestamp")));
        item.put("description", stringValue(record.get("description")));
        item.put(
            "affectedEntity",
            stringValue(record.get("affectedEntity"))
        );
        item.put(
            "recommendedAction",
            stringValue(record.get("recommendedAction"))
        );

        item.put("services", stringList(record.get("services")));
        item.put("users", stringList(record.get("users")));
        item.put("threats", stringList(record.get("threats")));
        item.put("controls", stringList(record.get("controls")));
        item.put("events", eventList(record.get("events")));

        return item;
    }

    private String stringValue(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }

        return value.asString();
    }

    /**
     * Convert a Neo4j list to a mutable Java list.
     *
     * Neo4j's Value.asList(...) can return an immutable singleton list.
     * Copying the result into ArrayList before removeIf prevents the
     * UnsupportedOperationException previously seen at runtime.
     */
    private List<String> stringList(Value value) {
        if (value == null || value.isNull()) {
            return new ArrayList<>();
        }

        List<String> values = new ArrayList<>(
            value.asList(v -> v == null || v.isNull()
                ? null
                : v.asString())
        );

        values.removeIf(v -> v == null || v.isBlank());
        return values;
    }

    /**
     * Convert the Neo4j collection of event maps into ordinary Java maps.
     */
    private List<Map<String, Object>> eventList(Value value) {
        List<Map<String, Object>> events = new ArrayList<>();

        if (value == null || value.isNull()) {
            return events;
        }

        List<Value> rawEvents = value.asList(v -> v);

        for (Value rawEvent : rawEvents) {
            if (rawEvent == null || rawEvent.isNull()
                    || !rawEvent.type().name().equals("MAP")) {
                continue;
            }

            Map<String, Object> rawMap = rawEvent.asMap();
            Map<String, Object> event = new LinkedHashMap<>();

            rawMap.forEach((key, entry) -> {
                if (key != null) {
                    event.put(key, entry);
                }
            });

            // Ignore null entries created by the optional graph match.
            if (event.get("eventId") != null) {
                events.add(event);
            }
        }

        return events;
    }

    private int score(String question, Map<String, Object> item) {
        String alertId = text(item.get("alertId"))
            .toLowerCase(Locale.ROOT);
        String rule = text(item.get("ruleName"))
            .toLowerCase(Locale.ROOT);
        String description = text(item.get("description"))
            .toLowerCase(Locale.ROOT);
        String entity = text(item.get("affectedEntity"))
            .toLowerCase(Locale.ROOT);

        if (!alertId.isBlank() && question.contains(alertId)) {
            return 100;
        }

        int score = 0;

        if (!rule.isBlank() && question.contains(rule)) {
            score += 6;
        }

        if (question.contains("credential")
                || question.contains("brute force")
                || question.contains("failed login")
                || question.contains("password")
                || question.contains("authentication")) {
            if (rule.contains("credential")) {
                score += 5;
            }
        }

        if (question.contains("access denial")
                || question.contains("access denied")
                || question.contains("authorization")
                || question.contains("403")
                || question.contains("401")) {
            if (rule.contains("access")) {
                score += 5;
            }
        }

        if (question.contains("error spike")
                || question.contains("http 500")
                || question.contains("status code 500")
                || question.contains("500 errors")
                || question.contains("availability")
                || question.contains("reliability")) {
            if (rule.contains("error")) {
                score += 5;
            }
        }

        if (question.contains("mitigat")
                || question.contains("control")
                || question.contains("prevent")) {
            if (!stringListFromObject(item.get("controls")).isEmpty()) {
                score += 2;
            }
        }

        if (question.contains("event") || question.contains("evidence")) {
            if (!eventListFromObject(item.get("events")).isEmpty()) {
                score += 2;
            }
        }

        for (String service : stringListFromObject(item.get("services"))) {
            if (!service.isBlank()
                    && question.contains(service.toLowerCase(Locale.ROOT))) {
                score += 5;
            }
        }

        for (String user : stringListFromObject(item.get("users"))) {
            if (!user.isBlank()
                    && question.contains(user.toLowerCase(Locale.ROOT))) {
                score += 4;
            }
        }

        if (!entity.isBlank() && question.contains(entity)) {
            score += 4;
        }

        if (!description.isBlank()) {
            for (String token : description.split("[^a-z0-9._:-]+")) {
                if (token.length() >= 5 && question.contains(token)) {
                    score++;
                }
            }
        }

        return score;
    }

    private List<Map<String, Object>> selectEvidence(
            String question,
            List<Map<String, Object>> candidates) {

        List<Map<String, Object>> selected = new ArrayList<>();

        boolean asksForAll =
            question.contains("all alerts")
                || question.contains("list alerts")
                || question.contains("show alerts")
                || question.contains("latest alerts");

        for (Map<String, Object> item : candidates) {
            int score = (Integer) item.get("_score");

            if (asksForAll || score >= 3) {
                selected.add(item);
            }

            if (!asksForAll && selected.size() >= 3) {
                break;
            }
        }

        return selected;
    }

    private String buildAnswer(
            String question,
            Map<String, Object> item) {

        String rule = text(item.get("ruleName"));
        String id = text(item.get("alertId"));
        String severity = text(item.get("severity"));
        String status = text(item.get("status"));
        String description = text(item.get("description"));

        StringBuilder answer = new StringBuilder();

        answer.append("The matching alert is ")
            .append(id)
            .append(" (")
            .append(rule)
            .append("), severity ")
            .append(severity)
            .append(", status ")
            .append(status)
            .append(".");

        if (!description.isBlank()) {
            answer.append(" Recorded description: ")
                .append(description);
        }

        if (question.contains("control")
                || question.contains("mitigat")
                || question.contains("prevent")) {

            List<String> controls =
                stringListFromObject(item.get("controls"));

            if (controls.isEmpty()) {
                answer.append(
                    " No mitigation controls are linked to this alert "
                        + "in the current graph."
                );
            } else {
                answer.append(" Controls retrieved from the graph: ")
                    .append(String.join(", ", controls))
                    .append(".");
            }
        }

        if (question.contains("why")
                || question.contains("evidence")
                || question.contains("event")) {

            List<Map<String, Object>> events =
                eventListFromObject(item.get("events"));

            answer.append(" The graph contains ")
                .append(events.size())
                .append(" linked supporting event(s).");

            if (!events.isEmpty()) {
                List<String> ids = new ArrayList<>();

                for (Map<String, Object> event : events) {
                    Object eventId = event.get("eventId");

                    if (eventId != null) {
                        ids.add(eventId.toString());
                    }
                }

                if (!ids.isEmpty()) {
                    answer.append(" Event IDs: ")
                        .append(String.join(", ", ids))
                        .append(".");
                }
            }
        }

        answer.append(
            " This is a rule-based detection result and does not alone "
                + "confirm malicious activity."
        );

        return answer.toString();
    }

    private void addUnique(List<String> target, Object values) {
        for (String value : stringListFromObject(values)) {
            if (!target.contains(value)) {
                target.add(value);
            }
        }
    }

    private List<String> stringListFromObject(Object value) {
        if (!(value instanceof List<?> list)) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();

        for (Object item : list) {
            if (item != null && !item.toString().isBlank()) {
                result.add(item.toString());
            }
        }

        return result;
    }

    private List<Map<String, Object>> eventListFromObject(Object value) {
        if (!(value instanceof List<?> list)) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> event = new LinkedHashMap<>();

                map.forEach((key, entry) -> {
                    if (key != null) {
                        event.put(key.toString(), entry);
                    }
                });

                if (event.get("eventId") != null) {
                    result.add(event);
                }
            }
        }

        return result;
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }
}
