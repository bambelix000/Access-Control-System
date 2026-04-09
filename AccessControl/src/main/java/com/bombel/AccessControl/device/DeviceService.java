package com.bombel.AccessControl.device;

import com.bombel.AccessControl.accessrule.AccessRule;
import com.bombel.AccessControl.accessrule.AccessRuleRepository;
import com.bombel.AccessControl.auditlog.Action;
import com.bombel.AccessControl.auditlog.AuditLog;
import com.bombel.AccessControl.auditlog.AuditLogRepository;
import com.bombel.AccessControl.device.dto.DeviceEventRequest;
import com.bombel.AccessControl.device.dto.WhitelistResponse;
import com.bombel.AccessControl.user.User;
import com.bombel.AccessControl.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service

public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final AccessRuleRepository accessRuleRepository;

    @Autowired
    public DeviceService(DeviceRepository deviceRepository, UserRepository userRepository, AuditLogRepository auditLogRepository, AccessRuleRepository accessRuleRepository) {
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.accessRuleRepository = accessRuleRepository;
    }

    @Transactional(readOnly = true)
    public WhitelistResponse getDeviceWhitelist(String macAddress) {

        Device device = deviceRepository.findByMacAddress(macAddress)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono urządzenia o MAC: " + macAddress));

        List<AccessRule> rules = accessRuleRepository.findAllByDevice(device);

        List<String> allowedIdentifiers = rules.stream()
                .map(rule -> rule.getTargetUser().getIdentifier())
                .toList();

        return new WhitelistResponse(allowedIdentifiers);
    }

    @Transactional
    public void reportEvent(DeviceEventRequest request) {
        Device device = deviceRepository.findByMacAddress(request.getMacAddress())
                .orElseThrow(() -> new IllegalArgumentException("Nieznany zamek."));

        User user = userRepository.findByIdentifier(request.getUserIdentifier())
                .orElseThrow(() -> new IllegalArgumentException("Nieznany użytkownik."));

        AuditLog log = new AuditLog();
        log.setTimestamp(LocalDateTime.now());
        log.setDevice(device);
        log.setTargetUser(user);
        log.setAction(Action.LOCK_OPENED);
        auditLogRepository.save(log);
    }
}