package com.bombel.AccessControl.accessrule;

import com.bombel.AccessControl.accessrule.dto.GrantAccessRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/access")
public class AccessRuleController {
    private final AccessRuleService accessRuleService;

    @Autowired
    public AccessRuleController(AccessRuleService accessRuleService) {
        this.accessRuleService = accessRuleService;
    }

    @PostMapping("/grant")
    public ResponseEntity<?> grantAccess(@RequestBody GrantAccessRequest request) {
        try {
            String result = accessRuleService.grantAccess(request);
            return ResponseEntity.ok(result);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/revoke")
    public ResponseEntity<?> revokeAccess(@RequestBody GrantAccessRequest request) {
        try {
            return ResponseEntity.ok(accessRuleService.revokeAccess(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
