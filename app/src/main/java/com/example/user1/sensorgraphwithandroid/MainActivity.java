package com.example.user1.sensorgraphwithandroid;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    SensorManager manager;
    Sensor sensor;
    TextView xTextView;
    TextView yTextView;
    TextView zTextView;

    LineChart mChart;

    String[] names = new String[]{"x-value", "y-value", "z-value"};
    int[] colors = new int[]{Color.RED, Color.GREEN, Color.BLUE};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        xTextView = (TextView)findViewById(R.id.xValue);
        yTextView = (TextView)findViewById(R.id.yValue);
        zTextView = (TextView)findViewById(R.id.zValue);

        manager = (SensorManager)getSystemService(SENSOR_SERVICE);
        sensor = manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        mChart = (LineChart) findViewById(R.id.lineChart);

        mChart.setDescription(""); // 表のタイトルを空にする
        mChart.setData(new LineData()); // 空のLineData型インスタンスを追加
    }


    private float sensorX;
    private float sensorY;
    private float sensorZ;
    final private float k = 0.1f;   //値が大きいほどローパスフィルタの効きが強くなる
    private float lowPassX = 0,lowPassY = 0,lowPassZ = 0; //ローパスフィルタ通過後の値

    private float rawAx = 0, rawAy=0, rawAz = 0;    //ハイパスフィルタを通した値

    long oldTime = 0;   //前回、センサの値が変更されたとき

    float vx = 0, vy = 0, vz = 0;   //速度だけど端末から見た座標(ローカル座標)なことに注意
    float x=0, y=0, z=0;    //位置。精度悪くてあてにならない。

    @Override
    public void onSensorChanged(SensorEvent event) {

        //センサーの生の値
        sensorX = event.values[0];
        sensorY = event.values[1];
        sensorZ = event.values[2];

        //LPF
        lowPassX += (event.values[0] - lowPassX) * k;
        lowPassY += (event.values[1] - lowPassY) * k;
        lowPassZ += (event.values[2] - lowPassZ) * k;

        // High Pass Filter
        rawAx = event.values[0] - lowPassX;
        rawAy = event.values[1] - lowPassY;
        rawAz = event.values[2] - lowPassZ;


        if(oldTime == 0) oldTime = System.currentTimeMillis();
        long nowTime = System.currentTimeMillis();
        long interval = nowTime - oldTime;
        oldTime = nowTime;

        vx += rawAx * interval / 10; // [cm/s] にする
        vy += rawAy * interval / 10;
        vz += rawAz * interval / 10;
        x += vx * interval / 1000; // [cm] にする
        y += vy * interval / 1000;
        z += vz * interval / 1000;

        float temp[] = new float[3];
        temp[0] = vx;
        temp[1] = vy;
        temp[2] = vz;

        xTextView.setText(String.valueOf(vx));
        yTextView.setText(String.valueOf(vy));
        zTextView.setText(String.valueOf(vz));


        LineData data = mChart.getLineData();
        if (data != null) {
            for (int i = 0; i < 3; i++) { // 3軸なのでそれぞれ処理します
                ILineDataSet set = data.getDataSetByIndex(i);
                if (set == null) {
                    set = createSet(names[i], colors[i]); // ILineDataSetの初期化は別メソッドにまとめました
                    data.addDataSet(set);
                }

                data.addEntry(new Entry(set.getEntryCount(), event.values[i]), i); // 実際にデータを追加する
                data.notifyDataChanged();
            }

            mChart.notifyDataSetChanged(); // 表示の更新のために変更を通知する
            mChart.setVisibleXRangeMaximum(50); // 表示の幅を決定する
            mChart.moveViewToX(data.getEntryCount()); // 最新のデータまで表示を移動させる
        }
    }

    private LineDataSet createSet(String label, int color) {
        LineDataSet set = new LineDataSet(null, label);
        set.setLineWidth(2.5f); // 線の幅を指定
        set.setColor(color); // 線の色を指定
        set.setDrawCircles(false); // ポイントごとの円を表示しない
        set.setDrawValues(false); // 値を表示しない

        return set;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        manager.unregisterListener(this);
    }
}
