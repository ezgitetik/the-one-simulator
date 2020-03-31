package custom;

//import com.sun.deploy.util.StringUtils;

//import com.sun.deploy.util.StringUtils;

import com.google.common.collect.Lists;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CptHelper {


    public static List<List<String>> read() throws IOException {
        List<String> allLines = Files.readAllLines(Paths.get(CptHelper.class.getClassLoader().getResource("taxi100_month1_week1_clusters.txt").getPath()));
        List<List<String>> splittedLines=allLines.stream().map(line->Arrays.asList(line.split(" "))).collect(Collectors.toList());
        return splittedLines;
    }

    public static void main(String[] args) throws IOException {
        List<List<String>> splittedLines=read();
        splittedLines.forEach(line->{
            /*List<List<String>> partitionedLines=Lists.partition(line,5);
            partitionedLines.forEach(partitioned->System.out.print(partitioned.stream().collect(Collectors.joining(" -1 "))));
            System.out.println(" -2 ");*/
            System.out.print(line.stream().collect(Collectors.joining(" -1 ")));
            System.out.println(" -2 ");
        });
    }

}
