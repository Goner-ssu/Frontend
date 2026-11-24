package com.goner.app.data.kakao;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

// 카카오 로컬 API (https://developers.kakao.com/docs/latest/ko/local/dev-guide)
public interface KakaoLocalApi {

    // 키워드로 장소 검색
    @GET("v2/local/search/keyword.json")
    Call<PlaceResponse> searchKeyword(@Query("query") String query);
}
