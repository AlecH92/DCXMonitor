package com.tinkermotion.dcxmonitor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    String address;
    public BluetoothAdapter mBTAdapter = null;
    public BluetoothSocket mBTSocket = null;
    public BluetoothDevice mBTDevice = null;
    public InputStream mBTInputStream  = null;
    public OutputStream mBTOutputStream = null;
    public static boolean enableDataSend;
    public SharedPreferences preferences;
    TextView connectionStatus;
    private FloatingActionButton fab;
    private TextView textBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        String voltageString = "24.56v";
        String currentString = "10amps";
        String temperatureString = "24.56c";
        String throttleString = "10%";
        preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();

        connectionStatus = (TextView) findViewById(R.id.connectionStatus);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        textBox = (TextView) findViewById(R.id.textView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Updating...", Snackbar.LENGTH_INDEFINITE).show();
                sendDataByte((byte) 1);
            }
        });
        final Handler handler = new Handler();
        Runnable checkData = new Runnable() {
            @Override
            public void run() {
                try {
                    if(mBTInputStream != null) {
                        if (mBTInputStream.available() > 0) {
                            Log.d("DCXMonitor", "reading data");
                            final String data = fromStream(mBTInputStream, 4);
                            Log.d("DCXMonitor", "Got value: " + data);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String[] lines = data.split(System.getProperty("line.separator"));
                                    String textForBox =
                                            "Voltage:\t" + lines[0] + System.getProperty("line.separator") +
                                            "Current:\t" + lines[1] + System.getProperty("line.separator") +
                                            "Temperature:\t" + lines[2] + System.getProperty("line.separator") +
                                            "Throttle:\t" + lines[3];
                                    textBox.setText(textForBox);
                                    Snackbar.make(findViewById(android.R.id.content), "Refreshed", Snackbar.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                handler.postDelayed(this, 750);
            }
        };
        handler.postDelayed(checkData, 750);
        textBox.setText("Voltage: " + voltageString + "\r\n" + "Current:" + currentString + "\r\n" + "Temperature:" + temperatureString + "\r\n" + "Throttle:" + throttleString);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.connect:
                ThreadExecutor.runTask(new Runnable() {
                    public void run() {
                        if (connect()) {
                            enableDataSend = true;
                            runOnUiThread(new Thread(new Runnable() {
                                public void run() {
                                    connectionStatus.setText("Connected");
                                    connectionStatus.setTextColor(Color.GREEN);
                                }
                            }));
                        }
                    }
                });
                return true;
            case R.id.disconnect:
                enableDataSend = false;
                resetConnection();
                connectionStatus.setText("Disconnected");
                connectionStatus.setTextColor(Color.RED);
                return true;
            case R.id.settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private void sendDataByte(byte message) {
        if(enableDataSend) {
            try {
                mBTOutputStream.write(message);
            } catch (IOException e) {
            }
        }
    }
    private void sendDataInt(Integer message) {
        if(enableDataSend) {
            Log.d("DCXMonitor", "Send data: " + message);
            try {
                mBTOutputStream.write(message);
            } catch (IOException e) {
            }
        }
    }
    private void resetConnection() {
        if (mBTInputStream != null) {
            try {mBTInputStream.close();} catch (Exception e) {}
            mBTInputStream = null;
        }
        if (mBTOutputStream != null) {
            try {mBTOutputStream.close();} catch (Exception e) {}
            mBTOutputStream = null;
        }
        if (mBTSocket != null) {
            try {mBTSocket.close();} catch (Exception e) {}
            mBTSocket = null;
        }
    }

    public boolean connect() {
        address = preferences.getString("btdevice", "00:00:00:00:00:00");
        resetConnection();
        if (mBTDevice == null) {
            mBTDevice = mBTAdapter.getRemoteDevice(address);
        }
        try {mBTSocket = mBTDevice.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (Exception e1) {
            return false;
        }
        try {
            mBTSocket.connect();
        } catch (Exception e) {
            return false;
        }
        try {
            mBTOutputStream = mBTSocket.getOutputStream();
            mBTInputStream  = mBTSocket.getInputStream();
            //beginListenForData();
        } catch (Exception e) {
            return false;
        }
        return true;
    }
    public static String fromStream(InputStream in, Integer lines) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder out = new StringBuilder();
        String newLine = System.getProperty("line.separator");
        String line;
        Integer loopLines = 0;
        while ((line = reader.readLine()) != null) {
            out.append(line);
            out.append(newLine);
            Log.d("DCXMonitor", "line: " + line);
            loopLines++;
            if(loopLines == lines) break;
        }
        Log.d("DCXMonitor", "returning");
        return out.toString();
    }
}
