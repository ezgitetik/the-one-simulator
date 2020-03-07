package custom;

import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

public class CustomerProbabilityDistribution {

    private static final double CUSTOMER_TIME_MINUTES = 20;
    private static final double NO_CUSTOMER_TIME_MINUTES = 15;

    private static final double MEAN_LOG_COUNT_PER_100_TAXI_MONTH1_DAY1 = 7546;
    private static final double MEAN_LOG_COUNT_PER_100_TAXI_MONTH1_DAY1_MOD10 = MEAN_LOG_COUNT_PER_100_TAXI_MONTH1_DAY1 / 10;

    /**
     * We calculated it with using month1 and day1 data...
     */
    private static final double MEAN_GPS_LOG_COUNT_WITH_CUSTOMER = (MEAN_LOG_COUNT_PER_100_TAXI_MONTH1_DAY1_MOD10 / 24D) / 60D * CUSTOMER_TIME_MINUTES;
    private static final double MEAN_GPS_LOG_COUNT_WITH_NO_CUSTOMER = (MEAN_LOG_COUNT_PER_100_TAXI_MONTH1_DAY1_MOD10 / 24D) / 60D * NO_CUSTOMER_TIME_MINUTES;

    private static double calculateNegativeExponential(double meanLogCount) {
        double random = Math.random();
        double result = -meanLogCount * Math.log(random);
        return Math.round(result);
    }

    public static void main(String[] args) throws IOException {
        ArffReader.read();
        List<ArffRegion> taxiFutureRegions = ArffReader.getArffRegionListByFileName("taxi-528.wkt");
        IntStream.range(0, 100).forEach(index -> System.out.println("customer expo: " + calculateNegativeExponential(MEAN_GPS_LOG_COUNT_WITH_CUSTOMER)));
        IntStream.range(0, 100).forEach(index -> System.out.println("no customer expo: " + calculateNegativeExponential(MEAN_GPS_LOG_COUNT_WITH_NO_CUSTOMER)));
    }


}
