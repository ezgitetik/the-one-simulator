package custom;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public class ConvertClusterDataToSequenceData {


    public static List<List<String>> read() throws IOException {
        List<String> allLines = Files.readAllLines(Paths.get(ConvertClusterDataToSequenceData.class.getClassLoader().getResource("taxi100_month1_week1_clusters.txt").getPath()));
        List<List<String>> splittedLines = allLines.stream().map(line -> Arrays.asList(line.split(" "))).collect(Collectors.toList());
        return splittedLines;
        //ArffReader.read();

    }

    public static void main(String[] args) throws IOException {

        List<List<String>> splittedLines = read();

        List<List<String>> partitionedLines = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        splittedLines.forEach(line -> {
            for (int i = 0; i < line.size() - 4; i++) {
                partitionedLines.add(line.subList(i, i + 5));
            }
            partitionedLines.forEach(partitioned -> {
                stringBuilder.append(partitioned.stream().collect(Collectors.joining(" -1 "))).append(" -2");
                stringBuilder.append(System.lineSeparator());
            });
    });

    writeToFile(stringBuilder.toString());

}


    private static void writeToFile(String content) throws IOException {
        Files.write(Paths.get(ConvertClusterDataToSequenceData.class.getClassLoader().getResource("training-data.txt").getPath()),
                content.getBytes(), StandardOpenOption.WRITE);
    }
}
