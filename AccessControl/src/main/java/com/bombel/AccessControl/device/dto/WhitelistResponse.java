package com.bombel.AccessControl.device.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class WhitelistResponse {
    private List<String> allowedIdentifiers;
}
