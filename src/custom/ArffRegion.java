package custom;

public class ArffRegion {

    private Double xPoint;
    private Double yPoint;
    private String region;
    private String taxiName;
    private double timeInSecond;

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

    public String getTaxiName() {
        return taxiName;
    }

    public void setTaxiName(String taxiName) {
        this.taxiName = taxiName;
    }

    public double getTimeInSecond() {
        return timeInSecond;
    }

    public void setTimeInSecond(double timeInSecond) {
        this.timeInSecond = timeInSecond;
    }
}
