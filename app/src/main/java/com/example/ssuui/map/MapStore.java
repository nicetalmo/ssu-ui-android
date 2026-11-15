package com.example.ssuui.map;

// 필드 이름은 Firestore stores 문서와 같음. lat, lng, booth는 아직 서버에 없음
public class MapStore {

    public static final String STATUS_OPEN = "open";
    public static final String STATUS_PAUSED = "paused";
    public static final String STATUS_CLOSED = "closed";

    private String id;
    private String name;
    private double lat;
    private double lng;
    private int waitingCount;
    private int minutesPerTeam;
    private boolean booth;
    private String status;

    public MapStore() {
    }

    public MapStore(String id, String name, double lat, double lng,
                    int waitingCount, int minutesPerTeam, boolean booth, String status) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.waitingCount = waitingCount;
        this.minutesPerTeam = minutesPerTeam;
        this.booth = booth;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }

    public int getWaitingCount() { return waitingCount; }
    public void setWaitingCount(int waitingCount) { this.waitingCount = waitingCount; }

    public int getMinutesPerTeam() { return minutesPerTeam; }
    public void setMinutesPerTeam(int minutesPerTeam) { this.minutesPerTeam = minutesPerTeam; }

    public boolean isBooth() { return booth; }
    public void setBooth(boolean booth) { this.booth = booth; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean acceptsQueue() {
        return STATUS_OPEN.equals(status);
    }

    public boolean hiddenOnMap() {
        return STATUS_CLOSED.equals(status);
    }
}
