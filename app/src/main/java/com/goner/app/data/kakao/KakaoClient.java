package com.goner.app.data.kakao;

import com.goner.app.BuildConfig;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// Retrofit 하나만 만들어서 같이 씀. 모든 요청에 카카오 키 헤더 붙임
public class KakaoClient {

    private static KakaoLocalApi api;

    public static KakaoLocalApi get() {
        if (api == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
                            .header("Authorization", "KakaoAK " + BuildConfig.KAKAO_REST_KEY)
                            .build()))
                    .build();
            api = new Retrofit.Builder()
                    .baseUrl("https://dapi.kakao.com/")
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(KakaoLocalApi.class);
        }
        return api;
    }
}
