package com.bombel.AccessControl.accessrule;

import com.bombel.AccessControl.device.Device;
import com.bombel.AccessControl.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "access_rules")
@Getter
@Setter
@NoArgsConstructor
public class AccessRule {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "access_seq")
    @SequenceGenerator(name = "access_seq", sequenceName = "access_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User userId;

    @ManyToOne
    @JoinColumn(name = "device_id")
    private Device deviceId;

}
