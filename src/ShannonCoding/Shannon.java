package ShannonCoding;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

/**
 * @author Lukas
 */

/**
 * Class implementing encoding and decoding of Shannon coding
 * https://en.wikipedia.org/wiki/Shannon_coding
 */
public class Shannon {

    //Number of bits which will be read from dataFile at once and used as one word
    private int blockLength;

    //Number of blocks in dataFile
    private int fileBlockCount = 0;

    //Length of last block in bits, used if last block left in file is shorter than usual block length
    private int lastBlockLength;
    //To check whether last block was shorter than usual block length

    private String dataFile;
    private String encodedFile;

    /*File which holds integer representation of the block, it's probability, it's code's length,
    probability sum up to that block and block's codeword
     */
    private String codeDataFile;

    private LinkedHashMap<Integer, Integer> frequencies;

    private ArrayList<RationalFraction> probabilities = new ArrayList<>();

    private BiMap<Integer, String> encodedAlphabet;

    //Bit count of encoded blocks
    private BigInteger encodedBlockBitCount = BigInteger.ZERO;

    public Shannon() { }

    public Shannon(int blockLength) {
        this.blockLength = blockLength;
        this.lastBlockLength = blockLength;
    }

    public void encode(String dataFile, String encodedFile){
        this.dataFile = dataFile;
        this.encodedFile = encodedFile;
        this.codeDataFile = "codeData.txt";
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
                System.out.println("CALCULATING SYMBOL ENCODING...");
                getSymbolCoding();
                System.out.println("FINISHED CALCULATING SYMBOL ENCODING, TIME ELAPSED: " + (System.currentTimeMillis() - startTime) + " milis");

                startTime = System.currentTimeMillis();
                System.out.println("ENCODING FILE...");
                writeEncodingToFile();
                System.out.println("FINISHED ENCODING FILE, TIME ELAPSED: " + (System.currentTimeMillis() - startTime) + " milis");
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
    }

    private void writeEncodingToFile(){

        try {

            BitWriter bw = new BitWriter(this.encodedFile);

            writeHeader(bw);

            BitReader br = new BitReader(this.dataFile);
            BigInteger dataFileBitCount = new BigInteger(Integer.toString(br.length()*8));
            while(dataFileBitCount.compareTo(BigInteger.ZERO) > 0){

                if(dataFileBitCount.compareTo(BigInteger.valueOf(blockLength)) < 0){
                    int block = br.readBits(dataFileBitCount.intValue());
                    String binaryRepr = this.encodedAlphabet.get(block);
                    if(binaryRepr != null){
                        bw.writeBits(Integer.parseInt(binaryRepr, 2), binaryRepr.length());
                    }
                    break;
                }

                int block = br.readBits(blockLength);
                String binaryRepr = this.encodedAlphabet.get(block);
                if(binaryRepr != null){
                    bw.writeBits(Integer.parseInt(binaryRepr, 2), binaryRepr.length());
                }

                dataFileBitCount = dataFileBitCount.subtract(new BigInteger(Integer.toString(blockLength)));
            }
            bw.flush();
        }
        catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Writes encoding header to file
     * [4 bits]: bit block length -1 (max length is 16, but it requires 5 bits while we can fit 16-1 = 15 to 4 bits)
     * [4 bits]: last bit block length -1 (same as before, can have value from 2 to 16)
     * [16 bits]: number of dictionary' blocks -1. Max value is 2^16 when block length is 16
     *
     * [8 bits] : number of trailing 0 which BitWriter appends to the end of file. E.g if 65 bits are written to file,
     *          then BitWriter will write 9 bytes by appending 7 zeroes to the end of file.
     *          These zeroes will have to be ignored or else they will get decoded if some block has codeWord of 0, 00, etc...
     *
     * [n bytes] - 'dictionary'. It's number of blocks was specified before.
     *
     *            Dictionary block: [bit block length bits]: integer value of bit block
     *                              [8 bits]: length x of bit block's codeWord
     *                              [x bits] bit block's codeword
     */
    private void writeHeader(BitWriter bw) throws FileNotFoundException {

        //block length - 1, 4 bits
        bw.writeBits(blockLength - 1, 4);
        //last block length -1, 4 bits
        bw.writeBits(lastBlockLength - 1, 4);

        int dictionaryLength = this.encodedAlphabet.size();
        //'dictionary' length -1, 16 bits
        bw.writeBits(dictionaryLength - 1, 16);


        //Adding reserved header lengths to encodedBitCount

        //bit block length, last bit block length, dictionary block length
        encodedBlockBitCount = encodedBlockBitCount.add(BigInteger.valueOf(4 + 4 + 16));

        for(Map.Entry<Integer, String> entry : this.encodedAlphabet.entrySet()) {
            encodedBlockBitCount = encodedBlockBitCount.add(BigInteger.valueOf(blockLength));
            encodedBlockBitCount = encodedBlockBitCount.add(BigInteger.valueOf(8));
            encodedBlockBitCount = encodedBlockBitCount.add(BigInteger.valueOf(entry.getValue().length()));
        }

        /*Calculating number of 0 which bitwriter automatically adds at the end of file if file's bit count
         can not be properly divided into bytes */
        int count = 0;
        while(!encodedBlockBitCount.mod(BigInteger.valueOf(8)).equals(BigInteger.ZERO)){
            encodedBlockBitCount = encodedBlockBitCount.add(BigInteger.ONE);
            count++;
        }
        //Trailing zeroes, 8 bits
        bw.writeBits(count, 8);

        for(Map.Entry<Integer, String> entry : this.encodedAlphabet.entrySet()){
            //Block integer value, blockLength bits
            bw.writeBits(entry.getKey(), blockLength);

            //Block codeWords length, 8 bits
            bw.writeBits(entry.getValue().length(), 8);
            //CodeWord, it's length bits
            bw.writeBits(Integer.parseInt(entry.getValue(), 2), entry.getValue().length());
        }
    }

    /**
     * Gets frequencies of blocks from dataFile
     * @throws FileNotFoundException if dataFile not found
     */
    private void getFrequencies() throws FileNotFoundException {

        this.frequencies = new LinkedHashMap<>();
        BitReader br = new BitReader(dataFile);
        int fileBitCount = br.length()*8;

        if(fileBitCount <= blockLength){
            throw new IllegalArgumentException("Block length is bigger than or equal to total file bit count !");
        }

        this.fileBlockCount = 0;

        while(fileBitCount > 0){
            if(fileBitCount < blockLength) {

                Integer block = br.readBits(fileBitCount);
                addToFrequencies(block);
                this.fileBlockCount+=1;
                this.lastBlockLength = fileBitCount;
                break;
            }

            Integer block = br.readBits(blockLength);
            addToFrequencies(block);
            fileBitCount-=blockLength;
            this.fileBlockCount+=1;
        }
    }

    private void addToFrequencies(Integer block){
        if(frequencies.containsKey(block)){
            frequencies.put(block, frequencies.get(block) + 1);
        }
        else
            frequencies.put(block, 1);
    }

    private void getSymbolCoding() {

        this.encodedAlphabet = HashBiMap.create();

        try(BufferedWriter bw = new BufferedWriter(new FileWriter(codeDataFile))) {

            //Sum of probabilities up to p_i not counting p_i. If p_0 then sum is 0
            RationalFraction currSum = new RationalFraction(0, fileBlockCount);
            for (int i = 0; i < this.probabilities.size(); ++i) {

                int numer = probabilities.get(i).getNumerator();
                int denom = probabilities.get(i).getDenominator();
                int digits = 0;
                while(numer < denom){
                    numer*=2;
                    digits++;
                }

                //Calculating probability sum up to current probability
                if (i > 0)
                    currSum.setNumerator(currSum.getNumerator() + probabilities.get(i - 1).getNumerator());

                //Getting codeword of the block
                String codeWord = rationalFracToCodeWord(currSum, digits);

                bw.write(String.format("%-10d %s %5d %15s %15s", probabilities.get(i).getRawByte(),
                        probabilities.get(i).getRational(), digits, currSum.getRational(), codeWord) + System.lineSeparator());
                //Assigning block it's codeword
                encodedAlphabet.put(probabilities.get(i).getRawByte(), codeWord);

                BigInteger temp = BigInteger.valueOf(probabilities.get(i).getNumerator()*codeWord.length());
                encodedBlockBitCount = encodedBlockBitCount.add(temp);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Converts fraction to it's codeword with specified precision
     * @param rf fraction
     * @param precision decimal spaces to take after 0.
     * @return codeword
     */
    private static String rationalFracToCodeWord(RationalFraction rf, int precision){
        String codeWord = "";
        int tempNum = rf.getNumerator();
        int tempDen = rf.getDenominator();

        for(int i = 0; i < precision; ++i){
            tempNum*=2;
            if(tempNum >= tempDen){
                codeWord += "1";
                tempNum-=tempDen;
            }
            else
                codeWord += "0";
        }
        return codeWord;
    }

    public void decode(String encodedFile, String decodedFile) {

        long startTime = System.currentTimeMillis();
        System.out.println("DECODING...");

        this.encodedAlphabet = HashBiMap.create();

        try {
            BitWriter bw = new BitWriter(decodedFile);
            BitReader br = new BitReader(encodedFile);
            BigInteger encodedFileLength = new BigInteger(Integer.toString(br.length() * 8));

            encodedFileLength = getDictionary(br, encodedFileLength);

            BiMap<String, Integer> inverseDictionary = this.encodedAlphabet.inverse();

            String bitSequence = "";
            while (encodedFileLength.compareTo(BigInteger.valueOf(uselessZeroes)) > 0) {
                bitSequence+= Integer.toString(br.readBit());
                if(inverseDictionary.get(bitSequence) != null){
                    if((encodedFileLength.subtract(BigInteger.valueOf(bitSequence.length())).intValue() == uselessZeroes)){
                        bw.writeBits(inverseDictionary.get(bitSequence), lastBlockLength);
                        break;
                    }
                    bw.writeBits(inverseDictionary.get(bitSequence), blockLength);
                    encodedFileLength = encodedFileLength.subtract(BigInteger.valueOf(bitSequence.length()));
                    bitSequence = "";
                }
            }
            bw.flush();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("DECODING FINISHED, TIME ELAPSED: " + (System.currentTimeMillis() - startTime) + " milis");
    }

    private int uselessZeroes = 0;

    private BigInteger getDictionary(BitReader br, BigInteger fileLength){
        this.encodedAlphabet = HashBiMap.create();

        this.blockLength = br.readBits(4) + 1;
        this.lastBlockLength = br.readBits(4) + 1;
        int dictionarySize = br.readBits(16) + 1;
        this.uselessZeroes = br.readBits(8);
        fileLength = fileLength.subtract(BigInteger.valueOf(4 + 4 + 16 + 8));

        //System.out.println(uselessZeroes + " " + blockLength + " " + lastBlockLength + " " + dictionarySize);

        for(int i = 0; i < dictionarySize; ++i){
            int block = br.readBits(blockLength);
            fileLength = fileLength.subtract(BigInteger.valueOf(blockLength));
            int codeWordLen = br.readBits(8);
            fileLength = fileLength.subtract(BigInteger.valueOf(8));
            String codeWord = "";
            while(codeWordLen > 0){
                int bt = br.readBit();
                fileLength = fileLength.subtract(BigInteger.ONE);
                if(bt == 0)
                    codeWord+="0";
                if(bt == 1)
                    codeWord+="1";
                codeWordLen--;
            }
            this.encodedAlphabet.put(block, codeWord);
        }
        return fileLength;
    }
}
