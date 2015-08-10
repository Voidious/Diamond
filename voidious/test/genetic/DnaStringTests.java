package voidious.test.genetic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.BitSet;

import org.junit.Test;

import voidious.utils.DiaUtils;
import voidious.utils.genetic.DnaSequence;
import voidious.utils.genetic.DnaSequence.Gene;
import voidious.utils.genetic.DnaSequence.GeneType;
import voidious.utils.genetic.DnaString;
import voidious.utils.genetic.DnaString.GeneTypeException;

/**
 * Copyright (c) 2012 - Voidious
 *
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event will the authors be held liable for any damages
 * arising from the use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 *
 *    1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software.
 *
 *    2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 *
 *    3. This notice may not be removed or altered from any source
 *    distribution.
 */

public class DnaStringTests {
  @Test
  public void testBitHexString() {
    DnaSequence seq = new DnaSequence();
    seq.addGene(new Gene("hair", GeneType.BYTE));
    seq.addGene(new Gene("eyes", GeneType.BYTE, true));

    BitSet bits = new BitSet(16);
    bits.set(3);
    bits.set(11);
    bits.set(13);
    final String BIT_STRING = "0001000000010100",
           HEX_STRING = "1014";

    DnaString dnaString = new DnaString(seq, bits);
    assertEquals("Generated incorrect bit string from BitSet constructor.",
      BIT_STRING, dnaString.bitString());
    assertEquals("Generated incorrect hex string from BitSet constructor.",
      "0x" + HEX_STRING, dnaString.hexString());

    final byte BLONDE = 0, RED = 11, BROWN = 69;
    final String BLONDE_BIT_STRING = "00000000",
           BLONDE_HEX_STRING = "00",
           RED_BIT_STRING =  "00001011",
           RED_HEX_STRING =  "0B",
           BROWN_BIT_STRING =  "01000101",
           BROWN_HEX_STRING =  "45";

    final byte BLUE = -56, GREEN = 15;
    final String BLUE_BIT_STRING =   "11001000",
           BLUE_HEX_STRING =   "C8",
           GREEN_BIT_STRING =  "00001111",
           GREEN_HEX_STRING =  "0F";

    dnaString = new DnaString(seq);
    dnaString.setByte("hair", BLONDE);
    dnaString.setByte("eyes", GREEN);
    assertEquals("Generated incorrect bit string from setByte calls.",
      BLONDE_BIT_STRING + GREEN_BIT_STRING, dnaString.bitString());
    assertEquals("Generated incorrect hex string from setByte calls.",
      "0x" + BLONDE_HEX_STRING + GREEN_HEX_STRING, dnaString.hexString());

    dnaString.setByte("hair", RED);
    dnaString.setByte("eyes", BLUE);
    assertEquals("Generated incorrect bit string from setByte calls.",
      RED_BIT_STRING + BLUE_BIT_STRING, dnaString.bitString());
    assertEquals("Generated incorrect hex string from setByte calls.",
      "0x" + RED_HEX_STRING + BLUE_HEX_STRING, dnaString.hexString());

    dnaString.setByte("hair", BROWN);
    dnaString.setByte("eyes", BLUE);
    assertEquals("Generated incorrect bit string from setByte calls.",
      BROWN_BIT_STRING + BLUE_BIT_STRING, dnaString.bitString());
    assertEquals("Generated incorrect hex string from setByte calls.",
      "0x" + BROWN_HEX_STRING + BLUE_HEX_STRING, dnaString.hexString());

    final float STACY = 106.4178F;
    final String STACY_BIT_STRING = "01000010110101001101010111101010",
           STACY_HEX_STRING = "42D4D5EA";
    final double RICHGUY = 20598.38238727;
    final String RICHGUY_BIT_STRING =
      "0100000011010100000111011001100001111001000010000111010011000100",
           RICHGUY_HEX_STRING =
      "40D41D98790874C4";

    seq = new DnaSequence();
    seq.addGene(new Gene("weight", GeneType.FLOAT));
    dnaString = new DnaString(seq);
    dnaString.setFloat("weight", STACY);
    assertEquals("Generated incorrect bit string from setFloat calls.",
      STACY_BIT_STRING, dnaString.bitString());
    assertEquals("Generated incorrect hex string from setFloat calls.",
      "0x" + STACY_HEX_STRING, dnaString.hexString());

    seq = new DnaSequence();
    seq.addGene(new Gene("balance", GeneType.DOUBLE));
    dnaString = new DnaString(seq);
    dnaString.setDouble("balance", RICHGUY);
    assertEquals("Generated incorrect bit string from setDouble calls.",
      RICHGUY_BIT_STRING, dnaString.bitString());
    assertEquals("Generated incorrect hex string from setDouble calls.",
      "0x" + RICHGUY_HEX_STRING, dnaString.hexString());

    seq = new DnaSequence();
    dnaString = new DnaString(seq);
    assertEquals("Generated incorrect bit string from empty DnaSequence.",
      "", dnaString.bitString());
    assertEquals("Generated incorrect bit string from empty DnaSequence.",
      "0x", dnaString.hexString());
  }

  @Test
  public void testParseBitString() {
    DnaSequence seq = new DnaSequence();
    seq.addGene(new Gene("height", GeneType.INTEGER));
    String bitString = "00000000000000000000000101110101";
    int bitStringValue = 373;
    DnaString dnaString = new DnaString(seq, bitString);
    assertEquals("DnaString from parsed bit string produced incorrect " +
        "integer value.", bitStringValue, dnaString.getInt("height"));

    String[] bitStrings = new String[]{
      "010110011010101001001000000010100",
      "100010111011001110110101",
      "10",
      "0001011101111101010000011111110000000011111111000000111111000"
    };

    for (String s : bitStrings) {
      DnaSequence seq2 = GenTestUtils.dnaSequenceBits(s.length());
      DnaString dnaString2 = new DnaString(seq2, s);
      assertEquals("DnaString.bitString() didn't match bit string " +
        "passed to DnaString constructor.", s, dnaString2.bitString());
    }
  }

  @Test
  public void testParseHexString() {
    DnaSequence seq = new DnaSequence();
    seq.addGene(new Gene("height", GeneType.INTEGER));
    String hexString = "0x00000175";
    int hexStringValue = 373;
    DnaString dnaString = new DnaString(seq, hexString);
    assertEquals("DnaString from parsed hex string produced incorrect " +
        "integer value.", hexStringValue, dnaString.getInt("height"));

    String[] hexStrings = new String[]{
      "0xF8289001C",
      "0x0010431D",
      "0x0",
      "0x9183848FA00A00CD3D991001029482910394829CF"
    };

    for (String s : hexStrings) {
      DnaSequence seq2 = GenTestUtils.dnaSequenceBits((s.length()-2) * 4);
      DnaString dnaString2 = new DnaString(seq2, s);
      assertEquals("DnaString.hexString() didn't match hex string " +
        "passed to DnaString constructor.", s, dnaString2.hexString());
    }
  }

  @Test
  public void testGetSourceString() {
    DnaSequence seq = new DnaSequence();
    DnaString string = new DnaString(seq, "", DnaString.CROSSED);
    assertEquals("Crossed DnaString returned incorrect source string",
      "Crossed", string.getSourceString());
    string = new DnaString(seq, "", DnaString.SEEDED);
    assertEquals("Crossed DnaString returned incorrect source string",
      "Seeded", string.getSourceString());
    string = new DnaString(seq, "", DnaString.SURVIVED);
    assertEquals("Crossed DnaString returned incorrect source string",
      "Survived", string.getSourceString());
    string = new DnaString(seq, "", DnaString.MUTATED);
    assertEquals("Crossed DnaString returned incorrect source string",
      "Mutated", string.getSourceString());
    string = new DnaString(seq, "", DnaString.RANDOM);
    assertEquals("Crossed DnaString returned incorrect source string",
      "Random", string.getSourceString());
  }

  @Test
  public void testGeneTypeExceptions() {
    for (GeneType type : GenTestUtils.getGeneTypes()) {
      DnaSequence seq = new DnaSequence();
      Gene g = new Gene(GenTestUtils.randomString(), type);
      seq.addGene(g);
      DnaString dnaString = new DnaString(seq);

      for (GeneType wrongType : GenTestUtils.getGeneTypes()) {
        if (wrongType != g.type) {
          boolean caughtException = false;
          try {
            trySetGeneType(dnaString, g, wrongType);
          } catch (GeneTypeException ex) {
            caughtException = true;
          }

          if (!caughtException)
            fail("Set value didn't throw exception on DnaString " +
              "Gene type=" + type + ", with incorrect setXXX, " +
              "type=" + wrongType + ".");

          caughtException = false;
          try {
            tryGetGeneType(dnaString, g, wrongType);
          } catch (GeneTypeException ex) {
            caughtException = true;
          }

          if (!caughtException)
            fail("Get value didn't throw exception on DnaString " +
              "Gene type=" + type + ", with incorrect getXXX, " +
              "type=" + wrongType + ".");
        }
      }

    }
  }

  @Test
  public void testSetBit() {
    DnaSequence seq = new DnaSequence();
    seq.addGene(new Gene("rich", GeneType.BIT));
    DnaString string = new DnaString(seq);
    string.setBit("rich", true);
    assertEquals("Bit not set to correct value.", true, string.getBitSet().get(0));
    string.setBit("rich", false);
    assertEquals("Bit not set to correct value.", false, string.getBitSet().get(0));
  }

  @Test
  public void testSetByte() {
    DnaSequence seq = new DnaSequence();
    seq.addGene(new Gene("reach", GeneType.BYTE));

    byte value = 77; // 01001101
    BitSet valueBits = new BitSet(8);
    valueBits.set(1);
    valueBits.set(4);
    valueBits.set(5);
    valueBits.set(7);
    DnaString string = new DnaString(seq);
    string.setByte("reach", value);
    assertEquals("Byte not set to correct value.", valueBits, string.getBitSet());
  }

  @Test
  public void testSetByteNegative() {
    DnaSequence seq = new DnaSequence();
    seq.addGene(new Gene("reach", GeneType.BYTE, true));
    DnaString string = new DnaString(seq);

    byte value = -1; // 11111111
    BitSet valueBits = new BitSet(8);
    valueBits.set(0, 8);
    string.setByte("reach", value);
    assertEquals("Byte not set to correct value.", valueBits, string.getBitSet());
  }

  @Test
  public void testSetShort() {
    DnaSequence seq = new DnaSequence();
    seq.addGene(new Gene("balance", GeneType.SHORT));

    short value = 5951; // 0001011100111111
    BitSet valueBits = new BitSet(16);
    valueBits.set(3);
    valueBits.set(5, 8);
    valueBits.set(10, 16);
    DnaString string = new DnaString(seq);
    string.setShort("balance", value);
    assertEquals("Short not set to correct value.", valueBits, string.getBitSet());
  }

  @Test
  public void testSetShortNegative() {
    DnaSequence seq = new DnaSequence();
    seq.addGene(new Gene("balance", GeneType.SHORT, true));
    BitSet valueBits = new BitSet(16);
    DnaString string = new DnaString(seq);
    
    short value = -1;
    valueBits.set(0, 16); // 1111111111111111
    string.setShort("balance", value);
    assertEquals("Short not set to correct value.", valueBits, string.getBitSet());
  }

  @Test
  public void testSetInt() {
    DnaSequence seq = new DnaSequence();
    seq.addGene(new Gene("planets", GeneType.INTEGER));

    int value = 901080009; // 00110101101101010110001111001001
    BitSet valueBits = new BitSet(32);
    valueBits.set(2, 4);
    valueBits.set(5);
    valueBits.set(7, 9);
    valueBits.set(10, 12);
    valueBits.set(13);
    valueBits.set(15);
    valueBits.set(17, 19);
    valueBits.set(22, 26);
    valueBits.set(28);
    valueBits.set(31);
    DnaString string = new DnaString(seq);
    string.setInt("planets", value);
    assertEquals("Integer not set to correct value.", valueBits, string.getBitSet());
  }

  @Test
  public void testSetIntNegative() {
    DnaSequence seq = new DnaSequence();
    seq.addGene(new Gene("planets", GeneType.INTEGER, true));
    DnaString string = new DnaString(seq);
    BitSet valueBits = new BitSet(32);

    int value = -1;
    valueBits.set(0, 32); // 11111111111111111111111111111111
    string.setInt("planets", value);
    assertEquals("Integer not set to correct value.", valueBits, string.getBitSet());
  }

  @Test
  public void testSetLong() {
    DnaSequence seq = new DnaSequence();
    seq.addGene(new Gene("planets", GeneType.LONG));

    long value = 50489277901080009L; // 0000000010110011010111111011101100010010010000101000010111001000
    BitSet valueBits = new BitSet(64);
    valueBits.set(8);
    valueBits.set(10, 12);
    valueBits.set(14, 16);
    valueBits.set(17);
    valueBits.set(19, 25);
    valueBits.set(26, 29);
    valueBits.set(30, 32);
    valueBits.set(35);
    valueBits.set(38);
    valueBits.set(41);
    valueBits.set(46);
    valueBits.set(48);
    valueBits.set(53);
    valueBits.set(55, 58);
    valueBits.set(60);
    valueBits.set(63);
    DnaString string = new DnaString(seq);
    string.setLong("planets", value);
    assertEquals("Long not set to correct value.", valueBits, string.getBitSet());
  }

  @Test
  public void testSetLongNegative() {
    DnaSequence seq = new DnaSequence();
    seq.addGene(new Gene("planets", GeneType.LONG, true));
    BitSet valueBits = new BitSet(64);
    DnaString string = new DnaString(seq);

    long value = -1;
    valueBits.set(0, 64); // 1111111111111111111111111111111111111111111111111111111111111111
    string.setLong("planets", value);
    assertEquals("Long not set to correct value.", valueBits, string.getBitSet());
  }

  @Test
  public void testSetNumber() {
    DnaSequence seq = new DnaSequence();
    seq.addGene(new Gene("planets", 11, 1635));

    long value = 1319L; // 10100100111
    BitSet valueBits = new BitSet(11);
    valueBits.set(0);
    valueBits.set(2);
    valueBits.set(5);
    valueBits.set(8, 11);
    DnaString string = new DnaString(seq);
    string.setNumber("planets", value);
    assertEquals("Number not set to correct value.", valueBits, string.getBitSet());
  }

  @Test
  public void testSetNumberNegative() {
    DnaSequence seq = new DnaSequence();
    seq.addGene(new Gene("score", GeneType.NUMBER, Long.MAX_VALUE, true, 6));
    BitSet valueBits = new BitSet(5);
    DnaString string = new DnaString(seq);

    long value = -28; // 100100
    valueBits.set(0);
    valueBits.set(3);
    string.setNumber("score", value);
    assertEquals("Number not set to correct value.", valueBits, string.getBitSet());
  }

  @Test
  public void testSetFloat() {
    DnaSequence seq = new DnaSequence();
    seq.addGene(new Gene("floaty", GeneType.FLOAT));

    float value = 106.4178F; // 01000010110101001101010111101010
    BitSet valueBits = new BitSet(64);
    valueBits.set(1);
    valueBits.set(6);
    valueBits.set(8, 10);
    valueBits.set(11);
    valueBits.set(13);
    valueBits.set(16, 18);
    valueBits.set(19);
    valueBits.set(21);
    valueBits.set(23, 27);
    valueBits.set(28);
    valueBits.set(30);
    DnaString string = new DnaString(seq);
    string.setFloat("floaty", value);
    assertEquals("Float not set to correct value.", valueBits, string.getBitSet());

    value = -1; // 10111111100000000000000000000000
    valueBits.clear();
    valueBits.set(0);
    valueBits.set(2, 9);
    string.setFloat("floaty", value);
    assertEquals("Float not set to correct value.", valueBits, string.getBitSet());
  }

  @Test
  public void testSetDouble() {
    DnaSequence seq = new DnaSequence();
    seq.addGene(new Gene("doubly", GeneType.DOUBLE));

    double value = 20598.38238727; // 0100000011010100000111011001100001111001000010000111010011000100
    BitSet valueBits = new BitSet(64);
    valueBits.set(1);
    valueBits.set(8, 10);
    valueBits.set(11);
    valueBits.set(13);
    valueBits.set(19, 22);
    valueBits.set(23, 25);
    valueBits.set(27, 29);
    valueBits.set(33, 37);
    valueBits.set(39);
    valueBits.set(44);
    valueBits.set(49, 52);
    valueBits.set(53);
    valueBits.set(56, 58);
    valueBits.set(61);
    DnaString string = new DnaString(seq);
    string.setDouble("doubly", value);
    assertEquals("Double not set to correct value.", valueBits, string.getBitSet());

    value = -1; // 1011111111110000000000000000000000000000000000000000000000000000
    valueBits.clear();
    valueBits.set(0);
    valueBits.set(2, 12);
    string.setDouble("doubly", value);
    assertEquals("Double not set to correct value.", valueBits, string.getBitSet());
  }

  @Test
  public void testGetBit() {
    DnaSequence seq = new DnaSequence();
    seq.addGene(new Gene("rich", GeneType.BIT));
    DnaString string = new DnaString(seq);

    string.setBit("rich", true);
    assertEquals("Incorrect bit value.", true, string.getBit("rich"));
    string.setBit("rich", false);
    assertEquals("Incorrect bit value.", false, string.getBit("rich"));
  }

  @Test
  public void testGetByte() {
    DnaSequence seq = new DnaSequence();
    seq.addGene(new Gene("reach", GeneType.BYTE, true));
    DnaString string = new DnaString(seq);

    byte value = Byte.MIN_VALUE;
    do {
      string.setByte("reach", value);
      assertEquals("Incorrect byte value.", value, string.getByte("reach"));
    } while (value++ < Byte.MAX_VALUE);
  }

  @Test
  public void testGetShort() {
    DnaSequence seq = new DnaSequence();
    seq.addGene(new Gene("balance", GeneType.SHORT, true));
    DnaString string = new DnaString(seq);

    short value = Short.MIN_VALUE;
    do {
      string.setShort("balance", value);
      assertEquals("Incorrect short value.", value, string.getShort("balance"));
    } while (value++ < Short.MAX_VALUE);
  }

  @Test
  public void testGetInt() {
    DnaSequence seq = new DnaSequence();
    seq.addGene(new Gene("planets", GeneType.INTEGER, true));
    DnaString string = new DnaString(seq);

    int value = Integer.MIN_VALUE;
    do {
      string.setInt("planets", value);
      assertEquals("Incorrect int value.", value, string.getInt("planets"));
    } while ((value += (int)(Math.random() * 10000)) < Integer.MAX_VALUE - 10000);

    string.setInt("planets", Integer.MAX_VALUE);
    assertEquals("Incorrect int value.", Integer.MAX_VALUE, string.getInt("planets"));
  }

  @Test
  public void testGetLong() {
    DnaSequence seq = new DnaSequence();
    seq.addGene(new Gene("planets", GeneType.LONG, true));
    DnaString string = new DnaString(seq);

    long value = Long.MIN_VALUE;
    do {
      string.setLong("planets", value);
      assertEquals("Incorrect long value. Bit string: " +
        string.bitString(), value, string.getLong("planets"));
    } while ((value += (long)(Math.random() * 100000000000000L))
             < Long.MAX_VALUE - 100000000000000L);

    string.setLong("planets", Long.MAX_VALUE);
    assertEquals("Incorrect long value.", Long.MAX_VALUE, string.getLong("planets"));
  }

  @Test
  public void testGetNumber() {
    DnaSequence seq = new DnaSequence();
    seq.addGene(new Gene("score", GeneType.NUMBER, Long.MAX_VALUE, true, 23));
    DnaString string = new DnaString(seq);

    long maxValue = Math.round(DiaUtils.power(2, 22)) - 1;
    long value = -maxValue;
    do {
      string.setNumber("score", value);
      assertEquals("Incorrect long value. Bit string: " +
        string.bitString(), value, string.getNumber("score"));
    } while ((value += (long) (Math.random() * 10000))
             < maxValue - 10000);

    string.setNumber("score", maxValue);
    assertEquals("Incorrect long value.", maxValue, string.getNumber("score"));
    string.setNumber("score", -maxValue - 1);
    assertEquals("Incorrect long value.", -maxValue - 1, string.getNumber("score"));
  }

  @Test
  public void testGetFloat() {
    DnaSequence seq = new DnaSequence();
    seq.addGene(new Gene("floaty", GeneType.FLOAT, true));
    DnaString string = new DnaString(seq);

    float value = -1000.0F;
    do {
      string.setFloat("floaty", value);
      assertEquals("Incorrect float value. Bit string: " +
        string.bitString(), value, string.getFloat("floaty"), .0001);
    } while ((value += (Math.random() / 100)) < 1000.0F);
  }

  @Test
  public void testGetDouble() {
    DnaSequence seq = new DnaSequence();
    seq.addGene(new Gene("doubly", GeneType.DOUBLE, true));
    DnaString string = new DnaString(seq);

    double value = -1000000.0;
    do {
      string.setDouble("doubly", value);
      assertEquals("Incorrect double value. Bit string: " +
        string.bitString(), value, string.getDouble("doubly"), .0001);
    } while ((value += (Math.random() * 10)) < 1000000.0);
  }

  @Test
  public void testMutate() {
    for (int x = 0; x < 1000; x++) {
      int length = (int)(Math.random() * 9900) + 100;
      DnaSequence seq = GenTestUtils.dnaSequenceBits(length);
      double mutationRate = 1.0 / length;

      DnaString string = new DnaString(seq, GenTestUtils.randomBitString(length));
      DnaString mutated;
      do {
        mutated = DnaString.mutate(string, mutationRate);
      } while (mutated.getSourceType() != DnaString.MUTATED);

      BitSet bs1 = string.getBitSet();
      BitSet bs2 = mutated.getBitSet();
      int same = 0;
      for (int y = 0; y < length; y++) {
        if (bs1.get(y) == bs2.get(y))
          same++;
      }

      if (same == length) {
        fail("Mutated member wasn't mutated.\n" +
          "Original: " + string.bitString() + "\n" +
          "Mutated:  " + mutated.bitString());
      }

      if (same < length / 2) {
        fail("Mutated member was extremely mutated - highly unlikely. " +
          "Length: " + length + ", rate: " + mutationRate +
          ", bits mutated: " + (length - same) + ".\n" +
          "Original: " + string.bitString() + "\n" +
          "Mutated:  " + mutated.bitString());
      }
    }
  }

  @Test
  public void testRandom() {
    for (int x = 0; x < 1000; x++) {
      int length = (int)(Math.random() * 9999) + 1;
      DnaSequence seq = GenTestUtils.dnaSequenceBits(length);
      DnaString string = DnaString.random(seq);

      boolean diff = false;
      for (int y = 0; y < 1000; y++) {
        DnaString string2 = DnaString.random(seq);
        if (!string.getBitSet().equals(string2.getBitSet())) {
          diff = true;
          break;
        }
      }

      assertEquals("Random DnaString is always the same.", true, diff);
      assertEquals("Random DnaString has wrong DnaSequence.", seq,
        string.getDnaSequence());
      assertEquals("Random DnaString has wrong number of bits.", length,
        string.bitString().length());
      assertEquals("Random DnaString not of type 'Random'.",
        DnaString.RANDOM, string.getSourceType());
    }
  }

  @Test
  public void testClone() {
    for (int x = 0; x < 1000; x++) {
      int length = (int)(Math.random() * 9999) + 1;
      DnaSequence seq = GenTestUtils.dnaSequenceBits(length);
      DnaString string = DnaString.random(seq);
      DnaString clone = (DnaString)string.clone();
      assertNotSame("Clone has same BitSet as original, should have a copy.",
        string.getBitSet(), clone.getBitSet());
      assertEquals("Clone's BitSet is not equal to original BitSet.",
        string.getBitSet(), clone.getBitSet());
      assertSame("Clone's DnaSequence should be same as original's.",
        string.getDnaSequence(), clone.getDnaSequence());
    }
  }

  @Test
  public void testEquals() {
    for (int x = 0; x < 1000; x++) {
      int length = (int)(Math.random() * 9999) + 1;
      DnaSequence seq = GenTestUtils.dnaSequenceBits(length);

      String bitString = GenTestUtils.randomBitString(length);
      DnaString string1 = new DnaString(seq, bitString);
      DnaString string2 = new DnaString(seq, bitString);

      assertEquals("DnaStrings based on same sequence and bit string " +
        "should be equal.", string1, string2);
    }

    for (int x = 0; x < 1000; x++) {
      int length = (int)(Math.random() * 9999) + 1;
      DnaSequence seq = GenTestUtils.dnaSequenceBits(length);

      String bitString = GenTestUtils.randomBitString(length);
      DnaString string1 = new DnaString(seq, bitString);
      DnaString string2 = new DnaString(seq, string1.getBitSet());

      assertEquals("DnaStrings based on same sequence and BitSet " +
        "should be equal.", string1, string2);
    }

    for (int x = 0; x < 1000; x++) {
      int length = (int)(Math.random() * 9999) + 1;
      DnaSequence seq = GenTestUtils.dnaSequenceBits(length);

      String bitString = GenTestUtils.randomBitString(length);
      DnaString string1 = new DnaString(seq, bitString);

      BitSet bits2 = (BitSet)string1.getBitSet().clone();
      int flipBit = (int)(Math.random() * length);
      bits2.set(flipBit, !bits2.get(flipBit));
      DnaString string2 = new DnaString(seq, bits2);

      if (string1.equals(string2))
        fail("DnaStrings based on same sequence, different " +
          "BitSet should NOT be equal.");
    }

    for (int x = 0; x < 1000; x++) {
      int length = ((int)((Math.random() * 1249) + 1)) * 8;
      DnaSequence seqBits = GenTestUtils.dnaSequenceBits(length);
      DnaSequence seqBytes = GenTestUtils.dnaSequenceBytes(length);

      String bitString = GenTestUtils.randomBitString(length);
      DnaString stringBits = new DnaString(seqBits, bitString);
      DnaString stringBytes = new DnaString(seqBytes, stringBits.getBitSet());

      if (stringBits.equals(stringBytes))
        fail("DnaStrings based on same BitSet, different " +
            "sequence should NOT be equal.");
    }
  }

  private void trySetGeneType(DnaString string, Gene g, GeneType tryType)
    throws GeneTypeException {

    switch (tryType) {
      case BIT:
        string.setBit(g.name, true);
        break;
      case BYTE:
        string.setByte(g.name, (byte)12);
        break;
      case SHORT:
        string.setShort(g.name, (short)1050);
        break;
      case INTEGER:
        string.setInt(g.name, 123456789);
        break;
      case LONG:
        string.setLong(g.name, 123456789L);
        break;
      case FLOAT:
        string.setFloat(g.name, 49.87F);
        break;
      case DOUBLE:
        string.setDouble(g.name, 905.65487);
        break;
    }
  }

  private void tryGetGeneType(DnaString string, Gene g, GeneType tryType)
    throws GeneTypeException {

    switch (tryType) {
      case BIT:
        string.getBit(g.name);
        break;
      case BYTE:
        string.getByte(g.name);
        break;
      case SHORT:
        string.getShort(g.name);
        break;
      case INTEGER:
        string.getInt(g.name);
        break;
      case LONG:
        string.getLong(g.name);
        break;
      case FLOAT:
        string.getFloat(g.name);
        break;
      case DOUBLE:
        string.getDouble(g.name);
        break;
    }
  }
}
