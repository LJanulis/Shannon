import ShannonCoding.Shannon;

import java.io.FileNotFoundException;


/**
 * @author Lukas
 */

public class Main {

    public static void main(String[] args) throws FileNotFoundException {
        if (args.length == 4) {
            if (args[0].equals("encode")) {
                Shannon sh = new Shannon(Integer.parseInt(args[3]));
                sh.encode(args[1], args[2]);
            } else
                throw new IllegalArgumentException("Incorrect argument");
        } else if (args.length == 3) {
            if (args[0].equals("decode")) {
                Shannon sh = new Shannon();
                sh.decode(args[1], args[2]);
            } else
                throw new IllegalArgumentException("Incorrect argument");
        } else
            throw new IllegalArgumentException("Incorrect arguments");
    }
}
