package custom.apriori;

import ca.pfv.spmf.algorithms.frequentpatterns.apriori.AlgoApriori;
import ca.pfv.spmf.patterns.itemset_array_integers_with_count.Itemsets;
import custom.ArffReader;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

public class Apriori {

    public static void main(String[] arg) throws IOException {
        ArffReader.read();
        ArffReader.REGIONS.forEach(region -> System.out.println(String.join(",", region)));
        System.out.println("############################");
        ArffReader.DISTINCTED_REGIONS.forEach(region -> System.out.println(String.join(" ", region)));

        //taxi50_month1_day1_clusters.txt
        String input = fileToPath("taxi100_month1_week1_clusters.txt.txt");
        String output = null;
        double minsup = 0.2D;
        AlgoApriori algorithm = new AlgoApriori();
        Itemsets result = algorithm.runAlgorithm(minsup, input, (String) output);
        algorithm.printStats();
        result.printItemsets(algorithm.getDatabaseSize());
    }

    public static String fileToPath(String filename) throws UnsupportedEncodingException {
        System.out.println("filename : " + filename);
        URL url = Apriori.class.getClassLoader().getResource(filename);
        return URLDecoder.decode(url.getPath(), "UTF-8");
    }
}
