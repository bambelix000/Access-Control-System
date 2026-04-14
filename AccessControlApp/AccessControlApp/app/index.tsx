import React, { useState, useEffect } from 'react';
import { 
  View, Text, TextInput, TouchableOpacity, StyleSheet, 
  ActivityIndicator, Alert, PermissionsAndroid, Platform 
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Link } from 'expo-router';
import { BleManager } from 'react-native-ble-plx';
import { encode } from 'base-64';

// Zgodnie z Twoim kodem ESP32:
const SERVICE_UUID = "d9a20a9d-bcbc-4d7c-95d5-44b09fd7be1e";
const CHARACTERISTIC_UUID = "441e4c3d-39f5-4776-bb5f-e58aeb5278d1";

// Inicjalizacja menedżera Bluetooth
const bleManager = new BleManager();

export default function HomeScreen() {
  const [identifier, setIdentifier] = useState<string | null>(null);
  const [inputVal, setInputVal] = useState('');
  const [isScanning, setIsScanning] = useState(false);

  // Funkcja prosząca o uprawnienia natywne Androida
  const requestPermissions = async () => {
    if (Platform.OS === 'android') {
      const apiLevel = parseInt(Platform.Version.toString(), 10);

      if (apiLevel < 31) {
        const granted = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION
        );
        return granted === PermissionsAndroid.RESULTS.GRANTED;
      } else {
        const result = await PermissionsAndroid.requestMultiple([
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
          PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
        ]);

        return (
          result['android.permission.BLUETOOTH_SCAN'] === PermissionsAndroid.RESULTS.GRANTED &&
          result['android.permission.BLUETOOTH_CONNECT'] === PermissionsAndroid.RESULTS.GRANTED &&
          result['android.permission.ACCESS_FINE_LOCATION'] === PermissionsAndroid.RESULTS.GRANTED
        );
      }
    }
    return true;
  };

  useEffect(() => {
    const init = async () => {
      // 1. Poproś o uprawnienia przy starcie
      const hasPermissions = await requestPermissions();
      if (!hasPermissions) {
        Alert.alert("Brak uprawnień", "Aplikacja potrzebuje zgody na Bluetooth i Lokalizację do działania.");
      }

      // 2. Wczytaj zapisany identyfikator
      const savedId = await AsyncStorage.getItem('USER_IDENTIFIER');
      if (savedId) setIdentifier(savedId);
    };
    init();
  }, []);

  const handleLogin = async () => {
    if (!inputVal.trim()) return;
    await AsyncStorage.setItem('USER_IDENTIFIER', inputVal.trim());
    setIdentifier(inputVal.trim());
  };

  const handleLogout = async () => {
    await AsyncStorage.removeItem('USER_IDENTIFIER');
    setIdentifier(null);
  };

  const openLockBLE = async () => {
    if (!identifier) return;
    setIsScanning(true);

    console.log("Rozpoczynam skanowanie BLE...");

    bleManager.startDeviceScan([], null, async (error, device) => {
      if (error) {
        console.log("Błąd skanowania:", error);
        Alert.alert("Błąd BLE", error.message);
        setIsScanning(false);
        return;
      }

      if (device && (device.name === "Zamek_ESP32" || device.localName === "Zamek_ESP32" || device.serviceUUIDs?.includes(SERVICE_UUID))) {
        console.log("Znaleziono zamek!");
        bleManager.stopDeviceScan();
        
        try {
          const connectedDevice = await device.connect();
          console.log("Połączono. Szukam usług...");
          await connectedDevice.discoverAllServicesAndCharacteristics();
          
          console.log("Wysyłam klucz...");
          await connectedDevice.writeCharacteristicWithResponseForService(
            SERVICE_UUID,
            CHARACTERISTIC_UUID,
            encode(identifier)
          );

          Alert.alert("Sukces", "Zamek otwarty!");
        } catch (err: any) {
          Alert.alert("Błąd połączenia", err.message);
        } finally {
          setIsScanning(false);
        }
      }
    });

    // Timeout: 10 sekund
    setTimeout(() => {
      bleManager.stopDeviceScan();
      setIsScanning((prev) => {
        if (prev) {
          Alert.alert("Błąd", "Nie znaleziono zamka. Upewnij się, że dotknąłeś sensora TTP223.");
        }
        return false;
      });
    }, 10000);
  };

  if (!identifier) {
    return (
      <View style={styles.container}>
        <Text style={styles.title}>Autoryzacja Klucza</Text>
        <TextInput
          style={styles.input}
          placeholder="Wpisz swój identyfikator (np. ANDRZEJ)"
          value={inputVal}
          onChangeText={setInputVal}
          autoCapitalize="none"
        />
        <TouchableOpacity style={styles.btnPrimary} onPress={handleLogin}>
          <Text style={styles.btnText}>Zapisz w Telefonie</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.welcome}>Zalogowano jako: {identifier}</Text>
      
      <TouchableOpacity 
        style={[styles.btnOpen, isScanning && styles.btnDisabled]} 
        onPress={openLockBLE}
        disabled={isScanning}
      >
        {isScanning ? (
          <ActivityIndicator color="#fff" size="large"/>
        ) : (
          <Text style={styles.btnOpenText}>OTWÓRZ ZAMEK</Text>
        )}
      </TouchableOpacity>

      <View style={styles.footerRow}>
        <Link href="/modal" asChild>
          <TouchableOpacity style={styles.btnSecondary}>
            <Text style={styles.btnTextDark}>Panel Admina</Text>
          </TouchableOpacity>
        </Link>
        <TouchableOpacity style={styles.btnDanger} onPress={handleLogout}>
          <Text style={styles.btnText}>Wyloguj</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', padding: 20, backgroundColor: '#f5f5f5' },
  title: { fontSize: 24, fontWeight: 'bold', marginBottom: 20, textAlign: 'center' },
  welcome: { fontSize: 18, color: '#666', marginBottom: 40, textAlign: 'center' },
  input: { backgroundColor: '#fff', padding: 15, borderRadius: 8, borderWidth: 1, borderColor: '#ddd', marginBottom: 20 },
  btnPrimary: { backgroundColor: '#007bff', padding: 15, borderRadius: 8, alignItems: 'center' },
  btnOpen: { backgroundColor: '#28a745', padding: 30, borderRadius: 100, alignItems: 'center', justifyContent: 'center', height: 200, width: 200, alignSelf: 'center', marginBottom: 40, elevation: 5 },
  btnDisabled: { backgroundColor: '#94d3a2' },
  btnOpenText: { color: '#fff', fontSize: 20, fontWeight: 'bold', textAlign: 'center' },
  btnSecondary: { backgroundColor: '#e2e3e5', padding: 15, borderRadius: 8, flex: 1, marginRight: 10, alignItems: 'center' },
  btnDanger: { backgroundColor: '#dc3545', padding: 15, borderRadius: 8, flex: 1, marginLeft: 10, alignItems: 'center' },
  btnText: { color: '#fff', fontWeight: 'bold' },
  btnTextDark: { color: '#333', fontWeight: 'bold' },
  footerRow: { flexDirection: 'row', justifyContent: 'space-between' }
});