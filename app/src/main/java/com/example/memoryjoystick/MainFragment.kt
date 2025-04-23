package com.example.memoryjoystick

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.os.Handler
import android.os.Looper
import java.util.UUID
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import java.util.ArrayList

private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val LOCATION_PERMISSION_REQUEST_CODE = 2

class MainFragment : Fragment() {

    private val SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    private val JOYSTICK_ACTION_CHAR_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    private val CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private val scanResults = mutableListOf<ScanResult>()
    private lateinit var bleDeviceAdapter: BleDeviceAdapter
    private lateinit var recyclerView: RecyclerView

    private var bluetoothGatt: BluetoothGatt? = null
    private var joystickActionCharacteristic: BluetoothGattCharacteristic? = null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            val deviceAddress = gatt?.device?.address
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("BLE", "Połączono z $deviceAddress")
                    // Sprawdź, czy mamy uprawnienie BLUETOOTH_CONNECT przed odkrywaniem serwisów
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        try {
                            bluetoothGatt?.discoverServices() // Odkryj serwisy po połączeniu
                            deviceConnected = true
                        } catch (e: SecurityException) {
                            Log.e("BLE", "Brak uprawnień BLUETOOTH_CONNECT do odkrywania serwisów: ${e.message}")
                            // Obsłuż brak uprawnień, np. wyświetl komunikat
                            closeConnection()
                            deviceConnected = false
                        }
                    } else {
                        Log.w("BLE", "Nie uzyskano uprawnień BLUETOOTH_CONNECT do odkrywania serwisów.")
                        closeConnection()
                        deviceConnected = false
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BLE", "Rozłączono z $deviceAddress")
                    closeConnection()
                    deviceConnected = false
                }
            } else {
                Log.e("BLE", "Błąd połączenia z $deviceAddress, kod: $status")
                closeConnection()
                deviceConnected = false
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w("BLE", "Odkryto serwisy na ${gatt?.device?.address}")
                gatt?.getService(SERVICE_UUID)?.let { service ->
                    joystickActionCharacteristic = service.getCharacteristic(JOYSTICK_ACTION_CHAR_UUID)
                    joystickActionCharacteristic?.let { characteristic ->
                        enableNotifications(gatt, characteristic)
                    } ?: Log.w("BLE", "Nie znaleziono charakterystyki akcji dżojstika.")
                } ?: Log.w("BLE", "Nie znaleziono serwisu UART.")
            } else {
                Log.w("BLE", "Błąd podczas odkrywania serwisów, kod: $status")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            characteristic?.let { char ->
                if (char.uuid == JOYSTICK_ACTION_CHAR_UUID) {
                    val value = char.value
                    if (value != null && value.isNotEmpty()) {
                        val actionValue = value[0].toInt()
                        Log.d("BLE", "Odebrano akcję: $actionValue")
                        // Tutaj będziemy przetwarzać akcje dżojstika
                    } else {
                        Log.w("BLE", "Odebrano pustą lub null wartość charakterystyki akcji.")
                    }
                }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w("BLE", "Powiadomienia włączone dla ${descriptor?.characteristic?.uuid}")
            } else {
                Log.w("BLE", "Błąd podczas włączania powiadomień, kod: $status")
            }
        }
    }

    private var deviceConnected = false

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let {
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val deviceName = it.device.name
                    if (deviceName == "Memory Joystick") {
                        bleDeviceAdapter.addDevice(it)
                    } else if (deviceName != null) {
                        Log.d("BLE Scan", "Znaleziono inne urządzenie: $deviceName (${it.device.address})")
                    } else {
                        Log.d("BLE Scan", "Znaleziono urządzenie (nazwa niedostępna): (${it.device.address})")
                    }
                } else {
                    Log.w("BLE Scan", "Brak uprawnień BLUETOOTH_CONNECT do uzyskania nazwy urządzenia.")
                    bleDeviceAdapter.addDevice(it) // Dodaj bez nazwy, ale z adresem
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BLE Scan", "Skanowanie BLE nie powiodło się z kodem: $errorCode")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.ble_devices_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        bleDeviceAdapter = BleDeviceAdapter(scanResults) { selectedDevice ->
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("BLE", "Wybrano urządzenie: ${selectedDevice.device.name} (${selectedDevice.device.address})")
            } else {
                Log.d("BLE", "Wybrano urządzenie (nazwa niedostępna): ${selectedDevice.device.address}")
            }
            stopBleScan() // Zatrzymaj skanowanie po wybraniu urządzenia
            connectToDevice(selectedDevice.device) // Rozpocznij łączenie
        }
        recyclerView.adapter = bleDeviceAdapter

        val bluetoothManager = requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner ?: return

        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        } else {
            checkLocationPermissionAndStartScanning()
        }
    }

    private fun checkLocationPermissionAndStartScanning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
                return
            }
        }
        startBleScan()
    }

    private fun startBleScan() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("BLE Scan", "Rozpoczęto skanowanie BLE...")
            bleDeviceAdapter.clearDevices() // Wyczyść listę przed rozpoczęciem nowego skanowania
            scanResults.clear()
            try {
                val filters: List<ScanFilter> = ArrayList() // Pusta lista filtrów
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // Możesz dostosować tryb skanowania
                    .build()

                bluetoothLeScanner.startScan(
                    filters,
                    settings,
                    scanCallback
                )
                // Możesz zatrzymać skanowanie po pewnym czasie:
                // Handler(Looper.getMainLooper()).postDelayed({ stopBleScan() }, SCAN_DURATION)
            } catch (e: SecurityException) {
                Log.e("BLE Scan", "Brak uprawnień do rozpoczęcia skanowania BLE: ${e.message}")
                // Obsłuż brak uprawnień
            }
        } else {
            Log.w("BLE Scan", "Nie uzyskano uprawnień lokalizacji do rozpoczęcia skanowania BLE.")
            // Obsłuż brak uprawnień
        }
    }

    private fun stopBleScan() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("BLE Scan", "Zatrzymano skanowanie BLE.")
            try {
                bluetoothLeScanner.stopScan(scanCallback)
            } catch (e: SecurityException) {
                Log.e("BLE Scan", "Brak uprawnień do zatrzymania skanowania BLE: ${e.message}")
                // Obsłuż brak uprawnień
            }
        } else {
            Log.w("BLE Scan", "Nie uzyskano uprawnień lokalizacji do zatrzymania skanowania BLE.")
            // Obsłuż brak uprawnień
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ENABLE_BLUETOOTH_REQUEST_CODE) {
            if (resultCode == android.app.Activity.RESULT_OK) {
                checkLocationPermissionAndStartScanning()
            } else {
                Log.w("BLE", "Użytkownik odrzucił włączenie Bluetooth.")
                // Obsłuż sytuację, gdy Bluetooth nie jest włączony
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startBleScan()
                } else {
                    Log.w("Location", "Użytkownik odrzucił uprawnienia lokalizacji.")
                    // Obsłuż sytuację, gdy uprawnienia lokalizacji nie są przyznane
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        Log.w("BLE", "Próba połączenia z ${device.address}")
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12 (API 31)
                device.connectGatt(requireContext(), false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(requireContext(), false, gattCallback)
            }
        } else {
            Log.w("BLE", "Nie uzyskano uprawnień BLUETOOTH_CONNECT do połączenia z urządzeniem.")
            // Tutaj możesz poinformować użytkownika o braku uprawnień
            // i ewentualnie poprosić o nie.
        }
    }

    private fun enableNotifications(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic) {
        if (gatt == null || characteristic == null) return

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            gatt.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID)
            descriptor?.let {
                it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(it)
            } ?: Log.w("BLE", "Nie znaleziono deskryptora CCCD.")
        } else {
            Log.w("BLE", "Nie uzyskano uprawnień BLUETOOTH_CONNECT do włączenia powiadomień.")
            // Tutaj możesz obsłużyć brak uprawnień, np. powiadomić użytkownika
        }
    }
    private fun closeConnection() {
        bluetoothGatt?.let { gatt ->
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                gatt.close()
                bluetoothGatt = null
                joystickActionCharacteristic = null
            } else {
                Log.w("BLE", "Nie uzyskano uprawnień BLUETOOTH_CONNECT do zamknięcia połączenia.")
                // Możesz tutaj spróbować innej strategii lub poinformować użytkownika
                bluetoothGatt = null // Upewnij się, że zwalniasz referencję, nawet jeśli nie udało się zamknąć GATT
                joystickActionCharacteristic = null
            }
        }
    }


    override fun onStop() {
        super.onStop()
        stopBleScan()
        closeConnection() // Zamknij połączenie, gdy fragment jest zatrzymywany
    }

    companion object {
        private const val SCAN_DURATION: Long = 10000 // Czas trwania skanowania w milisekundach
    }
}