package com.example.rog.helloworld;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;
//
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;


import java.nio.charset.Charset;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;

public class MainActivity extends AppCompatActivity {
    TextView txtTemp, txtHumid;
    ToggleButton button;
    MQTTHelper mqttHelper;
    LineChart mpLineChart;
    ArrayList<Entry> tempData = new ArrayList<>();
    ArrayList<Entry> humidData = new ArrayList<>();
    Float timeTemp =0.0f;
    Float timeHumid =0.0f;
    ProgressBar tempBar, humidBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        txtTemp = findViewById(R.id.txtTemperature);
        txtHumid = findViewById(R.id.txtHumid);
        tempBar = findViewById(R.id.temp_bar);
        humidBar = findViewById(R.id.humid_bar);
//        mplinechart
        mpLineChart = findViewById(R.id.lineChart);

//        end mp linechart
        button = findViewById(R.id.btn);
        button.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean check) {
                button.setVisibility(View.INVISIBLE);
                if(check == true){
                    Log.d("mqtt",  "Button is checked");
                    sendDataMQTT("edkenway/f/button", "1",true);
                }else{
                    Log.d("mqtt",  "Button is unchecked");
                    sendDataMQTT("edkenway/f/button", "0", true);
                }
            }
        });
        startMQTT();
        setupScheduler();


    }

//    mp line chart
private void createLineChart(){
    //Log.d("Temp Data:",tempData.toString());
    //Log.d("Humid Data:",humidData.toString());
    LineDataSet line_temp = new LineDataSet(tempData,"Temperature data");
    LineDataSet line_humid = new LineDataSet(humidData,"Humidity data");

    line_temp.setLineWidth(4);
    line_temp.setColor(Color.rgb(166,6,22));
    line_temp.setDrawCircles(true);
    line_temp.setDrawCircleHole(false);
    line_temp.setCircleColor(Color.GREEN);
    //line_temp.setCircleColorHole(Color.GREEN);
    line_temp.setCircleRadius(4);
    line_temp.setValueTextSize(14);

    line_humid.setLineWidth(4);
    line_humid.setColor(Color.BLUE);
    line_humid.setDrawCircles(true);
    line_humid.setDrawCircleHole(false);
    line_humid.setCircleColor(Color.GREEN);
    //line_humid.setCircleColorHole(Color.GREEN);
    line_humid.setCircleRadius(4);
    line_humid.setValueTextSize(14);

    mpLineChart.setNoDataText("No Data");
    mpLineChart.setNoDataTextColor(Color.BLUE);
    mpLineChart.setDrawGridBackground(true);
    mpLineChart.setDrawBorders(true);
    mpLineChart.setBorderColor(Color.BLACK);
    mpLineChart.setBorderWidth(1);

    Legend legend = mpLineChart.getLegend();
    legend.setEnabled(true);
    legend.setTextColor(Color.BLACK);
    legend.setTextSize(18);
    legend.setForm(Legend.LegendForm.LINE);
    legend.setFormSize(30);
    legend.setXEntrySpace(20);
    legend.setFormToTextSpace(10);

    Description description = new Description();
    description.setText("Time");
    description.setTextColor(Color.BLUE);
    description.setTextSize(15);
    mpLineChart.setDescription(description);

    ArrayList<ILineDataSet> dataSets = new ArrayList<>();
    dataSets.add(line_temp);
    dataSets.add(line_humid);
    LineData data = new LineData(dataSets);
    // data.setValueFormatter(new TempFormatter());
    //mpLineChart.animateXY(5000,5000, Easing.EasingOption.EaseInOutBounce, Easing.EasingOption.EaseInExpo);
    mpLineChart.setData(data);
    mpLineChart.invalidate();
}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        aTimer.cancel();
        list.clear();
    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//        android.os.Process.killProcess(android.os.Process.myPid());
//    }

    boolean send_message_again= false;
    int waiting_period=0;
    int again_count=0;
    Timer aTimer = new Timer();
    List<MQTTMessageBuff> list = new ArrayList<>();
    private void setupScheduler(){
        TimerTask scheduler = new TimerTask() {
            @Override
            public void run() {
                if(again_count>=1){
                    waiting_period=0;
                    again_count=0;
                    send_message_again=false;
                    list.clear();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            button.setVisibility(View.VISIBLE);
                        }
                    });
                }
                if(waiting_period>0){
                    waiting_period--;
                    if(waiting_period == 0){
                        send_message_again= true;
                        again_count++;
                    }
                    if(send_message_again==true){

                        Log.d("mqtt","Timer is executed");
                        Log.d("mqtt","Resent topic: " + list.get(0).topic.toString());
                        Log.d("mqtt","Resent value: " + list.get(0).mess);
                        sendDataMQTT(list.get(0).topic, list.get(0).mess,true);
                        list.remove(0);
                    }
                }
            }
        };
        aTimer.schedule(scheduler,5000,1000);
    }

    private void sendDataMQTT(String topic, String value,boolean resend){
        if (resend){
            waiting_period=2;
            send_message_again=false;
            MQTTMessageBuff buffer = new MQTTMessageBuff();
            buffer.topic = topic;
            buffer.mess = value;
            list.add(buffer);
        }
        Log.d("mqtt","sendData to: " + topic);
        Log.d("mqtt","send value: " + value);

        Log.d("mqtt","list: " + list);
        MqttMessage msg = new MqttMessage();
        msg.setId(1234);
        msg.setQos(0);
        msg.setRetained(true);


        byte[] b = value.getBytes(Charset.forName("UTF-8"));
        msg.setPayload(b);

        try {
            mqttHelper.mqttAndroidClient.publish(topic, msg);

        }catch (MqttException e){
        }
    }

    private void startMQTT(){
        mqttHelper = new MQTTHelper(getApplicationContext(), "kenwaydeLord");

        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.d("mqtt", "Connection is successful");
            }

            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.w("mqtt", "Received topic:   " + topic);
                Log.w("mqtt", "Received message: " + message.toString());


                if (topic.equals("edkenway/f/bbc-temp")) {
                    String temp=String.format("%s °C",message.toString() );
                    tempBar.setProgress(Math.round(Float.parseFloat(message.toString())));
                    txtTemp.setText(temp);
                    if(tempData.size()>=10){
                        tempData.remove(0);
                    }
                    tempData.add(new Entry(timeTemp += 1, Float.parseFloat(message.toString())));
                    String mess= "ACK_TEMP:" +  message.toString();
                    sendDataMQTT("edkenway/f/bbc-temp-error",mess,false );
                };
                if (topic.equals("edkenway/f/bbc-humid")) {
                    String humid=String.format("%s %%",message.toString() );
                    humidBar.setProgress(Math.round(Float.parseFloat(message.toString())));
                    txtHumid.setText(humid);
                    if(humidData.size()>=10){
                        humidData.remove(0);
                    }
                    humidData.add(new Entry(timeHumid += 1, Float.parseFloat(message.toString())));
                    createLineChart();
                    String mess= "ACK_HUMID:" +  message.toString();
                    sendDataMQTT("edkenway/f/bbc-humid-error",mess,false );
                };
                if (topic.equals("edkenway/f/btn-error")) {
                    String temp= message.toString();

                    String result[]=temp.split(":");
                    int btn= Integer.parseInt(result[1]);
                    if (!list.isEmpty()){

                        if(message.toString().contains(list.get(0).mess)){
                            //Log.w("mqtt", "we here");
                            button.setVisibility(View.VISIBLE);
                            send_message_again=false;
                            waiting_period=0;
                            again_count=0;
                            list.remove(0);
                        }
                    }
                };
                if (topic.equals("edkenway/f/button")){
                    int btn= Integer.parseInt(message.toString());
                    if (btn==0){
                        button.setChecked(false);
                    }
                    else{
                        button.setChecked(true);
                    }
                }

            }
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

    }
    public class MQTTMessageBuff{
        public String topic;
        public String mess;
        public int messIndex;
    }


}
