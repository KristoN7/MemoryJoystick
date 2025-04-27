package com.example.memoryjoystick

import android.Manifest
import android.bluetooth.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.memoryjoystick.viewmodel.GameViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.util.UUID

class MainFragment : Fragment() {
    private lateinit var difficultyButton: Button
    private val gameViewModel: GameViewModel by activityViewModels()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private val deviceAddress = "CC:7B:5C:24:BA:36"
    private lateinit var connectionStatusTextView: TextView
    private lateinit var connectButton: Button

    //UUID usługi i charakterystyki
    val SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    val JOYSTICK_ACTION_CHAR_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            activity?.runOnUiThread {
                characteristic?.let {
                    val data = it.value
                    if (data.isNotEmpty()) {

                        val action = data[0]
                        Log.d("Joystick", "Odebrano akcję z joysticka: $action")

                        Log.d("GameViewModel_Debug", "cards.value: ${gameViewModel.cards.value}")
                        Log.d(
                            "GameViewModel_Debug",
                            "selectedCardIndex.value: ${gameViewModel.selectedCardIndex.value}"
                        )

                        val currentCards = gameViewModel.cards.value
                        val currentSize = currentCards?.size

                        Log.d(
                            "GameViewModel_Debug",
                            "Rozmiar listy kart w onCharacteristicChanged: $currentSize"
                        )

                        if (currentCards == null) {
                            Log.d(
                                "JOYSTICK_ACTION",
                                "Lista kart jest NULL, akcja nie zostanie obsłużona."
                            )

                        }

                        gameViewModel.handleJoystickAction(action)
                    }
                }
            }
        }


        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            val deviceAddress = gatt?.device?.address
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("GATT", "Połączono z: $deviceAddress")
                bluetoothGatt = gatt
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    gatt?.discoverServices()
                    updateConnectionStatus("Connected with $deviceAddress")

                    // Bezpośrednio tu NIE nawigujemy — zamiast tego odpalamy coroutine na viewLifecycleOwner:
                    viewLifecycleOwner.lifecycleScope.launch {
                        yield() // poczekaj na bezpieczne miejsce w cyklu życia
                        findNavController().navigate(R.id.action_mainFragment_to_difficultyFragment)
                    }

                } else {
                    Log.w("GATT", "Brak uprawnień BLUETOOTH_CONNECT do odkrywania usług.")
                    updateConnectionStatus("Brak uprawnień do usług")
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        gatt?.disconnect()
                    }
                }
                gameViewModel.connectedDeviceAddress.postValue(deviceAddress)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("GATT", "Rozłączono z: $deviceAddress")
                closeGattConnection()
                gameViewModel.connectedDeviceAddress.postValue(null)
                updateConnectionStatus("Rozłączono")
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e("GATT", "Błąd połączenia GATT z $deviceAddress, status: $status")
                closeGattConnection()
                updateConnectionStatus("Błąd połączenia: $status")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                val service = gatt?.getService(SERVICE_UUID)
                val characteristic = service?.getCharacteristic(JOYSTICK_ACTION_CHAR_UUID)


                if (characteristic != null) {
                    if (context?.let {
                            ActivityCompat.checkSelfPermission(
                                it,
                                Manifest.permission.BLUETOOTH_CONNECT
                            )
                        } == PackageManager.PERMISSION_GRANTED
                    ) {
                        gatt.setCharacteristicNotification(characteristic, true)
                        val descriptor = characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG))
                        descriptor?.let {
                            it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(it)
                        }
                    } else {
                        Log.w("BluetoothGatt", "Brak uprawnień BLUETOOTH_CONNECT do włączenia powiadomień.")
                    }
                }
            } else {
                Log.w("BluetoothGatt", "onServicesDiscovered failed with status $status")
            }
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
        difficultyButton = view.findViewById(R.id.difficultyButton)

        difficultyButton.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_difficultyFragment)
        }

        connectionStatusTextView = view.findViewById(R.id.connectionStatusTextView)
        connectButton = view.findViewById(R.id.connectButton)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            updateConnectionStatus("Bluetooth nieobsługiwany")
            connectButton.isEnabled = false
        } else if (!bluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            connectButton.setOnClickListener {
                connectToDevice(deviceAddress)
            }
        }

        gameViewModel.connectedDeviceAddress.observe(viewLifecycleOwner) { address ->
            if (address != null) {
                updateConnectionStatus("Conntected with $address")
                connectButton.isEnabled = false
            } else {
                updateConnectionStatus("Not connected")
                connectButton.isEnabled = true
            }
        }
    }

    private fun connectToDevice(address: String) {
        bluetoothAdapter?.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    updateConnectionStatus("Connecting...")
                    bluetoothGatt = device.connectGatt(requireContext(), false, gattCallback)
                } else {
                    Log.e("Bluetooth", "Brak uprawnień BLUETOOTH_CONNECT")
                    updateConnectionStatus("Brak uprawnień Bluetooth")
                    requestBluetoothConnectPermission()
                }
            } catch (exception: IllegalArgumentException) {
                Log.w("Bluetooth", "Nieprawidłowy adres urządzenia: $address")
                updateConnectionStatus("Nieprawidłowy adres")
            }
        }
    }

    private fun closeGattConnection() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothGatt?.let { gatt ->
                gatt.close()
                bluetoothGatt = null
            }
        } else {
            Log.w("Bluetooth", "Brak uprawnień BLUETOOTH_CONNECT do zamknięcia połączenia GATT.")
            updateConnectionStatus("Brak uprawnień do rozłączenia")
        }
    }

    private fun updateConnectionStatus(status: String) {
        activity?.runOnUiThread {
            connectionStatusTextView.text = status
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == android.app.Activity.RESULT_OK) {
                connectButton.setOnClickListener {
                    connectToDevice(deviceAddress)
                }
            } else {
                updateConnectionStatus("Bluetooth nie został włączony")
                connectButton.isEnabled = false
            }
        }
    }

    private fun requestBluetoothConnectPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_BLUETOOTH_CONNECT_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_BLUETOOTH_CONNECT_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    connectToDevice(deviceAddress)
                } else {
                    updateConnectionStatus("Odmówiono uprawnień Bluetooth")
                }
            }
        }
    }

    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val REQUEST_BLUETOOTH_CONNECT_PERMISSION = 2
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        closeGattConnection()
    }
}
