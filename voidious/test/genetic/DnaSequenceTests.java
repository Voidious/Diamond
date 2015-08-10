package voidious.test.genetic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import voidious.utils.genetic.DnaSequence;
import voidious.utils.genetic.DnaSequence.Gene;
import voidious.utils.genetic.DnaSequence.GeneType;

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

public class DnaSequenceTests {
  @Test
  public void testDnaSequenceConstructors() {
    DnaSequence seq = new DnaSequence();

    assertTrue("DnaSequence default constructor didn't instantiate.",
      (seq instanceof DnaSequence));

    ArrayList<Gene> geneList = new ArrayList<Gene>();
    seq = new DnaSequence(geneList);

    assertTrue("DnaSequence(ArrayList<Gene> [empty]) didn't instantiate.",
        (seq instanceof DnaSequence));

    geneList.add(new Gene("hair", GeneType.BYTE));
    seq = new DnaSequence(geneList);

    assertTrue("DnaSequence(ArrayList<Gene>) didn't instantiate.",
        (seq instanceof DnaSequence));
  }

  @Test
  public void testAddGetGenes() {
    // TODO: test adding duplicate gene names
    Gene someGene = new Gene("eyes", GeneType.BYTE);
    ArrayList<Gene> geneLayout = new ArrayList<Gene>();
    geneLayout.add(someGene);

    DnaSequence seq = new DnaSequence(geneLayout);
    assertTrue("DnaSequence.getGeneLayout() returned different layout " +
      "than passed to constructor.",
      seq.getGeneLayout().equals(geneLayout));
    testGetGene(seq, someGene, "passing Gene layout via constructor");

    seq = new DnaSequence();
    seq.addGene(someGene);
    assertTrue("DnaSequence.addGene(...) didn't add to Gene layout.",
      seq.getGeneLayout().contains(someGene));
    testGetGene(seq, someGene, "adding Gene via addGene(...)");

    assertTrue("DnaSequence.getGene(...) didn't return null for unknown " +
      "Gene.", seq.getGene("hair") == null);
  }

  private void testGetGene(DnaSequence seq, Gene g, String howAdded) {
    assertTrue("DnaSequence.getGene(...) was null after " + howAdded + ".",
      seq.getGene("eyes") != null);
    assertTrue("DnaSequence.getGene(...) was wrong Gene after " + howAdded + ".",
      seq.getGene("eyes").equals(g));
  }

  @Test
  public void testLength() {
    List<GeneType> geneTypes = GenTestUtils.getGeneTypes();

    for (int x = 0; x < 1000; x++) {
      DnaSequence seq = new DnaSequence();
      int numGenes = (int) (Math.random() * 100);
      int length = 0;
      for (int y = 0; y < numGenes; y++) {
        int geneIndex = (int) (Math.random() * geneTypes.size());
        GeneType geneType = geneTypes.get(geneIndex);
        seq.addGene(new Gene(GenTestUtils.randomString(), geneType));
        length += geneType.getSize();
      }
      assertEquals("DnaSequence length was not correct for this set of " +
        "Genes.", length, seq.length());
    }
  }

  @Test
  public void testGeneConstructors() {
    Gene g = new Gene("a", GeneType.BIT);
    assertTrue("Gene(string, int) constructor set incorrect name.",
      g.name.equals("a"));
    assertTrue("Gene(string, int) constructor set incorrect type.",
      g.type == GeneType.BIT);

    g = new Gene("a", GeneType.BIT, true);
    assertTrue("Gene(string, int, boolean) constructor set incorrect name.",
      g.name.equals("a"));
    assertTrue("Gene(string, int, boolean) constructor set incorrect type.",
      g.type == GeneType.BIT);
    assertTrue("Gene(string, int, boolean) constructor set incorrect negatives.",
      g.negatives == true);
    g = new Gene("a", GeneType.BIT, false);
    assertTrue("Gene(string, int, boolean) constructor set incorrect negatives.",
      g.negatives == false);

    g = new Gene("a", GeneType.BIT, 50L, true);
    assertTrue("Gene(string, int, long, boolean) constructor set incorrect name.",
      g.name.equals("a"));
    assertTrue("Gene(string, int, long, boolean) constructor set incorrect type.",
      g.type == GeneType.BIT);
    assertTrue("Gene(string, int, long, boolean) constructor set incorrect max.",
      g.max == 50L);
    assertTrue("Gene(string, int, long, boolean) constructor set incorrect negatives.",
      g.negatives == true);
    g = new Gene("a", GeneType.BIT, 50L, false);
    assertTrue("Gene(string, int, long, boolean) constructor set incorrect negatives.",
      g.negatives == false);

    g = new Gene("a", GeneType.BIT, 50L);
    assertTrue("Gene(string, int, long) constructor set incorrect name.",
      g.name.equals("a"));
    assertTrue("Gene(string, int, long) constructor set incorrect type.",
      g.type == GeneType.BIT);
    assertTrue("Gene(string, int, long) constructor set incorrect max.",
      g.max == 50L);

    g = new Gene("a", GeneType.BIT, 100L);
    assertTrue("Gene(string, int, long, long) constructor set incorrect name.",
      g.name.equals("a"));
    assertTrue("Gene(string, int, long, long) constructor set incorrect type.",
      g.type == GeneType.BIT);
    assertTrue("Gene(string, int, long, long) constructor set incorrect max.",
      g.max == 100L);

    g = new Gene("a", GeneType.BIT, 250000001L, true);
    assertTrue("Gene(string, int, long, long, long, boolean) constructor set incorrect name.",
      g.name.equals("a"));
    assertTrue("Gene(string, int, long, long, boolean) constructor set incorrect type.",
      g.type == GeneType.BIT);
    assertTrue("Gene(string, int, long, long, boolean) constructor set incorrect max.",
      g.max == 250000001L);
    assertTrue("Gene(string, int, long, long, boolean) constructor set incorrect negatives.",
      g.negatives == true);
    g = new Gene("a", GeneType.BIT, 250000001L, false);
    assertTrue("Gene(string, int, long, long, boolean) constructor set incorrect negatives.",
      g.negatives == false);

  }

  @Test
  public void testGeneClone() {
    Gene g = new Gene("a", GeneType.BIT, 250000001L, false);
    Gene clone = (Gene)g.clone();

    assertEquals("Gene.clone() had incorrect name.", g.name, clone.name);
    assertEquals("Gene.clone() had incorrect type.", g.type, clone.type);
    assertEquals("Gene.clone() had incorrect max.", g.max, clone.max);
    assertEquals("Gene.clone() had incorrect negatives.",
        g.negatives, clone.negatives);
  }

  public void testGeneEquals() {
    Gene g1 = new Gene("a", GeneType.BIT, 250000001L, false);
    Gene g2 = new Gene("a", GeneType.BIT, 250000001L, false);
    assertTrue("Gene did not equal an identical gene.", g1.equals(g2));

    g2 = new Gene("b", GeneType.BIT, 250000001L, false);
    assertFalse("Gene equal to gene with different name.", g1.equals(g2));

    g2 = new Gene("a", GeneType.SHORT, 250000001L, false);
    assertFalse("Gene equal to gene with different type.", g1.equals(g2));

    g2 = new Gene("a", GeneType.BIT, 250000001L, false);
    assertFalse("Gene equal to gene with different min.", g1.equals(g2));

    g2 = new Gene("a", GeneType.BIT, 99L, false);
    assertFalse("Gene equal to gene with different max.", g1.equals(g2));

    g2 = new Gene("a", GeneType.BIT, 250000001L, true);
    assertFalse("Gene equal to gene with different negatives.", g1.equals(g2));
  }
}
