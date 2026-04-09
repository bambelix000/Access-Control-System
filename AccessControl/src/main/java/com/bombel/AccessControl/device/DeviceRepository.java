package com.bombel.AccessControl.device;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    boolean existsByMacAddress(String macAddress);

    Optional<Device> findByMacAddress(String macAddress);
}
