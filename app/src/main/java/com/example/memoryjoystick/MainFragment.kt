package com.example.memoryjoystick

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.memoryjoystick.viewmodel.GameViewModel
import java.util.UUID

private const val REQUEST_ENABLE_BT = 1
private const val BLUETOOTH_CONNECT_PERMISSION_REQUEST_CODE = 3
private const val SERVICE_UUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
private const val JOYSTICK_ACTION_CHAR_UUID = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
private const val CLIENT_CHARACTERISTIC_CONFIG_UUID = "00002902-0000-1000-8000-00805f9b34fb"
private var navigatedToGame = false
class MainFragment : Fragment(R.layout.fragment_main) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var gameViewModel: GameViewModel? = null
    private var bluetoothGatt: BluetoothGatt? = null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            val deviceAddress = gatt?.device?.address
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("GATT", "Połączono z: $deviceAddress")
                bluetoothGatt = gatt
                // Odkrywanie usług po połączeniu
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    gatt?.discoverServices()
                } else {
                    Log.w("GATT", "Brak uprawnień BLUETOOTH_CONNECT do odkrywania usług.")
                    // Możesz tutaj obsłużyć brak uprawnień, np. rozłączyć się
                    gatt?.disconnect()
                }
                gameViewModel?.connectedDeviceAddress?.postValue(deviceAddress)
                activity?.runOnUiThread {
                    findNavController().navigate(R.id.action_mainFragment_to_gameFragment)
                }
                if (!navigatedToGame) {
                    navigatedToGame = true
                    activity?.runOnUiThread {
                        findNavController().navigate(R.id.action_mainFragment_to_gameFragment)
                    }
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("GATT", "Rozłączono z: $deviceAddress")
                closeGattConnection()
                gameViewModel?.connectedDeviceAddress?.postValue(null)
                // Możesz tutaj podjąć działania w przypadku rozłączenia
                navigatedToGame = false
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e("GATT", "Błąd połączenia GATT z $deviceAddress, status: $status")
                closeGattConnection()
                // Obsłuż błąd połączenia
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                Log.i("GATT", "Odkryto usługi na urządzeniu: ${gatt.device?.address}")
                val service = gatt.getService(UUID.fromString(SERVICE_UUID))
                if (service != null) {
                    Log.i("GATT", "Znaleziono serwis: $SERVICE_UUID")
                    val joystickActionCharacteristic = service.getCharacteristic(UUID.fromString(JOYSTICK_ACTION_CHAR_UUID))
                    if (joystickActionCharacteristic != null) {
                        Log.i("GATT", "Znaleziono charakterystykę: $JOYSTICK_ACTION_CHAR_UUID")
                        setCharacteristicNotification(gatt, joystickActionCharacteristic, true)
                    } else {
                        Log.w("GATT", "Nie znaleziono charakterystyki: $JOYSTICK_ACTION_CHAR_UUID")
                    }
                } else {
                    Log.w("GATT", "Nie znaleziono serwisu: $SERVICE_UUID")
                }
            } else {
                Log.w("GATT", "Błąd podczas odkrywania usług, status: $status")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            val value = characteristic?.value
            Log.d("GATT", "Otrzymano dane z charakterystyki ${characteristic?.uuid}: ${value?.joinToString()}")
            if (characteristic?.uuid.toString().equals(JOYSTICK_ACTION_CHAR_UUID, ignoreCase = true)) {
                value?.getOrNull(0)?.let { action ->
                    gameViewModel?.joystickAction?.postValue(action.toInt())
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("GATT", "Odczytano charakterystykę ${characteristic?.uuid}: ${characteristic?.value?.joinToString()}")
            } else {
                Log.w("GATT", "Błąd podczas odczytu charakterystyki ${characteristic?.uuid}, status: $status")
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("GATT", "Zapisano deskryptor ${descriptor?.uuid}")
            } else {
                Log.w("GATT", "Błąd podczas zapisu deskryptora ${descriptor?.uuid}, status: $status")
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gameViewModel = ViewModelProvider(requireActivity()).get(GameViewModel::class.java)

        if (bluetoothAdapter == null) {
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            checkBondedDevices()
        }

        view.findViewById<View>(R.id.scan_button)?.setOnClickListener {
            val settingsIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            startActivity(settingsIntent)
        }
    }

    private fun checkBondedDevices() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    BLUETOOTH_CONNECT_PERMISSION_REQUEST_CODE
                )
                return
            }
            return
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        var foundJoystick = false
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceAddress = device.address
            Log.d("BONDED", "Name: $deviceName, Address: $deviceAddress")
            if (deviceName?.contains("Memory Joystick", ignoreCase = true) == true) {
                Log.i("BONDED", "Znaleziono sparowany dżojstik: $deviceName ($deviceAddress)")
                foundJoystick = true
                connectDevice(device)
                return@forEach
            }
        }

        if (!foundJoystick) {
            Log.i("BONDED", "Nie znaleziono sparowanego dżojstika 'MemoryJoystick'.")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == android.app.Activity.RESULT_OK) {
                checkBondedDevices()
            } else {
                // Użytkownik odrzucił włączenie Bluetooth
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkBondedDevices()
                } else {
                    // Obsłuż brak zgody
                }
            }
            BLUETOOTH_CONNECT_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("Bluetooth Connect", "Udzielono uprawnień BLUETOOTH_CONNECT.")
                    checkBondedDevices()
                } else {
                    Log.w("Bluetooth Connect", "Odrzucono uprawnienia BLUETOOTH_CONNECT.")
                    // Obsłuż brak uprawnień
                }
            }
        }
    }

    private fun connectDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.w("BLE", "Brak uprawnień BLUETOOTH_CONNECT do połączenia z ${device.address}")
            return
        }
        Log.i("BLE", "Inicjowanie połączenia z: ${device.name} (${device.address})")
        device.connectGatt(requireContext(), false, gattCallback)
    }

    override fun onStop() {
        super.onStop()
        closeGattConnection()
        navigatedToGame = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        navigatedToGame = false // Dodatkowe zabezpieczenie
    }

    private fun closeGattConnection() {
        if (bluetoothGatt != null) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                bluetoothGatt?.close()
                bluetoothGatt = null
            } else {
                Log.w("BLE", "Brak uprawnień BLUETOOTH_CONNECT do zamknięcia połączenia GATT.")
                // Możesz spróbować opóźnić zamknięcie lub podjąć inne działania
            }
        }
    }

    private fun setCharacteristicNotification(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        enabled: Boolean
    ) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.w("BLE", "Brak uprawnień BLUETOOTH_CONNECT do ustawiania powiadomień.")
            return
        }
        gatt.setCharacteristicNotification(characteristic, enabled)
        val descriptor = characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG_UUID))
        if (descriptor != null) {
            descriptor.value = if (enabled) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        } else {
            Log.w("BLE", "Nie znaleziono deskryptora CCC dla charakterystyki: ${characteristic.uuid}")
            gatt.setCharacteristicNotification(characteristic, enabled)
        }
    }
}