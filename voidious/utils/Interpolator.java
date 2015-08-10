package voidious.utils;

import java.awt.geom.Point2D;

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

public class Interpolator {
  private final long fireTime;
  private final long beforeTime;
  private final long afterTime;
  private final double weight1;
  private final double weight2;

  public Interpolator(long fireTime, long beforeTime, long afterTime) {
    if (fireTime <= beforeTime || fireTime >= afterTime
        || beforeTime >= afterTime) {
      throw new IllegalArgumentException(
          "Time values must be: beforeTime < fireTime < afterTime");
    }
    this.fireTime = fireTime;
    this.beforeTime = beforeTime;
    this.afterTime = afterTime;
    double rawWeight1 = Math.abs(afterTime - fireTime);
    double rawWeight2 = Math.abs(beforeTime - fireTime);
    this.weight1 = rawWeight1 / (rawWeight1 + rawWeight2);
    this.weight2 = rawWeight2 / (rawWeight1 + rawWeight2);
  }

  public double avg(double val1, double val2) {
    return (val1 * weight1) + (val2 * weight2);
  }

  public Point2D.Double getLocation(
      Point2D.Double location1, Point2D.Double location2) {
    return new Point2D.Double(
        avg(location1.x, location2.x), avg(location1.y, location2.y));
  }

  public double getHeading(double heading1, double heading2) {
    return avg(heading1, DiaUtils.normalizeAngle(heading2, heading1));
  }

  public long getTimer(long val1, long val2) {
    long newVal = val2 - (afterTime - fireTime);
    if (newVal < 0) {
      newVal = val1 + (fireTime - beforeTime);
    }
    return newVal;
  }
}
