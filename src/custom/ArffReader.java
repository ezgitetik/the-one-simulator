package custom;

import core.Settings;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

public class ArffReader {


    private static final Settings s = new Settings(); // don't use any namespace

    private static final String ARFF_WITHOUT_MOD = s.getSetting("ARFF_WITHOUT_MOD");
    private static final String ARFF_PATH = ARFF_WITHOUT_MOD;

    private static final String TAXI_SIMULATION = s.getSetting("TAXI_SIMULATION");
    private static final String TAXI_SECOND = s.getSetting("TAXI_SECOND");

    private static final String TAXI_PATH = TAXI_SIMULATION;

    //private static List<ArffRegion> ARFF_REGIONS = null;

    private static Map<String, ArffRegion> ARFF_REGIONS = null;

    public static List<List<String>> REGIONS = new ArrayList<>();

    //private static Map<String, List<ArffRegion>> pointsAndClusters = new HashMap<>();
    private static Map<String, Map<String, ArffRegion>> pointsAndClusters = new HashMap<>();

    public static Map<String, ArffRegion> read() throws IOException {
        if (ARFF_REGIONS == null) {
            List<String> allLines = Files.readAllLines(Paths.get(ArffReader.class.getClassLoader().getResource(ARFF_PATH).getPath()));
            ForkJoinPool forkJoinPool = new ForkJoinPool(12);
            List<ArffRegion> arffRegions = new ArrayList<>();
            ARFF_REGIONS = new HashMap<>();
            forkJoinPool.submit(() -> allLines.forEach(line -> {
                String[] regionString = line.split(",");
                if (regionString.length == 4) {
                    ArffRegion arffRegion = new ArffRegion(Double.parseDouble(regionString[1]), Double.parseDouble(regionString[2]), regionString[3]);
                    arffRegions.add(arffRegion);
                    ARFF_REGIONS.put(arffRegion.getxPoint() + "#" + arffRegion.getyPoint(), arffRegion);
                }
            })).join();

            //ARFF_REGIONS = arffRegions;
        }
        REGIONS = ArffReader.getRegionListForAllFiles();
        return ARFF_REGIONS;
    }

    private static List<List<String>> getRegionListForAllFiles() {
        String rootFolder = ArffReader.class.getClassLoader().getResource(TAXI_PATH).getPath();
        List<String> files = getFileNames(rootFolder);

        List<List<String>> regionList = new ArrayList<>();

        ForkJoinPool forkJoinPool = new ForkJoinPool(12);

        forkJoinPool.submit(() -> files.parallelStream().forEach(file -> {
            InputStream stream = ArffReader.class.getClassLoader().getResourceAsStream(TAXI_PATH + file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line;
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

                //List<ArffRegion> regions = new ArrayList<>();
                Map<String, ArffRegion> regions = new HashMap<>();
                List<String> regionNames = new ArrayList<>();
                for (Landmark landmark : lineStringReader.getLandmarks()) {
                    ArffRegion region = ArffReader.getRegionByPoints(landmark.getX(), landmark.getY());
                    //regions.add(region);
                    regions.put(region.getxPoint() + "#" + region.getyPoint(), region);
                    regionNames.add(region.getRegion());
                }
                regionList.add(regionNames);
                pointsAndClusters.put(file, regions);
            } catch (IOException e) {
                e.printStackTrace();
            }

        })).join();
        return regionList;

    }

    private static List<String> getFileNames(String rootFolder) {
        List<String> fileNames = new ArrayList<>();
        for (File file : new File(rootFolder).listFiles()) {
            if (!file.isDirectory()) {
                fileNames.add(file.getName());
            }
        }
        return fileNames;
    }

    private static ArffRegion getRegionByPoints(Double xPoint, Double yPoint) {
        /*for (ArffRegion arffRegion : ARFF_REGIONS) {
            if (arffRegion.getxPoint().equals(xPoint) && arffRegion.getyPoint().equals(yPoint)) return arffRegion;
        }
        return new ArffRegion(0.0, 0.0, "");*/
        ArffRegion region = ARFF_REGIONS.get(xPoint + "#" + yPoint);
        return region != null ? region : new ArffRegion(0.0, 0.0, "");
    }

    //##########################################################################################


    public static List<ArffRegion> getArffRegionListByFileName(String fileName) throws IOException {
        /*InputStream stream = ArffReader.class.getClassLoader().getResourceAsStream(TAXI_SIMULATION + fileName);
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

        List<ArffRegion> regions = new ArrayList<>();
        for (Landmark landmark : lineStringReader.getLandmarks()) {
            regions.add(ArffReader.getArffRegionByPoints(landmark.getX(), landmark.getY(), fileName));
        }
        return regions;*/
        List<String> allLines = Files.readAllLines(Paths.get(ArffReader.class.getClassLoader().getResource(TAXI_SECOND + fileName).getPath()));
        List<Landmark> landmarks = new ArrayList<>();
        for (String line : allLines) {
            Object[] data=line.split(",");
            Landmark landmark=new Landmark();
            landmark.setX(Double.parseDouble(data[0].toString()));
            landmark.setY(Double.parseDouble(data[1].toString()));
            landmark.setTimeInSecond(Integer.parseInt(data[2].toString()));
            landmarks.add(landmark);
        }

        List<ArffRegion> regions = new ArrayList<>();
        for (Landmark landmark : landmarks) {
            String cluster=ArffReader.getArffRegionByPoints(landmark.getX(), landmark.getY(), fileName).getRegion();
            ArffRegion region=new ArffRegion(landmark.getX(), landmark.getY(), cluster,landmark.getTimeInSecond());

            region.setTimeInSecond(landmark.getTimeInSecond());
            regions.add(region);
        }
        return regions;
    }

    private static ArffRegion getArffRegionByPoints(Double xPoint, Double yPoint, String fileName) {
        /*for (ArffRegion arffRegion : pointsAndClusters.get(fileName)) {
            if (arffRegion.getxPoint().equals(xPoint) && arffRegion.getyPoint().equals(yPoint)) return arffRegion;
        }
        return new ArffRegion(0.0, 0.0, "");*/
        ArffRegion region = pointsAndClusters.get(fileName.replace("-second","")).get(xPoint + "#" + yPoint);
        return region != null
                ? region
                : new ArffRegion(0.0, 0.0, "");
    }

}
