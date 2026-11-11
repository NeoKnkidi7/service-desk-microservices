
package za.ac.nwu.soc.alert;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "soc_alerts")
public class Alert {

    @Id
    private String alertId;

    private LocalDateTime timestamp;
    private String ruleName;
    private String severity;
    private String status;

    @Column(length = 3000)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "soc_alert_events",
        joinColumns = @JoinColumn(name = "alert_id")
    )
    @Column(name = "event_id")
    private List<String> relatedEvents = new ArrayList<>();

    private String affectedService;
    private String affectedUser;
    private String affectedEntity;

    @Column(length = 2000)
    private String recommendedAction;

    public Alert() {}

    public String getAlertId() { return alertId; }
    public void setAlertId(String value) { alertId = value; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime value) { timestamp = value; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String value) { ruleName = value; }

    public String getSeverity() { return severity; }
    public void setSeverity(String value) { severity = value; }

    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }

    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }

    public List<String> getRelatedEvents() { return relatedEvents; }
    public void setRelatedEvents(List<String> value) {
        relatedEvents = value;
    }

    public String getAffectedService() { return affectedService; }
    public void setAffectedService(String value) {
        affectedService = value;
    }

    public String getAffectedUser() { return affectedUser; }
    public void setAffectedUser(String value) { affectedUser = value; }

    public String getAffectedEntity() { return affectedEntity; }
    public void setAffectedEntity(String value) {
        affectedEntity = value;
    }

    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String value) {
        recommendedAction = value;
    }
}
