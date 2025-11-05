package com.neidra.dolcent;


import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RClient {

    private static Retrofit retrofit;
//    private static  String BASE_URL = "https://food.tukir.biz.id/";
    private static  String BASE_URL = "https://dolcent.neidra.my.id/";



    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }


}
