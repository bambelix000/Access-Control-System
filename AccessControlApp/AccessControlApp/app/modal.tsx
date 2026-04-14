import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, Alert } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';

// UWAGA: Wpisz IP swojego laptopa, na którym działa Spring Boot!
const API_URL = "http://192.168.2.2:8080/rules/grant";

export default function AdminModal() {
  const [targetId, setTargetId] = useState('');
  const [deviceId, setDeviceId] = useState('1'); // Domyślnie ID 1 dla PoC

  const handleGrantAccess = async () => {
    const adminIdStr = await AsyncStorage.getItem('USER_IDENTIFIER');
    
    // Prosta walidacja: sprawdzamy, czy "Admin" ma odpowiedni master key
    // W prawdziwym systemie serwer by to weryfikował, tu robimy to dla szybkiego testu
    if (adminIdStr !== 'ADMIN_MASTER_KEY') {
      Alert.alert("Odmowa", "Tylko użytkownik ADMIN_MASTER_KEY może nadawać uprawnienia!");
      return;
    }

    try {
      const response = await fetch(API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          adminId: 1, // Zakładamy, że Admin to ID 1 w bazie danych
          targetUserId: parseInt(targetId), // Tutaj podajesz ID użytkownika docelowego z tabeli Users
          deviceId: parseInt(deviceId)
        })
      });

      if (response.ok) {
        Alert.alert("Sukces", "Uprawnienia nadane pomyślnie. Zamek zaktualizuje się przy najbliższym wybudzeniu.");
        setTargetId('');
      } else {
        Alert.alert("Błąd", "Serwer odrzucił żądanie.");
      }
    } catch (error) {
      Alert.alert("Błąd połączenia", "Nie można połączyć się z serwerem Spring Boot.");
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.label}>ID Użytkownika Docelowego (z bazy danych):</Text>
      <TextInput
        style={styles.input}
        placeholder="Np. 2"
        keyboardType="numeric"
        value={targetId}
        onChangeText={setTargetId}
      />

      <Text style={styles.label}>ID Zamka (z bazy danych):</Text>
      <TextInput
        style={styles.input}
        placeholder="Np. 1"
        keyboardType="numeric"
        value={deviceId}
        onChangeText={setDeviceId}
      />

      <TouchableOpacity style={styles.btnPrimary} onPress={handleGrantAccess}>
        <Text style={styles.btnText}>Nadaj Uprawnienia</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 20, backgroundColor: '#fff' },
  label: { fontSize: 16, fontWeight: 'bold', marginBottom: 5, color: '#333' },
  input: { backgroundColor: '#f9f9f9', padding: 15, borderRadius: 8, borderWidth: 1, borderColor: '#eee', marginBottom: 20 },
  btnPrimary: { backgroundColor: '#000', padding: 15, borderRadius: 8, alignItems: 'center', marginTop: 10 },
  btnText: { color: '#fff', fontWeight: 'bold', fontSize: 16 }
});