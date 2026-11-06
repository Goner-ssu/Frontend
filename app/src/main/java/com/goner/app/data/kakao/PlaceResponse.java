package com.goner.app.data.kakao;

import java.util.List;

// keyword / category 검색 응답 (필드 이름은 카카오 JSON 그대로)
public class PlaceResponse {

    public List<Place> documents;

    public static class Place {
        public String place_name;
        public String address_name;
        public String road_address_name;
        public String x; // 경도
        public String y; // 위도

        // 도로명 주소 있으면 그거, 없으면 지번
        public String getAddress() {
            return road_address_name != null && !road_address_name.isEmpty() ? road_address_name : address_name;
        }
    }
}
