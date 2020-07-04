package custom;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConvertClusterDataToSequenceDataForTaxiByTaxi {

    private static final String ARFF_PATH = "custom/taxidata/100taxi-month2/100taxi-month2-weka-20.arff";
    private static final String TAXI_PATH = "custom/taxidata/100taxi-month1/100taxi-month1-training/";
    private static final String SIMULATION_PATH = "custom/taxidata/100taxi-month2/100taxi-month2-simulation/";


    public static void main(String[] args) throws IOException {
        Map<String, ArffRegion> arffRegions = read();

        String rootFolder = ArffReader.class.getClassLoader().getResource(SIMULATION_PATH).getPath();
        List<String> files = Stream.of(new File(rootFolder).listFiles())
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toList());

        ForkJoinPool forkJoinPool = new ForkJoinPool();
        forkJoinPool.submit(() -> files.stream().forEach(file -> {
            if(file.equalsIgnoreCase("taxi-320.wkt")){
                System.out.print("");
            }
           List<List<String>> splittedLines = null;
            try {
                splittedLines = getRegionListForTaxi(arffRegions, file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            StringBuilder stringBuilder = new StringBuilder();
            for (List<String> line : splittedLines) {
                List<List<String>> partitionedLines = new ArrayList<>();
                for (int i = 0; i < line.size() - 4; i++) {
                    partitionedLines.add(line.subList(i, i + 5));
                }
                for (List<String> partitioned : partitionedLines) {
                    stringBuilder.append(String.join(" ", partitioned));
                    stringBuilder.append(System.lineSeparator());
                }
            }
            try {
                writeToFile(stringBuilder.toString(), file);
            } catch (IOException e) {
                e.printStackTrace();
            }

        })).join();

        System.out.println("finished successfully.");
    }

    public static Map<String, ArffRegion> read() throws IOException {
        List<String> allLines = Files.readAllLines(Paths.get(ArffReader.class.getClassLoader().getResource(ARFF_PATH).getPath()));
        ForkJoinPool forkJoinPool = new ForkJoinPool(12);
        Map<String, ArffRegion> arffRegions = new HashMap<>();
        forkJoinPool.submit(() -> allLines.forEach(line -> {
            String[] regionString = line.split(",");
            if (regionString.length == 4) {
                ArffRegion arffRegion = new ArffRegion(Double.parseDouble(regionString[1]), Double.parseDouble(regionString[2]), regionString[3]);
                arffRegions.put(arffRegion.getxPoint() + "#" + arffRegion.getyPoint(), arffRegion);
            }
        })).join();

        return arffRegions;
    }

    private static List<List<String>> getRegionListForTaxi(Map<String, ArffRegion> arffRegions, String taxiFileName) throws IOException {

        List<List<String>> distinctedRegions = new ArrayList<>();

        for (int i = 1; i <= 7; i++) {
            InputStream stream = ArffReader.class.getClassLoader().getResourceAsStream(TAXI_PATH + taxiFileName.replace(".wkt", "") + "-" + i + ".wkt");
            if(stream == null){
                System.out.println("not exist file " + taxiFileName.replace(".wkt", "") + "-" + i + ".wkt");
                break;
            } else {
                System.out.println("file " + taxiFileName.replace(".wkt", "") + "-" + i + ".wkt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                String line = null;
                try {
                    line = reader.readLine();
                    String lineStringLine = "";
                    while (line != null) {
                        lineStringLine = line;
                        line = reader.readLine();
                    }
                    reader.close();
                    LineStringReader lineStringReader = new LineStringReader(lineStringLine);
                    lineStringReader.parse();

                    List<String> region = new ArrayList<>();
                    for (Landmark landmark : lineStringReader.getLandmarks()) {
                        region.add(getRegionByPoints(arffRegions, landmark.getX(), landmark.getY()));
                    }

                    distinctedRegions.add(getDistinctRegions(region));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return distinctedRegions;

    }

    private static List<String> getDistinctRegions(List<String> regions) {
        String lookupRegion = "";
        List<String> updatedTaxiPath = new ArrayList<>();
        for (String region : regions) {
            if (!region.equals(lookupRegion) && !region.equals("")) {
                lookupRegion = region;
                updatedTaxiPath.add(region);
            }
        }
        return updatedTaxiPath;
    }

    private static String getRegionByPoints(Map<String, ArffRegion> arffRegions, Double xPoint, Double yPoint) {
        ArffRegion region = arffRegions.get(xPoint + "#" + yPoint);
        if (region != null) {
            return region.getRegion().replace("cluster", "");
        } else {
            return "";
        }
    }

    private static void writeToFile(String content, String file) throws IOException {

        String resourceFolderPath = String.valueOf(Paths.get(ConvertClusterDataToSequenceDataForTaxiByTaxi.class.getClassLoader().getResource("sequence-20").getPath()));

        File myFile = new File(resourceFolderPath + "/"+file);

        if (!myFile.exists()) {
            Files.createFile(myFile.toPath());
        }

        Files.write(Paths.get(ConvertClusterDataToSequenceDataForTaxiByTaxi.class.getClassLoader().getResource("sequence-20/"+file).getPath()),
                content.getBytes(), StandardOpenOption.WRITE);

    }
}
