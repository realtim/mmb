package ru.mmb.sportiduinomanager.adapter;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ru.mmb.sportiduinomanager.MainApp;
import ru.mmb.sportiduinomanager.R;

/**
 * Provides the list of discovered Bluetooth devices.
 */
public class BTDeviceListAdapter extends RecyclerView.Adapter<BTDeviceListAdapter.DeviceHolder> {
    /**
     * Interface for list item click processing.
     */
    private final OnItemClicked mOnClick;
    /**
     * List of discovered Bluetooth devices.
     */
    // TODO: change to LinkedHashSet
    private List<BluetoothDevice> mBTDeviceList;
    /**
     * MAC of connected device (for showing list item with different icon for it).
     */
    private String mConnectedDevice;
    /**
     * True while connection process is active (fow showing hourglass icon).
     */
    private boolean mIsConnecting;

    /**
     * Adapter constructor.
     *
     * @param bluetoothDevices List of detected Bluetooth devices
     * @param onClick          Adapter for processing Connect button click
     */
    public BTDeviceListAdapter(final List<BluetoothDevice> bluetoothDevices, final OnItemClicked onClick) {
        super();
        mBTDeviceList = bluetoothDevices;
        mOnClick = onClick;
        mConnectedDevice = "";
        mIsConnecting = false;
    }

    /**
     * Replace the contents of a view (invoked by the layout manager).
     */
    @Override
    public void onBindViewHolder(@NonNull final DeviceHolder holder, final int position) {
        // Get BT device at this position
        final BluetoothDevice device = mBTDeviceList.get(position);
        // Update the contents of the view with that device
        String deviceName;
        try {
            deviceName = device.getName();
        } catch (SecurityException ignored) {
            deviceName = "";
        }
        holder.mName.setText(holder.itemView.getResources().getString(R.string.device_name,
                deviceName, device.getAddress()));
        if (mConnectedDevice == null || !mConnectedDevice.equals(device.getAddress())) {
            holder.mConnectButton.setImageResource(R.drawable.ic_disconnected);
        } else {
            if (mIsConnecting) {
                holder.mConnectButton.setImageResource(R.drawable.ic_hourglass);
            } else {
                holder.mConnectButton.setImageResource(R.drawable.ic_connected);
            }
        }
        // Disable button while connecting to this/other device
        if (mIsConnecting) {
            holder.mConnectButton.setAlpha(MainApp.DISABLED_BUTTON);
            holder.mConnectButton.setClickable(false);
        } else {
            holder.mConnectButton.setAlpha(MainApp.ENABLED_BUTTON);
            holder.mConnectButton.setClickable(true);
            // Set my listener for Connect button
            holder.mConnectButton
                    .setOnClickListener(view -> mOnClick.onItemClick(holder.getAdapterPosition()));
        }
    }

    /**
     * Create new views (invoked by the layout manager).
     */
    @NonNull
    @Override
    public BTDeviceListAdapter.DeviceHolder onCreateViewHolder(@NonNull final ViewGroup viewGroup, final int viewType) {
        final View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.device_list_item, viewGroup, false);
        return new BTDeviceListAdapter.DeviceHolder(view);
    }

    /**
     * Return the size of device list (invoked by the layout manager).
     */
    @Override
    public int getItemCount() {
        return mBTDeviceList.size();
    }

    /**
     * Remove all devices before new search.
     */
    public void clearList() {
        mBTDeviceList = new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * Add new device to the list.
     *
     * @param device New Bluetooth device to add to our list
     */
    public void insertItem(final BluetoothDevice device) {
        if (mBTDeviceList.contains(device)) {
            return;
        }
        mBTDeviceList.add(device);
        notifyItemInserted(getItemCount());
    }

    /**
     * Get device from the position.
     *
     * @param position Position in the list of devices
     * @return Bluetooth device at that position
     */
    public BluetoothDevice getDevice(final int position) {
        return mBTDeviceList.get(position);
    }

    /**
     * Save MAC of connected device in the adapter.
     *
     * @param address      MAC address as string
     * @param isConnecting True if we are connecting to this device right now
     */
    public void setConnectedDevice(final String address, final boolean isConnecting) {
        mConnectedDevice = address;
        mIsConnecting = isConnecting;
        notifyDataSetChanged();
    }

    /**
     * Declare interface for click processing.
     */
    public interface OnItemClicked {
        /**
         * Implemented in BluetoothActivity class.
         *
         * @param position Position of clicked device in the list of discovered devices
         */
        void onItemClick(int position);
    }

    /**
     * Custom ViewHolder for device_list_item layout.
     */
    public static final class DeviceHolder extends RecyclerView.ViewHolder {
        /**
         * Name of the Bluetooth device.
         */
        private final TextView mName;
        /**
         * Icon to show to the right of the name.
         */
        private final ImageButton mConnectButton;

        /**
         * View holder for a list item with device name and Connect button.
         *
         * @param view View of this list item
         */
        private DeviceHolder(final View view) {
            super(view);
            mName = view.findViewById(R.id.device_name);
            mConnectButton = view.findViewById(R.id.device_connect);
        }
    }
}
