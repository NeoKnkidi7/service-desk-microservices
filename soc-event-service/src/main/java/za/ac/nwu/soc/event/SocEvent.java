
package za.ac.nwu.soc.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "soc_events", indexes = {
    @Index(name = "idx_soc_event_time", columnList = "timestamp"),
    @Index(name = "idx_soc_event_type_time",
           columnList = "eventType,timestamp"),
    @Index(name = "idx_soc_event_ip_time",
           columnList = "sourceIp,timestamp"),
    @Index(name = "idx_soc_event_service_time",
           columnList = "serviceName,timestamp")
})
public class SocEvent {

    @Id
    @Column(length = 80)
    private String eventId;

    private LocalDateTime timestamp;

    @Column(nullable = false, length = 100)
    private String serviceName;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(length = 20)
    private String severity;

    @Column(length = 150)
    private String userId;

    @Column(length = 100)
    private String sourceIp;

    @Column(length = 300)
    private String endpoint;

    @Column(length = 10)
    private String httpMethod;

    private Integer statusCode;

    @Column(length = 2000)
    private String message;

    @Column(length = 150)
    private String correlationId;

    @Column(length = 200)
    private String affectedEntity;

    public SocEvent() {}

    public String getEventId() { return eventId; }
    public void setEventId(String value) { eventId = value; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime value) { timestamp = value; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String value) { serviceName = value; }

    public String getEventType() { return eventType; }
    public void setEventType(String value) { eventType = value; }

    public String getSeverity() { return severity; }
    public void setSeverity(String value) { severity = value; }

    public String getUserId() { return userId; }
    public void setUserId(String value) { userId = value; }

    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String value) { sourceIp = value; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String value) { endpoint = value; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String value) { httpMethod = value; }

    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer value) { statusCode = value; }

    public String getMessage() { return message; }
    public void setMessage(String value) { message = value; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String value) {
        correlationId = value;
    }

    public String getAffectedEntity() { return affectedEntity; }
    public void setAffectedEntity(String value) {
        affectedEntity = value;
    }
}
