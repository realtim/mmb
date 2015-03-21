package com.example.BCLogger;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bluetooth1.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final int REQUEST_ENABLE_BT = 1;
    // SPP UUID by default
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // MAC-address of Bluetooth device
    //private final String[] address = {"20:13:10:29:02:17", "00:14:02:11:01:53", "00:14:02:11:01:48", "00:14:02:11:01:40", "00:14:02:11:00:23", "98:d3:31:b0:fb:5a"};
    //private String[] address = {"20:13:10:29:02:17", "00:14:02:11:01:53", "00:14:02:11:01:48", "00:14:02:11:01:40", "00:14:02:11:00:23", "98:d3:31:b0:fb:5a"};
    private final ArrayList<String> devBTaddress = new ArrayList<String>();
    private final ArrayList<String> devBTname = new ArrayList<String>();
    private TextView TxtField, TxtBT;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private InputStream inStream = null;
    private long time_old;
    private int LoggerNum = 0;
    private String inBuff = "";
    int logNum = 0;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Button btnCNCT = (Button) findViewById(R.id.btnCNCT);
        Button btnGETS = (Button) findViewById(R.id.btnGETS);
        Button btnEND = (Button) findViewById(R.id.btnEND);
        Button btnGETD = (Button) findViewById(R.id.btnGETD);
        Button btnGETL = (Button) findViewById(R.id.btnGETL);
        Button btnDL = (Button) findViewById(R.id.btnDL);

        TxtField = (TextView) findViewById(R.id.Term);
        TxtField.setMovementMethod(new ScrollingMovementMethod());

        TxtBT = (TextView) findViewById(R.id.TxtBT);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            errorExit("\nBlueTooth adapter not found.");
        } else {
            checkBTState();
            if (LoggerNum < 1) errorExit("No loggers to connect to.");

            btnCNCT.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    //TxtField.setText("");
                    try {
                        btSocket.close();
                        TxtField.append("\nConnection closed.\n");
                    } catch (IOException e) {
                        try {
                            btSocket.close();
                        } catch (IOException e2) {
                            errorExit("unable to close socket " + e2.getMessage() + ".");
                        }
                    }
                    //checkBTState();
                    BluetoothDevice device = btAdapter.getRemoteDevice(devBTaddress.get(Integer.parseInt(TxtBT.getText().toString())));
                    try {
                        btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                    } catch (IOException e) {
                        errorExit("In onResume() and socket create failed: " + e.getMessage() + ".");
                    }
                    // Establish the connection.  This will block until it connects.
                    try {
                        btSocket.connect();
                        TxtField.append("\nConnected to " + TxtBT.getText().toString() + ": " + devBTaddress.get(Integer.parseInt(TxtBT.getText().toString())) + "\n");
                    } catch (IOException e) {
                        TxtField.append("\nCan't connect to " + TxtBT.getText().toString() + ": " + devBTaddress.get(Integer.parseInt(TxtBT.getText().toString())) + "\n");
                        try {
                            btSocket.close();
                        } catch (IOException e2) {
                            errorExit("In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
                        }
                    }
                    // Create a data stream so we can talk to server.
                    try {
                        outStream = btSocket.getOutputStream();
                        inStream = btSocket.getInputStream();
                    } catch (IOException e) {
                        errorExit("In onResume() and output stream creation failed:" + e.getMessage() + ".");
                    }
                }
            });

            btnGETS.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    inBuff = "";
                    TxtField.setText("");
                    sendData("GETS\n");
                }
            });

            btnEND.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    sendData("END\n");
                }
            });

            btnGETD.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    inBuff = "";
                    TxtField.setText("");
                    sendData("GETD\n");
                }
            });

            btnGETL.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    inBuff = "";
                    TxtField.setText("");
                    sendData("GETL\n");
                }
            });

            btnDL.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    sendData("DELLOG\n");
                }
            });
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(devBTaddress.get(Integer.parseInt(TxtBT.getText().toString())));

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            errorExit("In onResume() and socket create failed: " + e.getMessage());
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        try {
            btSocket.connect();
        } catch (IOException e) {
            TxtField.append("\nCan't connect to " + TxtBT.getText().toString() + ": " + devBTaddress.get(Integer.parseInt(TxtBT.getText().toString())) + "\n");
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        try {
            outStream = btSocket.getOutputStream();
            inStream = btSocket.getInputStream();
        } catch (IOException e) {
            errorExit("In onResume() and output stream creation failed:" + e.getMessage());
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (outStream != null) {
            try {
                outStream.flush();
            } catch (IOException e) {
                errorExit("In onPause() and failed to flush output stream: " + e.getMessage() + ".");
            }
        }

        try {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if (!btAdapter.isEnabled()) {
            //Prompt user to turn on Bluetooth
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

            time_old = System.currentTimeMillis();
            while (!btAdapter.isEnabled()) {
                if (System.currentTimeMillis() - time_old > 5000)
                    errorExit("Can't get BT enabled.");
            }
        }
        TxtField.append("\nBluetooth enabled...");

        // Listing paired devices
        TxtField.append("\nPaired Devices are:");
        LoggerNum = 0;
        devBTaddress.clear();
        devBTaddress.add(0, "00:00:00:00:00:00");
        Set<BluetoothDevice> devices = btAdapter.getBondedDevices();
        for (BluetoothDevice device : devices) {
            if (device.getName().contains("LOGGER")) {
                LoggerNum++;
                devBTaddress.add(LoggerNum, device.toString());
                devBTname.add(LoggerNum, device.getName());
                TxtField.append("\n " + String.valueOf(LoggerNum) + " : " + device.getName() + ", " + devBTaddress.get(LoggerNum));
            }
        }
    }

    private void errorExit(String message) {
        Toast.makeText(getBaseContext(), "Fatal Error" + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    private void sendData(String message) {
        byte[] msgBuffer = message.getBytes();
        byte[] Buffer = new byte[8192];
        int bytes;
        //TxtField.setText("");
        TxtField.append("sending: " + message);

        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
            if (devBTaddress.get(Integer.parseInt(TxtBT.getText().toString())).equals("00:00:00:00:00:00"))
                msg = msg + ".\n\naddress 00:00:00:00:00:00";
            msg = msg + ".\n\nSPP UUID: " + MY_UUID.toString();
            errorExit(msg);
        }
        /*
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        */
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Receiving data...");
            wl.acquire();
            time_old = System.currentTimeMillis();
            while (System.currentTimeMillis() - time_old < 1000) {
                while (inStream.available() > 0) {
                    bytes = inStream.read(Buffer);
                    inBuff += new String(Buffer, 0, bytes);
                    //TxtField.append(new String(Buffer, 0, bytes));
                    time_old = System.currentTimeMillis();
                }
            }
            wl.release();
        } catch (IOException e) {
            String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
            if (devBTaddress.get(Integer.parseInt(TxtBT.getText().toString())).equals("00:00:00:00:00:00"))
                msg = msg + ".\n\naddress 00:00:00:00:00:00";
            else msg = msg + ".\n\nSPP UUID: " + MY_UUID.toString();
            errorExit(msg);
        }
        TxtField.append(inBuff);
        saveFileExt(devBTname.get(logNum) + "-" + String.valueOf(logNum) + ".txt");
        //TxtField.append("\nSaved to \"log"+String.valueOf(logNum)+".txt\"\n");
        logNum++;
    }

    void saveFileExt(String logFileName) {
        // Create a path where we will place our picture in the user's
        // public pictures directory.  Note that you should be careful about
        // what you place here, since the user often manages these files.  For
        // pictures and other media owned by the application, consider
        // Context.getExternalMediaDir().

        //File file = new File(path, "log.txt");
        //File file = new File(context.getFilesDir(), filename);

        //String logFileName = "log.txt";

        //File path = Environment.getExternalStoragePublicDirectory(null);
        //File logFile = new File(Environment, logFileName);

        //File external = getFilesDir();
        //String logPath = getFilesDir();
        //File file = new File(getFilesDir() + logFileName);

        //String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
        //File myDir = new File(root + "/BCLogs");
        //myDir.mkdirs();

        File logFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString(), logFileName);
        TxtField.append("\nLog saved to: " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + logFileName + "\n");
        if (logFile.exists())
            logFile.delete();
        try {
            FileOutputStream out = new FileOutputStream(logFile);
            out.write(inBuff.getBytes());
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

    /*
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
    */

