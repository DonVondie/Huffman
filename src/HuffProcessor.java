import java.util.*;
public class HuffProcessor implements Processor {
  String[] codes = new String[ALPH_SIZE +1];
  int[] frequencies;
  HuffNode currentHuff;
  HuffNode decompHuff;
  HuffNode finalHead;
  public void compress(BitInputStream in, BitOutputStream out){
	  findFrequencies(in);
	  buildTree();
	  getCodes(currentHuff, "");
	  out.writeBits(BITS_PER_INT, HUFF_NUMBER);
	  writeHead(currentHuff, out);
	  writeBody(in, out);
	  writeEOF(out);
  }
  public void findFrequencies(BitInputStream input){
      frequencies = new int[ALPH_SIZE];
      int current = input.readBits(BITS_PER_WORD);
      while(current != -1){
        frequencies[current] += 1;
        current = input.readBits(BITS_PER_WORD);
      }
      input.reset();
  }
  public void buildTree(){
     PriorityQueue<HuffNode> queue = new PriorityQueue<>();
     int i = 0;
     while(i < ALPH_SIZE){
       if(frequencies[i] != 0){
         HuffNode newHuff = new HuffNode(i, frequencies[i]);
         queue.add(newHuff);
       }
       i++;
     } // end of while loop
     HuffNode EOFile = new HuffNode(PSEUDO_EOF, 0);
     queue.add(EOFile);
     System.out.println(queue.size());
     while(queue.size() > 1){
        HuffNode first = queue.remove();
        HuffNode second = queue.remove();
        int firstw = first.weight();
        int secondw = second.weight();
        int combined = firstw + secondw;
        HuffNode upper = new HuffNode(-1, combined, first, second);
        queue.add(upper);
     }
     HuffNode temp = queue.remove();
     currentHuff = temp;
     finalHead = temp; 
  }
  public void getCodes(HuffNode temp, String code){
	      HuffNode leftc = temp.left();
	      HuffNode rightc = temp.right();
	      if(leftc ==null && rightc ==null){
	          int tempVal = temp.value();
	          codes[tempVal] = code;
	          return;
	      }
	      String leftCode = code + "0";
	      String rightCode = code + "1";
	      getCodes(leftc, leftCode);
	      getCodes(rightc, rightCode);
	  }
  public void writeHead(HuffNode temp, BitOutputStream output){
        if(temp.value() == -1){
        	output.writeBits(1,0);
            HuffNode left = temp.left();
            HuffNode right = temp.right();
            writeHead(left, output);
            writeHead(right, output);
          }
        else{
        	output.writeBits(1,1);
            output.writeBits(9, temp.value());
            return;
        }
  }
  public void writeBody(BitInputStream input, BitOutputStream output){
      int tempBit = input.readBits(BITS_PER_WORD);
      while(tempBit != -1){
          String tempCode = codes[tempBit];
          int tempLength = tempCode.length();
          int huffBits = Integer.parseInt(tempCode, 2);
          output.writeBits(tempLength, huffBits);
          tempBit = input.readBits(BITS_PER_WORD);
      }
  }
  public void writeEOF(BitOutputStream output){
      String eofCode = codes[PSEUDO_EOF];
      int eofLength = eofCode.length();
      int eofInt = Integer.parseInt(eofCode, 2);
      output.writeBits(eofLength, eofInt);
  }
  public HuffNode readHead(BitInputStream input){
    if (input.readBits(1) != 0){
      int bitsRead = input.readBits(9);
      HuffNode output = new HuffNode(bitsRead, 0);
      decompHuff = output;
      return decompHuff; // let us see
    }
    else{
      HuffNode first = readHead(input);
      HuffNode second = readHead(input);
      int firstWeight = first.weight();
      int secondWeight = second.weight();
      int combined = firstWeight + secondWeight;
      HuffNode upper = new HuffNode(-1, combined, first, second);
      decompHuff = upper;
      return decompHuff;
    }
  }
  public void readBody(BitInputStream input, BitOutputStream output){
        int decompBit = input.readBits(1);
        HuffNode decomp = decompHuff;
        while(decompBit != -1){
          if(decompBit != 0){
        	  decompHuff = decompHuff.right();
          }
          else{
        	  decompHuff = decompHuff.left();
          }
          if(decompHuff.value() != -1){
              if(decompHuff.value() != PSEUDO_EOF){
                int decompVal = decompHuff.value();
                output.writeBits(BITS_PER_WORD, decompVal);
                decompHuff = decomp;
              }
              else{
                return;
              }
          }
          decompBit = input.readBits(1);
        }
  }
public void decompress(BitInputStream input, BitOutputStream output){
    int check = input.readBits(BITS_PER_INT);
    if (check != HUFF_NUMBER){
      throw new HuffException("No huff number dude!");
    }
    decompHuff = readHead(input);
    readBody(input, output);
}
}
