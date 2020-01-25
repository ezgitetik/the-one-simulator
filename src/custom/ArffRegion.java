package custom;

public class ArffRegion {

    private Double xPoint;
    private Double yPoint;
    private String region;

    public ArffRegion(Double xPoint, Double yPoint, String region) {
        this.xPoint = xPoint;
        this.yPoint = yPoint;
        this.region = region;
    }

    public Double getxPoint() {
        return xPoint;
    }

    public void setxPoint(Double xPoint) {
        this.xPoint = xPoint;
    }

    public Double getyPoint() {
        return yPoint;
    }

    public void setyPoint(Double yPoint) {
        this.yPoint = yPoint;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
