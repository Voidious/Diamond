package voidious.move;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;

import robocode.Bullet;
import robocode.Rules;
import voidious.gfx.RoboGraphic;
import voidious.gun.FireListener.FiredBullet;
import voidious.move.formulas.FlattenerFormula;
import voidious.move.formulas.NormalFormula;
import voidious.move.formulas.SimpleFormula;
import voidious.utils.BattleField;
import voidious.utils.DiaUtils;
import voidious.utils.Enemy;
import voidious.utils.KnnView;
import voidious.utils.MovementPredictor;
import voidious.utils.RobotState;
import voidious.utils.RobotStateLog;
import voidious.utils.TimestampedGuessFactor;
import voidious.utils.Wave;
import voidious.utils.WaveManager;
import voidious.utils.WaveManager.WaveBreakListener;
import voidious.utils.geom.Circle;
import voidious.utils.geom.LineSeg;
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

/**
 * General info about the enemy. We just keep one of these around for
 * each enemy bot the movement is aware of.
 */
public class MoveEnemy extends Enemy<TimestampedGuessFactor> {
  private static final double RECENT_SCANS_HIT_THRESHOLD = 2.5;
  private static final double LIGHT_FLATTENER_HIT_THRESHOLD = 3.0;
  private static final double FLATTENER_HIT_THRESHOLD = 5.9;
  private static final double DECAY_RATE = 1.8;
  private static final double BOT_WIDTH = 36;
  private static final double TYPICAL_ANGULAR_BOT_WIDTH = 0.1;
  private static final double TYPICAL_ESCAPE_RANGE = 0.98;

  public double damageTaken;
  public double lastBulletPower;
  public long lastBulletFireTime;
  public double totalBulletPower;
  public long totalTimesHit;
  public double totalDistance;
  public long lastTimeHit;
  public long lastTimeClosest;
  public boolean avoidBeingTargeted;
  public boolean stayPerpendicular;
  public double damageFactor;

  public int raw1v1ShotsFired;
  public int raw1v1ShotsHit;
  public double weighted1v1ShotsHit;
  public int raw1v1ShotsFiredThisRound;
  public int raw1v1ShotsHitThisRound;
  public double weighted1v1ShotsHitThisRound;
  public WallHitDamage wallHitDamage;
  public boolean isRobot;
  public KdTree.WeightedSqrEuclid<Double> powerTree;

  private Wave _imaginaryWave;
  private int _imaginaryWaveIndex;

  private Collection<RoboGraphic> _renderables;
  private PrintStream _out;

  public MoveEnemy(String botName, double distance, double energy,
      Point2D.Double location, double heading, double velocity,
      double absBearing, int round, long time,
      Collection<RoboGraphic> renderables, BattleField battleField,
      MovementPredictor predictor, PrintStream out) {
    super(botName, location, distance, energy, heading, velocity, absBearing,
        round, time, battleField, predictor, new WaveManager());
    _renderables = renderables;
    _out = out;
    damageTaken = 0;
    lastBulletPower = 0;
    totalBulletPower = 0;
    totalTimesHit = 0;
    totalDistance = 500;
    raw1v1ShotsFired = 0;
    weighted1v1ShotsHit = 0;
    avoidBeingTargeted = false;
    stayPerpendicular = false;
    wallHitDamage = new WallHitDamage(0, 0);
    isRobot = false;

    initSurfViews();
    powerTree =
        new KdTree.WeightedSqrEuclid<Double>(BULLET_POWER_WEIGHTS.length, null);
    powerTree.setWeights(BULLET_POWER_WEIGHTS);
    _imaginaryWave = null;
  }

  @Override
  public void initRound() {
    super.initRound();
    lastTimeHit = Long.MIN_VALUE;
    lastTimeClosest = Long.MIN_VALUE;
    lastBulletFireTime = 0;
    raw1v1ShotsFiredThisRound = 0;
    raw1v1ShotsHitThisRound = 0;
    weighted1v1ShotsHitThisRound = 0;
    lastBulletPower = 0;
    _imaginaryWave = null;
    clearNeighborCache();
  }

  public void initSurfViews() {
    KnnView<TimestampedGuessFactor> simple =
        new KnnView<TimestampedGuessFactor>(new SimpleFormula())
            .setWeight(3)
            .setK(25)
            .setKDivisor(5)
            .bulletHitsOn();

    // TODO: reconfigure all this, hit threshold = 3.0 makes no sense
    KnnView<TimestampedGuessFactor> normal =
        new KnnView<TimestampedGuessFactor>(new NormalFormula())
            .setWeight(40)
            .setK(20)
            .setKDivisor(5)
            .setHitThreshold(3.0)
            .bulletHitsOn();

    KnnView<TimestampedGuessFactor> recent =
        new KnnView<TimestampedGuessFactor>(new NormalFormula())
            .setWeight(100)
            .setK(1)
            .setMaxDataPoints(1)
            .setHitThreshold(RECENT_SCANS_HIT_THRESHOLD)
            .bulletHitsOn();
    KnnView<TimestampedGuessFactor> recent2 =
        new KnnView<TimestampedGuessFactor>(new NormalFormula())
            .setWeight(100)
            .setK(1)
            .setMaxDataPoints(5)
            .setHitThreshold(RECENT_SCANS_HIT_THRESHOLD)
            .bulletHitsOn();
    KnnView<TimestampedGuessFactor> recent3 =
        new KnnView<TimestampedGuessFactor>(new NormalFormula())
            .setWeight(100)
            .setK(1)
            .setHitThreshold(RECENT_SCANS_HIT_THRESHOLD)
            .setDecayRate(DECAY_RATE)
            .bulletHitsOn();
    KnnView<TimestampedGuessFactor> recent4 =
        new KnnView<TimestampedGuessFactor>(new NormalFormula())
            .setWeight(100)
            .setK(7)
            .setKDivisor(4)
            .setHitThreshold(RECENT_SCANS_HIT_THRESHOLD)
            .setDecayRate(DECAY_RATE)
            .bulletHitsOn();
    KnnView<TimestampedGuessFactor> recent5 =
        new KnnView<TimestampedGuessFactor>(new NormalFormula())
            .setWeight(100)
            .setK(35)
            .setKDivisor(3)
            .setHitThreshold(RECENT_SCANS_HIT_THRESHOLD)
            .setDecayRate(DECAY_RATE)
            .bulletHitsOn();
    KnnView<TimestampedGuessFactor> recent6 =
        new KnnView<TimestampedGuessFactor>(new NormalFormula())
            .setWeight(100)
            .setK(100)
            .setKDivisor(2)
            .setHitThreshold(RECENT_SCANS_HIT_THRESHOLD)
            .setDecayRate(DECAY_RATE)
            .bulletHitsOn();

    KnnView<TimestampedGuessFactor> lightFlattener =
        new KnnView<TimestampedGuessFactor>(new NormalFormula())
            .setWeight(10)
            .setK(50)
            .setMaxDataPoints(1000)
            .setKDivisor(5)
            .setPaddedHitThreshold(LIGHT_FLATTENER_HIT_THRESHOLD)
            .visitsOn();

    KnnView<TimestampedGuessFactor> flattener =
        new KnnView<TimestampedGuessFactor>(new FlattenerFormula())
            .setWeight(50)
            .setK(25)
            .setMaxDataPoints(300)
            .setKDivisor(12)
            .setPaddedHitThreshold(FLATTENER_HIT_THRESHOLD)
            .visitsOn();
    KnnView<TimestampedGuessFactor> flattener2 =
        new KnnView<TimestampedGuessFactor>(new FlattenerFormula())
            .setWeight(500)
            .setK(50)
            .setMaxDataPoints(2000)
            .setKDivisor(14)
            .setPaddedHitThreshold(FLATTENER_HIT_THRESHOLD)
            .setDecayRate(DECAY_RATE)
            .visitsOn();

    addView(simple);
    addView(normal);
    addView(recent);
    addView(recent2);
    addView(recent3);
    addView(recent4);
    addView(recent5);
    addView(recent6);
    addView(lightFlattener);
    addView(flattener);
    addView(flattener2);
  }

  public void execute1v1(
      int currentRound, long currentTime, Point2D.Double myLocation) {
    _waveManager.checkActiveWaves(currentTime, RobotState.newBuilder()
            .setLocation(myLocation).setTime(currentTime).build(),
        newWaveBreakListener(currentRound, currentTime));
  }

  public Wave processBullet(
      Bullet bullet, int currentRound, long currentTime, boolean painting) {
    Point2D.Double bulletLocation =
        new Point2D.Double(bullet.getX(), bullet.getY());
    String botName = bullet.getName();

    Wave hitWave = _waveManager.findClosestWave(bulletLocation, currentTime,
        Wave.FIRING_WAVE, botName, bullet.getPower());

    if (hitWave == null || !botName.equals(hitWave.botName)) {
      return null;
    }

    double hitGuessFactor = hitWave.guessFactor(bulletLocation);
    for (KnnView<TimestampedGuessFactor> view : views.values()) {
      if (view.logBulletHits) {
        view.logWave(hitWave, new TimestampedGuessFactor(
            currentRound, currentTime, hitGuessFactor));
      }
    }

    if (painting) {
      drawBulletHit(hitWave, hitGuessFactor, bulletLocation, currentTime);
    }

    return hitWave;
  }

  public double avgBulletPower() {
    if (totalBulletPower == 0) {
      return 3.0;
    }
    return totalBulletPower / totalTimesHit;
  }

  public double minDistanceSq() {
    double min = Double.POSITIVE_INFINITY;
    for (double distanceSq : _botDistancesSq.values()) {
      if (distanceSq < min) {
        min = distanceSq;
      }
    }
    return min;
  }

  public int botsCloser(double distanceSq) {
    int botsCloser = 0;
    for (double botDistanceSq : _botDistancesSq.values()) {
      if (botDistanceSq < distanceSq) {
        botsCloser++;
      }
    }
    return botsCloser;
  }

  public void clearNeighborCache() {
    for (KnnView<TimestampedGuessFactor> view : views.values()) {
      view.clearCache();
    }
  }

  public double getGunHeat(long time) {
    double gunHeat;
    if (time <= 30) {
      gunHeat = Math.max(0, 3.0 - (time * .1));
    } else {
      gunHeat = Math.max(0,
                         Rules.getGunHeat(lastBulletPower)
                             - ((time - lastBulletFireTime) * .1));
    }
    return DiaUtils.round(gunHeat, 6);
  }

  private static final double[] BULLET_POWER_WEIGHTS = new double[]{3, 5, 1};
  protected double[] bulletPowerDataPoint(
      double distance, double enemyEnergy, double myEnergy) {
    return new double[] {
      Math.min(distance, 800) / 800,
      Math.min(enemyEnergy, 125) / 125,
      Math.min(myEnergy, 125) / 125
    };
  }

  public void resetBulletShadows(final List<FiredBullet> firedBullets) {
    _waveManager.forAllWaves(new WaveManager.AllWaveListener() {
      @Override
      public void onWave(Wave w) {
        if (w.firingWave) {
          w.shadows.clear();
          for (FiredBullet bullet : firedBullets) {
            setShadows(w, bullet);
          }
        }
      }
    });
  }

  public void setShadows(final FiredBullet bullet) {
    _waveManager.forAllWaves(new WaveManager.AllWaveListener() {
      @Override
      public void onWave(Wave w) {
        if (w.firingWave) {
          setShadows(w, bullet);
        }
      }
    });
  }

  // TODO: I'm fairly certain the shadow created by a bullet going from
  //     inside to outside a wave is irrelevant, so I'm ignoring it.
  //     Would be nice to prove it or fix this code.
  public void setShadows(Wave w, FiredBullet bullet) {
    long startTime = Math.max(w.fireTime, bullet.fireTime);
    if (!w.processedBulletHit()
        && w.sourceLocation.distanceSq(bullet.position(startTime))
            > DiaUtils.square(w.distanceTraveled(startTime))) {
      long time = startTime;
      do {
        time++;
        if (w.sourceLocation.distanceSq(bullet.position(time))
                < DiaUtils.square(w.distanceTraveled(time))
            && time < bullet.deathTime) {
          addBulletShadows(w, bullet, time);
          break;
        }
      } while (_battleField.rectangle.contains(bullet.position(time)));
    }
  }

  private void addBulletShadows(Wave w, FiredBullet b, long time) {
    Circle waveCircle1 =
        new Circle(w.sourceLocation, w.distanceTraveled(time - 1));
    Circle waveCircle2 =
        new Circle(w.sourceLocation, w.distanceTraveled(time));
    Point2D.Double bulletPoint1 = b.position(time-1);
    Point2D.Double bulletPoint2 = b.position(time);
    LineSeg bulletSeg = new LineSeg(bulletPoint1, bulletPoint2);
    Point2D.Double[] xPoints1 = waveCircle1.intersects(bulletSeg);
    Point2D.Double[] xPoints2 = waveCircle2.intersects(bulletSeg);

    if (xPoints1[0] == null && xPoints2[0] == null) {
      // whole bullet line segment is between two wave circles,
      // shadow cast along all firing angles covered by line segment
      w.castShadow(bulletPoint1, bulletPoint2);
      // TODO: It's possible the previous bullet line intersected the
      //     inner wave circle (then the outer wave circle) last tick
      //     and cast 2 shadows, while remaining outside the wave on
      //     either endpoint. Should be super rare and probably could
      //     never be a reachable shadow anyway.
    } else if (xPoints1[0] != null) {
      if (xPoints2[0] != null) {
        // line segment intersects both wave circles, shadow cast
        // from one intersection to the other
        w.castShadow(xPoints2[0], xPoints1[0]);
      } else if (xPoints1[1] != null) {
        // line segment begins and ends between wave circles,
        // intersects inner circle in two spots, casts 2 shadows
        Point2D.Double intersect1, intersect2;
        if (bulletPoint1.distanceSq(xPoints1[0])
                < bulletPoint1.distanceSq(xPoints1[1])) {
          intersect1 = xPoints1[0];
          intersect2 = xPoints1[1];
        } else {
          intersect1 = xPoints1[1];
          intersect2 = xPoints1[0];
        }
        w.castShadow(bulletPoint1, intersect1);
        w.castShadow(intersect2, bulletPoint2);
      } else {
        // line segment crosses inner wave circle in one spot
        w.castShadow(bulletPoint1, xPoints1[0]);
      }
    } else if (xPoints2[0] != null) {
      // line segment crosses outer wave circle in one spot
      w.castShadow(xPoints2[0], bulletPoint2);
    }
  }

  public Wave newMoveWave(Point2D.Double sourceLocation,
      Point2D.Double targetLocation, double absBearing, int fireRound,
      long fireTime, double bulletPower, double myEnergy, double myHeading,
      double myVelocity, int velocitySign, double accel, double dl8t,
      double dl20t, double dl40t, long timeSinceReverseDirection,
      long timeSinceVelocityChange) {
    Wave w = new Wave(botName, sourceLocation, targetLocation,
        fireRound, fireTime, bulletPower, myHeading, myVelocity,
        velocitySign, _battleField, _predictor)
            .setAbsBearing(absBearing)
            .setAccel(accel)
            .setDistance(sourceLocation.distance(targetLocation))
            .setDchangeTime(timeSinceReverseDirection)
            .setVchangeTime(timeSinceVelocityChange)
            .setDistanceLast8Ticks(dl8t)
            .setDistanceLast20Ticks(dl20t)
            .setDistanceLast40Ticks(dl40t)
            .setTargetEnergy(myEnergy)
            .setSourceEnergy(energy);
    _waveManager.addWave(w);
    return w;
  }

  public void updateImaginaryWave(long currentTime, RobotState myRobotState,
      int wavesToSurf, MovementPredictor predictor) {
    _imaginaryWaveIndex = -1;
    for (int x = 0; x < wavesToSurf; x++) {
      if (findSurfableWave(x, myRobotState) == null) {
        _imaginaryWaveIndex = x;
        break;
      }
    }

    double enemyGunHeat = getGunHeat(currentTime);
    if (_imaginaryWaveIndex >= 0 && enemyGunHeat < 0.1000001) {
      clearNeighborCache();
      Point2D.Double aimedFromLocation = null;

      if (enemyGunHeat < 0.0000001 && _waveManager.size() >= 2) {
        _imaginaryWave = _waveManager.getWaveByFireTime(currentTime);
        aimedFromLocation = getState(currentTime - 1).location;
      } else {
        _imaginaryWave = _waveManager.getWaveByFireTime(currentTime + 1);
        aimedFromLocation = lastScanState.location;
        Point2D.Double sourceLocation = _battleField.translateToField(
            predictor.nextLocation(lastScanState));

        if (sourceLocation.distance(myRobotState.location) < BOT_WIDTH) {
          sourceLocation = lastScanState.location;
        }

        _imaginaryWave.sourceLocation = sourceLocation;
      }

      if (_imaginaryWave != null) { // null after a skipped turn
        // TODO: use Wave.setWallDistance
        _imaginaryWave.targetWallDistance =
            Math.min(1.5, _battleField.orbitalWallDistance(
                aimedFromLocation, _imaginaryWave.targetLocation,
                lastBulletPower, _imaginaryWave.orbitDirection));
        _imaginaryWave.targetRevWallDistance =
            Math.min(1.5, _battleField.orbitalWallDistance(
                aimedFromLocation, _imaginaryWave.targetLocation,
                lastBulletPower, -_imaginaryWave.orbitDirection));
        _imaginaryWave.absBearing = DiaUtils.absoluteBearing(
            aimedFromLocation, _imaginaryWave.targetLocation);
      }
    }
  }

  public Wave findSurfableWave(int surfWaveIndex, RobotState myRobotState) {
    Wave surfableWave = _waveManager.findSurfableWave(
        surfWaveIndex, myRobotState, Wave.WavePosition.BREAKING_CENTER);
    if (surfableWave == null && _imaginaryWave != null
        && surfWaveIndex == _imaginaryWaveIndex) {
      surfableWave = _imaginaryWave;
    }
    return surfableWave;
  }

  public void updateFiringWave(long currentTime, double bulletPower,
      RobotStateLog myStateLog, List<FiredBullet> firedBullets) {
    long fireTime = currentTime - 1;
    Wave enemyWave = _waveManager.getWaveByFireTime(fireTime);
    if (enemyWave == null) {
      enemyWave = _waveManager.interpolateWaveByFireTime(fireTime, currentTime,
          lastScanState.heading, lastScanState.velocity, myStateLog,
          _battleField, _predictor);
      _out.println("WARNING (move): Wave with fire time " + fireTime
          + " doesn't exist, interpolation "
          + (enemyWave == null ? "failed" : "succeeded") + ".");
      if (enemyWave != null) {
        enemyWave.firingWave = true;
        _waveManager.addWave(enemyWave);
      }
    }
    if (enemyWave != null) {
      Point2D.Double aimedFromLocation = getState(currentTime - 2).location;
      enemyWave.sourceLocation = getState(currentTime - 1).location;
      enemyWave.targetLocation = myStateLog.getState(currentTime - 2).location;
      enemyWave.absBearing = DiaUtils.absoluteBearing(
          aimedFromLocation, enemyWave.targetLocation);
      enemyWave.setBulletPower(bulletPower);
      // TODO: use Wave.setWallDistance
      enemyWave.targetWallDistance = Math.min(1.5,
          _battleField.orbitalWallDistance(aimedFromLocation,
              enemyWave.targetLocation, lastBulletPower,
              enemyWave.orbitDirection));
      enemyWave.targetRevWallDistance = Math.min(1.5,
          _battleField.orbitalWallDistance(aimedFromLocation,
              enemyWave.targetLocation, lastBulletPower,
              -enemyWave.orbitDirection));

      if (_imaginaryWave != null) {
        clearNeighborCache();
      }
      _imaginaryWave = null;

      enemyWave.firingWave = true;
      this.lastBulletPower = bulletPower;
      this.lastBulletFireTime = enemyWave.fireTime;
      for (FiredBullet bullet : firedBullets) {
        setShadows(enemyWave, bullet);
      }

      double[] dataPoint =
          bulletPowerDataPoint(
              enemyWave.targetDistance, enemyWave.sourceEnergy,
              enemyWave.targetEnergy);
      powerTree.addPoint(dataPoint, bulletPower);
    }
  }

  public void updateDamageFactor() {
    if (alive) {
      timeAliveTogether++;
      totalDistance += distance;
    }
    damageFactor = (((damageTaken + 10) * totalDistance) / timeAliveTogether);
  }

  public double guessBulletPower(double myEnergy) {
    int numBullets = powerTree.size();
    if (numBullets == 0) {
      return 1.9;
    } else {
      double[] searchPoint = bulletPowerDataPoint(distance, energy, myEnergy);
      List<Entry<Double>> bulletPowers =
          powerTree.nearestNeighbor(searchPoint,
              (int) Math.min(20, Math.ceil(numBullets / 3.0)), false);

      double powerTotal = 0;
      for (Entry<Double> entry : bulletPowers) {
        powerTotal += entry.value;
      }

      return DiaUtils.round(powerTotal / bulletPowers.size(), 6);
    }
  }

  private WaveBreakListener newWaveBreakListener(
      final int currentRound, final long currentTime) {
    return new WaveManager.WaveBreakListener() {
      @Override
      public void onWaveBreak(Wave w, List<RobotState> waveBreakStates) {
        if (w.firingWave) {
          Wave.Intersection preciseIntersection =
              w.preciseIntersection(waveBreakStates);

          for (KnnView<TimestampedGuessFactor> view : views.values()) {
            if (view.logVisits) {
              double guessFactor = w.guessFactor(preciseIntersection.angle);
              view.logWave(w, new TimestampedGuessFactor(
                  currentRound, currentTime, guessFactor));
            }
          }

          if (!w.bulletHitBullet) {
            raw1v1ShotsFired++;
            raw1v1ShotsFiredThisRound++;
            if (w.hitByBullet) {
              double angularBotWidth = preciseIntersection.bandwidth * 2;
              double thisHit = (TYPICAL_ANGULAR_BOT_WIDTH / angularBotWidth)
                  * (w.escapeAngleRange() / TYPICAL_ESCAPE_RANGE);
              weighted1v1ShotsHit += thisHit;
              weighted1v1ShotsHitThisRound += thisHit;
              raw1v1ShotsHit++;
              raw1v1ShotsHitThisRound++;
            }
          }
        }
      }
    };
  }

  public void drawRawWaves(final long currentTime) {
    _waveManager.forAllWaves(new WaveManager.AllWaveListener() {
      @Override
      public void onWave(Wave w) {
        if (w.firingWave) {
          _renderables.add(RoboGraphic.drawCircle(w.sourceLocation,
              (currentTime - w.fireTime + 1) * w.bulletSpeed(),
              Color.darkGray));
          _renderables.add(RoboGraphic.drawLine(w.sourceLocation,
              DiaUtils.project(w.sourceLocation, w.absBearing,
                  w.distanceTraveled(currentTime + 1)), Color.darkGray));
          _renderables.add(RoboGraphic.drawCircle(
              DiaUtils.project(w.sourceLocation, w.absBearing,
                  w.distanceTraveled(currentTime + 1)),
            5, Color.darkGray));
          _renderables.add(RoboGraphic.drawCircleFilled(
              w.sourceLocation, Color.darkGray, 4));
        }
      }
    });
  }

  private void drawBulletHit(Wave hitWave, double hitGuessFactor,
      Point2D.Double bulletLocation, long time) {
    _renderables.add(RoboGraphic.drawLine(hitWave.sourceLocation,
        DiaUtils.project(hitWave.sourceLocation, hitWave.absBearing,
            hitWave.distanceTraveled(time)),
        Color.yellow));
    if (Math.abs(hitGuessFactor) > 0.01) {
      _renderables.add(RoboGraphic.drawLine(hitWave.sourceLocation,
          bulletLocation, (hitGuessFactor > 0 ? Color.blue : Color.red)));
    }
  }

  public static class WallHitDamage {
    public final double min;
    public final double max;

    public WallHitDamage(double min, double max) {
      this.min = min;
      this.max = max;
    }
  }
}
