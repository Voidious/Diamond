package voidious.utils;

import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.util.List;

import robocode.AdvancedRobot;
import robocode.util.Utils;

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

public class DiaUtils {
  private static final double HALF_PI = Math.PI / 2;
  private static final double TWO_PI = Math.PI * 2;

  public static Point2D.Double project(Point2D.Double sourceLocation,
      double angle, double length) {
    return project(sourceLocation, Math.sin(angle), Math.cos(angle), length);
  }

  public static Point2D.Double project(Point2D.Double sourceLocation,
      double sinAngle, double cosAngle, double length) {
    return new Point2D.Double(sourceLocation.x + sinAngle * length,
        sourceLocation.y + cosAngle * length);
  }

  public static double absoluteBearing(Point2D.Double sourceLocation,
       Point2D.Double target) {
    return Math.atan2(target.x - sourceLocation.x,
        target.y - sourceLocation.y);
  }

  public static int nonZeroSign(double d) {
    if (d < 0) { return -1; }
    return 1;
  }

  public static double square(double d) {
    return d * d;
  }

  public static double cube(double d) {
    return d * d * d;
  }

  public static double power(double d, int exp) {
    double r = 1;
    for (int x = 0; x < exp; x++) {
      r *= d;
    }
    return r;
  }

  public static double limit(double min, double value, double max) {
    return Math.max(min, Math.min(value, max));
  }

  public static int limit(int min, int value, int max) {
    return Math.max(min, Math.min(value, max));
  }

  public static double botWidthAimAngle(double distance) {
    return Math.abs(18.0/distance);
  }

  public static int bulletTicksFromPower(double distance, double power) {
    return (int) Math.ceil(distance / (20 - (3 * power)));
  }

  public static int bulletTicksFromSpeed(double distance, double speed) {
    return (int) Math.ceil(distance / speed);
  }

  public static void setBackAsFront(AdvancedRobot robot, double goAngle) {
    double angle =
        Utils.normalRelativeAngle(goAngle - robot.getHeadingRadians());

    if (Math.abs(angle) > HALF_PI) {
      if (angle < 0) {
        robot.setTurnRightRadians(Math.PI + angle);
      } else {
        robot.setTurnLeftRadians(Math.PI - angle);
      }
      robot.setBack(100);
    } else {
      if (angle < 0) {
        robot.setTurnLeftRadians(-1 * angle);
      } else {
        robot.setTurnRightRadians(angle);
      }
      robot.setAhead(100);
    }
  }

  public static double rollingAverage(double previousValue, double newValue,
      double depth) {
    return ((previousValue * depth) + newValue) / (depth + 1);
  }

  public static double round(double d, int i) {
    long powerTen = 1;
    for (int x = 0; x < i; x++) {
      powerTen *= 10;
    }
    return ((double) Math.round(d * powerTen)) / powerTen;
  }

  public static double standardDeviation(double[] values) {
    double avg = average(values);
    double sumSquares = 0;
    for (int x = 0; x < values.length; x++) {
      sumSquares += square(avg - values[x]);
    }
    return Math.sqrt(sumSquares / values.length);
  }

  public static double average(double[] values) {
    double sum = 0;
    for (int x = 0; x < values.length; x++) {
      sum += values[x];
    }
    return (sum / values.length);
  }

  public static double accel(double velocity, double previousVelocity) {
    double accel = velocity - previousVelocity;
    if (previousVelocity == 0.0) {
      accel = Math.abs(accel);
    } else {
      accel *= Math.signum(previousVelocity);
    }
    return accel;
  }

  public static double distancePointToBot(
      Point2D.Double sourceLocation, RobotState robotState) {
    return distancePointToBot(
        sourceLocation, robotState.location, robotState.botSides());
  }

  public static double distancePointToBot(Point2D.Double sourceLocation,
      Point2D.Double botLocation, List<Line2D.Double> botSides) {
    if (sourceLocation.x > botLocation.x - 18 &&
        sourceLocation.x < botLocation.x + 18 &&
        sourceLocation.y > botLocation.y - 18 &&
        sourceLocation.y < botLocation.y + 18) {
      return 0;
    } else {
      double distance = Double.POSITIVE_INFINITY;
      for (Line2D.Double side : botSides) {
        distance = Math.min(distance, side.ptSegDist(sourceLocation));
      }
      return distance;
    }
  }

  public static double normalizeAngle(double angle, double reference) {
    double normDiff = reference - angle;
    while (Math.abs(normDiff) > Math.PI) {
      angle += Math.signum(normDiff) * TWO_PI;
      normDiff = reference - angle;
    }
    return angle;
  }

  public static double[] generateFiringAngles(int numAngles,
      double maxEscapeAngle) {
    int gfZero = (numAngles-1)/2;
    double[] firingAngles = new double[numAngles];
    for (int x = 0; x < numAngles; x++) {
      firingAngles[x] = (((double)(x - gfZero)) / gfZero)
          * maxEscapeAngle;
    }
    return firingAngles;
  }

  public static double distanceToWall(
      Point2D.Double enemyLocation, BattleField battleField) {
    return Math.abs(Math.min(
        Math.min(enemyLocation.x - 18,
                 battleField.width - 18 - enemyLocation.x),
        Math.min(enemyLocation.y - 18,
                 battleField.height - 18 - enemyLocation.y)));
  }

  public static double marginOfError(double probability, int numDataPoints) {
    return 1.96 * Math.sqrt(probability * (1 - probability) / numDataPoints);
  }
}
