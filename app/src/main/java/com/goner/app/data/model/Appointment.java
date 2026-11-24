package com.goner.app.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.List;

// appointments/{id}
public class Appointment {

    private String id;
    private String hostUid;
    private String hostNickname;
    private String title;
    private String placeName;
    private String placeAddress;
    private double placeLat;
    private double placeLng;
    private Timestamp appointmentTime;
    private Timestamp endTime;
    private Timestamp originalTime;
    private String penaltyType;  // DB 필드 이름은 penalty_type
    private String penaltyText;  // DB 필드 이름은 penalty_text
    private String status;
    private List<String> participantUids = new ArrayList<>();
    private boolean notStartedNotified;
    @ServerTimestamp
    private Timestamp createdAt; // null로 저장하면 서버 시간이 들어감
    private Timestamp endedAt;

    public Appointment() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getHostUid() { return hostUid; }
    public void setHostUid(String hostUid) { this.hostUid = hostUid; }

    public String getHostNickname() { return hostNickname; }
    public void setHostNickname(String hostNickname) { this.hostNickname = hostNickname; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPlaceName() { return placeName; }
    public void setPlaceName(String placeName) { this.placeName = placeName; }

    public String getPlaceAddress() { return placeAddress; }
    public void setPlaceAddress(String placeAddress) { this.placeAddress = placeAddress; }

    public double getPlaceLat() { return placeLat; }
    public void setPlaceLat(double placeLat) { this.placeLat = placeLat; }

    public double getPlaceLng() { return placeLng; }
    public void setPlaceLng(double placeLng) { this.placeLng = placeLng; }

    public Timestamp getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(Timestamp appointmentTime) { this.appointmentTime = appointmentTime; }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }

    public Timestamp getOriginalTime() { return originalTime; }
    public void setOriginalTime(Timestamp originalTime) { this.originalTime = originalTime; }

    @PropertyName("penalty_type")
    public String getPenaltyType() { return penaltyType; }
    @PropertyName("penalty_type")
    public void setPenaltyType(String penaltyType) { this.penaltyType = penaltyType; }

    @PropertyName("penalty_text")
    public String getPenaltyText() { return penaltyText; }
    @PropertyName("penalty_text")
    public void setPenaltyText(String penaltyText) { this.penaltyText = penaltyText; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getParticipantUids() { return participantUids; }
    public void setParticipantUids(List<String> participantUids) { this.participantUids = participantUids; }

    public boolean isNotStartedNotified() { return notStartedNotified; }
    public void setNotStartedNotified(boolean notStartedNotified) { this.notStartedNotified = notStartedNotified; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getEndedAt() { return endedAt; }
    public void setEndedAt(Timestamp endedAt) { this.endedAt = endedAt; }
}
