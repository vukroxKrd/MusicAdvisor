import java.util.Arrays;
import java.util.Scanner;

class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        String line = scanner.nextLine();

        StringReverser reverser = new StringReverser() {
            @Override
            public String reverse(String str) {
                char[] charArray = str.toCharArray();
                char[] result = new char[charArray.length];
                for (int i = charArray.length-1, j = 0; i >= 0 ; i--) {
                    result[j++] = charArray[i];
                }
                return String.valueOf(result);
            }
        };

        System.out.println(reverser.reverse(line));
    }

    interface StringReverser {

        String reverse(String str);
    }

}