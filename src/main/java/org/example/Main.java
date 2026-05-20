package org.example;

import org.example.service.PlayerService;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        PlayerService service = new PlayerService();
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== MLB Stat Engine ===");
        System.out.println("Commands: player name | 'leaders' | 'clear' | 'quit'\n");

        while (true) {
            System.out.print("Search player name: ");
            String nameInput = scanner.nextLine().trim();

            if (nameInput.equalsIgnoreCase("quit") || nameInput.equalsIgnoreCase("exit")) {
                System.out.println("Goodbye!");
                break;
            }

            if (nameInput.equalsIgnoreCase("clear")) {
                System.out.print("\033[H\033[2J");
                System.out.flush();
                System.out.println("=== MLB Stat Engine ===");
                System.out.println("Commands: player name | 'leaders' | 'clear' | 'quit'\n");
                continue;
            }

            if (nameInput.equalsIgnoreCase("leaders")) {
                System.out.print("Start year: ");
                String startYear = scanner.nextLine().trim();
                System.out.print("End year (or same as start): ");
                String endYear = scanner.nextLine().trim();
                System.out.print("Stat (AVG / ERA): ");
                String stat = scanner.nextLine().trim();
                service.lookupLeaders(startYear, endYear, stat);
                System.out.println();
                continue;
            }

            System.out.print("Search year: ");
            String yearInput = scanner.nextLine().trim();

            if (!yearInput.isEmpty()) {
                service.lookupPlayer(nameInput, yearInput);
            }
            System.out.println();
        }

        scanner.close();
    }
}
