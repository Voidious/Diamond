package voidious.gun;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import robocode.util.Utils;
import voidious.gfx.ColoredValueSet;
import voidious.gfx.RoboGraphic;
import voidious.gun.formulas.MainGunFormula;
import voidious.utils.BattleField;
import voidious.utils.DiaUtils;
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

public class MainGun extends DuelGun<TimestampedFiringAngle> {
  private static final String VIEW_NAME = "Main";
  private static final int K_SIZE = 100;
  private static final int K_DIVISOR = 10;

  private GunDataManager _gunDataManager;
  private BattleField _battleField;
  private Collection<RoboGraphic> _renderables;

  public MainGun(GunDataManager gunDataManager, BattleField battleField,
      Collection<RoboGraphic> renderables) {
    super();
    _gunDataManager = gunDataManager;
    _battleField = battleField;
    _renderables = renderables;
  }

  @Override
  public String getLabel() {
    return "Main Gun";
  }

  @Override
  protected double aimInternal(Wave w, boolean painting) {
    GunEnemy gunData = _gunDataManager.getEnemyData(w.botName);
    KnnView<TimestampedFiringAngle> view = gunData.views.get(VIEW_NAME);
    if (view.size() == 0) {
      return w.absBearing;
    }

    List<Entry<TimestampedFiringAngle>> nearestNeighbors =
        view.nearestNeighbors(w, true);
    int numScans = nearestNeighbors.size();
    Double[] firingAngles = new Double[numScans];
    for (int x = 0; x < numScans; x++) {
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
   
    Double bestAngle = null;
    double bestDensity = Double.NEGATIVE_INFINITY;
    double bandwidth = 2 * DiaUtils.botWidthAimAngle(
        w.sourceLocation.distance(w.targetLocation));
    ColoredValueSet cvs = new ColoredValueSet();

    for (int x = 0; x < numScans; x++) {
      if (firingAngles[x] == null) {
        continue;
      }

      double xFiringAngle = firingAngles[x];
      double xDensity = 0;
      for (int y = 0; y < numScans; y++) {
        if (x == y || firingAngles[y] == null) {
          continue;
        }

        // TODO: weight by inverse distance   
        double yFiringAngle = firingAngles[y];
        double ux = (xFiringAngle - yFiringAngle) / bandwidth;
        xDensity += Math.exp(-0.5 * ux * ux);
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
    List<KnnView<TimestampedFiringAngle>> views =
        new ArrayList<KnnView<TimestampedFiringAngle>>();
    views.add(new KnnView<TimestampedFiringAngle>(
        new MainGunFormula(_gunDataManager.getEnemiesTotal()))
            .setK(K_SIZE)
            .setKDivisor(K_DIVISOR)
            .visitsOn()
            .virtualWavesOn()
            .meleeOn()
            .setName(VIEW_NAME));
    return views;
  }
}
