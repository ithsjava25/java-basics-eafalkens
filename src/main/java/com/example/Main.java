package com.example;

import java.util.List;
import java.util.Scanner;
import java.time.LocalDate;
import com.example.api.ElpriserAPI;
import java.util.ArrayList;
import java.util.Comparator;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Electricity price program starting");

        ElpriserAPI elpriserAPI = new ElpriserAPI();

        System.out.print("Enter zone (SE1, SE2, SE3, SE4): ");
        String zone = scanner.nextLine().trim().toUpperCase();

        while (!zone.equals("SE1") && !zone.equals("SE2") && !zone.equals("SE3") && !zone.equals("SE4")) {
            System.out.print("Enter zone (SE1, SE2, SE3, SE4): ");
            zone = scanner.nextLine().trim().toUpperCase();
        }

        System.out.println("Selected zone: " + zone);

        switch (zone) {
            case "SE1":
                System.out.println("Zone SE1: Luleå/North Sweden.");
                break;
            case "SE2":
                System.out.println("Zone SE2. Sundsvall/North-central Sweden.");
                break;
            case "SE3":
                System.out.println("Zone SE3. Stockholm/South-central Sweden.");
                break;
            case "SE4":
                System.out.println("Zone SE4. Malmö/South Sweden.");
                break;
            default:
                System.out.println("Unknown zone.");
        }

        ElpriserAPI.Prisklass priceClass = ElpriserAPI.Prisklass.valueOf(zone);
        LocalDate today = LocalDate.now();
        List<ElpriserAPI.Elpris> prices = elpriserAPI.getPriser(today, priceClass);
        System.out.println("Number of prices fetched: " + prices.size());

        LocalDate tomorrow = today.plusDays(1);
        List<ElpriserAPI.Elpris> pricesTomorrow = elpriserAPI.getPriser(tomorrow, priceClass);
        System.out.println("Tomorrow prices fetched: " + pricesTomorrow.size());

        List<ElpriserAPI.Elpris> allPrices = new ArrayList<>(prices);
        allPrices.addAll(pricesTomorrow);

        allPrices.sort(Comparator.comparing(ElpriserAPI.Elpris::timeStart));
        System.out.println("All prices fetched: " + allPrices.size());

        if (allPrices.isEmpty()) {
            System.out.println("No prices available for today or tomorrow.");
            scanner.close();
            return;
        }

        int limit = Math.min(3, allPrices.size());
        for (int i = 0; i < limit; i++) {
            ElpriserAPI.Elpris price = allPrices.get(i);
            System.out.printf("Time: %s Price: %.2f SEK/kWh.%n", price.timeStart().toLocalTime(), price.sekPerKWh());
        }

        if (prices.isEmpty()) {
            System.out.println("No values for today's date");
        } else {
            double sum = 0.0;
            for (var price : prices) {
                sum += price.sekPerKWh();
            }
            int count = prices.size();
            double average = sum / count;
            System.out.printf("Average price for %s over %dh is %.2f SEK/kWh.%n", today, count, average);

            prices.sort(Comparator.comparing(ElpriserAPI.Elpris::timeStart));

            ElpriserAPI.Elpris minPrice = prices.get(0);
            ElpriserAPI.Elpris maxPrice = prices.get(0);

            for (int i = 1; i < prices.size(); i++) {
                ElpriserAPI.Elpris price = prices.get(i);
                if (price.sekPerKWh() < minPrice.sekPerKWh()) {
                    minPrice = price;
                }
                if (price.sekPerKWh() > maxPrice.sekPerKWh()) {
                    maxPrice = price;
                }
            }
            System.out.printf("Cheapest: %s %.2f SEK/kWh.%n", minPrice.timeStart().toLocalTime(), minPrice.sekPerKWh());
            System.out.printf("Most expensive: %s %.2f SEK/kWh.%n", maxPrice.timeStart().toLocalTime(), maxPrice.sekPerKWh());
        }
        scanner.close();
    }
}