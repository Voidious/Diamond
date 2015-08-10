package voidious.gun;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import robocode.Bullet;
import voidious.gfx.RoboGraphic;
import voidious.gun.GunDataManager.GunDataListener;
import voidious.utils.BattleField;
import voidious.utils.DiaUtils;
import voidious.utils.Enemy;
import voidious.utils.KnnView;
import voidious.utils.MovementPredictor;
import voidious.utils.RobotState;
import voidious.utils.TimestampedFiringAngle;
import voidious.utils.Wave;
import voidious.utils.Wave.WavePosition;
import voidious.utils.WaveManager;

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

/**
 * Targeting data about an enemy.
 */
public class GunEnemy extends Enemy<TimestampedFiringAngle> {
  private static final double NON_ZERO_VELOCITY_THRESHOLD = 0.1;

  public double previousVelocity;
  public double lastNonZeroVelocity;
  public long timeSinceDirectionChange;
  public long timeSinceVelocityChange;
  public long timeMovingAtMe;
  public Wave lastWaveFired;
  public List<Point2D.Double> hitLocations;
  private int _waveBreaks;
  private Collection<RoboGraphic> _renderables;

  public GunEnemy(String botName, double distance, double energy,
      Point2D.Double location, int round, long time, double heading,
      double velocity, double absBearing, BattleField battleField,
      MovementPredictor predictor, Collection<RoboGraphic> renderables) {
    this(botName, distance, energy, location, round, time, heading, velocity,
        absBearing, battleField, predictor, renderables, new WaveManager());
  }

  GunEnemy(String botName, double distance, double energy,
      Point2D.Double location, int round, long time, double heading,
      double velocity, double absBearing, BattleField battleField,
      MovementPredictor predictor, Collection<RoboGraphic> renderables,
      WaveManager waveManager) {
    super(botName, location, distance, energy, heading, velocity, absBearing,
        round, time, battleField, predictor, waveManager);

    _waveBreaks = 0;
    _renderables = renderables;
    lastNonZeroVelocity = velocity;
    previousVelocity = 0;
    timeSinceDirectionChange = 0;
    timeSinceVelocityChange = 0;
    timeMovingAtMe = 0;
    lastWaveFired = null;
    hitLocations = new ArrayList<Point2D.Double>();
  }

  @Override
  public void initRound() {
    super.initRound();
    lastNonZeroVelocity = 0;
    previousVelocity = 0;
    timeSinceDirectionChange = 0;
    timeSinceVelocityChange = 0;
    hitLocations.clear();
  }

  public void execute(final long currentTime, long lastBulletFiredTime,
      double bulletPower, double currentGunHeat, Point2D.Double myLocation,
      final boolean is1v1, final int enemiesTotal,
      final List<GunDataListener> gunDataListeners, final boolean painting) {
    _waveManager.checkCurrentWaves(currentTime,
        newUpdateWaveListener(myLocation, bulletPower, lastBulletFiredTime,
            currentGunHeat, is1v1, gunDataListeners));
    _waveManager.checkActiveWaves(currentTime, lastScanState,
        newWaveBreakListener(
            currentTime, enemiesTotal, is1v1, gunDataListeners, painting));
  }

  void updateWave(Wave w, Point2D.Double myLocation, double bulletPower,
      long lastBulletFiredTime, double gunHeat, boolean is1v1,
      List<GunDataListener> gunDataListeners) {
    if (!w.altWave) {
      w.gunHeat = gunHeat;
      w.lastBulletFiredTime = lastBulletFiredTime;

      if (lastScanState.time == w.fireTime) {
        w.sourceLocation = myLocation;
        w.targetLocation = lastScanState.location;
        w.absBearing =
            DiaUtils.absoluteBearing(myLocation, lastScanState.location);
      }
    }
  }

  void processWaveBreak(Wave w, List<RobotState> waveBreakStates,
      long currentTime, int enemiesTotal,
      List<GunDataListener> gunDataListeners) {
    if (waveBreakStates == null || waveBreakStates.isEmpty()) {
      // This should be impossible, but it's happening due to a bug in
      // either Robocode or my JVM. It's bad data, so just ignore it.
      return;
    }
    double guessFactor = 0;
    Wave.Intersection preciseIntersection = null;
    if (enemiesTotal == 1) {
      preciseIntersection = w.preciseIntersection(waveBreakStates);
      guessFactor = w.guessFactorPrecise(preciseIntersection.angle);
    }

    RobotState waveBreakState = getMedianWaveBreakState(waveBreakStates);
    Point2D.Double dispVector = w.displacementVector(waveBreakState);
    logWave(w, dispVector, guessFactor, currentTime, IS_VISIT);
    _waveBreaks++;
    
    if (w.enemiesAlive == 1 && w.firingWave && !w.altWave) {
      if (preciseIntersection == null) {
        preciseIntersection = w.preciseIntersection(waveBreakStates);
      }
      double hitAngle = preciseIntersection.angle;
      double tolerance = preciseIntersection.bandwidth;
      for (GunDataListener listener : gunDataListeners) {
        listener.on1v1FiringWaveBreak(w, hitAngle, tolerance);
      }
    }
  }

  public Wave processBulletHit(Bullet bullet, long currentTime,
      boolean is1v1Battle, boolean logHitWave) {
    Point2D.Double bulletLocation = bulletLocation(bullet);
    Wave hitWave = _waveManager.findClosestWave(bulletLocation, currentTime,
        Wave.FIRING_WAVE, botName, bullet.getPower());
    if (hitWave != null) {
      if (logHitWave) {
        // TODO: if we ever use bullet hits for something important in melee,
        //       queue this up until we can interpolate location, or use
        //       bulletLocation if last scan time ! = currentTime
        Point2D.Double hitVector =
            hitWave.displacementVector(lastScanState.location, currentTime);
        double hitFactor = 0;
        if (is1v1Battle) {
          hitFactor = hitWave.guessFactorPrecise(lastScanState.location);
        }
        logWave(hitWave, hitVector, hitFactor, currentTime, IS_BULLET_HIT);
      }
      return hitWave;
    }
    return null;
  }

  public void logBulletHitLocation(Bullet bullet) {
    hitLocations.add(bulletLocation(bullet));
  }

  private Double bulletLocation(Bullet bullet) {
    return new Point2D.Double(bullet.getX(), bullet.getY());
  }

  void logWave(Wave w, Point2D.Double dispVector, double guessFactor, long time,
      boolean isVisit) {
    for (KnnView<TimestampedFiringAngle> view : views.values()) {
      if ((isVisit && view.logVisits || !isVisit && view.logBulletHits)
          && (view.logVirtual || w.firingWave)
          && (view.logMelee || w.enemiesAlive <= 1)) {
        view.logWave(w, new TimestampedFiringAngle(
            w.fireRound, time, guessFactor, dispVector));
      }
    }
  }

  public Wave newGunWave(Point2D.Double sourceLocation,
      Point2D.Double targetLocation, int fireRound, long fireTime,
      long lastBulletFiredTime, double bulletPower, double myEnergy,
      double gunHeat, int enemiesAlive, double accel, double dl8t, double dl20t,
      double dl40t, boolean altWave) {
    Wave newWave = new Wave(botName, sourceLocation, targetLocation,
        fireRound, fireTime, bulletPower, lastScanState.heading,
        lastScanState.velocity, DiaUtils.nonZeroSign(lastNonZeroVelocity),
        _battleField, _predictor)
            .setAccel(accel)
            .setDistance(sourceLocation.distance(targetLocation))
            .setDchangeTime(timeSinceDirectionChange)
            .setVchangeTime(timeSinceVelocityChange)
            .setDistanceLast8Ticks(dl8t)
            .setDistanceLast20Ticks(dl20t)
            .setDistanceLast40Ticks(dl40t)
            .setTargetEnergy(energy)
            .setSourceEnergy(myEnergy)
            .setGunHeat(gunHeat)
            .setEnemiesAlive(enemiesAlive)
            .setLastBulletFiredTime(lastBulletFiredTime)
            .setAltWave(altWave);
    newWave.setWallDistances(enemiesAlive <= 1
        ? Wave.WallDistanceStyle.ORBITAL : Wave.WallDistanceStyle.DIRECT);

    if (myEnergy > 0) {
      _waveManager.addWave(newWave);
    }
    return newWave;
  }

  public void markFiringWaves(
      long currentTime, boolean is1v1, List<GunDataListener> gunDataListeners) {
    _waveManager.checkCurrentWaves(currentTime,
        newMarkFiringWavesListener(is1v1, gunDataListeners));
  }

  public Wave findSurfableWave(Wave aimWave, int surfIndex) {
    RobotState targetState = RobotState.newBuilder()
        .setLocation(aimWave.targetLocation)
        .setTime(aimWave.fireTime)
        .build();
    return _waveManager.findSurfableWave(
        surfIndex, targetState, Wave.WavePosition.BREAKING_CENTER);
  }

  public void updateTimers(double velocity) {
    if (Math.abs(velocity - lastNonZeroVelocity) > 0.5) {
      timeSinceVelocityChange = 0;
    }
    if (Math.abs(velocity) > NON_ZERO_VELOCITY_THRESHOLD) {
      if (Math.signum(velocity) != Math.signum(lastNonZeroVelocity)) {
        timeSinceDirectionChange = 0;
      }
      lastNonZeroVelocity = velocity;
    }
  }

  public long getLastWaveFireTime(String botName) {
    return _waveManager.getLastFireTime();
  }

  public Wave interpolateGunWave(long fireTime, long currentTime,
      double myHeading, double myVelocity, RobotState enemyState) {
    Wave w = _waveManager.interpolateWaveByFireTime(fireTime, currentTime,
        myHeading, myVelocity, stateLog, _battleField, _predictor);
    if (w.checkWavePosition(enemyState) == WavePosition.MIDAIR) {
      _waveManager.addWave(w);
      return w;
    }
    return null;
  }

  private RobotState getMedianWaveBreakState(List<RobotState> waveBreakStates) {
    return waveBreakStates.get(waveBreakStates.size() / 2);
  }

  public double advancingVelocity() {
    return
        -Math.cos(lastScanState.heading - absBearing) * lastScanState.velocity;
  }

  public boolean isRammer() {
    return (((double) timeMovingAtMe) / timeAliveTogether) > 0.5;
  }

  public int getWaveBreaks() {
    return _waveBreaks;
  }

  // TODO: a cleaner way to expose this for TripHammer would be nice
  WaveManager getWaveManager() {
    return _waveManager;
  }

  private WaveManager.CurrentWaveListener newUpdateWaveListener(
      final Point2D.Double myLocation, final double bulletPower,
      final long lastBulletFiredTime, final double currentGunHeat,
      final boolean is1v1, final List<GunDataListener> gunDataListeners) {
    return new WaveManager.CurrentWaveListener() {
      @Override
      public void onCurrentWave(Wave w) {
        updateWave(w, myLocation, bulletPower, lastBulletFiredTime,
            currentGunHeat, is1v1, gunDataListeners);
      }
    };
  }

  private WaveManager.WaveBreakListener newWaveBreakListener(
      final long currentTime, final int enemiesTotal, final boolean is1v1,
      final List<GunDataListener> gunDataListeners, final boolean painting) {
    return new WaveManager.WaveBreakListener() {
      @Override
      public void onWaveBreak(Wave w, List<RobotState> waveBreakStates) {
        processWaveBreak(
            w, waveBreakStates, currentTime, enemiesTotal, gunDataListeners);
        if (painting && !is1v1) {
          drawDisplacementVector(
              w, currentTime, getMedianWaveBreakState(waveBreakStates));
        }
      }
    };
  }

  private WaveManager.CurrentWaveListener newMarkFiringWavesListener(
      final boolean is1v1, final List<GunDataListener> gunDataListeners) {
    return new WaveManager.CurrentWaveListener() {
      @Override
      public void onCurrentWave(Wave w) {
        if (!w.altWave) {
          w.firingWave = true;
          if (is1v1) {
            for (GunDataListener listener : gunDataListeners) {
              listener.onMarkFiringWave(w);
            }
          }
        }
      }
    };
  }

  private void drawDisplacementVector(
      Wave w, long currentTime, RobotState waveBreakState) {
    Point2D.Double dispVector = w.displacementVector(waveBreakState);
    double drawBearing = (w.orbitDirection
        * DiaUtils.absoluteBearing(Wave.ORIGIN, dispVector))
        + w.effectiveHeading();
    double drawDistance =
        dispVector.distance(Wave.ORIGIN) * (waveBreakState.time - w.fireTime);
    _renderables.add(RoboGraphic.drawLine(
        DiaUtils.project(w.targetLocation, w.targetHeading, 25),
        DiaUtils.project(w.targetLocation, w.targetHeading + Math.PI, 25),
        Color.darkGray));
    _renderables.add(RoboGraphic.drawLine(
        DiaUtils.project(w.targetLocation, w.targetHeading + (Math.PI / 2), 25),
        DiaUtils.project(w.targetLocation, w.targetHeading - (Math.PI / 2), 25),
        Color.darkGray));
    _renderables.addAll(Arrays.asList(
        RoboGraphic.drawArrowHead(
            DiaUtils.project(w.targetLocation, w.effectiveHeading(), 25),
            10, w.effectiveHeading(), Color.darkGray)));
    _renderables.add(
        RoboGraphic.drawLine(w.targetLocation,
            DiaUtils.project(w.targetLocation, drawBearing, drawDistance),
        Color.red));
    _renderables.add(
        RoboGraphic.drawCircleFilled(waveBreakState.location, Color.red, 4));
    _renderables.add(RoboGraphic.drawPoint(w.targetLocation, Color.red));
    _renderables.add(RoboGraphic.drawText(
        Long.toString(currentTime-w.fireTime), w.targetLocation.x - 8,
        w.targetLocation.y - 20, Color.white));
  }
}
