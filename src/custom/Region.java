package custom;

public class Region {
    private String name;
    private double weight;
    private double flowCount;

    public double getFlowCount() {
        return flowCount;
    }

    public void setFlowCount(double flowCount) {
        this.flowCount = flowCount;
    }

    public void increaseFlowCount(){
        this.flowCount++;
    }

    public Region(String name, double weight) {
        this.name = name;
        this.weight = weight;
    }

    public Region(String name, double weight, double flowCount) {
        this.name = name;
        this.weight = weight;
        this.flowCount = flowCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
}
