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
import com.amap.api.maps.AMap.*;
import com.amap.api.maps.model.*;
import com.amap.api.services.core.*;
import com.amap.api.services.geocoder.*;
import com.amap.api.services.geocoder.GeocodeSearch.*;
import com.amap.api.services.route.*;
import com.amap.api.services.route.RouteSearch.*;

import static android.R.attr.*;

/**
 * 实现接口：按钮点击接口、定位监听回调接口、路线规划回调接口、逆地理编码异步处理回调接口（输入地址获取经纬度）、截图接口
 * <p>
 * Created by tc on 2017/6/1.
 */


public class MapActivity extends Activity implements OnClickListener, AMapLocationListener, OnRouteSearchListener, OnGeocodeSearchListener, OnMapScreenShotListener {

    private static final int STROKE_COLOR = Color.argb(180, 3, 145, 255);

    private static final int FILL_COLOR = Color.argb(10, 0, 0, 180);

    private Button btStart, btStop, btJieTu, btChaKan;
    private EditText etStartPoint, etStopPoint;
    private ImageView imageView;
    //地图view
    private MapView mMapView = null;

    //初始化地图控制器对象
    private AMap aMap;

    //定位图图标
    private Marker mLocMarker;

    //定位精度圈
    private Circle circle;

    //定位图图标方向辅助类
    private SensorEventHelper mSensorHelper;

    //声明AMapLocationClient类对象
    private AMapLocationClient mLocationClient = null;

    //声明AMapLocationClientOption对象
    private AMapLocationClientOption mLocationOption = null;

    //路线规划回调
    private RouteSearch mRouteSearch;

    //起点，终点
    private LatLonPoint mStartPoint, mEndPoint;

    //根据地址查询经纬度
    private GeocodeSearch geocoderSearch;

    //获取路径返回
    private DriveRouteResult mDriveRouteResult;

    private String cityCode;

    private PolylineOptions mPolyoptions; //行进路线的点

    private Polyline mpolyline;//行进路线的线段

    private boolean isFirst = true; //是否第一次定位

    private LatLng endLatlng; //最后一次定位的经纬度

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView(savedInstanceState);
        initAmapClient();
        initpolyline();
    }

    /**
     * 初始化控件
     *
     * @param savedInstanceState
     */
    private void initView(Bundle savedInstanceState) {
        etStartPoint = (EditText) findViewById(R.id.et_start_point);
        etStopPoint = (EditText) findViewById(R.id.et_stop_point);
        btChaKan = (Button) findViewById(R.id.chakan);
        btStart = (Button) findViewById(R.id.bt_start);
        btStop = (Button) findViewById(R.id.bt_stop);
        btJieTu = (Button) findViewById(R.id.jietu);
        btJieTu.setOnClickListener(this);
        btChaKan.setOnClickListener(this);
        btStart.setOnClickListener(this);
        btStop.setOnClickListener(this);
        imageView = (ImageView) findViewById(R.id.image);

        //获取地图控件引用
        mMapView = (MapView) findViewById(R.id.map);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mMapView.onCreate(savedInstanceState);
        if (aMap == null) {
            aMap = mMapView.getMap();
        }

        /* 定位图标相关 */
        mSensorHelper = new SensorEventHelper(this);
        if (mSensorHelper != null) {
            mSensorHelper.registerSensorListener();
        }

        /* 路线规划 */
        mRouteSearch = new RouteSearch(this);
        mRouteSearch.setRouteSearchListener(this);

        /* 输入地址获取经纬度 */
        geocoderSearch = new GeocodeSearch(this);
        geocoderSearch.setOnGeocodeSearchListener(this);
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

        //启动定位
        mLocationClient.startLocation();
    }

    @Override
    public void onClick(View v) {
        if (v == btStart) {
            imageView.setVisibility(View.GONE);
            aMap.clear();// 清理地图上的所有覆盖物
            //启动定位
            mLocationClient.startLocation();
        } else if (v == btStop) {
            mLocationClient.stopLocation();
            aMap.getMapScreenShot(this); //截图
        } else if (v == btChaKan) {
            imageView.setVisibility(View.GONE);
            mLocationClient.stopLocation();
            if (etStopPoint.getText().toString().trim() == null || etStopPoint.getText().toString().trim().equals("")) {
                Toast.makeText(this, "请输入终点位置", Toast.LENGTH_SHORT).show();
                return;
            }
            // name表示地址，第二个参数表示查询城市，中文或者中文全拼，citycode、adcode
            GeocodeQuery query = new GeocodeQuery(etStopPoint.getText().toString().trim(), cityCode);
            //query.getCity();
            geocoderSearch.getFromLocationNameAsyn(query);
        } else if (v == btJieTu) {
        }
    }

    /**
     * 开始搜索路径规划方案
     */
    private void searchRouteResult() {
        final RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(
                mStartPoint, mEndPoint);
        // 路径规划的起点和终点，驾车模式，途经点，避让区域，避让道路
        RouteSearch.DriveRouteQuery query = new RouteSearch.DriveRouteQuery(fromAndTo, mode, null,
                null, "");
        mRouteSearch.calculateDriveRouteAsyn(query);// 异步路径规划驾车模式查询
    }

    /**
     * 初始化线段配置
     */
    private void initpolyline() {
        mPolyoptions = new PolylineOptions();
        mPolyoptions.width(15f);
        mPolyoptions.color(Color.BLUE);
    }

    /**
     * 定位监听回调方法
     *
     * @param amapLocation
     */
    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (amapLocation != null) {
            if (amapLocation.getErrorCode() == 0) {
                if (isFirst) {//绘制开始Marker点
                    isFirst = false;
                    etStartPoint.setText(amapLocation.getStreet()); //设置街道信息
                    cityCode = amapLocation.getCityCode();//设置城市代码
                    endLatlng = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());//设置最后定位经纬度

                    addCircle(endLatlng, amapLocation.getAccuracy());//添加定位精度圆
                    addMarker(endLatlng);//添加定位图标

                    mStartPoint = new LatLonPoint(amapLocation.getLatitude(), amapLocation.getLongitude());//设置路线起点

                    mLocationClient.stopLocation(); //停止定位
                } else {
                    if (Utils.GetDistance(endLatlng.longitude, endLatlng.latitude, amapLocation.getLongitude(), amapLocation.getLatitude()) > 10) {
                        addCircle(endLatlng, amapLocation.getAccuracy());//添加定位精度圆
                        addMarker(endLatlng);//添加定位图标
                        endLatlng = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());
                        endLatlng = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());
                        mPolyoptions.add(endLatlng);
                        drawLine(amapLocation);
                    } else {
                        System.out.println("移动距离不到10米");
                    }

                    if (mLocMarker == null && circle == null) {
                        addCircle(endLatlng, amapLocation.getAccuracy());//添加定位精度圆
                        addMarker(endLatlng);//添加定位图标
                    }

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
     * 在地图上绘制线段
     */
    private void drawLine(AMapLocation amapLocation) {
        if (mPolyoptions.getPoints().size() > 1) {
            if (mpolyline != null) {
                mpolyline.setPoints(mPolyoptions.getPoints());
            } else {
                mpolyline = aMap.addPolyline(mPolyoptions);
                mpolyline.setZIndex(0);
            }
        }
    }

    //添加精度园
    private void addCircle(LatLng latlng, double radius) {
        CircleOptions options = new CircleOptions();
        options.strokeWidth(1f);
        options.fillColor(FILL_COLOR);
        options.strokeColor(STROKE_COLOR);
        options.center(latlng);
        options.radius(radius);
        circle = aMap.addCircle(options);
    }

    //添加定位图图标
    private void addMarker(LatLng latlng) {
        if (mLocMarker != null) {
            return;
        }
        MarkerOptions options = new MarkerOptions();
        options.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(this.getResources(),
                R.mipmap.navi_map_gps_locked)));
        options.anchor(0.5f, 0.5f);
        options.position(latlng);
        mLocMarker = aMap.addMarker(options);
        mSensorHelper.setCurrentMarker(mLocMarker);//定位图标旋转
        aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(endLatlng, 18));//设置地图中心点以及缩放大小
    }

    /**
     * 公交换乘路径规划结果的回调方法。
     *
     * @param busRouteResult
     * @param i
     */
    @Override
    public void onBusRouteSearched(BusRouteResult busRouteResult, int i) {

    }

    /**
     * 驾车路径规划结果的回调方法。
     *
     * @param driveRouteResult
     * @param i
     */
    @Override
    public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int i) {
        aMap.clear();// 清理地图上的所有覆盖物
        mLocMarker = null;
        circle = null;
        if (i == 1000) {
            if (driveRouteResult != null && driveRouteResult.getPaths() != null) {
                if (driveRouteResult.getPaths().size() > 0) {
                    this.mDriveRouteResult = driveRouteResult;
                    final DrivePath drivePath = mDriveRouteResult.getPaths()
                            .get(0);
                    DrivingRouteOverlay drivingRouteOverlay = new DrivingRouteOverlay(
                            this, aMap, drivePath,
                            mDriveRouteResult.getStartPos(),
                            mDriveRouteResult.getTargetPos(), null);
                    drivingRouteOverlay.setNodeIconVisibility(false);//设置节点marker是否显示
                    drivingRouteOverlay.setIsColorfulline(false);//是否用颜色展示交通拥堵情况，默认true
                    drivingRouteOverlay.removeFromMap();
                    drivingRouteOverlay.addToMap();
                    drivingRouteOverlay.zoomToSpan();
                } else if (driveRouteResult != null && driveRouteResult.getPaths() == null) {
                    Toast.makeText(this, "错误码：" + i, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "错误码：" + i, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "错误码：" + i, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 步行路径规划结果的回调方法。
     *
     * @param walkRouteResult
     * @param i
     */
    @Override
    public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int i) {

    }

    /**
     * 骑行路径规划结果的回调方法。
     *
     * @param rideRouteResult
     * @param i
     */
    @Override
    public void onRideRouteSearched(RideRouteResult rideRouteResult, int i) {

    }

    /**
     * 根据给定的经纬度和最大结果数返回逆地理编码的结果列表。
     *
     * @param regeocodeResult
     * @param i
     */
    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {

    }

    /**
     * 根据给定的地理名称和查询城市，返回地理编码的结果列表。
     *
     * @param geocodeResult
     * @param i
     */
    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

        if (i == 1000) {
            LatLonPoint latLonPoint;
            for (int j = 0; j < geocodeResult.getGeocodeAddressList().size(); j++) {
                latLonPoint = geocodeResult.getGeocodeAddressList().get(j).getLatLonPoint();
                mEndPoint = new LatLonPoint(latLonPoint.getLatitude(), latLonPoint.getLongitude());
                searchRouteResult();
            }
        }
    }

    /**
     * 地图截屏回调此方法。
     *
     * @param bitmap
     */
    @Override
    public void onMapScreenShot(Bitmap bitmap) {
        imageView.setVisibility(View.VISIBLE);
        imageView.setImageBitmap(bitmap);
    }

    /**
     * 地图截屏回调此方法,并返回截屏一瞬间地图是否渲染完成。
     *
     * @param bitmap
     * @param status
     */
    @Override
    public void onMapScreenShot(Bitmap bitmap, int status) {

    }
}
