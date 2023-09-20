package utils;

import java.util.Scanner;

public class ConsoleUtils {
    private static final Scanner SCANNER = new Scanner(System.in);

    public static final class Option {
        public String text;

        public Option(String text) {
            this.text = text;
        }
    }

    public static Option chooseOneOption(String message, Option... options) {
        boolean isInputValid = false;
        Option result = null;
        do {
            System.out.println(message);
            for (int i = 0; i < options.length; i++) {
                System.out.println("[" + i + "] - " + options[i].text);
            }
            String input = SCANNER.next();
            try {
                int index = Integer.parseInt(input);
                if (index >= 0 && index < options.length) {
                    result = options[index];
                    break;
                }
                System.out.println("Invalid choice: " + input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid choice: " + input);
            }
        } while (!isInputValid);

        return result;
    }
}
