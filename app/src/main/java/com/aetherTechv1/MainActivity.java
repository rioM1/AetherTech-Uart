
/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.aetherTechv1;




import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Date;
import com.jjoe64.graphview.*;
import com.jjoe64.graphview.series.*;
import java.lang.Math;

import android.os.Environment;
import android.widget.Chronometer;
import android.os.SystemClock;
import java.io.OutputStreamWriter;


import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.view.ViewGroup.LayoutParams;

import java.util.Locale;


public class MainActivity extends Activity implements RadioGroup.OnCheckedChangeListener {
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    public static final String TAG = "nRFUART";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;

    TextView mRemoteRssiVal;
    RadioGroup mRg;
    private int mState = UART_PROFILE_DISCONNECTED;
    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;

    private Button btnConnectDisconnect,btnSend, btnSwitch, btnReset;

    private TextView editMSG1;
    private TextView editMSG2;
    private TextView editMSG3;
    private TextView editMSG4;

    private TextView avgPmsg;
    private TextView curPmsg;
    private TextView switchMSG;

    //private EditText scaleIn;


    private LineGraphSeries<DataPoint> mSeries;
    private GraphView graph;

    int count=1;
    int switchCount=0;
    boolean isRunning=false;
    String beginMsg="BEGIN";
    String endMsg="END";

    double[] ADC= {0,0,0,0};
    double currentPower=0, avgPower=0;
    double currentPowerSum=0;
    double runningSum=0;
    double powerSum=0;
    int window=55;
    int graphCount=500;

    long elapsedMillis;

    GridLabelRenderer gridLabel;




    private Context mContext;
    private RelativeLayout mRelativeLayout;

    private Chronometer mChronometer;
    private long lastPause=0;
    private boolean resetFlag=true;




    boolean closePop=false;
    PopupWindow mPopupWindow;

    Button popButton;

    TextView tvMsg;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);


        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnConnectDisconnect=(Button) findViewById(R.id.btn_select);
        btnSend=(Button) findViewById(R.id.sendButton);
        btnSwitch=(Button) findViewById(R.id.outputSwitch);
        btnReset=(Button) findViewById(R.id.resetSwitch);

        editMSG1 = (TextView) findViewById(R.id.editText1);
        editMSG2 = (TextView)  findViewById(R.id.editText2);
        editMSG3 = (TextView)  findViewById(R.id.editText3);
        editMSG4 = (TextView)  findViewById(R.id.editText4);

        switchMSG= (TextView)  findViewById(R.id.currentSwitch);
        avgPmsg=(TextView) findViewById(R.id.avgValue);
        curPmsg=(TextView) findViewById(R.id.powerValue);
        mChronometer = (Chronometer) findViewById(R.id.simpleChronometer);



        mContext = getApplicationContext();

        // Get the activity


        // Get the widgets reference from XML layout
        mRelativeLayout = (RelativeLayout) findViewById(R.id.RelativeLayout1);
        popButton=(Button) findViewById(R.id.popUpButton);

        //closePop=!closePop;
        // Initialize a new instance of LayoutInflater service
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);

        // Inflate the custom layout/view
        View customView = inflater.inflate(R.layout.custom_layout,null);

        // Initialize a new instance of popup window
        mPopupWindow = new PopupWindow(
                customView, LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );
        mPopupWindow.setWidth(LinearLayout.LayoutParams.MATCH_PARENT);
        //mPopupWindow.setHeight(LinearLayout.LayoutParams.MATCH_PARENT);

        Button closeButton = (Button) customView.findViewById(R.id.ib_close);

        // Set a click listener for the popup window close button
        closeButton.setOnClickListener(new View.OnClickListener() {
                                           @Override
                                           public void onClick(View view) {
                                               // Dismiss the popup window
                                               mPopupWindow.dismiss();
                                           }
                                       }
        );


        graph = (GraphView) customView.findViewById(R.id.graph2);

        graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    // show normal x values
                    elapsedMillis=SystemClock.elapsedRealtime() - mChronometer.getBase();
                    return String.format("%.1f",(double)elapsedMillis/1000);
                } else {
                    // show currency for y values
                    return super.formatLabel(value, isValueX);
                }
            }
        });

        mSeries = new LineGraphSeries<DataPoint>();
        graph.addSeries(mSeries);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setYAxisBoundsManual(true);
        gridLabel = graph.getGridLabelRenderer();
        gridLabel.setHorizontalAxisTitle("Time(s)");

        graph.setBackgroundColor(getResources().getColor(R.color.black));


        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(300);


        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(2.4);






        popButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {



                // Get a reference for the custom view close button

                mPopupWindow.showAtLocation(mRelativeLayout, Gravity.CENTER,0,0);

            }
        });

        service_init();



        // Handle Disconnect & Connect button
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
                else {
                    if (btnConnectDisconnect.getText().equals("Connect")){

                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices

                        Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    } else {
                        //Disconnect button pressed
                        if (mDevice!=null)
                        {
                            mService.disconnect();

                        }
                    }
                }
            }
        });
        // Handle Send button
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                byte[] value;
                try {
                    if (!isRunning) {
                        value = beginMsg.getBytes("UTF-8");

                        isRunning=true;

                        if (lastPause==0)//new instance
                            mChronometer.setBase(SystemClock.elapsedRealtime());
                        else mChronometer.setBase(mChronometer.getBase()+SystemClock.elapsedRealtime()-lastPause);
                        mChronometer.start();
                    }
                    else{
                        value = endMsg.getBytes("UTF-8");
                        isRunning= false;

                        lastPause= SystemClock.elapsedRealtime();
                        mChronometer.stop();
                    }
                    //send data to service

                    mService.writeRXCharacteristic(value);

                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        });

        btnSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCount=(switchCount+1)%5;
                TextView swCount = (TextView) findViewById(R.id.currentSwitch);
                if (switchCount<4)
                    swCount.setText(Integer.toString(switchCount+1));
                else swCount.setText("Power");
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                graph.removeAllSeries();
                count=1;
                currentPower=0;
                avgPower=0;
                currentPowerSum=0;
                runningSum=0;
                powerSum=0;
                mSeries = new LineGraphSeries<DataPoint>();
                graph.addSeries(mSeries);
                mChronometer.setBase(SystemClock.elapsedRealtime());
                //mChronometer.stop();
                resetFlag=true;
                lastPause=0;
            }
        });




        // Set initial UI state

    }

    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

        }

        public void onServiceDisconnected(ComponentName classname) {
            ////     mService.disconnect(mDevice);
            mService = null;
        }
    };





    private Handler mHandler = new Handler() {
        @Override

        //Handler events that received from UART service
        public void handleMessage(Message msg) {

        }
    };

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(final Context context, Intent intent) {
            String action = intent.getAction();


            final Intent mIntent = intent;

            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_CONNECT_MSG");
                        btnConnectDisconnect.setText("Disconnect");

                        //edtMessage.setEnabled(true);
                        btnSend.setEnabled(true);
                        btnSwitch.setEnabled(true);
                        btnReset.setEnabled(true);
                        ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - ready");
                        //listAdapter.add("["+currentDateTimeString+"] Connected to: "+ mDevice.getName());
                        //	messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                        mState = UART_PROFILE_CONNECTED;
                    }
                });
            }


            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_DISCONNECT_MSG");
                        btnConnectDisconnect.setText("Connect");
                        isRunning=false;
                        mChronometer.stop();

                        //edtMessage.setEnabled(false);
                        btnSend.setEnabled(false);
                        btnSwitch.setEnabled(false);
                        btnReset.setEnabled(false);
                        ((TextView) findViewById(R.id.deviceName)).setText("Not Connected");
                        //listAdapter.add("["+currentDateTimeString+"] Disconnected to: "+ mDevice.getName());
                        mState = UART_PROFILE_DISCONNECTED;
                        mService.close();
                        //setUiState();

                    }
                });
            }



            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification();
            }

            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {

                final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                runOnUiThread(new Runnable() {
                    public void run() {
                        try {

                            int a=0;
                            String text = new String(txValue, "UTF-8");
                            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                            if(!(text.equals("END"))) {//detect end of sampling
                                String strToken[]=text.split("[a|b]");//split data from BLE packet in string form
                                if (text.substring(0,1).equals("a")) {//check for valid delimiter
                                    ADC[0]=calculatePower(Integer.parseInt(strToken[1]));//calculating power from each sensor
                                    ADC[1]=calculatePower(Integer.parseInt(strToken[2]));
                                    ADC[2]=calculatePower(Integer.parseInt(strToken[3]));
                                    ADC[3]=calculatePower(Integer.parseInt(strToken[4]));
                                    powerSum=(ADC[0] + ADC[1] + ADC[2] + ADC[3]);
                                    currentPower = powerSum * 10;
                                    currentPowerSum+=currentPower;
                                    runningSum = runningSum + currentPower;
                                    avgPower = runningSum / count;
                                    //ADC5=0;

                                    if (((count-1)%window)==window-1) {
                                        if (powerSum==0){// if no active input
                                            editMSG1.setText(String.format(Locale.US, "%.0f", 0));
                                            editMSG2.setText(String.format(Locale.US, "%.0f", 0));
                                            editMSG3.setText(String.format(Locale.US, "%.0f", 0));
                                            editMSG4.setText(String.format(Locale.US, "%.0f", 0));
                                        }else {//show power distribution
                                            editMSG1.setText(String.format(Locale.US, "%.0f", (ADC[0]) / (powerSum)*100));
                                            editMSG2.setText(String.format(Locale.US, "%.0f", (ADC[1]) / (powerSum)*100));
                                            editMSG3.setText(String.format(Locale.US, "%.0f", (ADC[2]) / (powerSum)*100));
                                            editMSG4.setText(String.format(Locale.US, "%.0f", (ADC[3]) / (powerSum)*100));
                                        }
                                        curPmsg.setText(String.format(Locale.US, "%.0f", currentPowerSum/window));

                                        avgPmsg.setText(String.format(Locale.US, "%.0f", avgPower));
                                        currentPowerSum=0;
                                    }



                                    if(powerSum!=0) {//handling data for graph
                                        if (switchCount<4){//displaying sensor dat
                                            graph.getViewport().setMaxY(2.4);
                                            gridLabel.setVerticalAxisTitle("Voltage (V)");
                                            if (Integer.parseInt(strToken[switchCount+1])<250)
                                                mSeries.appendData(new DataPoint((count), Integer.parseInt(strToken[switchCount+1])*2.4/255), true, graphCount);
                                            else mSeries.appendData(new DataPoint((count), 0), true, graphCount);
                                        }
                                        else {//displaying power data
                                            graph.getViewport().setMaxY(300);

                                            gridLabel = graph.getGridLabelRenderer();
                                            gridLabel.setVerticalAxisTitle("Power (W)");
                                            mSeries.appendData(new DataPoint((count), currentPower), true, graphCount);
                                        }
                                    }
                                    else{
                                        mSeries.appendData(new DataPoint((count), 0), true, graphCount);
                                    }
                                    writeToFile(String.format("%.0f \n",currentPower));//write to log file

                                    count++;
                                    //
                                }
                            }

                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
            }

            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)){
                showMessage("Device doesn't support UART. Disconnecting");
                mService.disconnect();
            }


        }
    };

    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService= null;

    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                    ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - connecting");
                    mService.connect(deviceAddress);


                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

    }


    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onBackPressed() {
        if (mState == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("nRFUART's running in background.\n             Disconnect to exit");
        }
        else {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.popup_title)
                    .setMessage(R.string.popup_message)
                    .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.popup_no, null)
                    .show();
        }
    }



    public double calculatePower(int adcRead){//calculate power from adc reading
        double powerValue=0;

        double powerOffset=1.25;
        if (adcRead>250){
            return 0.0000001;
        }
        double voltage=(adcRead*2.4/255-powerOffset)*31.6;
        if (Math.abs(voltage) < 1.5 )
            return 0.0000001;
        powerValue=0.78619* Math.log(Math.pow(voltage,2)) +2.8499;
        if (powerValue<=0)
            return 0.0000001;
        return powerValue;
    }

    public void writeToFile(String data)//write data to log file
    {
        // Get the directory for the user's public pictures directory.
        final File path =
                Environment.getExternalStoragePublicDirectory
                        (
                                //Environment.DIRECTORY_PICTURES
                                Environment.DIRECTORY_DCIM + "/aetherTech/"
                        );

        // Make sure the path directory exists.
        if(!path.exists())
        {
            // Make it, if it doesn't exit
            path.mkdirs();
        }

        final File file = new File(path, "powerLog.txt");

        // Save your stream, don't forget to flush() it before closing it.

        try
        {
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file,!resetFlag);
            resetFlag=false;
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(data);

            myOutWriter.close();

            fOut.flush();
            fOut.close();
        }
        catch (IOException e)
        {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }



}






