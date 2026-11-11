
package za.ac.nwu.soc.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, String> {

    List<Alert> findTop100ByOrderByTimestampDesc();

    boolean existsByRuleNameAndAffectedServiceAndStatus(
        String ruleName,
        String affectedService,
        String status
    );

    boolean existsByRuleNameAndAffectedEntityAndStatus(
        String ruleName,
        String affectedEntity,
        String status
    );
}
