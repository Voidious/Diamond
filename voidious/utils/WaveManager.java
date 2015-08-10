package voidious.utils;

import static voidious.utils.DiaUtils.square;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import voidious.utils.Wave.WavePosition;

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

public class WaveManager {
  private static final int WAVE_MATCH_THRESHOLD = 50;
  private static final double COOLING_RATE = 0.1;
  private static final double MAX_GUN_HEAT = 1.6;

  private List<Wave> _waves;
  private Map<Wave, RobotStateLog> _stateLogs;

  public WaveManager() {
    _waves = new ArrayList<Wave>();
    _stateLogs = new HashMap<Wave, RobotStateLog>();
  }

  public void initRound() {
    _waves.clear();
    _stateLogs.clear();
  }

  public void addWave(Wave wave) {
    _waves.add(wave);
  }

  public void checkCurrentWaves(
      long currentTime, CurrentWaveListener listener) {
    for (Wave w : _waves) {
      if (w.fireTime == currentTime) {
        listener.onCurrentWave(w);
      }
    }
  }

  public void forAllWaves(AllWaveListener listener) {
    for (Wave w : _waves) {
      listener.onWave(w);
    }
  }

  public void checkActiveWaves(
      long currentTime, RobotState lastScanState, WaveBreakListener listener) {
    if (lastScanState.time == currentTime) {
      Iterator<Wave> wavesIterator = _waves.iterator();
      while (wavesIterator.hasNext()) {
        Wave w = wavesIterator.next();
        addRobotState(w, lastScanState);
        if (w.checkWavePosition(lastScanState) == WavePosition.GONE) {
          List<RobotState> waveBreakStates = getWaveBreakStates(w, currentTime);
          listener.onWaveBreak(w, waveBreakStates);
          wavesIterator.remove();
        }
      }
    }
  }

  void addRobotState(Wave w, RobotState state) {
    RobotStateLog stateLog;
    if (_stateLogs.containsKey(w)) {
      stateLog = _stateLogs.get(w);
    } else {
      stateLog = new RobotStateLog();
      _stateLogs.put(w, stateLog);
    }
    stateLog.addState(state);
  }

  List<RobotState> getWaveBreakStates(Wave w, long currentTime) {
    List<RobotState> waveBreakStates = new ArrayList<RobotState>();
    if (_stateLogs.containsKey(w)) {
      RobotStateLog log = _stateLogs.get(w);
      for (long time = w.fireTime; time < currentTime; time++) {
        RobotState state = log.getState(time);
        if (state != null && w.checkWavePosition(state).isBreaking()) {
          waveBreakStates.add(state);
        }
      }
    }
    return waveBreakStates;
  }

  public Wave findClosestWave(Point2D.Double targetLocation, long currentTime,
      boolean onlyFiring, String botName, double bulletPower) {
    double closestDistance = Double.POSITIVE_INFINITY;
    Wave closestWave = null;

    for (Wave w : _waves) {
      if (!w.altWave && (!onlyFiring || w.firingWave)
          && (bulletPower == Wave.ANY_BULLET_POWER
              || Math.abs(bulletPower - w.bulletPower()) < 0.001)
          && (botName == null || botName.equals(w.botName)
              || botName.equals(""))) {
        double targetDistanceSq = w.sourceLocation.distanceSq(targetLocation);
        double waveDistanceTraveled = w.distanceTraveled(currentTime);
        if (targetDistanceSq < square(
                waveDistanceTraveled + WAVE_MATCH_THRESHOLD)
            && targetDistanceSq > square(
                Math.max(0, waveDistanceTraveled - WAVE_MATCH_THRESHOLD))) {
          double distanceFromTargetToWave =
              Math.sqrt(targetDistanceSq) - waveDistanceTraveled;
          if (Math.abs(distanceFromTargetToWave) < closestDistance) {
            closestDistance = Math.abs(distanceFromTargetToWave);
            closestWave = w;
          }
        }
      }
    }

    return closestWave;
  }

  public Wave findSurfableWave(
      int surfIndex, RobotState targetState, WavePosition unsurfablePosition) {
    int searchWaveIndex = 0;

    for (Wave w : _waves) {
      if (w.firingWave && !w.processedBulletHit()) {
        WavePosition wavePosition =
            w.checkWavePosition(targetState, unsurfablePosition);
        if (wavePosition.getIndex() < unsurfablePosition.getIndex()) {
          if (searchWaveIndex == surfIndex) {
            return w;
          } else {
            searchWaveIndex++;
          }
        }
      }
    }

    return null;
  }

  public Wave getPastWave(int x) {
    return _waves.get(_waves.size() - 1 - x);
  }

  public Wave getWaveByFireTime(long fireTime) {
    for (Wave w : _waves) {
      if (w.fireTime == fireTime) {
        return w;
      }
    }
    return null;
  }

  public Wave interpolateWaveByFireTime(long fireTime, long currentTime,
      double sourceHeading, double sourceVelocity, RobotStateLog stateLog,
      BattleField battleField, MovementPredictor predictor) {
    Wave beforeWave = null;
    Wave afterWave = null;
    for (Wave w : _waves) {
      if (!w.altWave) {
        if (w.fireTime < fireTime) {
          if (beforeWave == null || w.fireTime > beforeWave.fireTime) {
            beforeWave = w;
          }
        }
        if (w.fireTime > fireTime) {
          if (afterWave == null || w.fireTime < afterWave.fireTime) {
            afterWave = w;
          }
        }
      }
    }

    if (beforeWave == null && afterWave == null) {
      return null;
    } else if (beforeWave == null) {
      return interpolateWave(afterWave, fireTime - afterWave.fireTime,
          sourceHeading, sourceVelocity, battleField);
    } else if (afterWave == null) {
      return interpolateWave(beforeWave, fireTime - beforeWave.fireTime,
          sourceHeading, sourceVelocity, battleField);
    } else {
      return interpolateWave(
          beforeWave, afterWave, fireTime, stateLog, battleField, predictor);
    }
  }

  private Wave interpolateWave(Wave baseWave1, Wave baseWave2, long fireTime,
      RobotStateLog stateLog, BattleField battleField,
      MovementPredictor predictor) {
    Interpolator interpolator = new Interpolator(
        fireTime, baseWave1.fireTime, baseWave2.fireTime);

    Point2D.Double sourceLocation = interpolator.getLocation(
        baseWave1.sourceLocation, baseWave2.sourceLocation);
    Point2D.Double targetLocation = interpolator.getLocation(
        baseWave1.targetLocation, baseWave2.targetLocation);
    double bulletPower =
        interpolator.avg(baseWave1.bulletPower(), baseWave2.bulletPower());
    double targetHeading = interpolator.getHeading(
        baseWave1.targetHeading, baseWave2.targetHeading);
    double targetVelocity =
        interpolator.avg(baseWave1.targetVelocity, baseWave2.targetVelocity);
    int targetVelocitySign;
    if (DiaUtils.nonZeroSign(targetVelocity)
            == DiaUtils.nonZeroSign(baseWave1.targetVelocity)) {
      targetVelocitySign = baseWave1.targetVelocitySign;
    } else {
      targetVelocitySign = baseWave2.targetVelocitySign;
    }
    // TODO: not sure this is the best way to interpolate accel
    double targetAccel =
        interpolator.avg(baseWave1.targetAccel, baseWave2.targetAccel);
    long targetDchangeTime = interpolator.getTimer(
        baseWave1.targetDchangeTime, baseWave2.targetDchangeTime);
    long targetVchangeTime = interpolator.getTimer(
        baseWave1.targetVchangeTime, baseWave2.targetVchangeTime);
    double targetDl8t =
        stateLog.getDisplacementDistance(targetLocation, fireTime, 8);
    double targetDl20t =
        stateLog.getDisplacementDistance(targetLocation, fireTime, 20);
    double targetDl40t =
        stateLog.getDisplacementDistance(targetLocation, fireTime, 40);
    double targetEnergy =
        interpolator.avg(baseWave1.targetEnergy, baseWave2.targetEnergy);
    double sourceEnergy =
        interpolator.avg(baseWave1.sourceEnergy, baseWave2.sourceEnergy);

    long lastBulletFiredTime = baseWave1.lastBulletFiredTime;
    double gunHeat = 
        baseWave1.gunHeat - ((fireTime - baseWave1.fireTime) * COOLING_RATE);
    if (gunHeat <= 0) {
      lastBulletFiredTime = baseWave1.fireTime
          + (long) Math.ceil(baseWave1.gunHeat / COOLING_RATE);
      gunHeat = (baseWave2.gunHeat
              + ((baseWave2.fireTime - fireTime) * COOLING_RATE))
          % MAX_GUN_HEAT;
    }

    return new Wave(baseWave1.botName, sourceLocation, targetLocation,
        baseWave1.fireRound, fireTime, bulletPower, targetHeading,
        targetVelocity, targetVelocitySign, battleField, predictor)
            .setAbsBearing(
                DiaUtils.absoluteBearing(sourceLocation, targetLocation))
            .setAccel(targetAccel)
            .setDistance(sourceLocation.distance(targetLocation))
            .setDchangeTime(targetDchangeTime)
            .setVchangeTime(targetVchangeTime)
            .setDistanceLast8Ticks(targetDl8t)
            .setDistanceLast20Ticks(targetDl20t)
            .setDistanceLast40Ticks(targetDl40t)
            .setTargetEnergy(targetEnergy)
            .setSourceEnergy(sourceEnergy)
            .setGunHeat(gunHeat)
            .setEnemiesAlive(baseWave2.enemiesAlive)
            .setLastBulletFiredTime(lastBulletFiredTime);
  }

  private Wave interpolateWave(Wave baseWave, long timeOffset,
      double sourceHeading, double sourceVelocity, BattleField battleField) {
    Wave w = (Wave) baseWave.clone();
    w.sourceLocation = battleField.translateToField(
        DiaUtils.project(baseWave.sourceLocation, sourceHeading,
            sourceVelocity * timeOffset));
    w.targetLocation = battleField.translateToField(
        DiaUtils.project(baseWave.targetLocation, baseWave.targetHeading,
            baseWave.targetVelocity * timeOffset));
    w.absBearing = DiaUtils.absoluteBearing(w.sourceLocation, w.targetLocation);
    w.fireTime = w.fireTime + timeOffset;
    w.targetDistance = w.sourceLocation.distance(w.targetLocation);
    return w;
  }

  public long getLastFireTime() {
    long lastFireTime = -1;
    for (Wave w : _waves) {
      if (!w.altWave && w.fireTime > lastFireTime) {
        lastFireTime = w.fireTime;
      }
    }
    return lastFireTime;
  }

  public int size() {
    return _waves.size();
  }

  public interface CurrentWaveListener {
    void onCurrentWave(Wave w);
  }

  public interface WaveBreakListener {
    void onWaveBreak(Wave w, List<RobotState> waveBreakStates);
  }

  public interface AllWaveListener {
    void onWave(Wave w);
  }
}
