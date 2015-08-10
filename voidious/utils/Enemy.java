package voidious.utils;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class Enemy<T extends Timestamped> {
  // TODO: account for droids, team leaders
  protected static final double DEFAULT_ENERGY = 100;
  protected static final boolean IS_BULLET_HIT = false;
  protected static final boolean IS_VISIT = true;

  public String botName;
  public RobotState lastScanState;
  public double distance;
  public double absBearing;
  public double energy;
  public boolean alive;
  public int lastScanRound;
  public RobotStateLog stateLog;
  public double damageGiven;
  public long timeAliveTogether;

  public Map<String, KnnView<T>> views;
  protected Map<String, Double> _botDistancesSq;
  protected WaveManager _waveManager;
  protected BattleField _battleField;
  protected MovementPredictor _predictor;

  protected Enemy(String botName, Point2D.Double location, double distance,
      double energy, double heading, double velocity, double absBearing,
      int round, long time, BattleField battleField,
      MovementPredictor predictor, WaveManager waveManager) {
    this.botName = botName;
    this.absBearing = absBearing;
    this.distance = distance;
    this.energy = energy;
    this.lastScanRound = round;
    timeAliveTogether = 1;
    _battleField = battleField;
    _predictor = predictor;
    _waveManager = waveManager;

    damageGiven = 0;
    alive = true;
    views = new HashMap<String, KnnView<T>>();
    stateLog = new RobotStateLog();
    setRobotState(RobotState.newBuilder()
        .setLocation(location)
        .setHeading(heading)
        .setVelocity(velocity)
        .setTime(time)
        .build());
    _botDistancesSq = new HashMap<String, Double>();
  }

  public void initRound() {
    energy = DEFAULT_ENERGY;
    distance = 1000;
    alive = true;
    stateLog.clear();
    _waveManager.initRound();
    clearDistancesSq();
  }

  public void addViews(List<KnnView<T>> views) {
    for (KnnView<T> view : views) {
      addView(view);
    }
  }

  public void addView(KnnView<T> view) {
    views.put(view.name, view);
  }

  public void setRobotState(RobotState robotState) {
    lastScanState = robotState;
    stateLog.addState(robotState);
  }

  public RobotState getState(long time) {
    return stateLog.getState(time);
  }

  public double getBotDistanceSq(String name) {
    if (!_botDistancesSq.containsKey(name)) {
      return Double.NaN;
    }
    return _botDistancesSq.get(name);
  }

  public void setBotDistanceSq(String name, double distance) {
    _botDistancesSq.put(name, distance);
  }

  public void removeDistanceSq(String botName) {
    _botDistancesSq.remove(botName);
  }

  public void clearDistancesSq() {
    _botDistancesSq.clear();
  }
}
