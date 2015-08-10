package voidious.move;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import robocode.AdvancedRobot;
import voidious.gfx.RoboGraphic;
import voidious.utils.BattleField;
import voidious.utils.DiaUtils;
import voidious.utils.ErrorLogger;
import voidious.utils.MovementPredictor;
import voidious.utils.RobotState;

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

public class MeleeMover {
  private static final double CURRENT_DESTINATION_BIAS = 0.8;
  private static final int RECENT_LOCATIONS_TO_STORE = 50;
  private static final long NUM_SLICES_BOT = 64;

  private AdvancedRobot _robot;
  private BattleField _battleField;
  private Collection<RoboGraphic> _renderables;
  private MovementPredictor _predictor;
  private LinkedList<OldLocation> _recentLocations;
  private Destination _currentDestination;

  public MeleeMover(AdvancedRobot robot, BattleField battleField,
      Collection<RoboGraphic> renderables) {
    _robot = robot;
    _battleField = battleField;
    _renderables = renderables;
    _predictor = new MovementPredictor(battleField);
    _recentLocations = new LinkedList<OldLocation>();
  }

  public void initRound(AdvancedRobot robot, Point2D.Double myLocation) {
    _robot = robot;
    _currentDestination =
        new Destination(myLocation, Double.POSITIVE_INFINITY, 0);
    _recentLocations.clear();
  }

  public void move(Point2D.Double myLocation, Collection<MoveEnemy> enemies,
      MoveEnemy closestEnemy, boolean painting) {
    if (enemies.isEmpty()) {
      return;
    }

    updateRecentLocations(_robot.getTime(), myLocation);
    List<Destination> destinations =
        generateDestinations(myLocation, enemies, closestEnemy);
    Destination nextDestination =
        getNextDestination(myLocation, destinations);

    double goAngle =
        DiaUtils.absoluteBearing(myLocation, nextDestination.location);
    DiaUtils.setBackAsFront(_robot, goAngle);
    _currentDestination = nextDestination;

    if (painting) {
      destinations.add(_currentDestination);
      drawRisks(destinations);
    }
  }

  private void updateRecentLocations(
      long currentTime, Point2D.Double myLocation) {
    if (currentTime % 7 == 0) {
      for (int x = 0; x < 5; x++) {
        _recentLocations.addFirst(new OldLocation(
            DiaUtils.project(myLocation, Math.random() * Math.PI * 2,
                             5 + Math.random() * Math.random() * 200),
            currentTime));
      }
      while (_recentLocations.size() > RECENT_LOCATIONS_TO_STORE) {
        _recentLocations.removeLast();
      }
    }
  }

  private List<Destination> generateDestinations(Point2D.Double myLocation,
      Collection<MoveEnemy> enemies, MoveEnemy closestEnemy) {
    List<Destination> possibleDestinations = new ArrayList<Destination>();
    possibleDestinations.addAll(
        generatePointsAroundBot(myLocation, enemies, closestEnemy));

    if (myLocation.distance(_currentDestination.location)
            <= myLocation.distance(closestEnemy.lastScanState.location)) {
      double currentGoAngle =
          DiaUtils.absoluteBearing(myLocation, _currentDestination.location);
      double currentRisk = CURRENT_DESTINATION_BIAS
          * evaluateRisk(enemies, _currentDestination.location,
                         _currentDestination.goAngle);
      _currentDestination = new Destination(
          _currentDestination.location, currentRisk, currentGoAngle);
      possibleDestinations.add(_currentDestination);
    }

    return possibleDestinations;
  }

  private List<Destination> generatePointsAroundBot(
      Point2D.Double myLocation, Collection<MoveEnemy> enemies,
      MoveEnemy closestEnemy) {
    List<Destination> destinations = new ArrayList<Destination>();
    double distanceToClosestBot =
        myLocation.distance(closestEnemy.lastScanState.location);
    double movementStick =
        Math.min(100 + Math.random() * 100, distanceToClosestBot);

    double sliceSize = (2 * Math.PI) / NUM_SLICES_BOT;
    for (int x = 0; x < NUM_SLICES_BOT; x++) {
      double angle = x * sliceSize;
      Point2D.Double dest = _battleField.translateToField(
          DiaUtils.project(myLocation, angle, movementStick));
      destinations.add(
          new Destination(dest, evaluateRisk(enemies, dest, angle), angle));
    }

    return destinations;
  }

  private double evaluateRisk(Collection<MoveEnemy> enemies,
      Point2D.Double destination, double goAngle) {
    double risk = 0;
    for (MoveEnemy moveData : enemies) {
      if (moveData.alive) {
        double distanceSq =
            destination.distanceSq(moveData.lastScanState.location);
        risk += DiaUtils.limit(0.25, moveData.energy / _robot.getEnergy(), 4)
            * (1 + Math.abs(Math.cos(moveData.absBearing - goAngle)))
            * moveData.damageFactor
            / (distanceSq * (moveData.botsCloser(distanceSq * .8) + 1)
            );
      }
    }

    double randomRisk = 0;
    for (OldLocation oldLocation : _recentLocations) {
      randomRisk += 30.0 / oldLocation.location.distanceSq(destination);
    }
    risk *= 1 + randomRisk;

    return risk;
  }

  private Destination getNextDestination(
      Point2D.Double myLocation, List<Destination> destinations) {
    Destination nextDestination;
    RobotState currentState = RobotState.newBuilder()
        .setLocation(myLocation)
        .setHeading(_robot.getHeadingRadians())
        .setVelocity(_robot.getVelocity())
        .setTime(_robot.getTime())
        .build();
    do {
      nextDestination = safestDestination(myLocation, destinations);
      destinations.remove(nextDestination);
    } while (wouldHitWall(currentState, nextDestination));
    return nextDestination;
  }

  private Destination safestDestination(
      Point2D.Double myLocation, List<Destination> possibleDestinations) {
    double lowestRisk = Double.POSITIVE_INFINITY;
    Destination safest = null;

    for (Destination destination : possibleDestinations) {
      if (destination.risk < lowestRisk) {
        lowestRisk = destination.risk;
        safest = destination;
      }
    }

    if (safest == null) {
      String error = "No safe destinations found, there must be a bug "
          + "in the risk evaluation.\n"
          + "_myLocation: (" + DiaUtils.round(myLocation.x, 1) + ", "
          + DiaUtils.round(myLocation.y, 1) + ")\n"
          + "myEnergy: " + _robot.getEnergy() + "\n"
          + "getOthers(): " + _robot.getOthers();
      ErrorLogger.getInstance().logError(error);

      safest = _currentDestination;
    }

    return safest;
  }

  private boolean wouldHitWall(
      RobotState currentState, Destination destination) {
    long ticksAhead = 5;
    for (int x = 0; x < ticksAhead; x++) {
      currentState = _predictor.nextLocation(currentState, 8.0,
          DiaUtils.absoluteBearing(currentState.location, destination.location),
          true);
      if (!_battleField.rectangle.contains(currentState.location)) {
        return true;
      }
    }
    return false;
  }

  private void drawRisks(List<Destination> destinations) {
    double lowestRisk = Double.POSITIVE_INFINITY;
    double highestRisk = Double.NEGATIVE_INFINITY;

    double[] risks = new double[destinations.size()];
    int x = 0;
    for (Destination destination : destinations) {
      risks[x++] = destination.risk;
      if (destination.risk < lowestRisk) {
        lowestRisk = destination.risk;
      }
      if (destination.risk > highestRisk) {
        highestRisk = destination.risk;
      }
    }

    double avg = DiaUtils.average(risks);
    double stDev = DiaUtils.standardDeviation(risks);
    for (Destination destination : destinations) {
      _renderables.add(RoboGraphic.drawCircleFilled(destination.location,
          RoboGraphic.riskColor(destination.risk - lowestRisk, avg - lowestRisk,
              stDev, true, 1), 2));
    }
  }

  public static class Destination {
    public final Point2D.Double location;
    public final double risk;
    public final double goAngle;

    public Destination(Point2D.Double location, double risk, double goAngle) {
      this.location = location;
      this.risk = risk;
      this.goAngle = goAngle;
    }
  }

  public class OldLocation {
    public Point2D.Double location;
    public long time;

    public OldLocation(Point2D.Double l, long t) {
      location = l;
      time = t;
    }
  }
}
