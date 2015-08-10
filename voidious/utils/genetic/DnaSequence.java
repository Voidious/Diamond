package voidious.utils.genetic;

import java.util.ArrayList;
import java.util.HashMap;

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

public class DnaSequence {
  protected ArrayList<Gene> _geneLayout;
  protected HashMap<String, Gene> _geneMap;
  protected int _length = 0;

  public DnaSequence(ArrayList<Gene> genes) {
    _geneLayout = new ArrayList<Gene>();
    _geneMap = new HashMap<String, Gene>();

    if (genes != null) {
      for (Gene g : genes) {
        addGene((Gene)g.clone());
      }
    }
  }

  public DnaSequence() {
    this(null);
  }

  public void addGene(Gene gene) {
    // TODO: handle existing Gene with this name somehow
    //       replace it? throw exception? overwrite it? delete/clear Genes?
    _geneMap.put(gene.name, gene);
    _geneLayout.add(gene);
    gene.position = _length;
    _length += gene.size;
  }

  public Gene getGene(String name) {
    return _geneMap.get(name);
  }

  public int length() {
    return _length;
  }

  public ArrayList<Gene> getGeneLayout() {
    return _geneLayout;
  }

  public static class Gene implements Cloneable {
    public final String name;
    public final GeneType type;
    public final long max;
    public final boolean negatives;
    public final int size;
    public int position;

    public Gene(String name, GeneType type, long max, boolean negatives) {
      this(name, type, max, negatives, type.getSize());
    }

    public Gene(String name, GeneType type, long max,
        boolean negatives, int size) {
      if (type == GeneType.NUMBER && size == 0) {
        throw new IllegalArgumentException(
            "Must specify a size for GeneType.NUMBER!");
      }
      this.name = new String(name);
      this.type = type;
      this.max = max;
      this.negatives = negatives;
      this.size = size;
      position = 0;
    }

    public Gene(String name, GeneType type) {
      this(name, type, Long.MAX_VALUE);
    }

    public Gene(String name, GeneType type, long max) {
      this(name, type, max, false);
    }

    public Gene(String name, GeneType type, boolean negatives) {
      this(name, type, Long.MAX_VALUE, negatives);
    }

    public Gene(String name, int numBits) {
      this(name, numBits, Long.MAX_VALUE);
    }

    public Gene(String name, int numBits, long max) {
      this(name, GeneType.NUMBER, max, false, numBits);
    }

    public Object clone() {
      return new Gene(name, type, max, negatives);
    }

    public boolean equals(Object o) {
      if (!(o instanceof Gene)) {
        return false;
      }

      Gene that = (Gene)o;

      if (this.name.equals(that.name) && this.type == that.type
          && this.position == that.position && this.max == that.max
          && this.negatives == that.negatives
          && this.size == that.size) {
        return true;
      } else {
        return false;
      }
    }
  }

  public enum GeneType {
    BIT(1),
    BYTE(8),
    SHORT(16),
    INTEGER(32),
    LONG(64),
    FLOAT(32),
    DOUBLE(64),
    NUMBER(0);

    private int _size;

    private GeneType(int size) {
      _size = size;
    }

    public int getSize() {
      return _size;
    }
  }
}
