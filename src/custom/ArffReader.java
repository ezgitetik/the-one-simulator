package custom;

//import com.sun.deploy.util.StringUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArffReader {

    private static List<ArffRegion> ARFF_REGIONS = null;

    public static List<ArffRegion> read() throws IOException {
        if (ARFF_REGIONS == null) {
            InputStream stream = ArffReader.class.getClassLoader().getResourceAsStream("custom/taxidata/taxi-top10-all-0101-weka.arff");
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line = reader.readLine();
            List<ArffRegion> arffRegions = new ArrayList<>();
            while (line != null) {

                String[] regionString = line.split(",");
                if (regionString.length == 4) {
                    ArffRegion arffRegion = new ArffRegion(Double.parseDouble(regionString[1]), Double.parseDouble(regionString[2]), regionString[3]);
                    arffRegions.add(arffRegion);
                }
                line = reader.readLine();
            }
            reader.close();
            ARFF_REGIONS = arffRegions;
        }
        return ARFF_REGIONS;
    }

    public static String getRegionByPoints(Double xPoint, Double yPoint) {
        return ARFF_REGIONS.stream()
                .filter(arffRegion -> arffRegion.getxPoint().equals(xPoint) && arffRegion.getyPoint().equals(yPoint))
                .findFirst().orElse(new ArffRegion(0.0, 0.0, "")).getRegion();
    }

    public static List<String> getRegionListByFileName(String fileName) throws IOException {
        InputStream stream = ArffReader.class.getClassLoader().getResourceAsStream("custom/taxidata/bursa-0101/" + fileName);
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

    public static List<List<String>> getRegionListForAllFiles() throws IOException {
        String rootFolder = ArffReader.class.getClassLoader().getResource("custom/taxidata/bursa-0101").getPath();
        List<String> files = Stream.of(new File(rootFolder).listFiles())
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toList());
        List<List<String>> regions = new ArrayList<>();
        files.forEach(file -> {
            InputStream stream = ArffReader.class.getClassLoader().getResourceAsStream("custom/taxidata/bursa-0101/" + file);
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

                //System.out.println(System.lineSeparator());
                //System.out.println(file);
                //System.out.println(StringUtils.join(region, ","));
                regions.add(region);
            } catch (IOException e) {
                e.printStackTrace();
            }

        });
        return regions;

    }

    public static List<String> getDistinctRegionListByFileName(String fileName) throws IOException {
        InputStream stream = ArffReader.class.getClassLoader().getResourceAsStream("custom/taxidata/bursa-0101/" + fileName);
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

    public static <T> Predicate<T> distinctByKey(
            Function<? super T, ?> keyExtractor) {

        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    public static List<Region> getListOfRegions() {
        List<String> regionNames = ARFF_REGIONS.stream()
                .map(region -> region.getRegion())
                .filter(distinctByKey(region -> region))
                .sorted()
                .collect(Collectors.toList());

        return regionNames.stream().map(regionName -> {
            int weight = (int) ARFF_REGIONS.stream().filter(region -> region.getRegion().equalsIgnoreCase(regionName)).count();
            return new Region(regionName, (100000 - weight));
        }).collect(Collectors.toList());
    }

    public static Map<String, List<Region>> getGraphMap(List<List<String>> regions) {
        List<Region> listOfRegions = ArffReader.getListOfRegions();
        Map<String, List<Region>> graphMap = new HashMap<>();
        final String[] controlRegion = {null};
        regions.forEach(regionsx->{
            controlRegion[0] = null;
            for (String region : regionsx) {
                if (controlRegion[0] != null && !region.equalsIgnoreCase(controlRegion[0])) {
                    if (controlRegion[0].equalsIgnoreCase("cluster32") && region.equalsIgnoreCase("cluster1")
                            || controlRegion[0].equalsIgnoreCase("cluster1") && region.equalsIgnoreCase("cluster32")){
                        String x="";
                    }
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

    private static List<String> getShortestPath(String start, String finish) throws IOException {
        List<List<String>> regions = ArffReader.getRegionListForAllFiles();
        Map<String, List<Region>> graphMap = ArffReader.getGraphMap(regions);

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
        List<ArffRegion> arffRegions = ArffReader.read();

        //System.out.println(ArffReader.getRegionByPoints(28183.048, 34760.223));

        //List<String> regions = ArffReader.getDistinctRegionListByFileName("taxi-528.wkt");



        String start = "cluster17";
        String finish = "cluster4";
        List<String> path = ArffReader.getShortestPath(start, finish);

        Collections.reverse(path);
        //System.out.println(start + "->" + StringUtils.join(path, "->"));

        //System.out.println(StringUtils.join(regions, ","));
        //List<Region> regions=ArffReader.getListOfRegions();
    }

}
