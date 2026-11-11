
package za.ac.nwu.soc.alert;

import za.ac.nwu.soc.event.SocEvent;
import za.ac.nwu.soc.event.SocEventRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DetectionRuleEngine {

    private static final String OPEN = "OPEN";

    private final SocEventRepository eventRepository;
    private final AlertRepository alertRepository;

    @Value("${soc.detection.enabled:true}")
    private boolean enabled;

    public DetectionRuleEngine(
            SocEventRepository eventRepository,
            AlertRepository alertRepository) {
        this.eventRepository = eventRepository;
        this.alertRepository = alertRepository;
    }

    @Scheduled(
        fixedDelayString = "${soc.detection.fixed-delay-ms:30000}"
    )
    @Transactional
    public void runDetectionRules() {
        if (!enabled) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oldest = now.minusMinutes(10);

        List<SocEvent> recentEvents = eventRepository
            .findTop1000ByOrderByTimestampDesc()
            .stream()
            .filter(e -> e.getTimestamp() != null)
            .filter(e -> !e.getTimestamp().isBefore(oldest))
            .toList();

        detectCredentialAttacks(recentEvents, now);
        detectRepeatedAccessDenials(recentEvents, now);
        detectServiceErrorSpikes(recentEvents, now);
    }

    private void detectCredentialAttacks(
            List<SocEvent> events,
            LocalDateTime now) {

        LocalDateTime cutoff = now.minusMinutes(5);

        Map<String, List<SocEvent>> byIp = events.stream()
            .filter(e -> e.getTimestamp().isAfter(cutoff)
                || e.getTimestamp().isEqual(cutoff))
            .filter(this::isFailedLogin)
            .filter(e -> e.getSourceIp() != null
                && !e.getSourceIp().isBlank())
            .collect(Collectors.groupingBy(SocEvent::getSourceIp));

        byIp.forEach((ip, matching) -> {
            if (matching.size() < 5) {
                return;
            }

            String service = mostCommonService(matching);

            if (alertRepository
                    .existsByRuleNameAndAffectedEntityAndStatus(
                        "Credential Attack", ip, OPEN)) {
                return;
            }

            createAlert(
                "Credential Attack",
                "HIGH",
                "At least five failed-login events were observed " +
                    "from source IP " + ip + " within five minutes.",
                service,
                matching.get(0).getUserId(),
                ip,
                "Review authentication logs, apply rate limiting, " +
                    "consider temporary IP restrictions, and verify " +
                    "whether affected accounts require password resets.",
                matching
            );
        });
    }

    private void detectRepeatedAccessDenials(
            List<SocEvent> events,
            LocalDateTime now) {

        LocalDateTime cutoff = now.minusMinutes(10);

        Map<String, List<SocEvent>> byUser = events.stream()
            .filter(e -> e.getTimestamp().isAfter(cutoff)
                || e.getTimestamp().isEqual(cutoff))
            .filter(this::isAccessDenied)
            .filter(e -> e.getUserId() != null
                && !e.getUserId().isBlank())
            .collect(Collectors.groupingBy(SocEvent::getUserId));

        byUser.forEach((userId, matching) -> {
            if (matching.size() < 3) {
                return;
            }

            String service = mostCommonService(matching);
            String entity = "user:" + userId;

            if (alertRepository
                    .existsByRuleNameAndAffectedEntityAndStatus(
                        "Repeated Access Denials", entity, OPEN)) {
                return;
            }

            createAlert(
                "Repeated Access Denials",
                "MEDIUM",
                "At least three HTTP 401/403 events were observed " +
                    "for user " + userId + " within ten minutes.",
                service,
                userId,
                entity,
                "Review the user's recent activity, validate assigned " +
                    "roles and permissions, and investigate unexpected " +
                    "access attempts before changing privileges.",
                matching
            );
        });
    }

    private void detectServiceErrorSpikes(
            List<SocEvent> events,
            LocalDateTime now) {

        LocalDateTime cutoff = now.minusMinutes(5);

        Map<String, List<SocEvent>> byService = events.stream()
            .filter(e -> e.getTimestamp().isAfter(cutoff)
                || e.getTimestamp().isEqual(cutoff))
            .filter(e -> e.getStatusCode() != null
                && e.getStatusCode() >= 500)
            .filter(e -> e.getServiceName() != null
                && !e.getServiceName().isBlank())
            .collect(Collectors.groupingBy(SocEvent::getServiceName));

        byService.forEach((service, matching) -> {
            if (matching.size() < 3) {
                return;
            }

            if (alertRepository
                    .existsByRuleNameAndAffectedServiceAndStatus(
                        "Service Error Spike", service, OPEN)) {
                return;
            }

            createAlert(
                "Service Error Spike",
                "HIGH",
                "At least three HTTP 500+ events were observed for " +
                    service + " within five minutes.",
                service,
                null,
                "service:" + service,
                "Check service logs, dependency health, database " +
                    "connectivity, resource utilisation, and recent " +
                    "deployments. Restore service only after identifying " +
                    "the cause.",
                matching
            );
        });
    }

    private boolean isFailedLogin(SocEvent event) {
        String type = event.getEventType();

        if (type == null) {
            return false;
        }

        String normalized = type.trim()
            .toUpperCase()
            .replace('-', '_')
            .replace(' ', '_');

        return normalized.equals("LOGIN_FAILED")
            || normalized.equals("FAILED_LOGIN")
            || normalized.equals("AUTHENTICATION_FAILURE")
            || normalized.equals("AUTH_FAILED");
    }

    private boolean isAccessDenied(SocEvent event) {
        Integer code = event.getStatusCode();

        return code != null && (code == 401 || code == 403);
    }

    private String mostCommonService(List<SocEvent> events) {
        return events.stream()
            .map(SocEvent::getServiceName)
            .filter(s -> s != null && !s.isBlank())
            .collect(Collectors.groupingBy(
                Function.identity(),
                Collectors.counting()
            ))
            .entrySet()
            .stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("unknown");
    }

    private void createAlert(
            String ruleName,
            String severity,
            String description,
            String affectedService,
            String affectedUser,
            String affectedEntity,
            String recommendedAction,
            List<SocEvent> events) {

        Alert alert = new Alert();
        alert.setAlertId("alert-" + UUID.randomUUID());
        alert.setTimestamp(LocalDateTime.now());
        alert.setRuleName(ruleName);
        alert.setSeverity(severity);
        alert.setStatus(OPEN);
        alert.setDescription(description);
        alert.setAffectedService(affectedService);
        alert.setAffectedUser(affectedUser);
        alert.setAffectedEntity(affectedEntity);
        alert.setRecommendedAction(recommendedAction);

        List<String> eventIds = new ArrayList<>();
        for (SocEvent event : events) {
            if (event.getEventId() != null) {
                eventIds.add(event.getEventId());
            }
        }
        alert.setRelatedEvents(eventIds);

        alertRepository.save(alert);
    }
}
