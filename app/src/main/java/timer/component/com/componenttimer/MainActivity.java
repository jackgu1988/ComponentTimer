package timer.component.com.componenttimer;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter mBTAdapter;
    private TextView time;

    private long wifi_start;
    private long wifi_stop;
    private long bt_start;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        askPermissions();

        time = (TextView) findViewById(R.id.time);
        time.setText(String.format(getString(R.string.time_txt), (wifi_stop - wifi_start)));
    }

    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (Objects.equals(intent.getAction(), WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                wifi_stop = System.currentTimeMillis();
                time.setText(String.format(getString(R.string.time_txt), (wifi_stop - wifi_start)));
                unregisterReceiver(this);
            }
        }
    };

    private final BroadcastReceiver mBTScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                long bt_stop = System.currentTimeMillis();
                time.setText(String.format(getString(R.string.time_txt), (bt_stop - bt_start)));
                mBTAdapter.cancelDiscovery();
                unregisterReceiver(this);
            }
        }
    };

    /**
     * Dirty way to ask permissions, but fits the purposes of this demo app
     */
    private void askPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    1);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    2);
        }
    }

    private void removeListeners() {
        if (mWifiScanReceiver.isOrderedBroadcast())
            unregisterReceiver(mWifiScanReceiver);

        if (mBTScanReceiver.isOrderedBroadcast()) {
            mBTAdapter.cancelDiscovery();
            unregisterReceiver(mBTScanReceiver);
        }
    }

    public void wifiTester(View v) {
        removeListeners();

        WifiManager mWifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        if (mWifiManager.isWifiEnabled()) {
            wifi_start = System.currentTimeMillis();
            this.registerReceiver(mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            mWifiManager.startScan();
        } else
            noWiFi();
    }

    private void noWiFi() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.wifi_head)
                .setMessage(R.string.wifi_body)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void noBT() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.bt_head)
                .setMessage(R.string.bt_body)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public void BTTester(View v) {
        removeListeners();

        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBTAdapter.isEnabled()) {
            bt_start = System.currentTimeMillis();
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            this.registerReceiver(mBTScanReceiver, filter);
            mBTAdapter.startDiscovery();
        } else
            noBT();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        removeListeners();
    }
}
