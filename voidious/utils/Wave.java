package voidious.utils;

import static voidious.utils.DiaUtils.absoluteBearing;
import static voidious.utils.DiaUtils.normalizeAngle;
import static voidious.utils.DiaUtils.square;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import robocode.util.Utils;
import voidious.utils.geom.Circle;
import voidious.utils.geom.LineSeg;

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

public class Wave implements Cloneable {
  public static final double PRECISE_MEA_WALL_STICK = 120;
  public static final Point2D.Double ORIGIN = new Point2D.Double(0, 0);
  public static final double MAX_BOT_RADIUS = 18 / Math.cos(Math.PI / 4);
  public static final int CLOCKWISE = 1;
  public static final int COUNTERCLOCKWISE = -1;
  public static final boolean FIRING_WAVE = true;
  public static final boolean SURFABLE_WAVE = true;
  public static final boolean ANY_WAVE = false;
  public static final boolean POSITIVE_GUESSFACTOR = true;
  public static final boolean NEGATIVE_GUESSFACTOR = false;
  public static final double ANY_BULLET_POWER = -1;
  public static final int FIRST_WAVE = 0;
  protected static final double BOT_HALF_WIDTH = 18;
  protected static final double TWO_PI = Math.PI * 2;

  public String botName;
  public Point2D.Double sourceLocation;
  public Point2D.Double targetLocation;
  public double absBearing;
  public int fireRound;
  public long fireTime;
  private double _bulletPower;
  private double _bulletSpeed;
  private double _maxEscapeAngle;
  public int orbitDirection;
  public double targetHeading;
  public double targetRelativeHeading;
  public double targetVelocity;
  protected BattleField _battleField;
  protected MovementPredictor _predictor;

  public boolean hitByBullet;
  public boolean bulletHitBullet;
  public boolean firingWave;
  public boolean altWave;

  public double targetAccel;
  public int targetVelocitySign;
  public double targetDistance;
  public double targetDistanceToNearestBot;
  public long targetDchangeTime;
  public long targetVchangeTime;
  public double targetWallDistance;
  public double targetRevWallDistance;
  public double targetDl8t;
  public double targetDl20t;
  public double targetDl40t;
  public double targetEnergy;
  public double sourceEnergy;
  public double gunHeat;
  public int enemiesAlive;
  public long lastBulletFiredTime;

  public List<BulletShadow> shadows;

  protected Double _cachedPositiveEscapeAngle = null;
  protected Double _cachedNegativeEscapeAngle = null;
  public boolean usedNegativeSmoothingMea = false;
  public boolean usedPositiveSmoothingMea = false;

  protected Wave() {
    // needed for subclass with its own full constructor
  }

  public Wave(String botName,
      Point2D.Double sourceLocation,
      Point2D.Double targetLocation,
      int fireRound,
      long fireTime,
      double bulletPower,
      double targetHeading,
      double targetVelocity,
      int targetVelocitySign,
      BattleField battleField,
      MovementPredictor predictor) {
    this.botName = botName;
    this.sourceLocation = sourceLocation;
    this.targetLocation = targetLocation;
    this.fireRound = fireRound;
    this.fireTime = fireTime;
    setBulletPower(bulletPower);
    this.targetHeading = targetHeading;
    this.targetVelocity = targetVelocity;
    this.targetVelocitySign = targetVelocitySign;
    _battleField = battleField;
    _predictor = predictor;
    absBearing = absoluteBearing(sourceLocation, targetLocation);

    double relativeHeading = Utils.normalRelativeAngle(
        effectiveHeading() - absoluteBearing(sourceLocation, targetLocation));
    orbitDirection = (relativeHeading < 0) ? COUNTERCLOCKWISE : CLOCKWISE;
    targetRelativeHeading = Math.abs(relativeHeading);

    hitByBullet = false;
    bulletHitBullet = false;
    firingWave = false;
    altWave = false;
    shadows = new ArrayList<BulletShadow>();
  }

  public Wave setAbsBearing(double absBearing) {
    this.absBearing = absBearing;
    return this;
  }

  public Wave setBulletPower(double power) {
    _bulletPower = power;
    _bulletSpeed = (20 - (3 * power));
    _maxEscapeAngle = Math.asin(8.0 / _bulletSpeed);
    clearCachedPreciseEscapeAngles();
    return this;
  }

  public Wave setAccel(double accel) {
    this.targetAccel = accel;
    return this;
  }

  public Wave setDistance(double distance) {
    this.targetDistance = distance;
    return this;
  }

  public Wave setDistanceToNearestBot(double distanceToNearestBot) {
    this.targetDistanceToNearestBot = distanceToNearestBot;
    return this;
  }

  public Wave setDchangeTime(long dChangeTime) {
    this.targetDchangeTime = dChangeTime;
    return this;
  }

  public Wave setVchangeTime(long vChangeTime) {
    this.targetVchangeTime = vChangeTime;
    return this;
  }

  public Wave setDistanceLast8Ticks(double dl8t) {
    this.targetDl8t = dl8t;
    return this;
  }

  public Wave setDistanceLast20Ticks(double dl20t) {
    this.targetDl20t = dl20t;
    return this;
  }

  public Wave setDistanceLast40Ticks(double dl40t) {
    this.targetDl40t = dl40t;
    return this;
  }

  public Wave setTargetEnergy(double energy) {
    this.targetEnergy = energy;
    return this;
  }

  public Wave setSourceEnergy(double energy) {
    this.sourceEnergy = energy;
    return this;
  }

  public Wave setGunHeat(double gunHeat) {
    this.gunHeat = gunHeat;
    return this;
  }

  public Wave setEnemiesAlive(int enemiesAlive) {
    this.enemiesAlive = enemiesAlive;
    return this;
  }

  public Wave setLastBulletFiredTime(long lastBulletFiredTime) {
    this.lastBulletFiredTime = lastBulletFiredTime;
    return this;
  }

  public Wave setAltWave(boolean altWave) {
    this.altWave = altWave;
    return this;
  }

  private Wave setFiringWave(boolean firingWave) {
    this.firingWave = firingWave;
    return this;
  }

  private Wave setHitByBullet(boolean hitByBullet) {
    this.hitByBullet = hitByBullet;
    return this;
  }

  private Wave setBulletHitBullet(boolean bulletHitBullet) {
    this.bulletHitBullet = bulletHitBullet;
    return this;
  }

  public double bulletPower() {
    return _bulletPower;
  }

  public double bulletSpeed() {
    return _bulletSpeed;
  }

  public double maxEscapeAngle() {
    return _maxEscapeAngle;
  }

  public double effectiveHeading() {
    return Utils.normalAbsoluteAngle(targetHeading
        + (targetVelocitySign == 1 ? 0 : Math.PI));
  }

  public double distanceTraveled(long currentTime) {
    return (currentTime - fireTime) * _bulletSpeed;
  }

  public double lateralVelocity() {
    return Math.sin(targetRelativeHeading)
        * (targetVelocitySign * targetVelocity);
  }

  public boolean processedBulletHit() {
    return hitByBullet || bulletHitBullet;
  }

  public void setWallDistances(WallDistanceStyle style) {
    switch (style) {
      case ORBITAL:
        targetWallDistance = orbitalWallDistance(orbitDirection);
        targetRevWallDistance =  orbitalWallDistance(-orbitDirection);
        break;
      case DIRECT:
        targetWallDistance = directToWallDistance(true);
        targetRevWallDistance = directToWallDistance(false);
        break;
      case PRECISE_MEA:
        targetWallDistance =
            preciseEscapeAngle(POSITIVE_GUESSFACTOR) / _maxEscapeAngle;
        targetRevWallDistance =
            preciseEscapeAngle(NEGATIVE_GUESSFACTOR) / _maxEscapeAngle;
        break;
    }
  }

  private double orbitalWallDistance(int orientation) {
    return Math.min(1.5,
        _battleField.orbitalWallDistance(
            sourceLocation, targetLocation, bulletPower(), orientation));
  }

  private double directToWallDistance(boolean forward) {
    return Math.min(1.5,
        _battleField.directToWallDistance(targetLocation,
            sourceLocation.distance(targetLocation),
            effectiveHeading() + (forward ? 0 : Math.PI), bulletPower()));
  }

  public double virtuality() {
    long timeSinceLastBullet = (fireTime - lastBulletFiredTime);
    long timeToNextBullet = Math.round(Math.ceil(gunHeat * 10));

    if (firingWave) {
      return 0;
    } else if (lastBulletFiredTime > 0) {
      return Math.min(timeSinceLastBullet, timeToNextBullet) / 8.0;
    } else {
      return Math.min(1, timeToNextBullet / 8.0);
    }
  }

  public double firingAngle(double guessFactor) {
    return absBearing + (guessFactor * orbitDirection * _maxEscapeAngle);
  }

  public double firingAngleFromTargetLocation(Point2D.Double firingTarget) {
    return Utils.normalAbsoluteAngle(
        absoluteBearing(sourceLocation, firingTarget));
  }

  public Point2D.Double displacementVector(RobotState waveBreakState) {
    return displacementVector(waveBreakState.location, waveBreakState.time);
  }

  public Point2D.Double displacementVector(
      Point2D.Double botLocation, long time) {
    double vectorBearing = Utils.normalRelativeAngle(
        absoluteBearing(targetLocation, botLocation) - effectiveHeading());
    double vectorDistance =
        targetLocation.distance(botLocation) / (time - fireTime);
    return DiaUtils.project(
        ORIGIN, vectorBearing * orbitDirection, vectorDistance);
  }

  public Point2D.Double projectLocationFromDisplacementVector(
      Point2D.Double dispVector) {
    return projectLocation(sourceLocation, dispVector, 0);
  }

  public Point2D.Double projectLocationBlind(Point2D.Double myNextLocation,
      Point2D.Double dispVector, long currentTime) {
    return
        projectLocation(myNextLocation, dispVector, currentTime - fireTime + 1);
  }

  Point2D.Double projectLocation(Point2D.Double firingLocation,
      Point2D.Double dispVector, long extraTicks) {
    double dispAngle = effectiveHeading()
        + (absoluteBearing(ORIGIN, dispVector) * orbitDirection);
    double dispDistance = ORIGIN.distance(dispVector);

    Point2D.Double projectedLocation = targetLocation;
    long bulletTicks = -1;
    long prevBulletTicks = -1;
    long prevPrevBulletTicks;
    double daSin = Math.sin(dispAngle);
    double daCos = Math.cos(dispAngle);

    do {
      prevPrevBulletTicks = prevBulletTicks;
      prevBulletTicks = bulletTicks;
      bulletTicks = DiaUtils.bulletTicksFromSpeed(
          firingLocation.distance(projectedLocation), _bulletSpeed) - 1;
      projectedLocation = DiaUtils.project(targetLocation,
          daSin, daCos, (bulletTicks + extraTicks) * dispDistance);
    } while (bulletTicks != prevBulletTicks
             && bulletTicks != prevPrevBulletTicks);

    return projectedLocation;
  }

  public double guessFactor(Point2D.Double targetLocation) {
    double bearingToTarget = absoluteBearing(sourceLocation, targetLocation);
    return guessFactor(bearingToTarget);
  }

  public double guessFactor(double bearingToTarget) {
    return guessAngle(bearingToTarget) / _maxEscapeAngle;
  }

  public double guessAngle(Point2D.Double targetLocation) {
    double bearingToTarget = 
        DiaUtils.absoluteBearing(sourceLocation, targetLocation);
    return guessAngle(bearingToTarget);
  }
  
  public double guessAngle(double bearingToTarget) {
    return orbitDirection
        * Utils.normalRelativeAngle(bearingToTarget - absBearing);
  }

  public double guessFactorPrecise(Point2D.Double targetLocation) {
    double newBearingToTarget = absoluteBearing(sourceLocation, targetLocation);
    return guessFactorPrecise(newBearingToTarget);
  }

  public double guessFactorPrecise(double newBearingToTarget) {
    double guessAngle = orbitDirection
        * Utils.normalRelativeAngle(newBearingToTarget - absBearing);
    double maxEscapeAngle = preciseEscapeAngle(guessAngle >= 0);
    return guessAngle / maxEscapeAngle;
  }

  
  public double preciseEscapeAngle(boolean guessFactorSign) {
    if (guessFactorSign) {
      if (_cachedPositiveEscapeAngle == null) {
        _cachedPositiveEscapeAngle =
            calculatePreciseEscapeAngle(guessFactorSign).angle;
      }
      return _cachedPositiveEscapeAngle;
    } else {
      if (_cachedNegativeEscapeAngle == null) {
        _cachedNegativeEscapeAngle =
            calculatePreciseEscapeAngle(guessFactorSign).angle;
      }
      return _cachedNegativeEscapeAngle;
    }
  }

  public double escapeAngleRange() {
    return (preciseEscapeAngle(POSITIVE_GUESSFACTOR)
        + preciseEscapeAngle(NEGATIVE_GUESSFACTOR));
  }

  public MaxEscapeTarget calculatePreciseEscapeAngle(
      boolean positiveGuessFactor) {
    RobotState startState = RobotState.newBuilder()
        .setLocation((Point2D.Double) targetLocation.clone())
        .setHeading(targetHeading)
        .setVelocity(targetVelocity)
        .setTime(fireTime)
        .build();
    return _predictor.preciseEscapeAngle(
        orbitDirection * (positiveGuessFactor ? 1 : -1), sourceLocation,
        fireTime, _bulletSpeed, startState, PRECISE_MEA_WALL_STICK);
  }

  public void clearCachedPreciseEscapeAngles() {
    _cachedPositiveEscapeAngle = null;
    _cachedNegativeEscapeAngle = null;
  }

  public boolean shadowed(double firingAngle) {
    for (BulletShadow shadow : shadows) {
      firingAngle = normalizeAngle(firingAngle, shadow.minAngle);
      if (firingAngle >= shadow.minAngle && firingAngle <= shadow.maxAngle) {
        return true;
      }
    }
    return false;
  }

  public void castShadow(Point2D.Double p1, Point2D.Double p2) {
    castShadow(absoluteBearing(sourceLocation, p1),
               absoluteBearing(sourceLocation, p2));
  }

  private void castShadow(double shadowAngle1, double shadowAngle2) {
    shadowAngle1 = normalizeAngle(shadowAngle1, absBearing);
    shadowAngle2 = normalizeAngle(shadowAngle2, shadowAngle1);
    double min = Math.min(shadowAngle1, shadowAngle2);
    double max = Math.max(shadowAngle1, shadowAngle2);
    shadows.add(new BulletShadow(min, max));

    Set<BulletShadow> deadShadows = new HashSet<BulletShadow>();
    for (BulletShadow shadow1 : shadows) {
      if (!deadShadows.contains(shadow1)) {
        for (BulletShadow shadow2 : shadows) {
          if (shadow1 != shadow2 && !deadShadows.contains(shadow2)
              && shadow1.overlaps(shadow2)) {
            shadow1.minAngle = Math.min(shadow1.minAngle,
                normalizeAngle(shadow2.minAngle, shadow1.minAngle));
            shadow1.maxAngle = Math.max(shadow1.maxAngle,
                normalizeAngle(shadow2.maxAngle, shadow1.maxAngle));
            deadShadows.add(shadow2);
          }
        }
      }
    }
    for (BulletShadow deadShadow : deadShadows) {
      shadows.remove(deadShadow);
    }
  }

  public double shadowFactor(Wave.Intersection intersection) {
    double min = intersection.angle - intersection.bandwidth;
    double max = intersection.angle + intersection.bandwidth;
    double factor = 1;
    for (BulletShadow shadow : shadows) {
      double shadowMin = normalizeAngle(shadow.minAngle, min);
      double shadowMax = normalizeAngle(shadow.maxAngle, shadowMin);
      if (shadowMin <= min && shadowMax >= max) {
        return 0;
      } else if (shadowMin >= min && shadowMin <= max) {
        factor -= (Math.min(max, shadowMax) - shadowMin) / (max - min);
      } else if (shadowMax >= min && shadowMax <= max) {
        factor -= (shadowMax - Math.max(min, shadowMin)) / (max - min);
      }
    }

    return factor;
  }

  public Intersection preciseIntersection(List<RobotState> waveBreakStates) {
    if (waveBreakStates == null || waveBreakStates.size() == 0) {
      return null;
    }

    ArrayList<Double> aimAngles = new ArrayList<Double>();
    for (RobotState waveBreakState : waveBreakStates) {
      List<Point2D.Double> corners = waveBreakState.botCorners();
      Circle waveStart = new Circle(sourceLocation.x, sourceLocation.y,
          _bulletSpeed * (waveBreakState.time - fireTime));
      Circle waveEnd = new Circle(sourceLocation.x, sourceLocation.y,
          _bulletSpeed * (waveBreakState.time - fireTime + 1));

      for (Point2D.Double corner : corners) {
        if (waveEnd.contains(corner) && !waveStart.contains(corner)) {
          aimAngles.add(absoluteBearing(sourceLocation, corner));
        }
      }

      for (Line2D.Double side : waveBreakState.botSides()) {
        LineSeg seg = new LineSeg(side.x1, side.y1, side.x2, side.y2);
        Point2D.Double[] intersects = waveStart.intersects(seg);
        for (int x = 0; x < intersects.length; x++) {
          if (intersects[x] != null) {
            aimAngles.add(absoluteBearing(sourceLocation, intersects[x]));
          }
        }
        intersects = waveEnd.intersects(seg);
        if (intersects != null) {
          for (int x = 0; x < intersects.length; x++) {
            if (intersects[x] != null) {
              aimAngles.add(absoluteBearing(sourceLocation, intersects[x]));
            }
          }
        }
      }
    }

    double normalizeReference = aimAngles.get(0);
    double minAngle = normalizeReference;
    double maxAngle = normalizeReference;

    for (double thisAngle : aimAngles) {
      thisAngle = normalizeAngle(thisAngle, normalizeReference);
      maxAngle = Math.max(maxAngle, thisAngle);
      minAngle = Math.min(minAngle, thisAngle);
    }

    double centerAngle = (maxAngle + minAngle) / 2;
    double bandwidth = maxAngle - centerAngle;

    return new Intersection(centerAngle, bandwidth);
  }

  public WavePosition checkWavePosition(RobotState currentState) {
    return checkWavePosition(currentState, false, null);
  }

  public WavePosition checkWavePosition(
      RobotState currentState, boolean skipMidair) {
    return checkWavePosition(currentState, skipMidair, null);
  }


  public WavePosition checkWavePosition(
      RobotState currentState, WavePosition maxPosition) {
    return checkWavePosition(currentState, false, maxPosition);
  }

  public WavePosition checkWavePosition(
      RobotState currentState, boolean skipMidair, WavePosition maxPosition) {
    Point2D.Double location = currentState.location;
    double enemyDistSq = sourceLocation.distanceSq(location);
    double endBulletDistance = distanceTraveled(currentState.time + 1);
    if (!skipMidair && (maxPosition == WavePosition.MIDAIR
            || enemyDistSq > square(endBulletDistance + MAX_BOT_RADIUS)
            || DiaUtils.distancePointToBot(sourceLocation, currentState)
                > endBulletDistance)) {
      return WavePosition.MIDAIR;
    } else if (maxPosition == WavePosition.BREAKING_FRONT
               || enemyDistSq > square(endBulletDistance)) {
      return WavePosition.BREAKING_FRONT;
    } else {
      if (maxPosition == WavePosition.BREAKING_CENTER){
        return WavePosition.BREAKING_CENTER;
      }
      List<Point2D.Double> corners = currentState.botCorners();
      double startBulletDistance = distanceTraveled(currentState.time);
      for (Point2D.Double corner : corners) {
        if (corner.distanceSq(sourceLocation) > square(startBulletDistance)) {
          return WavePosition.BREAKING_CENTER;
        }
      }
      return WavePosition.GONE;
    }
  }

  public Object clone() {
    return new Wave(botName, sourceLocation, targetLocation,
        fireRound, fireTime, bulletPower(), targetHeading, targetVelocity,
        targetVelocitySign, _battleField, _predictor)
            .setAbsBearing(absBearing)
            .setAccel(targetAccel)
            .setDistance(targetDistance)
            .setDchangeTime(targetDchangeTime)
            .setVchangeTime(targetVchangeTime)
            .setDistanceLast8Ticks(targetDl8t)
            .setDistanceLast20Ticks(targetDl20t)
            .setDistanceLast40Ticks(targetDl40t)
            .setTargetEnergy(targetEnergy)
            .setSourceEnergy(sourceEnergy)
            .setAltWave(altWave)
            .setFiringWave(firingWave)
            .setHitByBullet(hitByBullet)
            .setBulletHitBullet(bulletHitBullet)
            .setEnemiesAlive(enemiesAlive)
            .setLastBulletFiredTime(lastBulletFiredTime);
  }

  public enum WavePosition {
    /**
     * Wave has not reached enemy at all.
     */
    MIDAIR(0, false),
    /**
     * The wave is intersecting the enemy bot.
     */
    BREAKING_FRONT(1, true),
    /**
     * The wave is intersecting the enemy bot and its front edge (next tick) is
     * past its center.
     */
    BREAKING_CENTER(2, true),
    /**
     * The wave is completely beyond the enemy bot.
     */
    GONE(3, false);

    private final int _index;
    private final boolean _breaking;

    private WavePosition(int index, boolean breaking) {
      _index = index;
      _breaking = breaking;
    }

    public int getIndex() {
      return _index;
    }

    public boolean isBreaking() {
      return _breaking;
    }
  }

  public static class Intersection {
    public final double angle;
    public final double bandwidth;

    public Intersection(double angle, double bandwidth) {
      this.angle = angle;
      this.bandwidth = bandwidth;
    }
  }

  public static class BulletShadow {
    public double minAngle;
    public double maxAngle;

    public BulletShadow(double minAngle, double maxAngle) {
      this.minAngle = minAngle;
      this.maxAngle = maxAngle;
    }

    public boolean overlaps(BulletShadow that) {
      double thatMinAngle = normalizeAngle(that.minAngle, minAngle);
      double thatMaxAngle = normalizeAngle(that.maxAngle, thatMinAngle);
      return overlaps(that.minAngle) || overlaps(that.maxAngle)
          || (thatMinAngle <= minAngle && thatMaxAngle >= maxAngle);
    }

    private boolean overlaps(double angle) {
      angle = normalizeAngle(angle, minAngle);
      return minAngle <= angle && maxAngle >= angle;
    }
  }

  public enum WallDistanceStyle {
    ORBITAL,
    DIRECT,
    PRECISE_MEA
  }
}
