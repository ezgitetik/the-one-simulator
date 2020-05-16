package custom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LineStringReader {
    private String lineStringLine;
    private List<Landmark> landmarks;

    public LineStringReader(String lineStringLine) {
        this.lineStringLine = lineStringLine;
        landmarks = new ArrayList<>();
    }

    public String getLineStringLine() {
        return lineStringLine;
    }

    public void setLineStringLine(String lineStringLine) {
        this.lineStringLine = lineStringLine;
    }

    public List<Landmark> getLandmarks() {
        return this.landmarks;
    }

    public void parse() {
        String coordinatesString = this.lineStringLine.substring(12, this.lineStringLine.length() - 1);
        List<String> coordinates = Arrays.asList(coordinatesString.split(","));

        for (String coordinate:coordinates){
            if (coordinate.contains(" ")){
                Landmark landmark = new Landmark();
                landmark.setY(Double.parseDouble(coordinate.trim().split(" ")[1].trim()));
                landmark.setX(Double.parseDouble(coordinate.trim().split(" ")[0].trim()));
                this.landmarks.add(landmark);
            }
        }
    }
}
