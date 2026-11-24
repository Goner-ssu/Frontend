package com.goner.app.data;

// DB에 들어가는 값(상태, 벌칙 종류)은 여기 모아 둠 (팀 ERD 기준)
public final class DbValues {

    private DbValues() {
    }

    // appointments.status
    public static final String APPOINTMENT_PENDING = "pending";     // 수락 대기
    public static final String APPOINTMENT_CONFIRMED = "confirmed"; // 모두 수락
    public static final String APPOINTMENT_ONGOING = "ongoing";
    public static final String APPOINTMENT_ENDED = "ended";
    public static final String APPOINTMENT_CANCELED = "canceled";

    // participants.status (출발·도착 관련 값은 레이더 담당이 추가)
    public static final String PARTICIPANT_INVITED = "invited";
    public static final String PARTICIPANT_JOINED = "joined";
    public static final String PARTICIPANT_REJECTED = "rejected";

    // penalty_type
    public static final String PENALTY_MEAL = "meal";
    public static final String PENALTY_COFFEE = "coffee";
    public static final String PENALTY_CUSTOM = "custom";
    public static final String PENALTY_NONE = "none";
}
