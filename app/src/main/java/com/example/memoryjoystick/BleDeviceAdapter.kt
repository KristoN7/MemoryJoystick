package com.example.memoryjoystick

import android.Manifest
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView

class BleDeviceAdapter(
    private val devices: MutableList<ScanResult>,
    private val onDeviceClickListener: (ScanResult) -> Unit
) : RecyclerView.Adapter<BleDeviceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deviceNameTextView: TextView = view.findViewById(R.id.device_name)
        val deviceAddressTextView: TextView = view.findViewById(R.id.device_address)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ble_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices[position]
        if (ActivityCompat.checkSelfPermission(
                holder.itemView.context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            holder.deviceNameTextView.text = device.device.name ?: "Nazwa nieznana"
        } else {
            holder.deviceNameTextView.text = "Nazwa niedostępna"
        }
        holder.deviceAddressTextView.text = device.device.address
        holder.itemView.setOnClickListener { onDeviceClickListener(device) }
    }

    override fun getItemCount() = devices.size

    fun addDevice(newDevice: ScanResult) {
        if (!devices.any { it.device.address == newDevice.device.address }) {
            devices.add(newDevice)
            notifyItemInserted(devices.size - 1)
        }
    }

    fun clearDevices() {
        devices.clear()
        notifyDataSetChanged()
    }
}