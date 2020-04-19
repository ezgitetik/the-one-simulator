package custom;

//import com.sun.deploy.util.StringUtils;

//import com.sun.deploy.util.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArffReader {

  //  private static final String ARFF_WITH_MOD = "custom/taxidata/taxi-top10-all-0101-weka.arff";
   // private static final String ARFF_WITHOUT_MOD = "custom/taxidata/100taxi-month1-week1-weka.arff";
    private static final String ARFF_WITHOUT_MOD = "custom/taxidata/100taxi-month1-weka.arff";
    private static final String ARFF_PATH = ARFF_WITHOUT_MOD;
 //   private static final String ARFF_PATH = "";
    private static final String TAXI_WITH_MOD = "custom/taxidata/100taxi-month1-training/";
  //  private static final String TAXI_WITHOUT_MOD = "custom/taxidata/bursa-0101-notmodulo/";
    private static final String TAXI_PATH = TAXI_WITH_MOD;


    private static List<ArffRegion> ARFF_REGIONS = null;

    public static List<List<String>> regions = new ArrayList<>();

    public static List<List<String>> distinctedRegions = new ArrayList<>();

    public static List<ArffRegion> read() throws IOException {
        if (ARFF_REGIONS == null) {
            List<String> allLines = Files.readAllLines(Paths.get(ArffReader.class.getClassLoader().getResource(ARFF_PATH).getPath()));
            ForkJoinPool forkJoinPool = new ForkJoinPool(50);
            List<ArffRegion> arffRegions = new ArrayList<>();
            forkJoinPool.submit(() -> allLines.forEach(line -> {
                String[] regionString = line.split(",");
                if (regionString.length == 4) {
                    ArffRegion arffRegion = new ArffRegion(Double.parseDouble(regionString[1]), Double.parseDouble(regionString[2]), regionString[3]);
                    arffRegions.add(arffRegion);
                }
            })).join();
            ARFF_REGIONS = arffRegions;
        }
        regions = ArffReader.getRegionListForAllFiles();
        return ARFF_REGIONS;
    }

    private static String getRegionByPoints(Double xPoint, Double yPoint) {
        return ARFF_REGIONS
                .parallelStream()
                .filter(arffRegion -> arffRegion.getxPoint().equals(xPoint) && arffRegion.getyPoint().equals(yPoint))
                .findAny()
                .orElse(new ArffRegion(0.0, 0.0, ""))
                .getRegion();
    }

    private static ArffRegion getArffRegionByPoints(Double xPoint, Double yPoint) {
        return ARFF_REGIONS.stream()
                .filter(arffRegion -> arffRegion.getxPoint().equals(xPoint) && arffRegion.getyPoint().equals(yPoint))
                .findFirst().orElse(new ArffRegion(0.0, 0.0, ""));
    }

    public static String getMostClosestRegionByPoints(Double xPoint, Double yPoint) {
        Map<Double, String> hypotenuseCluster = new HashMap<>();
        // TODO: calculate arff regions by taxi
     /*   ForkJoinPool forkJoinPool = new ForkJoinPool(20);
        forkJoinPool.submit(() -> ARFF_REGIONS.parallelStream().forEach(arffRegion -> {
            hypotenuseCluster.put(Math.hypot(xPoint - arffRegion.getxPoint(), yPoint - arffRegion.getyPoint()), arffRegion.getRegion());
        })).join();*/
        ARFF_REGIONS.forEach(arffRegion -> {
            hypotenuseCluster.put(Math.hypot(xPoint - arffRegion.getxPoint(), yPoint - arffRegion.getyPoint()), arffRegion.getRegion());
        });
        OptionalDouble key = hypotenuseCluster.keySet().stream().mapToDouble(v -> v).min();
        return hypotenuseCluster.get(key.getAsDouble());
    }

    public static ArffRegion getMostClosestArffRegionByPointsAndList(Double xPoint, Double yPoint, List<ArffRegion> arffRegions) {
        Map<Double, ArffRegion> hypotenuseCluster = new HashMap<>();
        arffRegions.forEach(arffRegion -> {
            hypotenuseCluster.put(Math.hypot(xPoint - arffRegion.getxPoint(), yPoint - arffRegion.getyPoint()), arffRegion);
        });
        OptionalDouble key = hypotenuseCluster.keySet().stream().mapToDouble(v -> v).min();
        return hypotenuseCluster.get(key.getAsDouble());
    }

    public static List<String> getRegionListByFileName(String fileName) throws IOException {
        InputStream stream = ArffReader.class.getClassLoader().getResourceAsStream(TAXI_PATH + fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line = reader.readLine();
        String lineStringLine = "";
        while (line != null) {
            lineStringLine = line;
            line = reader.readLine();
        }
        reader.close();
        LineStringReader lineStringReader = new LineStringReader(lineStringLine);
        lineStringReader.parse();
        return lineStringReader.getLandmarks()
                .stream()
                .map(landmark -> ArffReader.getRegionByPoints(landmark.getX(), landmark.getY())).collect(Collectors.toList());
    }

    public static List<ArffRegion> getArffRegionListByFileName(String fileName) throws IOException {
        InputStream stream = ArffReader.class.getClassLoader().getResourceAsStream(TAXI_PATH + fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line = reader.readLine();
        String lineStringLine = "";
        while (line != null) {
            lineStringLine = line;
            line = reader.readLine();
        }
        reader.close();
        LineStringReader lineStringReader = new LineStringReader(lineStringLine);
        lineStringReader.parse();
        return lineStringReader.getLandmarks()
                .stream()
                .map(landmark -> ArffReader.getArffRegionByPoints(landmark.getX(), landmark.getY())).collect(Collectors.toList());
    }

    private static List<List<String>> getRegionListForAllFiles() throws IOException {
        String rootFolder = ArffReader.class.getClassLoader().getResource(TAXI_PATH).getPath();
        List<String> files = Stream.of(new File(rootFolder).listFiles())
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toList());

        List<List<String>> regionList = new ArrayList<>();

        ForkJoinPool forkJoinPool = new ForkJoinPool(50);
        forkJoinPool.submit(() -> files.parallelStream().forEach(file -> {
            InputStream stream = ArffReader.class.getClassLoader().getResourceAsStream(TAXI_PATH + file);
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

                List<String> region = lineStringReader.getLandmarks()
                        .stream()
                        .map(landmark -> ArffReader.getRegionByPoints(landmark.getX(), landmark.getY())).collect(Collectors.toList());
                regionList.add(region);
                distinctedRegions.add(getDistinctRegions(region));
            } catch (IOException e) {
                e.printStackTrace();
            }

        })).join();
        return regionList;

    }

    private static List<String> getDistinctRegions(List<String> regions) {
        final String[] lookupRegion = {""};
        List<String> updatedTaxiPath = new ArrayList<>();
        regions.forEach(region -> {
            if (!region.equals(lookupRegion[0]) && !region.equals("")) {
                lookupRegion[0] = region;
                updatedTaxiPath.add(region);
            }
        });
        return updatedTaxiPath;
    }

    public static List<String> getDistinctRegionListByFileName(String fileName) throws IOException {
        InputStream stream = ArffReader.class.getClassLoader().getResourceAsStream(TAXI_PATH + fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line = reader.readLine();
        String lineStringLine = "";
        while (line != null) {
            lineStringLine = line;
            line = reader.readLine();
        }
        reader.close();
        LineStringReader lineStringReader = new LineStringReader(lineStringLine);
        lineStringReader.parse();

        List<Landmark> landmarks = lineStringReader.getLandmarks();

        return landmarks
                .stream()
                .map(landmark ->
                {
                    String region = ArffReader.getRegionByPoints(landmark.getX(), landmark.getY());
                    return region;
                })
                .filter(distinctByKey(region -> region))
                .collect(Collectors.toList());
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {

        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    private static List<Region> getListOfRegions() {
        List<String> regionNames = ARFF_REGIONS.stream()
                .map(region -> region.getRegion())
                .filter(distinctByKey(region -> region))
                .sorted()
                .collect(Collectors.toList());

        return regionNames.stream().map(regionName -> {
            int weight = (int) ARFF_REGIONS.stream().filter(region -> region.getRegion().equalsIgnoreCase(regionName)).count();
            return new Region(regionName, (1 / weight));
        }).collect(Collectors.toList());
    }

    public static Map<String, List<Region>> getGraphMapByTrafficVolume(List<List<String>> regions) {
        List<Region> listOfRegions = ArffReader.getListOfRegions();
        Map<String, List<Region>> graphMap = new HashMap<>();
        final String[] controlRegion = {null};
        regions.forEach(regionsx -> {
            controlRegion[0] = null;
            for (String region : regionsx) {
                if (controlRegion[0] != null && !region.equalsIgnoreCase(controlRegion[0])) {
                    if (graphMap.get(controlRegion[0]) == null) graphMap.put(controlRegion[0], new ArrayList<>());
                    List<Region> neighbors = graphMap.get(controlRegion[0]);
                    if (neighbors.stream().filter(neighbor -> neighbor.getName().equalsIgnoreCase(region)).count() <= 0) {
                        neighbors.add(listOfRegions.stream().filter(lor -> lor.getName().equalsIgnoreCase(region)).findFirst().get());
                        graphMap.put(controlRegion[0], neighbors);
                    }

                    if (graphMap.get(region) == null) graphMap.put(region, new ArrayList<>());
                    List<Region> reverseNeighbors = graphMap.get(region);
                    String finalControlRegion = controlRegion[0];
                    if (reverseNeighbors.stream().filter(reverseNeighbor -> reverseNeighbor.getName().equalsIgnoreCase(finalControlRegion)).count() <= 0) {
                        reverseNeighbors.add(listOfRegions.stream().filter(lor -> lor.getName().equalsIgnoreCase(finalControlRegion)).findFirst().get());
                        graphMap.put(region, reverseNeighbors);
                    }
                }
                controlRegion[0] = region;
            }
        });
        return graphMap;
    }

    private static Map<String, List<Region>> getGraphMapByTrafficFlow(List<List<String>> regions) {
        Map<String, List<Region>> graphMap = new HashMap<>();
        final String[] controlRegion = {null};
        regions.forEach(regionsx -> {
            controlRegion[0] = null;
            for (String region : regionsx) {
                if (controlRegion[0] != null && !region.equalsIgnoreCase(controlRegion[0])) {

                    if (graphMap.get(controlRegion[0]) == null) graphMap.put(controlRegion[0], new ArrayList<>());
                    List<Region> neighbors = graphMap.get(controlRegion[0]);

                    if (neighbors.stream().filter(neighbor -> neighbor.getName().equalsIgnoreCase(region)).count() <= 0) {
                        Region currentRegion = new Region(region, 0, 1);
                        neighbors.add(currentRegion);
                        graphMap.put(controlRegion[0], neighbors);
                    } else {
                        Region currentRegion = neighbors.stream().filter(neighbor -> neighbor.getName().equalsIgnoreCase(region)).findFirst().get();
                        currentRegion.increaseFlowCount();
                    }

                }
                controlRegion[0] = region;
            }
        });
        graphMap.values().forEach(regionList -> regionList.stream().forEach(region -> region.setWeight(1 / region.getFlowCount())));
        return graphMap;
    }

    public static List<String> getShortestPath(String start, String finish) throws IOException {
        //List<List<String>> regions = ArffReader.getRegionListForAllFiles();
        //Map<String, List<Region>> graphMap = ArffReader.getGraphMapByTrafficVolume(regions);
        Map<String, List<Region>> graphMap = ArffReader.getGraphMapByTrafficFlow(regions);

        Graph graph = new Graph();
        graphMap.forEach((key, value) -> {
            List<Vertex> vertexes = value.stream().map(region -> {
                return new Vertex(region.getName(), region.getWeight());
            }).collect(Collectors.toList());
            graph.addVertex(key, vertexes);
        });
        return graph.getShortestPath(start, finish);
    }

    public static void main(String[] args) throws IOException {
        ArffReader.read();
        regions.forEach(region -> System.out.println(region.stream().collect(Collectors.joining(","))));
    }

}
