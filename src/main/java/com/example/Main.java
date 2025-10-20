package com.example;

//Importera nödvändiga klasser
import com.example.api.ElpriserAPI;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.LocalDate;
import java.util.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class Main {

    private static final Locale SV = Locale.of("sv", "SE");
    private static final DecimalFormatSymbols SV_SYM = new DecimalFormatSymbols(SV);
    private static final DecimalFormat ORE_FMT = new DecimalFormat("0.00", SV_SYM);

    private static void printUsage() {
        System.out.println("Usage: java -jar app.jar [--zone SE1|SE2|SE3|SE4] [--date YYYY-MM-DD] [--charging 2h|4h|8h] [--sorted] [--help]");
        System.out.println("Zoner: SE1 SE2 SE3 SE4");
    }

    private static String ore(double sekPerKWh) {
        double ore = sekPerKWh * 100;
        return ORE_FMT.format(ore);
    }

    private static String spanHH(ZonedDateTime ts, ZonedDateTime te) {
        var s = ts.toLocalTime();
        var e = te.toLocalTime();
        return String.format("%02d-%02d", s.getHour(), e.getHour());
    }

    private static void printNoData() {
        System.out.println("Ingen data");
    }

    public static void main(String[] args) {
        System.out.println("Elprisprogrammet startar!");

        // Argument parsing
        if (args.length == 0) {
            printUsage();
            return;
        }

        Map<String, String>kv = new HashMap<>();
        Set<String>flags = new HashSet<>();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.startsWith("--")) {
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    kv.put(a, args[i + 1]);
                    i++;
                } else {
                    flags.add(a);
                }
            }
        }

        if (flags.contains("--help")) {
            printUsage();
            return;
        }

        String zone = kv.get("--zone");
        String dateStr = kv.get("--date");
        String charging = kv.get("--charging");
        boolean wantSorted = flags.contains("--sorted");

        if (zone == null) {
            System.out.println("Zon saknas. Ange till exempel: --zone SE3");
            return;
        }

        zone = zone.trim().toUpperCase();
        if (!zone.matches("SE[1-4]")) {
            System.out.println("Ogiltig zon: " + zone);
            return;
        }

        System.out.println("Vald zon: " + zone);

        switch (zone) {
            case "SE1":
                System.out.println("Zon SE1: Luleå/Norra Sverige.");
                break;
            case "SE2":
                System.out.println("Zon SE2: Sundsvall/Norra mellansverige.");
                break;
            case "SE3":
                System.out.println("Zon SE3: Stockholm/Södra mellansverige.");
                break;
            case "SE4":
                System.out.println("Zon SE4: Malmö/Södra Sverige.");
                break;
        }

        LocalDate date;
        if (dateStr == null) {
            date = LocalDate.now();
        } else {
            try {
                date = LocalDate.parse(dateStr);
            } catch (Exception e) {
                System.out.println("Ogiltigt datum. Använd formatet YYYY-MM-DD.");
                return;
            }
        }

        System.out.println("Datum: " + date);

        // Hämta data (idag + imorgon)
        ElpriserAPI elpriserAPI = new ElpriserAPI();
        ElpriserAPI.Prisklass priceClass = ElpriserAPI.Prisklass.valueOf(zone);

        List<ElpriserAPI.Elpris> today = elpriserAPI.getPriser(date, priceClass);
        List<ElpriserAPI.Elpris> tomorrow = elpriserAPI.getPriser(date.plusDays(1), priceClass);

        List<ElpriserAPI.Elpris> all = new ArrayList<>(today);
        all.addAll(tomorrow);
        all.sort(Comparator.comparing(ElpriserAPI.Elpris::timeStart));

        if (all.isEmpty()) {
            printNoData();
            return;
        }

        System.out.println("Antal priser hämtade: " + today.size());
        System.out.println("Priser hämtade imorgon: " + tomorrow.size());
        System.out.println("Alla priser hämtade: " + all.size());

        // --sorted: fallande prislista (HH-HH xx,xx öre)
        if (wantSorted) {
            var lines = all.stream()
                    .sorted(Comparator.comparingDouble(ElpriserAPI.Elpris::sekPerKWh).reversed())
                    .map(p -> String.format("%s %s öre", spanHH(p.timeStart(), p.timeEnd()), ore(p.sekPerKWh())))
                    .toList();
            lines.forEach(System.out::println);
            return;
        }

        // -charging: hitta billigaste sammanhängande fönster över dygnsgräns
        if (charging != null) {
            int hours;
            if (charging.equalsIgnoreCase("2h")) hours = 2;
            else if (charging.equalsIgnoreCase("4h")) hours = 4;
            else if (charging.equalsIgnoreCase("8h")) hours = 8;
            else {
                System.out.println("Ogiltigt värde för --charging. Använd 2h, 4h eller 8h.");
                return;
            }

            int slotMinutes = (int) Duration.between(all.getFirst().timeStart(), all.getFirst().timeEnd()).toMinutes();
            if (slotMinutes <= 0 || 60 % slotMinutes != 0) {
                System.out.println("Ogiltig slot-storlek.");
                return;
            }

            int slotsPerHour = 60 / slotMinutes;
            int windowSize = hours * slotsPerHour;

            if (all.size() < windowSize) {
                printNoData();
                return;
            }

            int bestStart = 0;
            double bestSum = Double.MAX_VALUE;

            for (int i = 0; i <= all.size() - windowSize; i++) {
                double sum = 0;
                for (int j = i; j < i + windowSize; j++) sum += all.get(j).sekPerKWh();
                if (sum < bestSum) {
                    bestSum = sum;
                    bestStart = i;
                }
            }

            var start = all.get(bestStart).timeStart();
            var end = all.get(bestStart + windowSize - 1).timeEnd();
            double avgSek = bestSum / windowSize;

            System.out.printf("Påbörja laddning kl %s (fönster %s–%s).%n",
                    start.toLocalTime(), start.toLocalTime(), end.toLocalTime());
            System.out.println("Medelpris för fönster: " + ore(avgSek) + " öre");
            return;
        }

        // Standardrapport för idag: Medelpris + lägsta/högsta timme
        if (today.isEmpty()) {
            printNoData();
            return;
        }

        // Medelpris över alla slots under dagen
        double sumSek = today.stream().mapToDouble(ElpriserAPI.Elpris::sekPerKWh).sum();
        double meanSek = sumSek / today.size();
        System.out.println("Medelpris: " + ore(meanSek) + " öre");

        // Aggregera till hel timme även om slots < 60 min (t.ex. 15-minutersposter)
        Map<Integer, List<ElpriserAPI.Elpris>> byHour = new TreeMap<>();
        for (var p : today) {
            int h = p.timeStart().toLocalTime().getHour();
            byHour.computeIfAbsent(h, _ -> new ArrayList<>()).add(p);
        }

        double bestAvg = Double.POSITIVE_INFINITY;
        double worstAvg = Double.NEGATIVE_INFINITY;
        int bestHour = 0, worstHour = 0;

        for (var e : byHour.entrySet()) {
            double avg = e.getValue().stream().mapToDouble(ElpriserAPI.Elpris::sekPerKWh).average().orElse(Double.NaN);
            int hour = e.getKey();
            if (avg < bestAvg || (avg == bestAvg && hour < bestHour)) {
                bestAvg = avg; bestHour = hour;
            }
            if (avg > worstAvg || (avg == worstAvg && hour < worstHour)) {
                worstAvg = avg; worstHour = hour;
            }
        }

        String bestSpan = String.format("%02d-%02d", bestHour, (bestHour + 1) % 24);
        String worstSpan = String.format("%02d-%02d", worstHour, (worstHour + 1) % 24);

        System.out.println("Lägsta pris: " + bestSpan + " " + ore(bestAvg) + " öre");
        System.out.println("Högsta pris: " + worstSpan + " " + ore(worstAvg) + " öre");
    }
}