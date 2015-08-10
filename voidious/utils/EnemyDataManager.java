package voidious.utils;

import java.awt.geom.Point2D;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import robocode.RobotDeathEvent;
import voidious.gfx.RoboGraphic;

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

public abstract class EnemyDataManager<T extends Enemy<? extends Timestamped>> {
  static final String WARNING_ROBOT_DEATH_UNKNOWN =
      "A bot died that I never knew existed!";
  protected T _duelEnemy;
  protected int _enemiesTotal;
  protected PrintStream _out;
  protected Map<String, T> _enemies;
  protected BattleField _battleField;
  protected MovementPredictor _predictor;
  protected Collection<RoboGraphic> _renderables;

  protected EnemyDataManager(int enemiesTotal, BattleField battleField,
      MovementPredictor predictor, Collection<RoboGraphic> renderables,
      OutputStream out) {
    _duelEnemy = null;
    _enemiesTotal = enemiesTotal;
    _battleField = battleField;
    _predictor = predictor;
    _renderables = renderables;
    _out = new PrintStream(out);
    _enemies = new HashMap<String, T>(enemiesTotal);
  }

  public boolean hasEnemy(String botName) {
    return _enemies.containsKey(botName);
  }

  public T getEnemyData(String botName) {
    return _enemies.get(botName);
  }

  public Collection<T> getAllEnemyData() {
    return Collections.unmodifiableCollection(_enemies.values());
  }

  public void initRound() {
    for (T enemyData : getAllEnemyData()) {
      enemyData.initRound();
    }
    _duelEnemy = null;
  }

  public void onRobotDeath(RobotDeathEvent e) {
    try {
      getEnemyData(e.getName()).alive = false;
    } catch (NullPointerException npe) {
      _out.println(warning() + WARNING_ROBOT_DEATH_UNKNOWN);
    }
  }

  protected void updateBotDistances(Point2D.Double myLocation) {
    if (_enemies.size() > 1) {
      String[] botNames = getBotNames();
      for (int x = 0; x < botNames.length; x++) {
        T enemyData1 = getEnemyData(botNames[x]);
        for (int y = x + 1; y < botNames.length; y++) {
          T enemyData2 = getEnemyData(botNames[y]);
          if (enemyData1.alive && enemyData2.alive) {
            double distanceSq = enemyData1.lastScanState.location.distanceSq(
                enemyData2.lastScanState.location);
            enemyData1.setBotDistanceSq(botNames[y], distanceSq);
            enemyData2.setBotDistanceSq(botNames[x], distanceSq);
          } else {
            if (!enemyData1.alive) {
              enemyData2.removeDistanceSq(botNames[x]);
            }
            if (!enemyData2.alive) {
              enemyData1.removeDistanceSq(botNames[y]);
            }
          }
          enemyData1.distance =
              myLocation.distance(enemyData1.lastScanState.location);
        }
      }
    }
  }

  private String[] getBotNames() {
    String[] botNames = new String[_enemies.size()];
    _enemies.keySet().toArray(botNames);
    return botNames;
  }

  public T getClosestLivingBot(Point2D.Double location) {
    T closestEnemy = null;
    double closestDistance = Double.POSITIVE_INFINITY;
    for (T enemyData : getAllEnemyData()) {
      if (enemyData.alive) {
        double thisDistance =
            location.distanceSq(enemyData.lastScanState.location);
        if (thisDistance < closestDistance) {
          closestEnemy = enemyData;
          closestDistance = thisDistance;
        }
      }
    }
    return closestEnemy;
  }

  public T duelEnemy() {
    return _duelEnemy;
  }

  protected boolean isMeleeBattle() {
    return (_enemiesTotal > 1);
  }

  protected String warning() {
    return "WARNING (" + getLabel() + "): ";
  }

  abstract protected String getLabel();
}
