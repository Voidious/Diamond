package voidious.gun;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import robocode.util.Utils;
import voidious.gfx.ColoredValueSet;
import voidious.gfx.RoboGraphic;
import voidious.gun.formulas.TripHammerFormula;
import voidious.utils.DiaUtils;
import voidious.utils.DistanceFormula;
import voidious.utils.KnnView;
import voidious.utils.TimestampedFiringAngle;
import voidious.utils.Wave;
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

public class TripHammerKnnGun extends DuelGun<TimestampedFiringAngle> {
  private static final String VIEW_NAME = "TripHammerKNN";
  private static final int FIRING_ANGLES = 59;
  private static final int MAX_K_SIZE = 225;
  private static final int K_DIVISOR = 9;

  private GunDataManager _gunDataManager;
  private Collection<RoboGraphic> _renderables;
  private int _numFiringAngles;
  private int _maxK;
  private int _kDivisor;
  private DistanceFormula _formula;

  private static final double[] INITIAL_WEIGHTS = new double[]
      {0.94, 10.0, 1.73, 3.7, 3.31, 2.13, 5.51, 1.26, 1.57, 5.51};
  private static final double[] FINAL_WEIGHTS = new double[]
      {4.25, 5.43, 0.16, 4.25, 8.74, 3.39, 4.41, 8.03, 7.24, 4.41};
  private static final int[] FINAL_TIMES = new int[]
      {28920, 23040, 23100, 1740, 16680, 5580, 0, 11280, 21420, 1920};

  public TripHammerKnnGun(GunDataManager gunDataManager,
      Collection<RoboGraphic> renderables) {
    this(gunDataManager, renderables, FIRING_ANGLES, MAX_K_SIZE, K_DIVISOR);
  }

  protected TripHammerKnnGun(GunDataManager gunDataManager,
      Collection<RoboGraphic> renderables,
      int numFiringAngles, int maxK, int kDivisor) {
    super();
    _gunDataManager = gunDataManager;
    _renderables = renderables;
    _numFiringAngles = numFiringAngles;
    _maxK = maxK;
    _kDivisor = kDivisor;
    _formula = new TripHammerFormula();
  }

  @Override
  public String getLabel() {
    return "Main Gun";
  }

  @Override
  protected double aimInternal(Wave w, boolean painting) {
    GunEnemy enemyData = _gunDataManager.getEnemyData(w.botName);
    KnnView<TimestampedFiringAngle> view = enemyData.views.get(VIEW_NAME);
    int viewSize = view.size();
    if (enemyData == null || viewSize == 0) {
      return w.absBearing;
    }

    view.setWeights(getWeights(view.size()));
    List<Entry<TimestampedFiringAngle>> nearestNeighbors =
        view.nearestNeighbors(w, true);
    int numScans = nearestNeighbors.size();
    Double[] firingAngles = new Double[numScans];
    double[] weights = new double[numScans];
    for (int x = 0; x < numScans; x++) {
      double guessFactor = nearestNeighbors.get(x).value.guessFactor;
      firingAngles[x] = Utils.normalRelativeAngle(
          (guessFactor * w.orbitDirection
              * w.preciseEscapeAngle(guessFactor >= 0)));
      weights[x] = 1 / Math.sqrt(nearestNeighbors.get(x).distance);
    }

    double bandwidth = 2 * DiaUtils.botWidthAimAngle(
        w.sourceLocation.distance(w.targetLocation));
    Double bestAngle = null;
    double bestDensity = Double.NEGATIVE_INFINITY;
    ColoredValueSet cvs = new ColoredValueSet();
    double[] realAngles =
        DiaUtils.generateFiringAngles(_numFiringAngles, w.maxEscapeAngle());

    for (int x = 0; x < _numFiringAngles; x++) {
      double density = 0;
      for (int y = 0; y < numScans; y++) {
        if (firingAngles[y] == null) {
          continue;
        }
        double ux = (realAngles[x] - firingAngles[y]) / bandwidth;
        if (Math.abs(ux) < 1) {
          density += (1 - DiaUtils.cube(Math.abs(ux))) * weights[y];
        }
      }

      if (density > bestDensity) {
        bestAngle = realAngles[x];
        bestDensity = density;
      }

      if (painting) {
        cvs.addValue(density, w.absBearing + realAngles[x]);
      }
    }

    if (bestAngle == null) {
      return w.absBearing;
    }

    if (painting) {
      DiamondFist.drawGunAngles(
          _renderables, w, cvs, w.absBearing + bestAngle, bandwidth);
    }

    return Utils.normalAbsoluteAngle(w.absBearing + bestAngle);
  }

  private double[] getWeights(int viewSize) {
    double[] newWeights = new double[INITIAL_WEIGHTS.length];
    for (int x = 0; x < newWeights.length; x++) {
      newWeights[x] = INITIAL_WEIGHTS[x]
          + (FINAL_TIMES[x] == 0
              ? 1 : Math.min(1, ((double) (viewSize - 1)) / FINAL_TIMES[x]))
          * (FINAL_WEIGHTS[x] - INITIAL_WEIGHTS[x]);
    }
    return newWeights;
  }

  @Override
  public List<KnnView<TimestampedFiringAngle>> newDataViews() {
    List<KnnView<TimestampedFiringAngle>> views =
        new ArrayList<KnnView<TimestampedFiringAngle>>();
    KnnView<TimestampedFiringAngle> view =
        new KnnView<TimestampedFiringAngle>(_formula)
            .setK(_maxK)
            .setKDivisor(_kDivisor)
            .visitsOn()
            .virtualWavesOn()
            .setName(VIEW_NAME);
    views.add(view);
    return views;
  }
}
