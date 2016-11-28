package ShannonCoding;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.io.*;
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

    private String dataFile;
    private String encodedFile;
    private String codeWordFile;

    private int blockLength = 2;
    private int writtenBitCount = 0;

    private int fileBlockCount = 0;

    private int lastBlockLength;
    private boolean lastBlockUnequal = false;

    private LinkedHashMap<Integer, Integer> frequencies;
    private BiMap<Integer, String> encodedAlphabet;

    private ArrayList<RationalFraction> probabilities = new ArrayList<>();

    public Shannon(int blockLength) {
        this.blockLength = blockLength;
    }

    public void encode(String dataFile, String encodedFile, String codeWordFile){
        this.dataFile = dataFile;
        this.encodedFile = encodedFile;
        this.codeWordFile = codeWordFile;
            try
            {
                long startTime = System.currentTimeMillis();
                System.out.println("CALCULATING FREQUENCIES...");
                getFrequencies();
                System.out.println("FINISHED CALCULATING FREQUENCIES, TIME ELAPSED: " + (System.currentTimeMillis() - startTime) + " milis");

                startTime = System.currentTimeMillis();
                System.out.println("CALCULATING PROBABILITIES...");
                for(Map.Entry<Integer, Integer> freq : this.frequencies.entrySet()){
                    probabilities.add(new RationalFraction(freq.getKey(), freq.getValue(), fileBlockCount));
                }
                System.out.println("FINISHED CALCULATING PROBABILITIES, TIME ELAPSED: " + (System.currentTimeMillis() - startTime) + " milis");

                startTime = System.currentTimeMillis();
                System.out.println("SORTING PROBABILITIES...");
                Collections.sort(probabilities);
                System.out.println("FINISHED SORTING PROBABILITIES, TIME ELAPSED: " + (System.currentTimeMillis() - startTime) + " milis");

                startTime = System.currentTimeMillis();
                System.out.println("CALCULATING SYMBOL ENNCODING...");
                getSymbolCoding();
                System.out.println("FINISHED CALCULATING SYMBOL ENCODING, TIME ELAPSED: " + (System.currentTimeMillis() - startTime) + " milis");

                startTime = System.currentTimeMillis();
                System.out.println("ENCODING FILE...");
                writeToFile();
                System.out.println("FINISHED ENCODING FILE, TIME ELAPSED: " + (System.currentTimeMillis() - startTime) + " milis");
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
    }

    public void decode(String decodedFile){

        long startTime = System.currentTimeMillis();
        System.out.println("DECODING FILE...");
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
                        bitCount-= temp.length();
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
        System.out.println("FINISHED DECODING FILE, TIME ELAPSED: " + (System.currentTimeMillis() - startTime) + " milis");
    }

    private void writeToFile(){

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
    }

    private void getFrequencies() throws FileNotFoundException {
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

    private void getSymbolCoding() {

        this.encodedAlphabet = HashBiMap.create();

        try(BufferedWriter bw = new BufferedWriter(new FileWriter(codeWordFile))) {
            RationalFraction currSum = new RationalFraction(0, fileBlockCount);
            for (int i = 0; i < this.probabilities.size(); ++i) {

                double l1 = Math.log(1) / Math.log(2);
                double l2 = Math.log(probabilities.get(i).getNumerator()) / Math.log(2);
                double l3 = Math.log(probabilities.get(i).getDenominator()) / Math.log(2);

                //base2 logarithm of probability
                double logRes = l1 - (l2 - l3);

                //length of codeword
                int digits = (int) Math.ceil(logRes);

                //Probability sym
                if (i > 0) {
                    currSum.setNumerator(currSum.getNumerator() + probabilities.get(i - 1).getNumerator());
                }

                //Code of symbol
                String binSum = rationalFracToBinary(currSum, digits);

                bw.write(String.format("%-10d %s %5d %15s %15s", probabilities.get(i).getRawByte(),
                        probabilities.get(i).getRational(), digits, currSum.getRational(), binSum) + System.lineSeparator());
                encodedAlphabet.put(probabilities.get(i).getRawByte(), binSum);

            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String rationalFracToBinary(RationalFraction rf, int precision){
        String binaryForm = "";
        int tempNum = rf.getNumerator();
        int tempDen = rf.getDenominator();
        for(int i = 0; i < precision; ++i){
            tempNum*=2;
            if(tempNum >= tempDen){
                binaryForm += "1";
                tempNum-=tempDen;
            }
            else{
                binaryForm += "0";
            }
        }
        return binaryForm;
    }
}
