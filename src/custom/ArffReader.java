package custom;

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

    private static final String ARFF_WITHOUT_MOD = "custom/taxidata/10taxi-month1/10taxi-month1-weka.arff";
    private static final String ARFF_PATH = ARFF_WITHOUT_MOD;

    private static final String TAXI_WITHOUT_MOD = "custom/taxidata/10taxi-month1/10taxi-month1-training-day1/";
    private static final String TAXI_SIMULATION = "custom/taxidata/10taxi-month1/10taxi-month1-simulation/";

    private static final String TAXI_PATH = TAXI_WITHOUT_MOD;

    private static List<ArffRegion> ARFF_REGIONS = null;

    public static List<List<String>> REGIONS = new ArrayList<>();

    public static List<List<String>> DISTINCTED_REGIONS = new ArrayList<>();

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
        REGIONS = ArffReader.getRegionListForAllFiles();
        return ARFF_REGIONS;
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
                DISTINCTED_REGIONS.add(getDistinctRegions(region));
            } catch (IOException e) {
                e.printStackTrace();
            }

        })).join();
        return regionList;

    }

    public static List<ArffRegion> getArffRegionListByFileName(String fileName) throws IOException {
        InputStream stream = ArffReader.class.getClassLoader().getResourceAsStream(TAXI_SIMULATION + fileName);
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
                .map(landmark -> ArffReader.getArffRegionByPoints(landmark.getX(), landmark.getY()))
                .collect(Collectors.toList());
    }

    private static ArffRegion getArffRegionByPoints(Double xPoint, Double yPoint) {
        return ARFF_REGIONS.parallelStream()
                .filter(arffRegion -> arffRegion.getxPoint().equals(xPoint) && arffRegion.getyPoint().equals(yPoint))
                .findFirst()
                .orElse(new ArffRegion(0.0, 0.0, ""));
    }

    private static String getRegionByPoints(Double xPoint, Double yPoint) {
        return ARFF_REGIONS
                .parallelStream()
                .filter(arffRegion -> arffRegion.getxPoint().equals(xPoint) && arffRegion.getyPoint().equals(yPoint))
                .findAny()
                .orElse(new ArffRegion(0.0, 0.0, ""))
                .getRegion();
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

    public static List<Region> getListOfRegions() {
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

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {

        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
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

    public static void main(String[] args) throws IOException {
        ArffReader.read();
        REGIONS.forEach(region -> System.out.println(region.stream().collect(Collectors.joining(","))));
    }

}
