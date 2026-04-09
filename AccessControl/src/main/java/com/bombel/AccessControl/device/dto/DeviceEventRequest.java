package com.bombel.AccessControl.device.dto;

import lombok.Data;

@Data
public class DeviceEventRequest {
    private String macAddress;
    private String userIdentifier;
}
