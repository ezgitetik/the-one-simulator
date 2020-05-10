package custom;

public class Landmark {
    long id = 0;
    double latitude = 0;
    double longitude = 0;
    double x = 0;
    double y = 0;
    private Long timeInSecond;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public Long getTimeInSecond() {
        return timeInSecond;
    }

    public void setTimeInSecond(Long timeInSecond) {
        this.timeInSecond = timeInSecond;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Landmark landmark = (Landmark) o;
        return id == landmark.id &&
                Double.compare(landmark.latitude, latitude) == 0 &&
                Double.compare(landmark.longitude, longitude) == 0 &&
                Double.compare(landmark.x, x) == 0 &&
                Double.compare(landmark.y, y) == 0;
    }

}
