package com.example;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Scanner;
import java.time.LocalDate;
import com.example.api.ElpriserAPI;
import java.util.ArrayList;
import java.util.Comparator;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        LocalDate chosenDate = null;

        System.out.println("Electricity price program starting");

        ElpriserAPI elpriserAPI = new ElpriserAPI();

        String zone = null;

        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("--zone")) {
                zone = args[i + 1].trim().toUpperCase();
            }
        }

        if (zone == null) {
            System.out.print("Enter zone (SE1, SE2, SE3, SE4): ");
            zone = scanner.nextLine().trim().toUpperCase();
        }

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

        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("--date")) {
                try {
                    chosenDate = LocalDate.parse(args[i + 1]);
                } catch (Exception e) {
                    System.out.println("Invalid date format. Please use YYYY-MM-DD.");
                    scanner.close();
                    return;
                }
            }
        }

        ElpriserAPI.Prisklass priceClass = ElpriserAPI.Prisklass.valueOf(zone);
        LocalDate today = (chosenDate != null) ? chosenDate : LocalDate.now();
        System.out.println("Using date: " + today);
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

        System.out.print("Enter window length in hours (2, 4 or 8): ");
        int windowHours = scanner.nextInt();

        while (windowHours != 2 && windowHours != 4 && windowHours != 8) {
            System.out.println("Please enter 2, 4 or 8: ");
            windowHours = scanner.nextInt();
        }

        int slotMinutes = (int) Duration.between(
                allPrices.get(0).timeStart(),
                allPrices.get(0).timeEnd()
        ).toMinutes();

        int slotsPerHour = 60 / slotMinutes;
        int windowSize = windowHours * slotsPerHour;

        int startIndex = 0;
        if (today.equals(LocalDate.now())) {
            var nextHour = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(1);
            while (startIndex < allPrices.size()
                    && allPrices.get(startIndex).timeStart().isBefore(nextHour)) {
                startIndex++;
            }
        }

        if (allPrices.size() - startIndex < windowSize) {
            System.out.printf("Not enough data for a %dh window starting now.%n", windowHours);
        } else {
            int bestStart = startIndex;
            double bestSum = Double.MAX_VALUE;

            for (int i = startIndex; i <= allPrices.size() - windowSize; i++) {
                double sum = 0.0;
                for (int j = i; j < i + windowSize; j++) {
                    sum += allPrices.get(j).sekPerKWh();
                }
                if (sum < bestSum) {
                    bestSum = sum;
                    bestStart = i;
                }
            }

            var start = allPrices.get(bestStart);
            var end = allPrices.get(bestStart + windowSize - 1);
            double avg = bestSum / windowSize;
            System.out.printf("The cheapest %dh window is from %s to %s with an average price of %.2f SEK/kWh.%n", windowHours, start.timeStart().toLocalTime(), end.timeEnd().toLocalTime(), avg);
        }

        if (prices.isEmpty()) {
            System.out.println("No values for today's date");
        } else {
            double sum = 0.0;
            for (var price : prices) {
                sum += price.sekPerKWh();
            }
            int count = prices.size();
            int slotMinutesToday = (int) Duration.between(
                    prices.get(0).timeStart(),
                    prices.get(0).timeEnd()
            ).toMinutes();
            int totalHours = (count * slotMinutesToday) / 60;

            double average = sum / count;
            System.out.printf("Average pri4ce for %s over %dh is %.2f SEK/kWh.%n", today, totalHours, average);

            prices.sort(Comparator.comparing(ElpriserAPI.Elpris::timeStart));

            ElpriserAPI.Elpris minPrice = prices.get(0);
            ElpriserAPI.Elpris maxPrice = prices.get(0);

            for (int i = 1; i < prices.size(); i++) {
                ElpriserAPI.Elpris price = prices.get(i);
                if (price.sekPerKWh() < minPrice.sekPerKWh() ) {
                    minPrice = price;
                } else if (price.sekPerKWh() ==  minPrice.sekPerKWh() && price.timeStart().isBefore(minPrice.timeStart())) {
                    minPrice = price;
                }
                if (price.sekPerKWh() > maxPrice.sekPerKWh()) {
                    maxPrice = price;
                } else if (price.sekPerKWh() ==  maxPrice.sekPerKWh() && price.timeStart().isBefore(maxPrice.timeStart())) {
                    maxPrice = price;
                }
            }
            System.out.printf("Cheapest: %s %.2f SEK/kWh.%n", minPrice.timeStart().toLocalTime(), minPrice.sekPerKWh());
            System.out.printf("Most expensive: %s %.2f SEK/kWh.%n", maxPrice.timeStart().toLocalTime(), maxPrice.sekPerKWh());
        }
        scanner.close();
    }
}