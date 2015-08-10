package voidious.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ags.utils.KdTree;
import ags.utils.KdTree.Entry;

/**
 * Copyright (c) 2009-2012 - Voidious
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

public class KnnView<T> {
  private static int _nameIndex = 0;
  public double weight;
  public DistanceFormula formula;
  public int kSize;
  public int kDivisor;
  public int maxDataPoints;
  public boolean logBulletHits;
  public boolean logVisits;
  public boolean logVirtual;
  public boolean logMelee;
  public double hitThreshold;
  public double paddedHitThreshold;
  public double decayRate;
  public String name;

  private KdTree.WeightedSqrEuclid<T> _tree;
  public Map<Integer, List<KdTree.Entry<T>>> cachedNeighbors;

  public static final double NO_DECAY = 0;

  public KnnView(DistanceFormula formula) {
    this.formula = formula;
    weight = 1;
    kSize = 1;
    kDivisor = 1;
    logBulletHits = false;
    logVisits = false;
    logVirtual = false;
    logMelee = false;
    hitThreshold = 0;
    paddedHitThreshold = 0;
    maxDataPoints = 0;
    decayRate = NO_DECAY;
    name = (new Long(Math.round(Math.random() * 10000000))).toString()
        + "-" + _nameIndex++;
    initTree();
    cachedNeighbors = new HashMap<Integer, List<KdTree.Entry<T>>>();
  }

  private void initTree() {
    _tree = new KdTree.WeightedSqrEuclid<T>(
        formula.weights.length, maxDataPoints == 0 ? null : maxDataPoints);
    _tree.setWeights(formula.weights);
  }

  public KnnView<T> setFormula(DistanceFormula formula) {
    this.formula = formula;
    initTree();
    return this;
  }

  public KnnView<T> setWeight(double weight) {
    this.weight = weight;
    return this;
  }

  public KnnView<T> setK(int kSize) {
    this.kSize = kSize;
    return this;
  }

  public KnnView<T> setKDivisor(int kDivisor) {
    this.kDivisor = kDivisor;
    return this;
  }

  public KnnView<T> bulletHitsOn() {
    logBulletHits = true;
    return this;
  }

  public KnnView<T> visitsOn() {
    logVisits = true;
    return this;
  }

  public KnnView<T> virtualWavesOn() {
    logVirtual = true;
    return this;
  }

  public KnnView<T> meleeOn() {
    logMelee = true;
    return this;
  }

  public KnnView<T> setHitThreshold(double hitThreshold) {
    this.hitThreshold = hitThreshold;
    return this;
  }

  public KnnView<T> setPaddedHitThreshold(double paddedHitThreshold) {
    this.paddedHitThreshold = paddedHitThreshold;
    return this;
  }

  public KnnView<T> setMaxDataPoints(int maxDataPoints) {
    this.maxDataPoints = maxDataPoints;
    initTree();
    return this;
  }

  public KnnView<T> setDecayRate(double decayRate) {
    this.decayRate = decayRate;
    return this;
  }

  public KnnView<T> setName(String name) {
    this.name = name;
    return this;
  }

  public double[] logWave(Wave w, T value) {
    double[] dataPoint = formula.dataPointFromWave(w);
    return logDataPoint(dataPoint, value);
  }

  protected double[] logDataPoint(double[] dataPoint, T value) {
    _tree.addPoint(dataPoint, value);
    return dataPoint;
  }

  public void clearCache() {
    cachedNeighbors.clear();
  }

  public boolean enabled(double hitPercentage, double marginOfError) {
    return (size() > 0 && (hitPercentage >= hitThreshold)
        && (Math.max(0, hitPercentage - marginOfError) >= paddedHitThreshold));
  }

  public int size() {
    return _tree.size();
  }

  public List<Entry<T>> nearestNeighbors(Wave w, boolean aiming) {
    return nearestNeighbors(
        w, aiming, DiaUtils.limit(1, size() / kDivisor, kSize));
  }

  public List<KdTree.Entry<T>> nearestNeighbors(Wave w, boolean aiming, int k) {
    double[] wavePoint = formula.dataPointFromWave(w, aiming);
    return _tree.nearestNeighbor(wavePoint, k, false);
  }

  public void setWeights(double[] weights) {
    formula.weights = weights;
    _tree.setWeights(weights);
  }

  public Map<Timestamped, Double> getDecayWeights(
      List<? extends KdTree.Entry<? extends Timestamped>> timestampedEntries) {
    Map<Timestamped, Double> weightMap = new HashMap<Timestamped, Double>();
    int numScans = timestampedEntries.size();
    if (decayRate == KnnView.NO_DECAY) {
      for (KdTree.Entry<? extends Timestamped> entry : timestampedEntries) {
        weightMap.put(entry.value, 1.0);
      }
    } else {
      Timestamped[] sorted = new Timestamped[numScans];
      for (int x = 0; x < numScans; x++) {
        sorted[x] = timestampedEntries.get(x).value;
      }
      Arrays.sort(sorted);
      for (int x = 0; x < numScans; x++) {
        weightMap.put(sorted[x],
            1.0 / DiaUtils.power(decayRate, numScans - x - 1));
      }
    }
    return weightMap;
  }
}
