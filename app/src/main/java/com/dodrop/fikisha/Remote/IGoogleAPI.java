package com.dodrop.fikisha.Remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

/**
 * Created by Lenny Kamande on 5/8/2018.
 */

public interface IGoogleAPI {
    @GET
    Call<String> getPath(@Url String url);

}
