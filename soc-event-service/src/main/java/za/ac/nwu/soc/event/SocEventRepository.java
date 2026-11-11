
package za.ac.nwu.soc.event;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SocEventRepository
        extends JpaRepository<SocEvent, String> {

    List<SocEvent> findTop100ByOrderByTimestampDesc();

    List<SocEvent> findTop1000ByOrderByTimestampDesc();

    List<SocEvent> findByServiceNameOrderByTimestampDesc(
        String serviceName
    );

    List<SocEvent> findBySeverityOrderByTimestampDesc(
        String severity
    );

    List<SocEvent> findByUserIdOrderByTimestampDesc(
        String userId
    );
}
