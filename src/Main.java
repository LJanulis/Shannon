import ShannonCoding.Shannon;

/**
 * @author Lukas
 */
public class Main {

    public static void main(String[] args) {

        Shannon sh = new Shannon(8, true);
        sh.encode("base.txt", "encoded.txt");
        sh.decode("decoded.txt");
    }
}
