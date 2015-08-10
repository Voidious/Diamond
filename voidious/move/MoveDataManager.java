package voidious.move;

import java.awt.geom.Point2D;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.HitByBulletEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

import voidious.gfx.RoboGraphic;
import voidious.gun.FireListener.FiredBullet;
import voidious.move.MoveEnemy.WallHitDamage;
import voidious.utils.BattleField;
import voidious.utils.DiaUtils;
import voidious.utils.EnemyDataManager;
import voidious.utils.MovementPredictor;
import voidious.utils.RobotState;
import voidious.utils.RobotStateLog;
import voidious.utils.Wave;

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

public class MoveDataManager extends EnemyDataManager<MoveEnemy> {
  private static final double NON_ZERO_VELOCITY_THRESHOLD = 0.1;
  private static final double DIRECTION_CHANGE_THRESHOLD = Math.PI / 2;
  private static final long EARLIEST_FIRE_TIME = 30L;

  private double _previousHeading;
  private double _currentHeading;
  private long _timeSinceReverseDirection;
  private long _timeSinceVelocityChange;
  private double _lastVelocity; // TODO: consolidate these? map by time instead?
  private double _previousVelocity;
  private double _lastNonZeroVelocity;
  private RobotStateLog _myStateLog;
  private List<FiredBullet> _firedBullets;

  protected MoveDataManager(int enemiesTotal, BattleField battleField,
      MovementPredictor predictor, Collection<RoboGraphic> renderables,
      OutputStream out) {
    super(enemiesTotal, battleField, predictor, renderables, out);
    _previousVelocity = _lastVelocity = _lastNonZeroVelocity = 0;
    _myStateLog = new RobotStateLog();
    _firedBullets = new ArrayList<FiredBullet>();
  }

  public void execute(int round, long time, Point2D.Double myLocation,
      double heading, double velocity) {
    _myStateLog.addState(RobotState.newBuilder()
        .setLocation(myLocation)
        .setHeading(heading)
        .setVelocity(velocity)
        .setTime(time)
        .build());
    _previousHeading = _currentHeading;
    if (Math.abs(velocity) > NON_ZERO_VELOCITY_THRESHOLD) {
      _lastNonZeroVelocity = velocity;
    }
    _previousVelocity = velocity;
    _currentHeading = Utils.normalAbsoluteAngle(
        heading + (_lastNonZeroVelocity < 0 ? Math.PI : 0));
    updateBotDistances(myLocation);
    updateDamageFactors();
    removeOldFiredBullets(time);
    if (_duelEnemy != null) {
      _duelEnemy.execute1v1(round, time, myLocation);
    } else {
      updateTimers(velocity);
    }
  }

  private void removeOldFiredBullets(long currentTime) {
    Iterator<FiredBullet> bulletIterator = _firedBullets.iterator();
    while (bulletIterator.hasNext()) {
      FiredBullet firedBullet = bulletIterator.next();
      if (!_battleField.rectangle.contains(firedBullet.position(currentTime))) {
        bulletIterator.remove();
      }
    }
  }

  private void updateDamageFactors() {
    if (isMeleeBattle()) {
      for (MoveEnemy moveData : getAllEnemyData()) {
        moveData.updateDamageFactor();
      }
    }
  }

  @Override
  public void initRound() {
    super.initRound();
    _currentHeading = _previousHeading = 0;
    _timeSinceReverseDirection = 0;
    _timeSinceVelocityChange = 0;
    _previousVelocity = _lastVelocity = _lastNonZeroVelocity = 0;
    _myStateLog.clear();
    _firedBullets.clear();
  }

  public MoveEnemy newEnemy(ScannedRobotEvent e, Point2D.Double enemyLocation,
      double absBearing, int currentRound, boolean is1v1) {
    String botName = e.getName();
    MoveEnemy moveData = new MoveEnemy(botName, e.getDistance(), e.getEnergy(),
        enemyLocation, e.getHeadingRadians(), e.getVelocity(), absBearing,
        currentRound, e.getTime(), _renderables, _battleField, _predictor,
        _out);
    saveEnemy(botName, moveData);
    if (is1v1) {
      _duelEnemy = moveData;
    }
    return moveData;
  }

  void saveEnemy(String botName, MoveEnemy moveData) {
    _enemies.put(botName, moveData);
  }

  public MoveEnemy updateEnemy(ScannedRobotEvent e,
      Point2D.Double enemyLocation, double absBearing, int currentRound,
      boolean is1v1) {
    MoveEnemy moveData = getEnemyData(e.getName());
    moveData.wallHitDamage = getWallHitDamage(e, enemyLocation, moveData);
    moveData.setRobotState(RobotState.newBuilder()
        .setLocation(enemyLocation)
        .setHeading(e.getHeadingRadians())
        .setVelocity(e.getVelocity())
        .setTime(e.getTime())
        .build());
    moveData.energy = e.getEnergy();
    moveData.distance = e.getDistance();
    moveData.absBearing = absBearing;
    moveData.lastScanRound = currentRound;
    if (is1v1) {
      _duelEnemy = moveData;
    }
    return moveData;
  }

  private WallHitDamage getWallHitDamage(
      ScannedRobotEvent e, Point2D.Double enemyLocation, MoveEnemy moveData) {
    RobotState lastState = moveData.lastScanState;
    if (!moveData.isRobot && e.getTime() - lastState.time == 1
        && Math.abs(lastState.velocity - e.getVelocity()) > 2
        && Math.abs(e.getVelocity()) < 0.0001
        && DiaUtils.distanceToWall(enemyLocation, _battleField) < 0.0001) {
      if (Math.abs(e.getEnergy() - moveData.energy) < 0.0001) {
        moveData.isRobot = true;
      } else {
        double maxSpeed =
            Math.min(8.0, Math.abs(lastState.velocity) + Rules.ACCELERATION);
        double minSpeed = Math.abs(lastState.velocity) - Rules.DECELERATION;
        return new MoveEnemy.WallHitDamage(
            Rules.getWallHitDamage(minSpeed), Rules.getWallHitDamage(maxSpeed));
      }
    }
    return new MoveEnemy.WallHitDamage(0, 0);
  }

  public void onHitByBullet(HitByBulletEvent e, int currentRound,
      long currentTime, boolean painting) {
    String botName = e.getName();
    if (hasEnemy(botName)) {
      MoveEnemy moveData = getEnemyData(botName);
      moveData.lastTimeHit = currentTime;
      moveData.damageTaken += Rules.getBulletDamage(e.getBullet().getPower());
      moveData.totalBulletPower += e.getBullet().getPower();
      moveData.totalTimesHit++;
      moveData.energy += Rules.getBulletHitBonus(e.getBullet().getPower());
      Wave hitWave = moveData.processBullet(
          e.getBullet(), currentRound, currentTime, painting);
      if (hitWave != null) {
        hitWave.hitByBullet = true;
      }
    } else {
      _out.println(warning() + "A bot shot me that I never knew existed! ("
          + botName + ")");
    }

    if (_duelEnemy != null) {
      _duelEnemy.clearNeighborCache();
    }
  }

  public void onBulletHitBullet(BulletHitBulletEvent e, int currentRound,
      long currentTime, boolean painting) {
    String botName = e.getHitBullet().getName();
    if (hasEnemy(botName)) {
      MoveEnemy moveData = getEnemyData(botName);
      Wave hitWave = moveData.processBullet(
          e.getHitBullet(), currentRound, currentTime, painting);
      if (hitWave != null) {
        hitWave.bulletHitBullet = true;
      }
    } else if (!botName.equals(e.getBullet().getName())) {
      _out.println(warning() + "One of my bullets hit a bullet from a "
          + "bot that I never knew existed! (" + botName + ")");
    }

    removeFiredBullet(e);
    if (_duelEnemy != null) {
      _duelEnemy.resetBulletShadows(_firedBullets);
      _duelEnemy.clearNeighborCache();
    }
  }

  public void onBulletHit(BulletHitEvent e) {
    String botName = e.getName();
    try {
      MoveEnemy moveData = getEnemyData(botName);
      double bulletDamage = Rules.getBulletDamage(e.getBullet().getPower());
      moveData.energy -= bulletDamage;
      moveData.damageGiven += bulletDamage;
    } catch (NullPointerException npe) {
      _out.println(warning() + "One of my bullets hit a bot that I never "
          + "knew existed! (" + botName + ")");
    }
  }

  public void updateEnemyWaves(Point2D.Double myLocation,
      double previousEnemyEnergy, String botName, int currentRound,
      long currentTime, double myEnergy, double myHeading, double myVelocity,
      int wavesToSurf) {
    RobotState myRobotState = RobotState.newBuilder()
        .setLocation(myLocation)
        .setHeading(myHeading)
        .setVelocity(myVelocity)
        .setTime(currentTime)
        .build();
    _myStateLog.addState(myRobotState);
    MoveEnemy moveData = getEnemyData(botName);
    boolean detectedEnemyBullet = false;
    double energyDrop =
        previousEnemyEnergy - moveData.energy - moveData.wallHitDamage.max;
    if (energyDrop > 0.0999 && energyDrop < 3.0001
        && moveData.getGunHeat(currentTime) < 0.0001) {
      detectedEnemyBullet = true;
    }

    updateTimers(myVelocity);
    int velocitySign = DiaUtils.nonZeroSign(
        Math.abs(myVelocity) > NON_ZERO_VELOCITY_THRESHOLD
            ? myVelocity : _lastNonZeroVelocity);
    double accel =
        DiaUtils.limit(-Rules.DECELERATION,
                       DiaUtils.accel(myVelocity, _previousVelocity),
                       Rules.ACCELERATION);
    double dl8t =
        _myStateLog.getDisplacementDistance(myLocation, currentTime, 8);
    double dl20t =
        _myStateLog.getDisplacementDistance(myLocation, currentTime, 20);
    double dl40t =
        _myStateLog.getDisplacementDistance(myLocation, currentTime, 40);
    double guessedPower = moveData.guessBulletPower(myEnergy);
    long fireTime = currentTime + 1;
    Point2D.Double enemyNextLocation = _battleField.translateToField(
        _predictor.nextLocation(moveData.lastScanState));

    moveData.newMoveWave(enemyNextLocation, myLocation,
        DiaUtils.absoluteBearing(moveData.lastScanState.location, myLocation),
        currentRound, fireTime, guessedPower, myEnergy, myHeading, myVelocity,
        velocitySign, accel, dl8t, dl20t, dl40t, _timeSinceReverseDirection,
        _timeSinceVelocityChange);
    moveData.updateImaginaryWave(
        currentTime, myRobotState, wavesToSurf, _predictor);
    if (detectedEnemyBullet && currentTime > EARLIEST_FIRE_TIME) {
      moveData.updateFiringWave(
          currentTime, energyDrop, _myStateLog, _firedBullets);
    }
  }

  // In FFA, do this from execute().
  // In 1v1, do this before firing waves.
  void updateTimers(double velocity) {
    if (Math.abs(Utils.normalRelativeAngle(_currentHeading - _previousHeading))
            > DIRECTION_CHANGE_THRESHOLD) {
      _timeSinceReverseDirection = 0;
    } else {
      _timeSinceReverseDirection++;
    }

    double newVelocity = velocity;
    if (Math.abs(newVelocity - _lastVelocity) > 0.5) {
      _timeSinceVelocityChange = 0;
    } else {
      _timeSinceVelocityChange++;
    }
    _lastVelocity = newVelocity;
  }

  public RobotStateLog myStateLog() {
    return _myStateLog;
  }

  public void addFiredBullet(FiredBullet bullet) {
    _firedBullets.add(bullet);
    if (_duelEnemy != null) {
      _duelEnemy.setShadows(bullet);
    }
  }

  private void removeFiredBullet(BulletHitBulletEvent e) {
    double closestDistanceSq = Double.POSITIVE_INFINITY;
    FiredBullet closestFiredBullet = null;
    Point2D.Double bulletPoint =
        new Point2D.Double(e.getBullet().getX(), e.getBullet().getY());
    for (FiredBullet firedBullet : _firedBullets) {
      double thisDistanceSq =
          firedBullet.position(e.getTime()).distanceSq(bulletPoint);
      if (thisDistanceSq < closestDistanceSq) {
        closestDistanceSq = thisDistanceSq;
        closestFiredBullet = firedBullet;
      }
    }
    int bulletDistanceThreshold = 40;
    if (closestFiredBullet != null
        && closestDistanceSq < DiaUtils.square(bulletDistanceThreshold)) {
      closestFiredBullet.deathTime = e.getTime();
    }
  }

  @Override
  protected String getLabel() {
    return "move";
  }

  RobotStateLog getMyStateLog() {
    return _myStateLog;
  }

  List<FiredBullet> getFiredBullets() {
    return _firedBullets;
  }
}
