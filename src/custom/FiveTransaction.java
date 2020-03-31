package custom;

//import com.sun.deploy.util.StringUtils;

//import com.sun.deploy.util.StringUtils;

import com.google.common.collect.Lists;
import org.mockito.internal.util.collections.ListUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FiveTransaction {


    public static List<List<String>> read() throws IOException {
        List<String> allLines = Files.readAllLines(Paths.get(FiveTransaction.class.getClassLoader().getResource("taxi100_month1_week1_clusters.txt").getPath()));
        List<List<String>> splittedLines=allLines.stream().map(line->Arrays.asList(line.split(" "))).collect(Collectors.toList());
        return splittedLines;
    }

    public static void main(String[] args) throws IOException {
        List<List<String>> splittedLines=read();
        splittedLines.forEach(line->{
            List<List<String>> partitionedLines=Lists.partition(line,5);
            partitionedLines.forEach(partitioned->System.out.println(partitioned.stream().collect(Collectors.joining(" "))));
        });
    }

}
