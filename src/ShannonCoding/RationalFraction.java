package ShannonCoding;

/**
 * @author Lukas
 * Class which represents a fraction number
 */
public class RationalFraction implements Comparable{

    private int rawByte;
    private int numerator;
    private int denominator;

    public RationalFraction(int numerator, int denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public RationalFraction(int rawByte, int numerator, int denominator) {
        this.rawByte = rawByte;
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public int getRawByte() {
        return rawByte;
    }

    public void setRawByte(int rawByte) {
        this.rawByte = rawByte;
    }

    public int getNumerator() {
        return numerator;
    }

    public void setNumerator(int numerator) {
        this.numerator = numerator;
    }

    public int getDenominator() {
        return denominator;
    }

    public void setDenominator(int denominator) {
        this.denominator = denominator;
    }

    public String getRational(){
        return numerator + "/" + denominator;
    }

    @Override
    public int compareTo(Object o) {
        if(o instanceof RationalFraction){
            if(this.getNumerator() > ((RationalFraction) o).getNumerator())
                return -1;
            if(this.getNumerator() < ((RationalFraction) o).getNumerator())
                return 1;
            return 0;
        }
        throw new IllegalArgumentException("RationalFraction compareTo couldn't compare");
    }
}
