package com.bombel.AccessControl.accessrule.dto;

import lombok.Data;

@Data
public class GrantAccessRequest {
    private Long adminId;
    private Long targetUserId;
    private Long deviceId;
}
