package com.bombel.AccessControl.accessrule;

import com.bombel.AccessControl.accessrule.dto.GrantAccessRequest;
import com.bombel.AccessControl.auditlog.Action;
import com.bombel.AccessControl.auditlog.AuditLog;
import com.bombel.AccessControl.auditlog.AuditLogRepository;
import com.bombel.AccessControl.device.Device;
import com.bombel.AccessControl.device.DeviceRepository;
import com.bombel.AccessControl.user.User;
import com.bombel.AccessControl.user.UserRepository;
import com.bombel.AccessControl.user.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AccessRuleService {

    private final AccessRuleRepository accessRuleRepository;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final AuditLogRepository auditLogRepository;

    @Autowired
    public AccessRuleService(AccessRuleRepository accessRuleRepository, UserRepository userRepository, DeviceRepository deviceRepository, AuditLogRepository auditLogRepository) {
        this.accessRuleRepository = accessRuleRepository;
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public String grantAccess(GrantAccessRequest request) {
        User admin = userRepository.findById(request.getAdminId())
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono administratora o podanym ID."));
        if (admin.getUserRole() != UserRole.ADMIN) {
            throw new SecurityException("Tylko administrator może nadawać uprawnienia!");
        }

        User targetUser = userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono użytkownika docelowego"));
        Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono urządzenia"));

        if (accessRuleRepository.existsByTargetUserAndDevice(targetUser, device)) {
            throw new IllegalArgumentException("Ten użytkownik ma już dostęp do tego zamka");
        }

        AccessRule rule = new AccessRule();
        rule.setTargetUser(targetUser);
        rule.setDevice(device);
        accessRuleRepository.save(rule);

        AuditLog log = new AuditLog();
        log.setTimestamp(LocalDateTime.now());
        log.setAdmin(admin);
        log.setTargetUser(targetUser);
        log.setDevice(device);
        log.setAction(Action.ACCESS_GRANTED);
        auditLogRepository.save(log);

        return "Uprawnienie zostało pomyślnie nadane i zapisane w logach.";
    }

    @Transactional
    public String revokeAccess(GrantAccessRequest request) {
        User admin = userRepository.findById(request.getAdminId())
                .orElseThrow(() -> new IllegalArgumentException("Admin nie istnieje."));

        if (admin.getUserRole() != UserRole.ADMIN) {
            throw new SecurityException("Brak uprawnień admina!");
        }

        User targetUser = userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new IllegalArgumentException("Użytkownik nie istnieje."));

        Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new IllegalArgumentException("Zamek nie istnieje."));

        AccessRule rule = accessRuleRepository.findByTargetUserAndDevice(targetUser, device)
                .orElseThrow(() -> new IllegalArgumentException("Ten użytkownik i tak nie ma dostępu."));

        accessRuleRepository.delete(rule);

        AuditLog log = new AuditLog();
        log.setTimestamp(LocalDateTime.now());
        log.setAdmin(admin);
        log.setTargetUser(targetUser);
        log.setDevice(device);
        log.setAction(Action.ACCESS_REVOKED);
        auditLogRepository.save(log);

        return "Dostęp został odebrany.";
    }
}
