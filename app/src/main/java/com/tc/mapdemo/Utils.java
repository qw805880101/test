package com.tc.mapdemo;

import com.amap.api.location.*;
import com.amap.api.trace.*;

/**
 * Created by tc on 2017/5/31.
 */

public class Utils {

    public static TraceLocation parseTraceLocation(AMapLocation amapLocation) {
        TraceLocation location = new TraceLocation();
        location.setBearing(amapLocation.getBearing());
        location.setLatitude(amapLocation.getLatitude());
        location.setLongitude(amapLocation.getLongitude());
        location.setSpeed(amapLocation.getSpeed());
        location.setTime(amapLocation.getTime());
        return  location;
    }

}
