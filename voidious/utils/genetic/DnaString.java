package voidious.utils.genetic;

import java.util.ArrayList;
import java.util.BitSet;

import voidious.utils.genetic.DnaSequence.Gene;
import voidious.utils.genetic.DnaSequence.GeneType;

/**
 * Copyright (c) 2011-2012 - Voidious
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

public class DnaString implements Cloneable {
  // TODO: allow for a "header" that doesn't change with evolution. info about
  //     structure of DnaSequence
  protected DnaSequence _dnaSequence;
  protected BitSet _bits;
  protected int _sourceType;

  public static final int RANDOM = 0;
  public static final int MUTATED = 1;
  public static final int CROSSED = 2;
  public static final int SEEDED = 3;
  public static final int SURVIVED = 4;
  public static final String[] sourceTypes =
      new String[]{"Random", "Mutated", "Crossed", "Seeded", "Survived"};

  public DnaString(DnaSequence dnaSequence, BitSet bits, int sourceType) {
    _dnaSequence = dnaSequence;
    if (bits == null) {
      _bits = new BitSet(dnaSequence.length());
    } else {
      _bits = (BitSet)bits.clone();
    }
    _sourceType = sourceType;
  }

  public DnaString(DnaSequence dnaSequence, String numString, int sourceType) {
    this(dnaSequence, parseString(numString, dnaSequence.length()), sourceType);
  }

  public DnaString(DnaSequence dnaSequence, BitSet bits) {
    this(dnaSequence, bits, SEEDED);
  }

  public DnaString(DnaSequence dnaSequence, String numString) {
    this(dnaSequence, numString, SEEDED);
  }

  public DnaString(DnaSequence dnaSequence) {
    this(dnaSequence, new BitSet(dnaSequence.length()));
  }

  public DnaSequence getDnaSequence() {
    return _dnaSequence;
  }

  public BitSet getBitSet() {
    return _bits;
  }

  public int getSourceType() {
    return _sourceType;
  }

  public String getSourceString() {
    return sourceTypes[_sourceType];
  }

  public void setSourceType(int sourceType) {
    _sourceType = sourceType;
  }

  public void setBit(String geneName, boolean value) {
    Gene g = _dnaSequence.getGene(geneName);
    checkGeneType(g, GeneType.BIT);
    _bits.set(g.position, value);
  }

  public void setByte(String geneName, byte value) {
    Gene g = _dnaSequence.getGene(geneName);
    checkGeneType(g, GeneType.BYTE);
    set(g.negatives ? g.position : g.position + 1, value,
        g.negatives ? g.size : g.size - 1, g.negatives);
  }

  public void setShort(String geneName, short value) {
    Gene g = _dnaSequence.getGene(geneName);
    checkGeneType(g, GeneType.SHORT);
    set(g.negatives ? g.position : g.position + 1, value,
        g.negatives ? g.size : g.size - 1, g.negatives);
  }

  public void setInt(String geneName, int value) {
    Gene g = _dnaSequence.getGene(geneName);
    checkGeneType(g, GeneType.INTEGER);
    set(g.negatives ? g.position : g.position + 1, value,
        g.negatives ? g.size : g.size - 1, g.negatives);
  }

  public void setLong(String geneName, long value) {
    Gene g = _dnaSequence.getGene(geneName);
    checkGeneType(g, GeneType.LONG);
    set(g.negatives ? g.position : g.position + 1, value,
        g.negatives ? g.size : g.size - 1, g.negatives);
  }

  public void setFloat(String geneName, float value) {
    Gene g = _dnaSequence.getGene(geneName);
    checkGeneType(g, GeneType.FLOAT);
    set(g.position, Float.floatToIntBits(value), g.size, true);
  }

  public void setDouble(String geneName, double value) {
    Gene g = _dnaSequence.getGene(geneName);
    checkGeneType(g, GeneType.DOUBLE);
    set(g.position, Double.doubleToLongBits(value), g.size, true);
  }

  public void setNumber(String geneName, long value) {
    Gene g = _dnaSequence.getGene(geneName);
    checkGeneType(g, GeneType.NUMBER);
    set(g.position, value, g.size, g.negatives);
  }

  private void set(int position, long value, int numBits, boolean negatives) {
    int x = 0;
    long pow = 2;
    while (x < (negatives ? numBits - 1 : numBits)) {
      long rem = value % pow;
      while (rem < 0) {
        rem += pow;
      }
      boolean bit = ((rem >> x) == 1);
      _bits.set(position + numBits - 1 - x, bit);
      x++;
      pow *= 2;
    }
    if (negatives) {
      _bits.set(position, (value < 0));
    }
  }

  public boolean getBit(String geneName) {
    Gene g = _dnaSequence.getGene(geneName);
    checkGeneType(g, GeneType.BIT);
    return _bits.get(g.position);
  }

  public byte getByte(String geneName) {
    Gene g = _dnaSequence.getGene(geneName);
    checkGeneType(g, GeneType.BYTE);
    long value = get(g.negatives ? g.position : g.position + 1,
                     g.negatives ? g.size : g.size - 1, g.negatives);
    if (g.max != Long.MAX_VALUE) {
      value = (long) (value % (g.max + 1));
    }
    return (byte) value;
  }

  public short getShort(String geneName) {
    Gene g = _dnaSequence.getGene(geneName);
    checkGeneType(g, GeneType.SHORT);
    long value = get(g.negatives ? g.position : g.position + 1,
                     g.negatives ? g.size : g.size - 1, g.negatives);
    if (g.max != Long.MAX_VALUE) {
      value = (long) (value % (g.max + 1));
    }
    return (short) value;
  }

  public int getInt(String geneName) {
    Gene g = _dnaSequence.getGene(geneName);
    checkGeneType(g, GeneType.INTEGER);
    long value = get(g.negatives ? g.position : g.position + 1,
                     g.negatives ? g.size : g.size - 1, g.negatives);
    if (g.max != Long.MAX_VALUE)
      value = (long) (value % (g.max + 1));
    return (int) value;
  }

  public long getLong(String geneName) {
    Gene g = _dnaSequence.getGene(geneName);
    checkGeneType(g, GeneType.LONG);
    long value = get(g.negatives ? g.position : g.position + 1,
                     g.negatives ? g.size : g.size - 1, g.negatives);
    if (g.max != Long.MAX_VALUE) {
      value = (long) (value % (g.max + 1));
    }
    return value;
  }

  public long getNumber(String geneName) {
    Gene g = _dnaSequence.getGene(geneName);
    checkGeneType(g, GeneType.NUMBER);
    long value = get(g.position, g.size, g.negatives);
    if (g.max != Long.MAX_VALUE) {
      value = (long) (value % (g.max + 1));
    }
    return value;
  }

  public float getFloat(String geneName) {
    Gene g = _dnaSequence.getGene(geneName);
    checkGeneType(g, GeneType.FLOAT);
    float f = Float.intBitsToFloat((int) (get(g.position, g.size, true)));
    if (!g.negatives && f < 0) {
      f *= -1; // TODO: this doesn't seem good.
    }
    if (g.max != Long.MAX_VALUE) {
      f = f % g.max;
    }
    return (float) f;
  }

  public double getDouble(String geneName) {
    Gene g = _dnaSequence.getGene(geneName);
    checkGeneType(g, GeneType.DOUBLE);
    double d = Double.longBitsToDouble(((get(g.position, g.size, true))));
    if (!g.negatives && d < 0) {
      d *= -1; // TODO: this doesn't seem good.
    }
    if (g.max != Long.MAX_VALUE) {
      d = d % g.max;
    }
    return d;
  }

  public long get(int position, int numBits, boolean negatives) {
    BitSet bits = _bits.get(position, position + numBits);
    long value = 0;
    long pow = 1;
    for (int x = 0; x < (negatives ? numBits - 1 : numBits); x++) {
      boolean bit = bits.get(numBits - x - 1);
      if (bit) {
        value += pow;
      }
      pow *= 2;
    }

    if (negatives && bits.get(0)) {
      value -= pow;
    }

    return value;
  }

  private void checkGeneType(Gene g, GeneType type) {
    if (g.type != type) {
      throw new GeneTypeException("Gene " + g.name + " is not a "
          + type.name().toLowerCase() + "!");
    }
  }

  public static DnaString mutate(DnaString string, double mutationRate) {
    DnaSequence sequence = string.getDnaSequence();
    boolean altered = false;
    BitSet bits = (BitSet)string.getBitSet().clone();
    for (int x = 0; x < sequence.length(); x++) {
      if (Math.random() < mutationRate) {
        bits.flip(x);
        altered = true;
      }
    }
    return new DnaString(sequence, bits,
      (altered ? MUTATED : string.getSourceType()));
  }

  public static DnaString random(DnaSequence dnaSequence) {
    BitSet bits = new BitSet(dnaSequence.length());
    for (int x = 0; x < dnaSequence.length(); x++) {
      if (Math.random() < 0.5) {
        bits.set(x);
      }
    }
    return new DnaString(dnaSequence, bits, RANDOM);
  }

  public String bitString() {
    StringBuilder sb = new StringBuilder();
    for (int x = 0; x < _dnaSequence.length(); x++) {
      if (_bits.get(x)) {
        sb.append("1");
      } else {
        sb.append("0");
      }
    }
    return sb.toString();
  }

  public String hexString() {
    StringBuilder sb = new StringBuilder();
    sb.append("0x");
    for (int x = 0; x < _dnaSequence.length(); x += 4) {
      int i = 0;
      int bitVal = 8;
      for (int y = 0; y < Math.min(4, _dnaSequence.length() - x); y++) {
        if (_bits.get(x + y)) {
          i += bitVal;
        }
        bitVal /= 2;
      }
      sb.append(Integer.toHexString(i).toUpperCase());
    }
    return sb.toString();
  }

  public String seedString() {
    return "seed.add(new DnaString(dnaSequence, \"" + hexString() + "\", " +
      _sourceType + "));";
  }

  public static BitSet parseString(String numString, int length) {
    if (numString.length() > 2 &&
      numString.substring(0, 2).equals("0x")) {
      return parseHexString(numString, length);
    } else {
      return parseBitString(numString);
    }
  }

  public static BitSet parseBitString(String bitString) {
    BitSet bits = new BitSet(bitString.length());
    char[] bitChars = bitString.toCharArray();
    for (int x = 0; x < bitChars.length; x++) {
      if (bitChars[x] == '1') {
        bits.set(x);
      }
    }
    return bits;
  }

  public static BitSet parseHexString(String hexString, int length) {
    StringBuilder sb = new StringBuilder();
    char[] hexChars = hexString.toCharArray();
    for (int x = 2; x < hexChars.length; x++) {
      int i = Integer.parseInt(Character.toString(hexChars[x]), 16);
      sb.append((i >> 3));
      sb.append(((i >> 2) & 0x1));
      sb.append(((i >> 1) & 0x1));
      sb.append((i & 0x1));
    }

    while (sb.length() > length) {
      sb.deleteCharAt(sb.length()-1);
    }

    return parseBitString(sb.toString());
  }

  public Object clone() {
    return new DnaString(_dnaSequence, _bits, _sourceType);
  }

  public boolean equals(Object o) {
    if (!(o instanceof DnaString)) {
      return false;
    }

    DnaString that = (DnaString)o;

    if (this.getDnaSequence() != that.getDnaSequence()) {
      return false;
    }

    if (this.getBitSet().equals(that.getBitSet())) {
      return true;
    }

    boolean same = true;
    ArrayList<Gene> geneLayout = getDnaSequence().getGeneLayout();
    for (Gene g : geneLayout) {
      switch (g.type) {
        case BIT:
          same = same && (this.getBit(g.name) == that.getBit(g.name));
          break;
        case BYTE:
          same = same && (this.getByte(g.name) == that.getByte(g.name));
          break;
        case SHORT:
          same = same && (this.getShort(g.name) == that.getShort(g.name));
          break;
        case INTEGER:
          same = same && (this.getInt(g.name) == that.getInt(g.name));
          break;
        case LONG:
          same = same && (this.getLong(g.name) == that.getLong(g.name));
          break;
        case FLOAT:
          same = same && (Math.abs(this.getFloat(g.name)
              - that.getFloat(g.name)) < 0.0000001);
          break;
        case DOUBLE:
          same = same && (Math.abs(this.getDouble(g.name)
              - that.getDouble(g.name)) < 0.00000000001);
        case NUMBER:
          same = same && (this.getNumber(g.name) == that.getNumber(g.name));
          break;
      }

      if (!same) {
        break;
      }
    }

    return same;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    ArrayList<Gene> geneLayout = _dnaSequence.getGeneLayout();
    boolean newLine = false;
    for (Gene g : geneLayout) {
      if (newLine) {
        sb.append("\n");
      }
      newLine = true;

      sb.append(g.name);
      sb.append(": ");
      switch (g.type) {
        case BIT:
          sb.append(getBit(g.name)); break;
        case BYTE:
          sb.append(getByte(g.name)); break;
        case SHORT:
          sb.append(getShort(g.name)); break;
        case INTEGER:
          sb.append(getInt(g.name)); break;
        case LONG:
          sb.append(getLong(g.name)); break;
        case FLOAT:
          sb.append(getFloat(g.name)); break;
        case DOUBLE:
          sb.append(getDouble(g.name)); break;
      }
    }

    return sb.toString();
  }

  public class GeneTypeException extends RuntimeException {
    private static final long serialVersionUID = -881296837849502009L;
    public GeneTypeException(String message) {
      super(message);
    }
  }
}
