package ShannonCoding;

import java.math.BigDecimal;

/**
 * @author Lukas
 */

class SourceProbability implements Comparable{

    private Integer rawByte;
    private BigDecimal probability;

    public SourceProbability(Integer rawByte, String probability) {
        this.rawByte = rawByte;
        this.probability = new BigDecimal(probability);
    }

    public Integer getByte() {
        return rawByte;
    }

    public Character getSymbol() { return (char)rawByte.byteValue(); }

    public void setByte(Integer rawByte) {
        this.rawByte = rawByte;
    }

    public BigDecimal getProbability() {
        return probability;
    }

    public void setProbability(String probability) {
        this.probability = new BigDecimal(probability);
    }

    @Override
    public String toString() {
        return rawByte + "=" + probability;
    }

    @Override
    public int compareTo(Object o) {
        if(o instanceof SourceProbability){
            return (-this.getProbability().compareTo(((SourceProbability) o).getProbability()));
        }
        throw new IllegalArgumentException("Could not compare probabilities of two symbols");
    }
}