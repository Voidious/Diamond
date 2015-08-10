package voidious.gun;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import robocode.util.Utils;
import voidious.gfx.ColoredValueSet;
import voidious.gfx.RoboGraphic;
import voidious.gun.formulas.AntiSurferFormula;
import voidious.utils.BattleField;
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

public class AntiSurferGun extends DuelGun<TimestampedFiringAngle> {
  private static final int FIRING_ANGLES = 59;

  private final GunDataManager _gunDataManager;
  private BattleField _battleField;
  private Collection<RoboGraphic> _renderables;
  private List<String> _viewNames;
  private DistanceFormula _formula;
  private boolean _is1v1Battle;

  public AntiSurferGun(GunDataManager gunDataManager, BattleField battleField,
      Collection<RoboGraphic> renderables) {
    super();
    _gunDataManager = gunDataManager;
    _battleField = battleField;
    _renderables = renderables;
    _viewNames = new ArrayList<String>();
    _formula = new AntiSurferFormula();
    _is1v1Battle = (_gunDataManager.getEnemiesTotal() == 1);
  }

  @Override
  public String getLabel() {
    return "Anti-Surfer Gun";
  }

  @Override
  protected double aimInternal(Wave w, boolean painting) {
    GunEnemy gunData = _gunDataManager.getEnemyData(w.botName);
    List<Entry<TimestampedFiringAngle>> nearestNeighbors = null;
    double[] neighborWeights = null;
    for (String viewName : _viewNames) {
      KnnView<TimestampedFiringAngle> view = gunData.views.get(viewName);
      if (view.size() < view.kDivisor) {
        continue;
      }

      List<Entry<TimestampedFiringAngle>> thisNeighbors =
          view.nearestNeighbors(w, true);
      double[] thisWeights = new double[thisNeighbors.size()];
      Arrays.fill(thisWeights, view.weight);

      if (nearestNeighbors == null) {
        nearestNeighbors = thisNeighbors;
        neighborWeights = thisWeights;
      } else {
        nearestNeighbors.addAll(thisNeighbors);
        int newIndex = neighborWeights.length;
        neighborWeights =
            Arrays.copyOf(neighborWeights, nearestNeighbors.size());
        for (int x = newIndex; x < neighborWeights.length; x++) {
          neighborWeights[x] = thisWeights[x - newIndex];
        }
      }
    }

    if (nearestNeighbors == null || nearestNeighbors.size() == 0) {
      return w.absBearing;
    }

    int numScans = nearestNeighbors.size();
    Double[] firingAngles = new Double[numScans];
    for (int x = 0; x < numScans; x++) {
      if (_is1v1Battle) {
        double guessFactor = nearestNeighbors.get(x).value.guessFactor;
        firingAngles[x] = Utils.normalRelativeAngle(
            (guessFactor * w.orbitDirection
                * w.preciseEscapeAngle(guessFactor >= 0)));
      } else {
        Point2D.Double dispVector =
            nearestNeighbors.get(x).value.displacementVector;
        Point2D.Double projectedLocation =
            w.projectLocationFromDisplacementVector(dispVector);
        if (!_battleField.rectangle.contains(projectedLocation)) {
          firingAngles[x] = null;
        } else {
          firingAngles[x] = Utils.normalRelativeAngle(
              w.firingAngleFromTargetLocation(projectedLocation) 
              - w.absBearing);
        }
      }
    }

    Double bestAngle = null;
    double bestDensity = Double.NEGATIVE_INFINITY;
    double bandwidth = DiaUtils.botWidthAimAngle(
        w.sourceLocation.distance(w.targetLocation)) * 2;
    ColoredValueSet cvs = new ColoredValueSet();
    double[] realAngles =
        DiaUtils.generateFiringAngles(FIRING_ANGLES, w.maxEscapeAngle());
    for (int x = 0; x < FIRING_ANGLES; x++) {
      double xFiringAngle = realAngles[x];

      double xDensity = 0;
      for (int y = 0; y < numScans; y++) {
        if (firingAngles[y] == null) {
          continue;
        }

        double yFiringAngle = firingAngles[y];
        double ux = (xFiringAngle - yFiringAngle) / bandwidth;
        xDensity += Math.exp(-0.5 * ux * ux) * neighborWeights[y];
      }

      if (xDensity > bestDensity) {
        bestAngle = xFiringAngle;
        bestDensity = xDensity;
      }

      if (painting) {
        cvs.addValue(xDensity, w.absBearing + xFiringAngle);
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

  @Override
  public List<KnnView<TimestampedFiringAngle>> newDataViews() {
    KnnView<TimestampedFiringAngle> asView =
        new KnnView<TimestampedFiringAngle>(_formula)
            .setK(3)
            .setMaxDataPoints(125)
            .setKDivisor(10)
            .visitsOn()
            .virtualWavesOn()
            .setName("asView1");
    KnnView<TimestampedFiringAngle> asView2 =
        new KnnView<TimestampedFiringAngle>(_formula)
            .setK(3)
            .setMaxDataPoints(400)
            .setKDivisor(10)
            .visitsOn()
            .virtualWavesOn()
            .setName("asView2");
    KnnView<TimestampedFiringAngle> asView3 =
        new KnnView<TimestampedFiringAngle>(_formula)
            .setK(3)
            .setMaxDataPoints(1500)
            .setKDivisor(10)
            .visitsOn()
            .virtualWavesOn()
            .setName("asView3");
    KnnView<TimestampedFiringAngle> asView4 =
        new KnnView<TimestampedFiringAngle>(_formula)
            .setK(3)
            .setMaxDataPoints(4000)
            .setKDivisor(10)
            .visitsOn()
            .virtualWavesOn()
            .setName("asView4");
    List<KnnView<TimestampedFiringAngle>> views =
        new ArrayList<KnnView<TimestampedFiringAngle>>();
    views.add(asView);
    views.add(asView2);
    views.add(asView3);
    views.add(asView4);

    for (KnnView<TimestampedFiringAngle> view : views) {
      if (!_viewNames.contains(view.name)) {
        _viewNames.add(view.name);
      }
    }

    return views;
  }
}
