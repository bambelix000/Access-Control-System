package com.bombel.AccessControl.accessrule;

import com.bombel.AccessControl.device.Device;
import com.bombel.AccessControl.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccessRuleRepository extends JpaRepository<AccessRule, Long> {
    boolean existsByTargetUserAndDevice(User targetUser, Device device);
    List<AccessRule> findAllByDevice(Device device);
    Optional<AccessRule> findByTargetUserAndDevice(User targetUser, Device device);
}
