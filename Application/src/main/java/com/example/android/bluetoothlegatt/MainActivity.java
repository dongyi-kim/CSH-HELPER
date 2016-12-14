package com.example.android.bluetoothlegatt;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.UUID;

/**
 * Created by waps12b on 16. 12. 12..
 */
public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private Button btnSearchBle;
    private ImageView imgBleOn;
    private ImageView imgBleOff;
    private TextView txtDeviceStatus;
    private TextView txtDeviceName;

    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;

    private boolean mConnected = false;

    public static final int REQUEST_BLE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSearchBle = (Button) findViewById(R.id.btn_search_ble);
        imgBleOn = (ImageView) findViewById(R.id.img_ble_enabled);
        imgBleOff = (ImageView) findViewById(R.id.img_ble_disabled);

        txtDeviceName = (TextView) findViewById(R.id.txt_device_name);
        txtDeviceStatus = (TextView) findViewById(R.id.txt_device_status);

        btnSearchBle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DeviceScanActivity.class);
                startActivityForResult(intent, REQUEST_BLE);
            }
        });

        updateConnectionState(R.string.disconnected);
        requestPermission();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode)
        {
            case REQUEST_BLE:
                if(resultCode == Activity.RESULT_OK)
                {
                    mDeviceAddress = data.getStringExtra(BluetoothLeService.EXTRAS_DEVICE_ADDRESS);
                    mDeviceName  = data.getStringExtra(BluetoothLeService.EXTRAS_DEVICE_NAME);
                    mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
                    Intent gattServiceIntent = new Intent("START_BLE_SERVICE");
                    gattServiceIntent.setPackage("com.example.android.bluetoothlegatt");
                    gattServiceIntent.putExtra(BluetoothLeService.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
                    gattServiceIntent.putExtra(BluetoothLeService.EXTRAS_DEVICE_NAME, mDeviceName);
                    startService(gattServiceIntent);
                }
                break;
        }
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                updateConnectionState(R.string.connected);
//                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                updateConnectionState(R.string.disconnected);
//                invalidateOptionsMenu();
            }
//            else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
//                // Show all the supported services and characteristics on the user interface.
//                displayGattServices(mBluetoothLeService.getSupportedGattServices());
//            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
//            }
        }
    };

    protected void updateConnectionState(int state)
    {
        switch (state)
        {
            case R.string.connected:
                mConnected = true;
                imgBleOn.setVisibility(View.VISIBLE);
                imgBleOff.setVisibility(View.GONE);
                txtDeviceName.setText(mDeviceName);
                txtDeviceStatus.setText("연결됨");
                btnSearchBle.setVisibility(View.GONE);
                break;
            case R.string.disconnected:
                mConnected = false;
                imgBleOn.setVisibility(View.GONE);
                imgBleOff.setVisibility(View.VISIBLE);
                txtDeviceName.setText("--:--:--");
                txtDeviceStatus.setText("연결되지 않음");
                btnSearchBle.setVisibility(View.VISIBLE);
                break;
        }

    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    private void requestPermission()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.BLUETOOTH) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.BLUETOOTH_ADMIN) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)  ) {
                Toast.makeText(this, "블루투스 사용에 동의해주셔야 합니다", Toast.LENGTH_LONG);
            }
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WAKE_LOCK}, 0);
        }else
        {

        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
