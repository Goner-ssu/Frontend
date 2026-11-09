package com.goner.app.data.model;

import com.google.firebase.Timestamp;

// appointments/{id}/participants/{uid}
// departedAt, etaMinutes, arrivedAt, arrivalType, lateMinutes, rank 는 레이더/서버가 씀
public class Participant {

    private String appointmentId;
    private String uid;
    private String nickname;
    private String photoUrl;
    private String status;
    private Timestamp joinedAt;
    private Timestamp departedAt;
    private Long etaMinutes;
    private Timestamp arrivedAt;
    private String arrivalType;
    private Long lateMinutes;
    private Long rank;

    public Participant() {
    }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Timestamp joinedAt) { this.joinedAt = joinedAt; }

    public Timestamp getDepartedAt() { return departedAt; }
    public void setDepartedAt(Timestamp departedAt) { this.departedAt = departedAt; }

    public Long getEtaMinutes() { return etaMinutes; }
    public void setEtaMinutes(Long etaMinutes) { this.etaMinutes = etaMinutes; }

    public Timestamp getArrivedAt() { return arrivedAt; }
    public void setArrivedAt(Timestamp arrivedAt) { this.arrivedAt = arrivedAt; }

    public String getArrivalType() { return arrivalType; }
    public void setArrivalType(String arrivalType) { this.arrivalType = arrivalType; }

    public Long getLateMinutes() { return lateMinutes; }
    public void setLateMinutes(Long lateMinutes) { this.lateMinutes = lateMinutes; }

    public Long getRank() { return rank; }
    public void setRank(Long rank) { this.rank = rank; }
}
