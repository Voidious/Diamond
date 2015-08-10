package voidious.gun;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import robocode.util.Utils;
import voidious.gfx.ColoredValueSet;
import voidious.gfx.RoboGraphic;
import voidious.gun.formulas.MeleeFormula;
import voidious.utils.BattleField;
import voidious.utils.DiaUtils;
import voidious.utils.KnnView;
import voidious.utils.TimestampedFiringAngle;
import voidious.utils.Wave;
import ags.utils.KdTree.Entry;

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

public class MeleeGun {
  private static final String VIEW_NAME = "Melee";
  private static final int MAX_SCANS = 100;

  private GunDataManager _gunDataManager;
  private BattleField _battleField;
  private Collection<RoboGraphic> _renderables;

  public MeleeGun(GunDataManager gunDataManager, BattleField battleField,
      Collection<RoboGraphic> renderables) {
    _gunDataManager = gunDataManager;
    _battleField = battleField;
    _renderables = renderables;
  }

  public double aimAtEveryone(Point2D.Double myNextLocation,
      long currentTime, int enemiesAlive, double bulletPower,
      GunEnemy closestBot, boolean painting) {
    List<MeleeFiringAngle> firingAngles = new ArrayList<MeleeFiringAngle>();

    int kSize = getCommonKsize(enemiesAlive);
    for (GunEnemy gunData : _gunDataManager.getAllEnemyData()) {
      if (gunData.alive && gunData.views.get(VIEW_NAME).size() >= 10
          && gunData.lastWaveFired != null) {
        List<MeleeFiringAngle> enemyAngles = new ArrayList<MeleeFiringAngle>();
        Wave aimWave = gunData.lastWaveFired;
        aimWave.setBulletPower(bulletPower);
        KnnView<TimestampedFiringAngle> view = gunData.views.get(VIEW_NAME);
        List<Entry<TimestampedFiringAngle>> nearestNeighbors =
            view.nearestNeighbors(aimWave, true, kSize);

        int numScans = nearestNeighbors.size();
        double totalScanWeight = 0;
        for (int x = 0; x < numScans; x++) {
          Entry<TimestampedFiringAngle> entry = nearestNeighbors.get(x);
          double scanWeight = 1 / Math.sqrt(entry.distance);
          totalScanWeight += scanWeight;
          Point2D.Double vector = entry.value.displacementVector;
          MeleeFiringAngle firingAngle = getFiringAngle(
              myNextLocation, currentTime, vector, scanWeight, aimWave);
          if (firingAngle != null) {
            enemyAngles.add(firingAngle);
          }
        }
        for (MeleeFiringAngle enemyAngle : enemyAngles) {
          enemyAngle.scanWeight /= totalScanWeight;
        }
        firingAngles.addAll(enemyAngles);
      }
    }

    Double bestAngle = null;
    double bestDensity = Double.NEGATIVE_INFINITY;
    Wave bestWave = null;
    ColoredValueSet cvs = new ColoredValueSet();

    for (MeleeFiringAngle xFiringAngle : firingAngles) {
      double xDensity = 0;
      for (MeleeFiringAngle yFiringAngle : firingAngles) {
        double ux =
            Utils.normalRelativeAngle(xFiringAngle.angle - yFiringAngle.angle)
                / yFiringAngle.bandwidth;
        xDensity += yFiringAngle.scanWeight * Math.exp(-0.5 * ux * ux)
            / yFiringAngle.distance;
      }

      if (xDensity > bestDensity) {
        bestAngle = xFiringAngle.angle;
        bestDensity = xDensity;
        bestWave = xFiringAngle.wave;
      }

      if (painting) {
        cvs.addValue(xDensity, xFiringAngle.angle);
      }
    }

    if (firingAngles.isEmpty() || bestAngle == null) {
      return closestBot.lastWaveFired.absBearing;
    }

    if (painting) {
      double bandwidth = DiaUtils.botWidthAimAngle(
          myNextLocation.distance(closestBot.lastScanState.location));
      DiamondFist.drawGunAngles(
          _renderables, bestWave, cvs, bestAngle, bandwidth);
    }

    return Utils.normalAbsoluteAngle(bestAngle);
  }

  private int getCommonKsize(int enemiesAlive) {
    int kSize = MAX_SCANS / enemiesAlive;
    for (GunEnemy gunData : _gunDataManager.getAllEnemyData()) {
      if (gunData.alive && gunData.views.get(VIEW_NAME).size() >= 10
          && gunData.lastWaveFired != null) {
        kSize = Math.min(kSize, gunData.views.get(VIEW_NAME).size() / 10);
      }
    }
    return kSize;
  }

  private MeleeFiringAngle getFiringAngle(Point2D.Double myNextLocation,
      long currentTime, Point2D.Double dispVector, double scanWeight,
      Wave aimWave) {
    Point2D.Double projectedLocation = aimWave.projectLocationBlind(
        myNextLocation, dispVector, currentTime);
    if (_battleField.rectangle.contains(projectedLocation)) {
      double distance = myNextLocation.distance(projectedLocation);
      return new MeleeFiringAngle(
          DiaUtils.absoluteBearing(myNextLocation, projectedLocation),
          distance, DiaUtils.botWidthAimAngle(distance), scanWeight,
          aimWave);
    }
    return null;
  }

  public List<KnnView<TimestampedFiringAngle>> newDataViews() {
    List<KnnView<TimestampedFiringAngle>> views =
        new ArrayList<KnnView<TimestampedFiringAngle>>();
    KnnView<TimestampedFiringAngle> meleeView =
        new KnnView<TimestampedFiringAngle>(new MeleeFormula())
            .visitsOn()
            .virtualWavesOn()
            .meleeOn()
            .setName(VIEW_NAME);
    views.add(meleeView);
    return views;
  }

  private static class MeleeFiringAngle {
    public final double angle;
    public final double distance;
    public final double bandwidth;
    public double scanWeight;
    public final Wave wave;

    public MeleeFiringAngle(double angle, double distance, double bandwidth,
        double scanWeight, Wave wave) {
      this.angle = angle;
      this.distance = distance;
      this.bandwidth = bandwidth;
      this.scanWeight = scanWeight;
      this.wave = wave;
    }
  }
}
