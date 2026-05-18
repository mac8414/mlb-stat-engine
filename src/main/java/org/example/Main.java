package org.example;

import org.example.service.PlayerService;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        PlayerService service = new PlayerService();
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== MLB Stat Engine ===");
        System.out.println("Type a player name to look up stats, or 'quit' to exit.\n");

        while (true) {
            System.out.print("Search player name: ");
            String nameInput = scanner.nextLine().trim();

            System.out.print("Search year: ");
            String yearInput = scanner.nextLine().trim();

            if (nameInput.equalsIgnoreCase("quit") || nameInput.equalsIgnoreCase("exit")) {
                System.out.println("Goodbye!");
                break;
            }

            if (!yearInput.isEmpty()) {
                service.lookupPlayer(nameInput, yearInput);
            }
            System.out.println();
        }

        scanner.close();
    }
}
