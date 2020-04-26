package custom.shortestpath;

import custom.ArffReader;
import custom.Region;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ShortestPathCalculator {

    private static Map<String, List<Region>> graphMap = null;
    private static Graph graph = null;

    public static List<String> getShortestPath(String start, String finish) {
        //List<List<String>> regions = ArffReader.getRegionListForAllFiles();
        //Map<String, List<Region>> graphMap = ArffReader.getGraphMapByTrafficVolume(regions);
        if (graphMap == null) {
            graphMap = getGraphMapByTrafficFlow(ArffReader.REGIONS);
            graph = new Graph();
            graphMap.forEach((key, value) -> {
                List<Vertex> vertexes = value.stream().map(region -> {
                    return new Vertex(region.getName(), region.getWeight());
                }).collect(Collectors.toList());
                graph.addVertex(key, vertexes);
            });
        }
        return graph.getShortestPath(start, finish);
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

        graphMap.values().forEach(regionList -> regionList.forEach(region -> region.setWeight(1 / region.getFlowCount())));
        return graphMap;
    }

    /*public static Map<String, List<Region>> getGraphMapByTrafficVolume(List<List<String>> regions) {
        List<Region> listOfRegions = getListOfRegions();
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
    }*/

    /*public static List<Region> getListOfRegions() {
        List<String> regionNames = ArffReader.ARFF_REGIONS.stream()
                .map(region -> region.getRegion())
                .filter(distinctByKey(region -> region))
                .sorted()
                .collect(Collectors.toList());

        return regionNames.stream().map(regionName -> {
            int weight = (int) ArffReader.ARFF_REGIONS.stream().filter(region -> region.getRegion().equalsIgnoreCase(regionName)).count();
            return new Region(regionName, (1 / weight));
        }).collect(Collectors.toList());
    }*/

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {

        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }


}
