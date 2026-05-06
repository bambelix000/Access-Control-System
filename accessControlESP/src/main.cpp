#include <Arduino.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <Wire.h>
#include <RTClib.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <ESP32Servo.h>
#include <LittleFS.h>
#include <ArduinoJson.h>

// ==========================================
// 🔧 KONFIGURACJA
// ==========================================
const int SERVO_PIN = 25;
const int TOUCH_PIN = 32;
const char* ssid = "bambol";
const char* password = "proste12345";
const String serverUrl = "http://20.215.200.34:8080/devices/"; 

const long gmtOffset_sec = 3600;      // UTC+1
const int daylightOffset_sec = 3600; // Czas letni

// ==========================================
// ZMIENNE I OBIEKTY
// ==========================================
Servo zamekServo;
RTC_DS3231 rtc;
bool authorized = false;
bool forceUrgentSync = false;
String lastUsedId = "UNKNOWN";

// --- POBIERANIE CZASU (RTC Z FALLBACKIEM NA NTP) ---
String getTimestamp() {
    if (rtc.begin()) {
        DateTime now = rtc.now();
        if (now.year() > 2023) {
            char buf[25];
            sprintf(buf, "%04d-%02d-%02dT%02d:%02d:%02d", 
                    now.year(), now.month(), now.day(), 
                    now.hour(), now.minute(), now.second());
            return String(buf);
        }
    }
    struct tm timeinfo;
    if (getLocalTime(&timeinfo)) {
        char buf[25];
        strftime(buf, sizeof(buf), "%Y-%m-%dT%H:%M:%S", &timeinfo);
        return String(buf);
    }
    return "2024-01-01T00:00:00";
}

// --- STEROWANIE ZAMKIEM ---
void controlLock(bool open) {
    zamekServo.attach(SERVO_PIN);
    if (open) {
        Serial.println("[Servo] Otwieram...");
        zamekServo.write(90); 
        delay(5000); 
        Serial.println("[Servo] Zamykam...");
        zamekServo.write(0);  
        delay(1500); // Czas na fizyczny powrót mechanizmu
    } else {
        zamekServo.write(0);
        delay(1000);
    }
    zamekServo.detach();
}

// --- LOGOWANIE OFFLINE ---
void saveLogToFlash(String id, String action) {
    DynamicJsonDocument doc(4096);
    File file = LittleFS.open("/logs.json", "r");
    if (file) {
        deserializeJson(doc, file);
        file.close();
    }
    JsonArray logs = doc.as<JsonArray>();
    if (logs.isNull()) logs = doc.to<JsonArray>();

    JsonObject newLog = logs.createNestedObject(); 
    newLog["macAddress"] = WiFi.macAddress();
    newLog["userIdentifier"] = id;
    newLog["action"] = action;
    newLog["eventTime"] = getTimestamp();

    file = LittleFS.open("/logs.json", "w");
    if (file) {
        serializeJson(doc, file);
        file.close();
        Serial.println("[Log] Zapisano zdarzenie: " + action);
    }
}

// --- SYNCHRONIZACJA (LOGI + WHITELISTA + CZAS) ---
void syncWithServer() {
    Serial.println("[WiFi] Łączenie...");
    WiFi.begin(ssid, password);
    int attempts = 0;
    while (WiFi.status() != WL_CONNECTED && attempts < 20) { delay(500); attempts++; }

    if (WiFi.status() == WL_CONNECTED) {
        Serial.println("[WiFi] Połączono. Synchronizacja czasu...");
        configTime(gmtOffset_sec, daylightOffset_sec, "pool.ntp.org", "time.google.com");
        
        struct tm timeinfo;
        if (getLocalTime(&timeinfo)) {
            rtc.adjust(DateTime(timeinfo.tm_year + 1900, timeinfo.tm_mon + 1, timeinfo.tm_mday, 
                               timeinfo.tm_hour, timeinfo.tm_min, timeinfo.tm_sec));
            Serial.println("[RTC] Czas zaktualizowany z NTP");
        }

        // Wysyłanie logów
        File logFile = LittleFS.open("/logs.json", "r");
        if (logFile) {
            DynamicJsonDocument doc(4096);
            deserializeJson(doc, logFile);
            logFile.close();
            JsonArray logs = doc.as<JsonArray>();
            HTTPClient http;
            http.begin(serverUrl + "event");
            http.addHeader("Content-Type", "application/json");
            for (JsonObject log : logs) {
                String out;
                serializeJson(log, out);
                http.POST(out);
            }
            http.end();
            LittleFS.remove("/logs.json");
            Serial.println("[Sync] Logi wysłane.");
        }

        // Pobieranie whitelist
        HTTPClient http;
        http.begin(serverUrl + WiFi.macAddress() + "/whitelist");
        if (http.GET() == 200) {
            File f = LittleFS.open("/whitelist.json", "w");
            f.print(http.getString());
            f.close();
            Serial.println("[Sync] Whitelista pobrana.");
        }
        http.end();
    } else {
        Serial.println("[WiFi] Brak połączenia - tryb offline.");
    }
    WiFi.mode(WIFI_OFF);
}

// --- CALLBACKI BLE ---
class MyCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
        std::string val = pCharacteristic->getValue();
        if (val.length() > 0) {
            String id = String(val.c_str());
            lastUsedId = id;
            Serial.println("[BLE] Otrzymano ID: " + id);

            File f = LittleFS.open("/whitelist.json", "r");
            StaticJsonDocument<1024> d;
            deserializeJson(d, f);
            f.close();

            bool ok = false;
            JsonArray allowedIds = d["allowedIdentifiers"].as<JsonArray>();
            for (String a : allowedIds) {
                if (a == id) { ok = true; break; }
            }

            if (ok) {
                authorized = true;
                saveLogToFlash(id, "LOCK_OPENED");
            } else {
                forceUrgentSync = true;
                saveLogToFlash(id, "ACCESS_DENIED");
            }
        }
    }
};

// ==========================================
// GŁÓWNA LOGIKA
// ==========================================
void setup() {
    Serial.begin(115200);
    Wire.begin(21, 22); // I2C dla RTC
    
    if (!rtc.begin()) Serial.println("[RTC] Błąd - sprawdź połączenie!");
    if (!LittleFS.begin(true)) Serial.println("[FS] Błąd LittleFS!");

    esp_sleep_wakeup_cause_t wakeup_reason = esp_sleep_get_wakeup_cause();

    if (wakeup_reason == ESP_SLEEP_WAKEUP_EXT0) {
        Serial.println("[Wakeup] Wybudzenie sensorem.");
        
        BLEDevice::init("Zamek_ESP32");
        BLEServer *ps = BLEDevice::createServer();
        BLEService *svc = ps->createService("d9a20a9d-bcbc-4d7c-95d5-44b09fd7be1e");
        BLECharacteristic *bc = svc->createCharacteristic("441e4c3d-39f5-4776-bb5f-e58aeb5278d1", BLECharacteristic::PROPERTY_WRITE);
        bc->setCallbacks(new MyCallbacks());
        svc->start();
        BLEDevice::getAdvertising()->start();

        unsigned long wait = millis();
        while (millis() - wait < 15000) {
            if (authorized) { 
                controlLock(true); 
                break; 
            }
            if (forceUrgentSync) {
                Serial.println("[BLE] Nieznany klucz, sprawdzam serwer...");
                break;
            }
            delay(100);
        }
        
        BLEDevice::getAdvertising()->stop();
        syncWithServer();

        // Jeśli klucza nie było w pamięci, ale serwer go dodał - otwórz teraz
        if (forceUrgentSync) {
            File f = LittleFS.open("/whitelist.json", "r");
            StaticJsonDocument<1024> d;
            deserializeJson(d, f);
            f.close();
            bool nowOk = false;
            for (String a : d["allowedIdentifiers"].as<JsonArray>()) {
                if (a == lastUsedId) { nowOk = true; break; }
            }
            if (nowOk) {
                Serial.println("[Sync] Klucz znaleziony na serwerze! Otwieram...");
                controlLock(true);
                saveLogToFlash(lastUsedId, "LOCK_OPENED");
                syncWithServer(); // Wyślij log otwarcia
            }
        }
    } else {
        // Twardy reset lub zasilanie - synchronizuj na start
        syncWithServer();
    }

    Serial.println("[System] Idę spać...");
    esp_sleep_enable_ext0_wakeup((gpio_num_t)TOUCH_PIN, 1);
    esp_deep_sleep_start();
}

void loop() {}