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

private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val LOCATION_PERMISSION_REQUEST_CODE = 2

class MainFragment : Fragment() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private val scanResults = mutableListOf<ScanResult>()
    private lateinit var bleDeviceAdapter: BleDeviceAdapter
    private lateinit var recyclerView: RecyclerView

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
            Log.d("BLE", "Wybrano urządzenie: ${selectedDevice.device.name} (${selectedDevice.device.address})")
            stopBleScan() // Zatrzymaj skanowanie po wybraniu urządzenia
            // Tutaj dodaj logikę łączenia z wybranym urządzeniem
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
                bluetoothLeScanner.startScan(null, null, scanCallback)
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

    override fun onStop() {
        super.onStop()
        stopBleScan()
    }

    companion object {
        private const val SCAN_DURATION: Long = 10000 // Czas trwania skanowania w milisekundach
    }
}