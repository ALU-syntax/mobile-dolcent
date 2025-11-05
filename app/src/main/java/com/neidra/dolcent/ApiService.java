package com.neidra.dolcent;


import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {

    @GET("kasir/api-struk/{id}")
    Call<GetStruk> getStruk(@Path("id") String id);
}
