package voidious.utils;

import static voidious.utils.DiaUtils.absoluteBearing;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import robocode.Rules;
import robocode.util.Utils;
import voidious.Diamond;

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

public class MovementPredictor {
  private static final int PRECISE_MEA_ITERATIONS = 3;
  private static final double FULL_SPEED = 8.0;
  private static final double TWO_PI = Math.PI * 2;
  private static final double HALF_PI = Math.PI / 2;
  private static final double QUARTER_PI = Math.PI / 4;

  private final BattleField _battleField;
  private final Rectangle2D.Double _rectangle;

  public MovementPredictor(BattleField battleField) {
    _battleField = battleField;
    _rectangle = _battleField.rectangle;
  }

  public RobotState predict(RobotState startState, double distance, double turn,
      double maxVelocity, long ticks, boolean ignoreWalls) {
    RobotState state = startState;
    for (long x = 0; x < ticks; x++) {
      double nextHeading = state.heading;
      double maxTurnRate = Math.abs(Rules.getTurnRateRadians(state.velocity));
      if (Math.abs(turn) < maxTurnRate) {
        nextHeading += turn;
        turn = 0;
      } else {
        double turnAmount = maxTurnRate * Math.signum(turn);
        nextHeading += turnAmount;
        turn -= turnAmount;
      }

      double nextVelocity =
          getNewVelocity(state.velocity, distance, maxVelocity);
      distance -= nextVelocity;

      Point2D.Double nextLocation =
          DiaUtils.project(state.location, nextHeading, nextVelocity);
      if (!ignoreWalls && !_rectangle.contains(nextLocation)) {
        adjustForWalls(nextLocation, nextHeading);
      }
      state = RobotState.newBuilder()
          .setLocation(nextLocation)
          .setHeading(nextHeading)
          .setVelocity(nextVelocity)
          .setTime(state.time + 1)
          .build();
    }

    return state;
  }

  // Examined net.sf.robocode.battle.peer.RobotPeer to see how Robocode
  // handles the physics of wall collisions, but rewrote the code myself.
  private void adjustForWalls(Point2D.Double location, double heading) {
    double xOut = Math.min(0, _rectangle.getMaxX() - location.x);
    double yOut = Math.min(0, _rectangle.getMaxY() - location.y);
    if (xOut == 0) {
      xOut = Math.max(0, _rectangle.getMinX() - location.x);
    }
    if (yOut == 0) {
      yOut = Math.max(0, _rectangle.getMinY() - location.y);
    }

    double xOffset = xOut;
    double yOffset = yOut;

    while (heading < 0) {
      heading += TWO_PI;
    }
    if (heading % (QUARTER_PI) != 0) {
      double tanHeading = Math.tan(heading);
      if (Math.abs(xOut) > 0) {
        yOffset = xOut / tanHeading;
      }
      if (Math.abs(yOut) > 0) {
        xOffset = yOut * tanHeading;
      }
      if (Math.abs(yOut) > Math.abs(yOffset)) {
        yOffset = yOut;
      }
      if (Math.abs(xOut) > Math.abs(xOffset)) {
        xOffset = xOut;
      }
    }

    location.x += xOffset;
    location.y += yOffset;
  }

  // The following 3 methods adapted from:
  //     http://robowiki.net/wiki/User:Voidious/Optimal_Velocity#Hijack_2
  // ...which was a collaboration of me (Voidious), Skilgannon, and Positive,
  // and was also used in Robocode engine.
  private double getNewVelocity(
      double velocity, double distance, double maxVelocity) {
    if (distance < 0) {
      // If the distance is negative, then change it to be positive
      // and change the sign of the input velocity and the result
      return -getNewVelocity(-velocity, -distance, maxVelocity);
    }

    final double goalVel;
    if (distance == Double.POSITIVE_INFINITY) {
      goalVel = maxVelocity;
    } else {
      goalVel = Math.min(getMaxVelocity(distance), maxVelocity);
    }

    if (velocity >= 0) {
      return DiaUtils.limit(velocity - Rules.DECELERATION,
                            goalVel,
                            velocity + Rules.ACCELERATION);
    } else {
      return DiaUtils.limit(velocity - Rules.ACCELERATION,
                            goalVel,
                            velocity + maxDecel(-velocity));
    }
  }

  private double getMaxVelocity(double distance) {
    final double decelTime =  Math.max(1,Math.ceil(
        (Math.sqrt((4*2/Rules.DECELERATION)*distance + 1) - 1)/2));
        // sum of 0..decelTime, solving for decelTime using quadratic formula

    final double decelDist =
        (decelTime / 2.0) * (decelTime-1) // sum of 0..(decelTime-1)
        * Rules.DECELERATION;

    return ((decelTime - 1) * Rules.DECELERATION)
        + ((distance - decelDist) / decelTime);
  }

  private double maxDecel(double velocity) {
    velocity = Math.abs(velocity);
    if (velocity > Rules.DECELERATION) {
      return Rules.DECELERATION;
    } else {
      double tickFractionDecel = velocity / Rules.DECELERATION;
      double tickFractionAccel = 1 - tickFractionDecel;
      return (tickFractionDecel * Rules.DECELERATION)
           + (tickFractionAccel * Rules.ACCELERATION);
    }
  }

  public RobotState nextRobotState(Diamond robot) {
    RobotState newRobotState = predict(
        RobotState.newBuilder()
            .setLocation(new Point2D.Double(robot.getX(), robot.getY()))
            .setHeading(robot.getHeadingRadians())
            .setVelocity(robot.getVelocity())
            .setTime(robot.getTime())
            .build(),
        robot.getDistanceRemaining(), robot.getTurnRemainingRadians(),
        robot.getMaxVelocity(), 1, false);
    return newRobotState;
  }

  public Point2D.Double nextLocation(Diamond robot) {
    return nextRobotState(robot).location;
  }

  public Point2D.Double nextLocation(RobotState robotState) {
    return nextLocation(
        robotState.location, robotState.heading, robotState.velocity);
  }

  public Point2D.Double nextLocation(Point2D.Double botLocation,
      double heading, double velocity) {
    return new Point2D.Double(
        botLocation.x + (Math.sin(heading) * velocity),
        botLocation.y + (Math.cos(heading) * velocity));
  }

  // TODO: rename this
  public RobotState nextPerpendicularLocation(RobotState robotState,
      double absBearing, int orientation, double attackAngle,
      boolean ignoreWallHits) {
    return nextPerpendicularWallSmoothedLocation(
        robotState, absBearing, 8.0, attackAngle, orientation, 0, ignoreWallHits);
  }

  public RobotState nextPerpendicularWallSmoothedLocation(RobotState robotState,
      double absBearing, double maxVelocity, double attackAngle,
      int orientation, double wallStick, boolean ignoreWallHits) {
    double goAngle = Utils.normalRelativeAngle(
        absBearing + (orientation * (HALF_PI + attackAngle)));
    if (wallStick != 0) {
      goAngle = _battleField.wallSmoothing(
          robotState.location, goAngle, orientation, wallStick);
    }

    return nextLocation(robotState, maxVelocity, goAngle, ignoreWallHits);
  }

  public RobotState nextLocation(RobotState robotState, double maxVelocity,
      double goAngle, boolean ignoreWallHits) {
    double futureTurn = Utils.normalRelativeAngle(goAngle - robotState.heading);
    double futureDistance;
    if (Math.abs(futureTurn) > HALF_PI) {
      futureTurn = futureTurn - (Math.signum(futureTurn) * Math.PI);
      futureDistance = -1000;
    } else {
      futureDistance = 1000;
    }

    return predict(
        robotState, futureDistance, futureTurn, maxVelocity, 1, ignoreWallHits);
  }

  public double escapeAngleRange(Point2D.Double sourceLocation,
      long fireTime, double bulletSpeed, RobotState startState,
      double wallStick) {
    return preciseEscapeAngle(1, sourceLocation, fireTime, bulletSpeed,
        startState, wallStick).angle
        + preciseEscapeAngle(-1, sourceLocation, fireTime, bulletSpeed,
            startState, wallStick).angle;
  }

  public MaxEscapeTarget preciseEscapeAngle(int predictDirection,
      Point2D.Double sourceLocation, long fireTime, double bulletSpeed,
      RobotState startState, double wallStick) {
    return preciseEscapeAngle(predictDirection, sourceLocation, fireTime,
        bulletSpeed, startState, 0, wallStick);
  }

  public MaxEscapeTarget preciseEscapeAngle(int predictDirection,
      Point2D.Double sourceLocation, long fireTime, double bulletSpeed,
      RobotState startState, double attackAngle, double wallStick) {
    double absBearing =
        DiaUtils.absoluteBearing(sourceLocation, startState.location);

    MaxEscapeTarget straightEscapeTarget = straightPreciseEscapeAngle(
        predictDirection, absBearing, sourceLocation, fireTime, bulletSpeed,
        startState, attackAngle);

    MaxEscapeTarget bestSmoothingTarget = null;
    if (straightEscapeTarget.hitWall) {
      bestSmoothingTarget = smoothingPreciseEscapeAngle(predictDirection,
          absBearing, sourceLocation, fireTime, bulletSpeed, startState,
          attackAngle, wallStick, PRECISE_MEA_ITERATIONS);
    }

    if (bestSmoothingTarget != null
        && bestSmoothingTarget.angle > straightEscapeTarget.angle) {
      return bestSmoothingTarget;
    } else {
      return straightEscapeTarget;
    }
  }

  MaxEscapeTarget straightPreciseEscapeAngle(int predictDirection,
      double absBearing, Point2D.Double sourceLocation, long fireTime,
      double bulletSpeed, RobotState startState, double attackAngle) {
    double straightEscapeAngle = 0;
    RobotState predictedState = startState;
    boolean hitWall = false;
    boolean wavePassed = false;
    do {
      predictedState = nextPerpendicularLocation(
          predictedState, absBearing, predictDirection, attackAngle, true);
      if (!_rectangle.contains(predictedState.location)) {
        hitWall = true;
      } else if (wavePassed(
          sourceLocation, fireTime, bulletSpeed, predictedState)) {
        wavePassed = true;
      }
    } while (!hitWall && !wavePassed);

    Point2D.Double meaLocation =
        _battleField.translateToField(predictedState.location);
    straightEscapeAngle = predictDirection * Utils.normalRelativeAngle(
        absoluteBearing(sourceLocation, meaLocation) - absBearing);
    return new MaxEscapeTarget(
        straightEscapeAngle, meaLocation, predictedState.time, hitWall);
  }

  MaxEscapeTarget smoothingPreciseEscapeAngle(int predictDirection,
      double absBearing, Point2D.Double sourceLocation, long fireTime,
      double bulletSpeed, RobotState startState, double attackAngle,
      double wallStick, int iterations) {
    MaxEscapeTarget bestSmoothingTarget = null;
    boolean wavePassed = false;
    bestSmoothingTarget =
        new MaxEscapeTarget(0, startState.location, startState.time, false);

    double goAngle = absBearing + (predictDirection * (HALF_PI + attackAngle));
    goAngle = _battleField.wallSmoothing(
        startState.location, goAngle, predictDirection, wallStick);
    for (int x = 0; x < iterations; x++) {
      wavePassed = false;
      RobotState predictedState = startState;
      do {
        // TODO: try fancy stick
        predictedState =
            nextLocation(predictedState, FULL_SPEED, goAngle, true);
        if (wavePassed(
                sourceLocation, fireTime, bulletSpeed, predictedState)) {
          wavePassed = true;
        } else {
          goAngle = _battleField.wallSmoothing(
              predictedState.location, goAngle, predictDirection, wallStick);
        }
      } while (!wavePassed);

      Point2D.Double predictedLocation =
          _battleField.translateToField(predictedState.location);
      double thisSmoothingEscapeAngle =
          predictDirection * Utils.normalRelativeAngle(
              absoluteBearing(sourceLocation, predictedLocation) - absBearing);
      if (thisSmoothingEscapeAngle > bestSmoothingTarget.angle) {
        bestSmoothingTarget = new MaxEscapeTarget(thisSmoothingEscapeAngle,
            predictedLocation, predictedState.time, false);
      }
      if (x + 1 < iterations) {
        goAngle = absoluteBearing(startState.location, predictedLocation);
      }
    }

    return bestSmoothingTarget;
  }

  private boolean wavePassed(Point2D.Double sourceLocation,
      long fireTime, double bulletSpeed, RobotState enemyState) {
    double threshold = bulletSpeed * (enemyState.time - fireTime) + bulletSpeed;
    if (enemyState.location.distanceSq(sourceLocation)
            < DiaUtils.square(threshold) * Math.signum(threshold)) {
      return true;
    } else {
      return false;
    }
  }
}
