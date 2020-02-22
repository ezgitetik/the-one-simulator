package custom;

import java.util.Scanner;

public class PoissonDistribution {

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        double lambda = in.nextDouble();
        int expected = in.nextInt();
        System.out.format("%.2f", calculate(expected, lambda));
        //System.out.println(factorial(4));

    }

    private static double calculate(int expected, double lambda) {
        return (Math.pow(lambda, expected) * Math.pow(Math.E, -1 * lambda)) / factorial(expected);
    }

    private static Long factorial(int n) {
        if (n < 0) return null;
        long result = 1;
        while (n > 0) {
            result *= n--;
        }
        return result;
    }

}
