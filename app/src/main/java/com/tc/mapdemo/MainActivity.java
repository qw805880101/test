package com.tc.mapdemo;

import android.app.*;
import android.graphics.*;
import android.os.*;
import android.support.annotation.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;

import com.amap.api.location.*;
import com.amap.api.location.AMapLocationClientOption.*;
import com.amap.api.maps.*;
import com.amap.api.maps.model.*;
import com.amap.api.navi.*;
import com.amap.api.navi.model.*;
import com.amap.api.services.core.*;
import com.amap.api.services.geocoder.*;
import com.amap.api.services.geocoder.GeocodeSearch.*;
import com.amap.api.trace.*;
import com.autonavi.tbt.*;

import java.util.*;

import static android.content.ContentValues.TAG;

/**
 * Created by tc on 2017/5/27.
 */

public class MainActivity extends Activity implements OnClickListener, AMapLocationListener, AMapNaviListener, OnGeocodeSearchListener, TraceListener {

    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;

    private MapView mMapView = null;

    //初始化地图控制器对象
    private AMap aMap;

    private Polyline mpolyline;

    private PolylineOptions mPolyoptions, tracePolytion;

    private Button btStart, btStop;

    private EditText etStartPoint, etStopPoint;

    private boolean isFirst = true; //是否第一次定位

    private LatLng endLatlng;

    private AMapNavi mAMapNavi;

    private List<NaviLatLng> from, to, wayPoints;
    int strategy;

    private GeocodeSearch geocoderSearch;

    private String cityCode;

    private LBSTraceClient mTraceClient;

    private List<TraceLocation> mTraceLocations;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView(savedInstanceState);
        initAmapClient();
        initpolyline();
    }

    private void initView(Bundle savedInstanceState) {
        //获取地图控件引用
        mMapView = (MapView) findViewById(R.id.map);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mMapView.onCreate(savedInstanceState);
        if (aMap == null) {
            aMap = mMapView.getMap();
        }
        mTraceClient = LBSTraceClient.getInstance(this);
        mTraceLocations = new ArrayList<>();


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
        mLocationOption.setInterval(5000);

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

        geocoderSearch = new GeocodeSearch(this);
        geocoderSearch.setOnGeocodeSearchListener(this);
        //启动定位
        mLocationClient.startLocation();
    }

    /**
     * 初始化线段配置
     */
    private void initpolyline() {
        mPolyoptions = new PolylineOptions();
        mPolyoptions.width(15f);
        mPolyoptions.color(Color.BLUE);

        tracePolytion = new PolylineOptions();
        tracePolytion.width(40);
        tracePolytion.setCustomTexture(BitmapDescriptorFactory.fromResource(R.drawable.grasp_trace_line));
    }

    private void start() {
        Toast.makeText(this, "开始行程", Toast.LENGTH_SHORT).show();
        // name表示地址，第二个参数表示查询城市，中文或者中文全拼，citycode、adcode
        GeocodeQuery query = new GeocodeQuery(etStopPoint.getText().toString().trim(), cityCode);
//        query.getCity();
        geocoderSearch.getFromLocationNameAsyn(query);


        //启动定位
        mLocationClient.startLocation();
    }

    private void stop() {
        isFirst = true;
        Toast.makeText(this, "停止行程", Toast.LENGTH_SHORT).show();
        //停止定位
        mLocationClient.stopLocation();
        final Marker marker = aMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.end)))
                .position(endLatlng));
    }

    @Override
    public void onClick(View v) {
        if (v == btStart) {

            if (etStopPoint.getText().toString().trim() == null || etStopPoint.getText().toString().trim().equals("")) {
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
                    LatLng latLng = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());
                    CameraUpdate mCamer = CameraUpdateFactory.newLatLngZoom(latLng, 20);
                    aMap.animateCamera(mCamer);
                    final Marker marker = aMap.addMarker(new MarkerOptions()
                            .icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.start)))
                            .position(latLng));
                    from = new ArrayList<>();
                    from.add(new NaviLatLng(amapLocation.getLatitude(), amapLocation.getLongitude()));
                    System.out.println(amapLocation.toString());
                    cityCode = amapLocation.getCityCode();
                    //停止定位
                    mLocationClient.stopLocation();

                    MyLocationStyle myLocationStyle;
                    myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
                    myLocationStyle.interval(5000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
                    aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
                    myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW) ;//连续定位、且将视角移动到地图中心点，定位蓝点跟随设备移动。（1秒1次定位）
//aMap.getUiSettings().setMyLocationButtonEnabled(true);设置默认定位按钮是否显示，非必需设置。
                    aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。

                } else {
                    endLatlng = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());
                    mPolyoptions.add(endLatlng);
                    drawLine(amapLocation);
                }

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
    private void drawLine(AMapLocation amapLocation) {
        System.out.println("画线，画线。。。。。");
//        Polyline polyline = aMap.addPolyline(new PolylineOptions().add(new LatLng(0,0)).width(20).color(Color.RED));

        if (mPolyoptions.getPoints().size() > 1) {
            if (mpolyline != null) {
                mpolyline.setPoints(mPolyoptions.getPoints());
                mTraceLocations.add(Utils.parseTraceLocation(amapLocation));
            } else {
                mpolyline = aMap.addPolyline(mPolyoptions);
                mpolyline.setZIndex(0);
            }
        }
        mTraceClient.queryProcessedTrace(1, mTraceLocations,
                LBSTraceClient.TYPE_AMAP, this);
//        Polyline polyline = aMap.addPolyline(new PolylineOptions().addAll(latLngs).width(10).color(Color.RED));
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

    @Override
    public void onInitNaviFailure() {

        System.out.println("onInitNaviFailure");

    }

    @Override
    public void onInitNaviSuccess() {

        System.out.println("onInitNaviSuccess");
        /**
         * 方法:
         *   int strategy=mAMapNavi.strategyConvert(congestion, avoidhightspeed, cost, hightspeed, multipleroute);
         * 参数:
         * @congestion 躲避拥堵
         * @avoidhightspeed 不走高速
         * @cost 避免收费
         * @hightspeed 高速优先
         * @multipleroute 多路径
         *
         * 说明:
         *      以上参数都是boolean类型，其中multipleroute参数表示是否多条路线，如果为true则此策略会算出多条路线。
         * 注意:
         *      不走高速与高速优先不能同时为true
         *      高速优先与避免收费不能同时为true
         */
        int strategy = 0;
        try {
            strategy = mAMapNavi.strategyConvert(true, false, false, false, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mAMapNavi.calculateDriveRoute(from, to, null, strategy);
    }

    @Override
    public void onStartNavi(int i) {

    }

    @Override
    public void onTrafficStatusUpdate() {

    }

    @Override
    public void onLocationChange(AMapNaviLocation aMapNaviLocation) {

    }

    @Override
    public void onGetNavigationText(int i, String s) {

    }

    @Override
    public void onEndEmulatorNavi() {

    }

    @Override
    public void onArriveDestination() {

    }

    @Override
    public void onCalculateRouteSuccess() {
        //单一策略
        AMapNaviPath mapNaviPath = mAMapNavi.getNaviPath();

        List<LatLng> latLngs = new ArrayList<>();

        for (int i = 0; i < mapNaviPath.getCoordList().size(); i++) {
            latLngs.add(new LatLng(mapNaviPath.getCoordList().get(i).getLatitude(), mapNaviPath.getCoordList().get(i).getLongitude()));
        }

        aMap.addPolyline(new PolylineOptions().addAll(latLngs).width(10).color(Color.RED));
    }

    @Override
    public void onCalculateRouteFailure(int i) {

    }

    @Override
    public void onReCalculateRouteForYaw() {

    }

    @Override
    public void onReCalculateRouteForTrafficJam() {

    }

    @Override
    public void onArrivedWayPoint(int i) {

    }

    @Override
    public void onGpsOpenStatus(boolean b) {

    }

    @Override
    public void onNaviInfoUpdate(NaviInfo naviInfo) {

    }

    @Override
    public void onNaviInfoUpdated(AMapNaviInfo aMapNaviInfo) {

    }

    @Override
    public void updateCameraInfo(AMapNaviCameraInfo[] aMapNaviCameraInfos) {

    }

    @Override
    public void onServiceAreaUpdate(AMapServiceAreaInfo[] aMapServiceAreaInfos) {

    }

    @Override
    public void showCross(AMapNaviCross aMapNaviCross) {

    }

    @Override
    public void hideCross() {

    }

    @Override
    public void showLaneInfo(AMapLaneInfo[] aMapLaneInfos, byte[] bytes, byte[] bytes1) {

    }

    @Override
    public void hideLaneInfo() {

    }

    @Override
    public void onCalculateMultipleRoutesSuccess(int[] ints) {
        //多策略
    }

    @Override
    public void notifyParallelRoad(int i) {

    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo aMapNaviTrafficFacilityInfo) {

    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo[] aMapNaviTrafficFacilityInfos) {

    }

    @Override
    public void OnUpdateTrafficFacility(TrafficFacilityInfo trafficFacilityInfo) {

    }

    @Override
    public void updateAimlessModeStatistics(AimLessModeStat aimLessModeStat) {

    }

    @Override
    public void updateAimlessModeCongestionInfo(AimLessModeCongestionInfo aimLessModeCongestionInfo) {

    }

    @Override
    public void onPlayRing(int i) {

    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {

    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

        System.out.println("i = " + i + "geocodeResult.getGeocodeAddressList().size() " + geocodeResult.getGeocodeAddressList().size());

        //解析result获取坐标信息
        if (i == 1000) {
            LatLonPoint latLonPoint;
            to = new ArrayList<>();
            for (int j = 0; j < geocodeResult.getGeocodeAddressList().size(); j++) {
                latLonPoint = geocodeResult.getGeocodeAddressList().get(j).getLatLonPoint();
                to.add(new NaviLatLng(latLonPoint.getLatitude(), latLonPoint.getLongitude()));
                CameraUpdate mCamer = CameraUpdateFactory.newLatLngZoom(new LatLng(latLonPoint.getLatitude(), latLonPoint.getLongitude()), 20);
                aMap.animateCamera(mCamer);
                initNavi();
                System.out.println("结束坐标" + geocodeResult.getGeocodeAddressList().get(j).getLatLonPoint());
            }
        }
    }

    /**
     * 初始化导航
     */
    private void initNavi() {
        //获取AMapNavi实例
        mAMapNavi = AMapNavi.getInstance(getApplicationContext());
        //添加监听回调，用于处理算路成功
        mAMapNavi.addAMapNaviListener(this);
    }

    @Override
    public void onRequestFailed(int i, String s) {

    }

    @Override
    public void onTraceProcessing(int i, int i1, List<LatLng> list) {

    }

    @Override
    public void onFinished(int i, List<LatLng> list, int i1, int i2) {
        Log.d(TAG, "onFinished");
        Toast.makeText(this.getApplicationContext(), "onFinished",
                Toast.LENGTH_SHORT).show();
//        if (mOverlayList.containsKey(lineID)) {
//            TraceOverlay overlay = mOverlayList.get(lineID);
//            overlay.setTraceStatus(TraceOverlay.TRACE_STATUS_FINISH);
//            overlay.setDistance(distance);
//            overlay.setWaitTime(waitingtime);
//            setDistanceWaitInfo(overlay);
//        }
    }
}
