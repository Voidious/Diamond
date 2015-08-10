package voidious.gun;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import voidious.gfx.RoboGraphic;
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

public class GunDataManager extends EnemyDataManager<GunEnemy> {
  static final String WARNING_BULLET_HIT_UNKNOWN =
      "I shot a bot that I never knew existed!";
  static final String WARNING_BULLET_HIT_BULLET_UNKNOWN =
      "I shot a bullet by a bot that I never knew existed!";

  private List<GunDataListener> _listeners;
  private long _lastBulletFiredTime;
  private Map<Long, List<RoboGraphic>> _victoryGraphics;

  public GunDataManager(int enemiesTotal, BattleField battleField,
      MovementPredictor predictor, Collection<RoboGraphic> renderables,
      OutputStream out) {
    super(enemiesTotal, battleField, predictor, renderables, out);
    _listeners = new ArrayList<GunDataListener>();
    _victoryGraphics = new HashMap<Long, List<RoboGraphic>>();
  }

  @Override
  public void initRound() {
    super.initRound();
    _lastBulletFiredTime = 0;
    _victoryGraphics.clear();
  }

  public void execute(int currentRound, long currentTime, double bulletPower,
      double currentGunHeat, Point2D.Double myLocation, boolean is1v1,
      boolean painting) {
    updateBotDistances(myLocation);
    for (GunEnemy gunData : getAllEnemyData()) {
      if (gunData.alive) {
        gunData.execute(currentTime, _lastBulletFiredTime, bulletPower,
            currentGunHeat, myLocation, is1v1, _enemiesTotal, _listeners,
            painting);
      }
    }
    if (painting && _victoryGraphics.containsKey(currentTime)) {
      for (RoboGraphic graphic : _victoryGraphics.get(currentTime)) {
        _renderables.add(graphic);
      }
    }
  }

  public GunEnemy newEnemy(ScannedRobotEvent e, Point2D.Double enemyLocation,
      double absBearing, int currentRound, boolean is1v1) {
    if (hasEnemy(e.getName())) {
      throw new IllegalArgumentException(
          "GunEnemy already exists for bot: " + e.getName());
    }

    GunEnemy gunData = new GunEnemy(e.getName(), e.getDistance(),
        e.getEnergy(), enemyLocation, currentRound, e.getTime(),
        e.getHeadingRadians(), e.getVelocity(), absBearing,
        _battleField, _predictor, _renderables);
    gunData.updateTimers(e.getVelocity());
    saveEnemy(e.getName(), gunData);
    if (is1v1) {
      _duelEnemy = gunData;
    }
    return gunData;
  }

  void saveEnemy(String botName, GunEnemy gunData) {
    _enemies.put(botName, gunData);
  }

  public GunEnemy updateEnemy(ScannedRobotEvent e, Point2D.Double enemyLocation,
      double absBearing, int currentRound, boolean is1v1) {
    GunEnemy gunData = getEnemyData(e.getName());
    long timeSinceLastScan = e.getTime() - gunData.lastScanState.time;
    gunData.timeSinceDirectionChange += timeSinceLastScan;
    gunData.timeSinceVelocityChange += timeSinceLastScan;

    gunData.updateTimers(e.getVelocity());
    gunData.distance = e.getDistance();
    gunData.absBearing = absBearing;
    gunData.energy = e.getEnergy();
    gunData.lastScanRound = currentRound;
    gunData.previousVelocity = gunData.lastScanState.velocity;
    gunData.setRobotState(RobotState.newBuilder()
        .setLocation(enemyLocation)
        .setHeading(e.getHeadingRadians())
        .setVelocity(e.getVelocity())
        .setTime(e.getTime())
        .build());
    gunData.timeAliveTogether++;
    if (gunData.advancingVelocity() > 6) {
      gunData.timeMovingAtMe++;
    }
    if (is1v1) {
      _duelEnemy = gunData;
    }
    return gunData;
  }

  public void onBulletHit(BulletHitEvent e, long currentTime) {
    String botName = e.getName();
    if (hasEnemy(botName)) {
      GunEnemy gunData = getEnemyData(e.getName());
      double bulletDamage = Math.min(
          Rules.getBulletDamage(e.getBullet().getPower()), gunData.energy);
      gunData.damageGiven += bulletDamage;
      gunData.logBulletHitLocation(e.getBullet());
    } else {
      _out.println(warning() + WARNING_BULLET_HIT_UNKNOWN
          + " (" + botName + ")");
    }
    for (GunEnemy gunData : getAllEnemyData()) {
      Wave hitWave = gunData.processBulletHit(
          e.getBullet(), currentTime, (_enemiesTotal == 1), true);
      if (hitWave != null) {
        hitWave.hitByBullet = true;
      }
    }
  }

  public void onBulletHitBullet(BulletHitBulletEvent e, long currentTime) {
    String botName = e.getHitBullet().getName();
    for (GunEnemy gunData : getAllEnemyData()) {
      Wave hitWave = gunData.processBulletHit(e.getBullet(), currentTime,
          (_enemiesTotal == 1), botName.equals(gunData.botName));
      if (hitWave != null) {
        hitWave.bulletHitBullet = true;
      }
    }
    if (!hasEnemy(botName) && !botName.equals(e.getBullet().getName())) {
      _out.println(warning() + WARNING_BULLET_HIT_BULLET_UNKNOWN
          + " (" + botName + ")");
    }
  }

  public double getDamageGiven() {
    double damageGiven = 0;
    for (GunEnemy gunData : getAllEnemyData()) {
      damageGiven += gunData.damageGiven;
    }
    return damageGiven;
  }

  public double getAverageEnergy() {
    double totalEnergy = 0;
    int enemiesAlive = 0;
    for (GunEnemy gunData : getAllEnemyData()) {
      if (gunData.alive) {
        totalEnergy += gunData.energy;
        enemiesAlive++;
      }
    }
    return (enemiesAlive == 0) ? 0 : totalEnergy / enemiesAlive;
  }

  public void markFiringWaves(long currentTime, boolean is1v1) {
    _lastBulletFiredTime = currentTime;
    for (GunEnemy gunData : getAllEnemyData()) {
      gunData.markFiringWaves(currentTime, is1v1, _listeners);
    }
  }

  public void fireNextTickWave(Point2D.Double myNextLocation,
      Point2D.Double targetLocation, String targetName, int currentRound,
      long currentTime, double bulletPower, double myEnergy, double gunHeat,
      double myHeading, double myVelocity, int enemiesAlive) {
    GunEnemy gunData = getEnemyData(targetName);
    RobotState lastScanState = gunData.lastScanState;
    Point2D.Double enemyNextLocation = _battleField.translateToField(
        _predictor.nextLocation(
            targetLocation, lastScanState.heading, lastScanState.velocity));
    double accel = DiaUtils.limit(-Rules.DECELERATION,
                                  DiaUtils.accel(lastScanState.velocity,
                                                 gunData.previousVelocity),
                                  Rules.ACCELERATION);
    long fireTime = currentTime + 1;
    long lastWaveFireTime = gunData.getLastWaveFireTime(gunData.botName);
    RobotStateLog stateLog = gunData.stateLog;
    double dl8t =
        stateLog.getDisplacementDistance(targetLocation, currentTime, 8);
    double dl20t =
        stateLog.getDisplacementDistance(targetLocation, currentTime, 20);
    double dl40t =
        stateLog.getDisplacementDistance(targetLocation, currentTime, 40);

    Wave nextWave = gunData.newGunWave(myNextLocation, enemyNextLocation,
        currentRound, fireTime, _lastBulletFiredTime, bulletPower, myEnergy,
        gunHeat, enemiesAlive, accel, dl8t, dl20t, dl40t, false);
    gunData.lastWaveFired = nextWave;

    for (GunEnemy altGunData : getAllEnemyData()) {
      if (altGunData.alive && !altGunData.botName.equals(targetName)) {
        Point2D.Double altNextLocation = _battleField.translateToField(
            _predictor.nextLocation(altGunData.lastScanState.location,
                altGunData.lastScanState.velocity,
                altGunData.lastScanState.heading));
        gunData.newGunWave(altNextLocation, enemyNextLocation, currentRound,
            fireTime, _lastBulletFiredTime, bulletPower, altGunData.energy,
            gunHeat, enemiesAlive, accel, dl8t, dl20t, dl40t, true);
      }
    }

    if (_duelEnemy != null && lastWaveFireTime > 0) {
      for (long time = lastWaveFireTime + 1; time < fireTime; time++) {
        gunData.interpolateGunWave(
            time, currentTime, myHeading, myVelocity, lastScanState);
      }
    }
  }

  public void addListener(GunDataListener listener) {
    _listeners.add(listener);
  }

  public int getEnemiesTotal() {
    return _enemiesTotal;
  }

  public int getDuelDataSize() {
    return _duelEnemy.getWaveBreaks();
  }

  @Override
  protected String getLabel() {
    return "gun";
  }

  public void drawVictory(long startTime) {
    for (GunEnemy gunData : getAllEnemyData()) {
      long hitStartTime = startTime;
      Point2D.Double previousHit = null;
      for (Point2D.Double hitLocation : gunData.hitLocations) {
        boolean deathBlow = (hitLocation
            == gunData.hitLocations.get(gunData.hitLocations.size() - 1));
        for (int x = 0; x < 7; x++) {
          addVictoryGraphic(hitStartTime + x,
              RoboGraphic.drawCircleFilled(hitLocation, Color.WHITE, 5));
        }
        for (int x = 0; x < (deathBlow ? 5 : 3); x++) {
          for (int y = 0; y < (x + 1) * 3; y++) {
            addVictoryGraphic(hitStartTime + y + x,
                RoboGraphic.drawCircle(hitLocation, (y + 2) * 5, Color.RED));
          }
        }
        if (previousHit != null) {
          for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 8; y++) {
              addVictoryGraphic(hitStartTime - 2 + x + y, RoboGraphic.drawLine(
                  averagePoints(previousHit, hitLocation, ((double) x) / 3),
                  averagePoints(previousHit, hitLocation, ((double) x + 1) / 3),
                  Color.WHITE));
            }
          }
        }
        previousHit = hitLocation;
        hitStartTime += 3;
      }
    }
  }

  private Point2D.Double averagePoints(
      Point2D.Double p1, Point2D.Double p2, double weight) {
    return new Point2D.Double(p1.x * (1 - weight) + p2.x * weight,
                              p1.y * (1 - weight) + p2.y * weight);
  }

  private void addVictoryGraphic(long time, RoboGraphic graphic) {
    if (!_victoryGraphics.containsKey(time)) {
      _victoryGraphics.put(time, new ArrayList<RoboGraphic>());
    }
    _victoryGraphics.get(time).add(graphic);
  }
  public interface GunDataListener {
    void on1v1FiringWaveBreak(Wave w, double hitAngle, double tolerance);
    void onMarkFiringWave(Wave w);
  }
}
