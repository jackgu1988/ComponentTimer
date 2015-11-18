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
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_DEFAULT;
    private BluetoothAdapter mBTAdapter;
    private TextView time;
    private long wifi_start;
    private long wifi_stop;
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
    private long bt_start;
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
    private AudioRecord recorder;
    private Thread th;
    private int BufferElements2Rec = 1024;
    private int BytesPerElement = 2;
    private boolean isRecording = false;
    private int cnt = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        askPermissions();

        time = (TextView) findViewById(R.id.time);
        time.setText(String.format(getString(R.string.time_txt), (wifi_stop - wifi_start)));
    }

    private void startRecording() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        isRecording = true;

        th = new Thread(new Runnable() {
            @Override
            public void run() {
                recorder.startRecording();

                writeAudioDataToFile();
            }
        });

        th.start();
    }

    private void writeAudioDataToFile() {
        // Write the output audio in byte
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/8k16bitMono" + cnt++ + ".pcm";

        short sData[] = new short[BufferElements2Rec];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (isRecording) {
            recorder.read(sData, 0, BufferElements2Rec);
            try {
                byte bData[] = short2byte(sData);

                assert os != null;
                os.write(bData, 0, BufferElements2Rec * BytesPerElement);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            assert os != null;
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];

        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

    private void stopRecording() {
        if (null != recorder) {
            isRecording = false;

            recorder.stop();
            recorder.release();

            recorder = null;
            th = null;
        }
    }

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

    public void recordSound(View v) {
        startRecording();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                stopRecording();
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        removeListeners();
    }
}
