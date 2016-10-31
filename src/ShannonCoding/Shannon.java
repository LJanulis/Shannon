package ShannonCoding;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Lukas
 */

/**
 * Class implementing encoding and decoding of Shannon coding
 * https://en.wikipedia.org/wiki/Shannon_coding
 */
public class Shannon {

    private boolean displayProbabilities;

    private String dataFile;
    private String encodedFile;

    private int blockLength = 2;
    private int writtenBitCount = 0;

    private int fileBlockCount = 0;


    private int lastBlockLength;
    private boolean lastBlockUnequal = false;


    private LinkedHashMap<Integer, Integer> frequencies;
    private ArrayList<SourceProbability> probabilities;
    private BiMap<Integer, String> encodedAlphabet;

    public Shannon(int blockLength, boolean displayProbabilities) {
        this.blockLength = blockLength;
        this.displayProbabilities = displayProbabilities;
    }

    public void encode(String dataFile, String encodedFile){
        this.dataFile = dataFile;
        this.encodedFile = encodedFile;
            try
            {
                getFrequencies();
                getProbabilities();
                roundProbabilities();
                Collections.sort(this.probabilities, (o1, o2) -> -(o1.getProbability().compareTo(o2.getProbability())));
                getSymbolCoding();
                writeToFile();
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
    }

    public void decode(String decodedFile){

        System.out.println("DECODING");

        try {
            BitReader br = new BitReader(this.encodedFile);
            BitWriter bw = new BitWriter(decodedFile);
            int bitCount = writtenBitCount;
            String temp = "";

            BiMap<String, Integer> inv = this.encodedAlphabet.inverse();

            while(bitCount > 0){
                temp +=Integer.toString(br.readBit());

                if(inv.get(temp) != null){

                    if(bitCount - temp.length() == 0 && lastBlockUnequal){
                        bw.writeBits(inv.get(temp), lastBlockLength);
                        bitCount-= lastBlockLength;
                    }
                    else{
                        bw.writeBits(inv.get(temp), blockLength);
                        bitCount-= temp.length();
                        temp = "";
                    }
                }
            }
            bw.flush();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("DECODING FINISHED");
    }

    private void writeToFile(){

        System.out.println("ENCODING");

        try {
            BitReader br = new BitReader(this.dataFile);
            BitWriter bw = new BitWriter(this.encodedFile);
            int bitCount = br.length()*8;
            while(bitCount > 0){
                if(bitCount < blockLength){
                    int b = br.readBits(bitCount);
                    String binaryRepr = encodedAlphabet.get(b);
                    bw.writeBits(Integer.parseInt(binaryRepr, 2), binaryRepr.length());
                    bitCount-=blockLength;
                    writtenBitCount+=binaryRepr.length();
                    break;
                }
                int b = br.readBits(this.blockLength);

                String binaryRepr = encodedAlphabet.get(b);
                if(binaryRepr != null){
                    bw.writeBits(Integer.parseInt(binaryRepr, 2), binaryRepr.length());
                }
                bitCount -=this.blockLength;
                writtenBitCount+=binaryRepr.length();
            }
            bw.flush();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("ENCODING FINISHED");
    }

    private void getFrequencies() throws FileNotFoundException {
        System.out.println("GETTING FREQUENCIES");
        this.frequencies = new LinkedHashMap<>();

        BitReader br = new BitReader(dataFile);
        int fileBitCount = br.length()*8;
        this.fileBlockCount = 0;
        while(fileBitCount > 0){

            //If there are less bits in file than block length
            if(fileBitCount < blockLength) {
                Integer temp = br.readBits(fileBitCount);

                if(frequencies.containsKey(temp)){
                    frequencies.put(temp, frequencies.get(temp) + 1);
                }
                else
                    frequencies.put(temp, 1);

                this.fileBlockCount+=1;

                //For decoding if there happens to be less bits to read in file than specified block length
                this.lastBlockUnequal = true;
                this.lastBlockLength = fileBitCount;

                break;
            }
            Integer temp = br.readBits(blockLength);
            if(frequencies.containsKey(temp)){
                frequencies.put(temp, frequencies.get(temp) + 1);
            }
            else
                frequencies.put(temp, 1);
            fileBitCount-=blockLength;
            this.fileBlockCount+=1;
        }
    }

    private void getProbabilities(){
        System.out.println("CALCULATING PROBABILITIES");
        this.probabilities = new ArrayList<>();

        for(Map.Entry<Integer, Integer> entry : frequencies.entrySet()){
            BigDecimal freq = new BigDecimal(entry.getValue());
            BigDecimal total = new BigDecimal(fileBlockCount);
            BigDecimal probability = freq.divide(total, 10, BigDecimal.ROUND_UP);
            probabilities.add(new SourceProbability(entry.getKey(), probability.toString()));
        }
    }

    //Rounds probabilities so their total sum is 1
    private void roundProbabilities(){

        System.out.println("ROUNDING PROBABILITIES");

        while(this.totalProbabilitySum().compareTo(BigDecimal.ONE) != 0){
            for(SourceProbability sa : this.probabilities){
                if(totalProbabilitySum().compareTo(BigDecimal.ONE) > 0){
                    sa.setProbability(sa.getProbability().subtract(new BigDecimal("0.0000000001")).toString());
                }
                if(totalProbabilitySum().compareTo(BigDecimal.ONE) < 0){
                    sa.setProbability(sa.getProbability().add(new BigDecimal("0.0000000001")).toString());
                }
            }
        }
    }

    private void getSymbolCoding(){

        System.out.println("GETTING SYMBOL CODING");

        this.encodedAlphabet = HashBiMap.create();
        for (int i = 0; i < probabilities.size(); ++i) {

            //base 2 logarithm of probability
            double log = Math.log(1 / probabilities.get(i).getProbability().doubleValue()) / Math.log(2);

            //Length of codeword
            int digits = (int)Math.ceil(log);

            //Probability sum
            BigDecimal probSum = probabilitySum(i, probabilities);

            //Binary representation of probability sum
            String binaryProbSum = sumToBinary(probSum, digits);

            //Code of symbol
            String code = binaryProbSum.substring(2, binaryProbSum.length());

            if(displayProbabilities)
                System.out.println(String.format("%-10d %s %5d %15s %15s" , probabilities.get(i).getByte(),
                         probabilities.get(i).getProbability().toString(),
                         digits, probSum.toString(), code));
            //System.out.println(probabilities.get(i).getByte() + " " + probabilities.get(i).getProbability() + " ");
            //System.out.printf("%10d %10f %10s %30s\n", digits, probSum, binaryProbSum, code);
            encodedAlphabet.put(probabilities.get(i).getByte(), code);
        }
    }

    private static BigDecimal probabilitySum(int i, ArrayList<SourceProbability> s_p){
        BigDecimal sum = BigDecimal.ZERO;
        for(int k = 0; k < i; ++k){
            sum = sum.add(s_p.get(k).getProbability());
        }
        return sum;
    }

    private static String sumToBinary(BigDecimal probSum, int expandTo){

        String binaryForm = "0.";
        Double temp = probSum.doubleValue();
        for(int i = 0; i < expandTo; ++i){
            temp*= 2d;
            if(temp >= 1){
                binaryForm += "1";
                temp-=1d;
            }
            else{
                binaryForm += "0";
            }
        }
        return binaryForm;
    }

    public void displayFrequencies(){
        frequencies.forEach((f, k) -> System.out.println((char)f.intValue() + " " + k));
    }

    public void displayProbabilities(){
        for(SourceProbability sc : this.probabilities){
            System.out.println((char)sc.getByte().intValue() + " " + sc.getProbability());
        }
    }

    private BigDecimal totalProbabilitySum(){
        BigDecimal probSum = BigDecimal.ZERO;
        for(SourceProbability sc : this.probabilities){
            probSum = probSum.add(sc.getProbability());
        }
        return probSum;
    }

}
