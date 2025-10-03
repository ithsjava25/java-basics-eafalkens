package com.example;

import java.util.Scanner;
import com.example.api.ElpriserAPI;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Programmet för elpriser startar!");

        ElpriserAPI elpriserAPI = new ElpriserAPI();

        System.out.print("Skriv in zon (SE1, SE2, SE3 eller SE4): ");
        String zone = scanner.nextLine();

        while (!zone.equals("SE1")&&!zone.equals("SE2")&&!zone.equals("SE3")&&!zone.equals("SE4")) {
            System.out.print("Skriv in zon (SE1, SE2, SE3 eller SE4): ");
            zone = scanner.nextLine();
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
        scanner.close();
    }
}
// if zone inte är se123 visa felmeddelande