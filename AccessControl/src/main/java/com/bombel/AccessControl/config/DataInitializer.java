package com.bombel.AccessControl.config;

import com.bombel.AccessControl.device.Device;
import com.bombel.AccessControl.device.DeviceRepository;
import com.bombel.AccessControl.user.User;
import com.bombel.AccessControl.user.UserRepository;
import com.bombel.AccessControl.user.UserRole;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    private static final String MOCK_ESP32_MAC = "A0:B1:C2:D3:E4:F5";

    @Bean
    CommandLineRunner initDatabase(DeviceRepository deviceRepository, UserRepository userRepository) {
        return args -> {

            // 1. Inicjalizacja udawanego zamka (ESP32)
            if (!deviceRepository.existsByMacAddress(MOCK_ESP32_MAC)) {
                Device mockDevice = new Device("Zamek Główny (Prototyp)", MOCK_ESP32_MAC);
                deviceRepository.save(mockDevice);
                System.out.println("[ZAINICJALIZOWANO] Dodano testowe urządzenie ESP32: " + MOCK_ESP32_MAC);
            }

            // 2. Inicjalizacja domyślnego Admina (Opcjonalnie, dla wygody testów)
            if (!userRepository.existsByIdentifier("ADMIN_MASTER_KEY")) {
                User admin = new User();
                admin.setName("Główny Inżynier");
                admin.setIdentifier("ADMIN_MASTER_KEY");
                admin.setUserRole(UserRole.ADMIN);
                userRepository.save(admin);
                System.out.println("[ZAINICJALIZOWANO] Dodano konto Super Admina.");
            }
        };
    }
}
