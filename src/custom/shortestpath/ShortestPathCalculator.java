package custom.shortestpath;

import custom.ArffReader;
import custom.Region;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ShortestPathCalculator {

    public static List<String> getShortestPath(String start, String finish) throws IOException {
        //List<List<String>> regions = ArffReader.getRegionListForAllFiles();
        //Map<String, List<Region>> graphMap = ArffReader.getGraphMapByTrafficVolume(regions);
        Map<String, List<Region>> graphMap = getGraphMapByTrafficFlow(ArffReader.REGIONS);

        Graph graph = new Graph();
        graphMap.forEach((key, value) -> {
            List<Vertex> vertexes = value.stream().map(region -> {
                return new Vertex(region.getName(), region.getWeight());
            }).collect(Collectors.toList());
            graph.addVertex(key, vertexes);
        });
        return graph.getShortestPath(start, finish);
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
}
