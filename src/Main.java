import ShannonCoding.Shannon;

/**
 * @author Lukas
 */
public class Main {

    public static void main(String[] args) {

        Shannon sh = new Shannon(16);
        sh.encode("test.txt", "encoded.txt", "codeData.txt");
        sh.decode("decoded.txt");
    }
}
