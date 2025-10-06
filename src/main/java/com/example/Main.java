package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.time.LocalDate;
import java.util.ArrayList;
import com.example.api.ElpriserAPI;

import javax.naming.CompositeName;

public class Main {
    private static CompositeName idagPriser;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Programmet för elpriser startar!");

        ElpriserAPI elpriserAPI = new ElpriserAPI();

        System.out.print("Skriv in zon (SE1, SE2, SE3 eller SE4): ");
        String zone = scanner.nextLine().trim().toUpperCase();

        while (!zone.equals("SE1") && !zone.equals("SE2") && !zone.equals("SE3") && !zone.equals("SE4")) {
            System.out.print("Skriv in zon (SE1, SE2, SE3 eller SE4): ");
            zone = scanner.nextLine().trim().toUpperCase();
        }

        System.out.println("Vald zon är: " + zone);

        switch (zone) {
            case "SE1":
                System.out.println("Zonen är SE1. Det är Luleå/Norra Sverige.");
                break;
            case "SE2":
                System.out.println("Zonen är SE2. Det är Sundsvall/Norra Mellansverige.");
                break;
            case "SE3":
                System.out.println("Zonen är SE3. Det är Stockholm/Södra Mellansverige.");
                break;
            case "SE4":
                System.out.println("Zonen är SE4. Det är Malmö/Södra Sverige.");
                break;
            default:
                System.out.println("Okänd zon.");
        }

        ElpriserAPI.Prisklass prisklass = ElpriserAPI.Prisklass.valueOf(zone);
        LocalDate idag = LocalDate.now();
        List<ElpriserAPI.Elpris> priser = elpriserAPI.getPriser(idag, prisklass);
        System.out.println("Antal priser hämtade: " + priser.size());

        int gräns = Math.min(3, priser.size());
        if (priser.isEmpty()) {
            System.out.println("Det finns inga priser för " + idag + ".");
        } else {
            for (int i = 0; i < gräns; i++) {
            ElpriserAPI.Elpris pris = priser.get(i);
            System.out.printf("Tid: %s Pris: %.2f SEK/kWh.%n", pris.timeStart().toLocalTime(), pris.sekPerKWh());
            }
        }

        double summa = 0.0;
        int antal = 0;

        for (ElpriserAPI.Elpris pris : priser) {
            boolean ärIdag = pris.timeStart().toLocalDate().equals(idag);
            if (ärIdag) {
                summa += pris.sekPerKWh();
                antal++;
            }
        }

        if (antal == 0) {
            System.out.println("Det finns inga värden.");
        } else {
            double medelvärdeAvPriser = summa / antal;
            System.out.printf("Medelpriset för %s under %dh är %.2f sek/kWh.%n", idag, antal, medelvärdeAvPriser);
        }

        if (idagPriser.isEmpty()) {
            System.out.println("Det finns inga priser för dagens datum.");
        } else {
            ElpriserAPI.Elpris max = idagPriser.get(0);

            for (int i = 1; i < idagPriser.size(); i++) {
                ElpriserAPI.Elpris pris = idagPriser.get(i);
                if (pris.sekPerKWh() < min.sekPerKWh()) {
                    min = pris;
                }
                if (pris.sekPerKWh() > max.sekPerKWh()) {
                    max = pris;
                }
            }
            System.out.printf("Billigast: %s %.2f sek/kWh.%n", min.timeStart().toLocalTime(), min.sekPerKWh());
            System.out.printf("Dyrast: %s %.2f sek/kWh.%n", max.timeStart().toLocalTime(), max.sekPerKWh());
        }
    }
}
    // lägg in scanner.close();