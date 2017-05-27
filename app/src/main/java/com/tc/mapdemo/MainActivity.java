package com.tc.mapdemo;

import android.app.*;
import android.graphics.*;
import android.os.*;
import android.support.annotation.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;

import com.amap.api.location.*;
import com.amap.api.location.AMapLocationClientOption.*;
import com.amap.api.maps.*;
import com.amap.api.maps.model.*;

import java.util.*;

/**
 * Created by tc on 2017/5/27.
 */

public class MainActivity extends Activity implements OnClickListener, AMapLocationListener {

    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;

    private MapView mMapView = null;

    //初始化地图控制器对象
    private AMap aMap;

    private Button btStart, btStop;

    private EditText etStartPoint, etStopPoint;

    List<LatLng> latLngs = new ArrayList<LatLng>();

    private LatLng oldLatLng;

    private LatLng newLatLng;

    private boolean isFirst = true; //是否第一次定位
    private boolean isStop = false; //是否停止定位

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView(savedInstanceState);
        initAmapClient();

        if (aMap == null) {
            aMap = mMapView.getMap();
        }
    }

    private void initView(Bundle savedInstanceState) {

        //获取地图控件引用
        mMapView = (MapView) findViewById(R.id.map);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mMapView.onCreate(savedInstanceState);

        etStartPoint = (EditText) findViewById(R.id.et_start_point);

        etStopPoint = (EditText) findViewById(R.id.et_stop_point);

        btStart = (Button) findViewById(R.id.bt_start);

        btStop = (Button) findViewById(R.id.bt_stop);

        btStart.setOnClickListener(this);
        btStop.setOnClickListener(this);

    }

    /**
     * 初始化定位监听
     */
    private void initAmapClient() {
        //初始化定位
        mLocationClient = new AMapLocationClient(getApplicationContext());
        //初始化AMapLocationClientOption对象
        mLocationOption = new AMapLocationClientOption();

        //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
        mLocationOption.setLocationMode(AMapLocationMode.Hight_Accuracy);
        //设置定位模式为AMapLocationMode.Battery_Saving，低功耗模式。
//        mLocationOption.setLocationMode(AMapLocationMode.Battery_Saving);
        //设置定位模式为AMapLocationMode.Device_Sensors，仅设备模式。
//        mLocationOption.setLocationMode(AMapLocationMode.Device_Sensors);

        //设置定位间隔,单位毫秒,默认为2000ms，最低1000ms。
        mLocationOption.setInterval(1000);

        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);

        //设置是否强制刷新WIFI，默认为true，强制刷新。
        mLocationOption.setWifiActiveScan(false);

        //单位是毫秒，默认30000毫秒，建议超时时间不要低于8000毫秒。
        mLocationOption.setHttpTimeOut(20000);

        //设置定位回调监听
        mLocationClient.setLocationListener(this);

        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
    }

    private void start() {
        Toast.makeText(this, "开始行程", Toast.LENGTH_SHORT).show();
        isFirst = true;
        //启动定位
        mLocationClient.startLocation();
    }

    private void stop() {
        Toast.makeText(this, "停止行程", Toast.LENGTH_SHORT).show();
        isStop = true;
        //停止定位
        mLocationClient.stopLocation();
    }

    @Override
    public void onClick(View v) {
        if (v == btStart) {
            if (etStopPoint.getText().toString().trim() == null) {
                Toast.makeText(this, "请输入终点位置", Toast.LENGTH_SHORT).show();
                return;
            }
            start();
        } else if (v == btStop) {
            stop();
        }
    }

    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (amapLocation != null) {
            if (amapLocation.getErrorCode() == 0) {

                if (isFirst) {//绘制开始Marker点
                    isFirst = false;
                    etStartPoint.setText(amapLocation.getStreet());
                    CameraUpdate mCamer = CameraUpdateFactory.newLatLngZoom(new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude()), 20);
                    aMap.animateCamera(mCamer);
                    LatLng latLng = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());
                    final Marker marker = aMap.addMarker(new MarkerOptions().position(latLng).title("起点").snippet("DefaultMarker"));
                }

                if (isStop) { //绘制结束Marker点
                    isFirst = false;
                    LatLng latLng = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());
                    final Marker marker = aMap.addMarker(new MarkerOptions().position(latLng).title("终点").snippet("DefaultMarker"));
                }

                if (!isStop) {
                    latLngs.add(new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude()));
                    drawLine();
                }
//                //可在其中解析amapLocation获取相应内容。
//                oldLatLng = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());
//                if (newLatLng == null) {
//
//                }

            } else {
                //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                Toast.makeText(this, "location Error, ErrCode:"
                        + amapLocation.getErrorCode() + ", errInfo:"
                        + amapLocation.getErrorInfo(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 绘制线段
     */
    private void drawLine() {
        System.out.println("画线，画线。。。。。");
        Polyline polyline = aMap.addPolyline(new PolylineOptions().
                addAll(latLngs).width(10).color(Color.RED));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        mMapView.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mMapView.onPause();
    }

}
