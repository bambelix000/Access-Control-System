package com.bombel.AccessControl.device.dto;

import com.bombel.AccessControl.auditlog.Action;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceEventRequest {
    private String macAddress;
    private String userIdentifier;
    private Action action;
    private LocalDateTime eventTime;
}
