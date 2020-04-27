package custom;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

public class ArffReader {

    private static final String ARFF_WITHOUT_MOD = "custom/taxidata/60taxi-month1/60taxi-month1-weka.arff";
    private static final String ARFF_PATH = ARFF_WITHOUT_MOD;

    private static final String TAXI_WITHOUT_MOD = "custom/taxidata/60taxi-month1/60taxi-month1-training-day1/";
    private static final String TAXI_SIMULATION = "custom/taxidata/60taxi-month1/60taxi-month1-simulation/";

    private static final String TAXI_PATH = TAXI_WITHOUT_MOD;

    //private static List<ArffRegion> ARFF_REGIONS = null;

    private static Map<String, ArffRegion> ARFF_REGIONS = null;

    public static List<List<String>> REGIONS = new ArrayList<>();

    //private static Map<String, List<ArffRegion>> pointsAndClusters = new HashMap<>();
    private static Map<String, Map<String, ArffRegion>> pointsAndClusters = new HashMap<>();

    public static Map<String, ArffRegion> read() throws IOException {
        if (ARFF_REGIONS == null) {
            List<String> allLines = Files.readAllLines(Paths.get(ArffReader.class.getClassLoader().getResource(ARFF_PATH).getPath()));
            ForkJoinPool forkJoinPool = new ForkJoinPool(60);
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

        ForkJoinPool forkJoinPool = new ForkJoinPool(files.size());

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

        List<ArffRegion> regions = new ArrayList<>();
        for (Landmark landmark : lineStringReader.getLandmarks()) {
            regions.add(ArffReader.getArffRegionByPoints(landmark.getX(), landmark.getY(), fileName));
        }
        return regions;
    }

    private static ArffRegion getArffRegionByPoints(Double xPoint, Double yPoint, String fileName) {
        /*for (ArffRegion arffRegion : pointsAndClusters.get(fileName)) {
            if (arffRegion.getxPoint().equals(xPoint) && arffRegion.getyPoint().equals(yPoint)) return arffRegion;
        }
        return new ArffRegion(0.0, 0.0, "");*/
        ArffRegion region = pointsAndClusters.get(fileName).get(xPoint + "#" + yPoint);
        return region != null
                ? region
                : new ArffRegion(0.0, 0.0, "");
    }

}
