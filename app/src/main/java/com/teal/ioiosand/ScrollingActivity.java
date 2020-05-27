package com.teal.ioiosand;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.CharBuffer;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.IOIOLooperProvider;
import ioio.lib.util.android.IOIOAndroidApplicationHelper;

public class ScrollingActivity extends AppCompatActivity implements IOIOLooperProvider {

    private static String TAG = "ScrollingActivity";
    private final IOIOAndroidApplicationHelper helper = new IOIOAndroidApplicationHelper(this, this);
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    PendingIntent permissionIntent;
    TextView mout;
    TextView min;
    TextView sout;
    TextView sin;

    private static int TX_PIN = 10;
    private static int RX_PIN = 11;
    private static int TX2_PIN = 12;
    private static int RX2_PIN = 13;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver, filter);
        helper.create();
        setContentView(R.layout.activity_scrolling);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mout = findViewById(R.id.masterOut);
        min = findViewById(R.id.masterIn);
        sout = findViewById(R.id.slaveOut);
        sin = findViewById(R.id.slaveIn);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Shutting Down Service", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
//               startService(new Intent("stop", null, getApplicationContext(), UartIoioService.class));
            }
        });
//        startService(new Intent(this, UartIoioService.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        helper.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        helper.stop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
            helper.restart();
        }
    }


    @Override
    public IOIOLooper createIOIOLooper(String connectionType, Object extra) {
        return new Looper();
    }

    class Looper extends BaseIOIOLooper {


        private DigitalOutput led_;
        private Uart uartStm;
        private InputStream stm_in;
        private OutputStream stm_out;
        private Uart uartOther;
        private InputStream other_in;
        private OutputStream other_out;
        private int s,o;

        @Override
        public void setup() throws ConnectionLostException {
            Uart.Parity parity = Uart.Parity.NONE;
            Uart.StopBits stopBit = Uart.StopBits.ONE;
            uartStm = ioio_.openUart(RX_PIN, TX_PIN, 115200, parity, stopBit);
            stm_out = uartStm.getOutputStream();
            stm_in = uartStm.getInputStream();
            uartOther =  ioio_.openUart(RX2_PIN, TX2_PIN, 115200, parity, stopBit);
            other_out = uartOther.getOutputStream();
            other_in = uartOther.getInputStream();
            toast("IOIO Connected");
        }

        @Override
        public void loop() throws ConnectionLostException, InterruptedException  {
            // Do all our writing here based on whether the state changed in the app
            // between the last state and the next.
            //out.write();
            Log.d(TAG, "Inside run loop.");
            byte[] buffer = new byte[1];
            buffer[0] = (byte) 'A';
            byte[] inBuffer = new byte[1];
            inBuffer[0] = (byte) 0;
            //Apparently one of the exceptions above extends IOException
            try {
                Log.d(TAG, "Writing byte to device");
                stm_out.write(buffer[0]);
                mout.append(String.valueOf((char) buffer[0]));
                Thread.sleep(10);
                inBuffer[0] = (byte) other_in.read();
                sin.append(String.valueOf((char) inBuffer[0]));
            } catch (IOException e) {
                Log.e(TAG, "EL Command Lost");
            }
            toast("IOIO write");
            Thread.sleep(500);
        }

        @Override
        public void disconnected() {
            try {
                stm_out.close();
                stm_in.close();
            } catch (IOException e) {
                Log.e(TAG, "Output Stream did not close, uart is closing");
            }
            uartStm.close();
            toast("IOIO disconnected");
        }

    }

    private void toast(final String message) {
        final Context context = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            //call method to set up device communication
                        }
                    }
                    else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };

}
