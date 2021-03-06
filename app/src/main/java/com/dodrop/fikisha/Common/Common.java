package com.dodrop.fikisha.Common;

import android.location.Location;

import com.dodrop.fikisha.Model.User;
import com.dodrop.fikisha.Remote.FCMClient;
import com.dodrop.fikisha.Remote.IFCMService;
import com.dodrop.fikisha.Remote.IGoogleAPI;
import com.dodrop.fikisha.Remote.RetrofitClient;

/**
 * Created by Lenny Kamande on 5/8/2018.
 */

public class Common {

	public static final String driver_tbl = "Drivers";
    public static final String user_driver_tbl = "DriversInformation";
    public static final String user_rider_tbl = "RidersInformation";
    public static final String pickup_request_tbl = "PickupRequest";
    public static final String token_tbl = "Tokens";

    public static User currentUser;

    public static Location mLastLocation=null;

    public static final String baseURL = "https://maps.googleapis.com";
    public static final String fcmURL = "https://fcm.googleapis.com";

    public static final double base_fare = 20.00;
    public static final double time_rate = 4.00;
    public static final double distance_rate = 5.00;

    public static double getPrice(double km, double min)
    {
        return (base_fare+(time_rate*min)+(distance_rate*km));
    }



    public static IGoogleAPI getGoogleAPI()
    {
        return RetrofitClient.getClient(baseURL).create(IGoogleAPI.class);
    }

    public static IFCMService getFCMService()
    {
        return FCMClient.getClient(fcmURL).create(IFCMService.class);
    }
}
