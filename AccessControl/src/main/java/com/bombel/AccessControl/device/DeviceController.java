package com.bombel.AccessControl.device;

import com.bombel.AccessControl.device.dto.DeviceEventRequest;
import com.bombel.AccessControl.device.dto.WhitelistResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping("/{macAddress}/whitelist")
    public ResponseEntity<?> getWhitelist(@PathVariable String macAddress) {
        try {
            WhitelistResponse response = deviceService.getDeviceWhitelist(macAddress);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/event")
    public ResponseEntity<?> reportEvent(@RequestBody DeviceEventRequest request) {
        deviceService.reportEvent(request);
        return ResponseEntity.ok().build();
    }
}