package com.teal.ioiosand;

import android.app.NotificationManager;
import android.content.Intent;
import android.os.IBinder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.IOIOFactory;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.api.exception.IncompatibilityException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;

public class UartIoioService extends IOIOService {

    public UartIoioService() {
        IOIO ioio = IOIOFactory.create();
    }

    @Override
    protected IOIOLooper createIOIOLooper() {
        return new BaseIOIOLooper() {
            private DigitalOutput led_;
            private Uart uartStm;
            private InputStream stm_in;
            private OutputStream stm_out;
            private Uart uartOther;
            private InputStream other_in;
            private OutputStream other_out;
            private int s,o;

            @Override
            protected void setup() throws ConnectionLostException,
                    InterruptedException {
                try {
                    ioio_.waitForConnect();
                } catch (IncompatibilityException e) {
                    e.printStackTrace();
                }
                led_ = ioio_.openDigitalOutput(IOIO.LED_PIN);

                int rxPin = 10;
                int txPin = 11;
                int baud = 115200;
                Uart.Parity parity = Uart.Parity.NONE;
                Uart.StopBits stopBits = Uart.StopBits.ONE;
                uartStm = ioio_.openUart(rxPin, txPin, baud, parity, stopBits);
                stm_in = uartStm.getInputStream();
                stm_out = uartStm.getOutputStream();

                rxPin = 12;
                txPin = 13;
                uartOther = ioio_.openUart(rxPin, txPin, baud, parity, stopBits);
                other_in = uartStm.getInputStream();
                other_out = uartStm.getOutputStream();
            }

            @Override
            public void loop() throws ConnectionLostException, InterruptedException {
                led_.write(false);
                try {
                    stm_out.write(s);
                    o = other_in.read();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Thread.sleep(500);

                try {
                    other_out.write(s);
                    o = stm_in.read();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                led_.write(true);
                Thread.sleep(500);
            }

            @Override
            public void disconnected() {
                uartStm.close();
                uartOther.close();
                ioio_.disconnect();
                try {
                    ioio_.waitForDisconnect();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int result = super.onStartCommand(intent, flags, startId);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (intent != null && intent.getAction() != null
                && intent.getAction().equals("stop")) {
            // User clicked the notification. Need to stop the service.
            nm.cancel(0);
            stopSelf();
        } else {
            // Service starting. Create a notification.
            // setup notification to stop
        }
        return result;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
