package custom.cptplus;

//import com.sun.deploy.util.StringUtils;

//import com.sun.deploy.util.StringUtils;

import custom.ConvertClusterDataToSequenceData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CptHelper {


    public static List<List<String>> read() throws IOException {
        List<String> allLines = Files.readAllLines(Paths.get(CptHelper.class.getClassLoader().getResource("taxi100_month1_week1_cpt_5transactions_combination.txt").getPath()));
        List<List<String>> splittedLines = allLines.stream().map(line -> Arrays.asList(line.split(" "))).collect(Collectors.toList());
        return splittedLines;
    }

    public static void main(String[] args) throws IOException {
        List<List<String>> splittedLines = read();
        StringBuilder stringBuilder=new StringBuilder();
        splittedLines.forEach(line -> {
            /*List<List<String>> partitionedLines=Lists.partition(line,5);
            partitionedLines.forEach(partitioned->System.out.print(partitioned.stream().collect(Collectors.joining(" -1 "))));
            System.out.println(" -2 ");*/


            stringBuilder.append(line.stream().collect(Collectors.joining(" -1 "))).append(" -2 ");
            stringBuilder.append(System.lineSeparator());
          /*  System.out.print(line.stream().collect(Collectors.joining(" -1 ")));
            System.out.println(" -2 ");*/
        });
        writeToFile(stringBuilder.toString());
    }

    private static void writeToFile(String content) throws IOException {
        Files.write(Paths.get(ConvertClusterDataToSequenceData.class.getClassLoader().getResource("taxi100_month1_week1_cpt_5transactions_combination.txt").getPath()),
                content.getBytes(), StandardOpenOption.WRITE);
    }

}
