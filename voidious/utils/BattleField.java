package voidious.utils;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Copyright (c) 2011 - Voidious
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

public class BattleField {
  private static final double HALF_PI = Math.PI / 2;

  public final Rectangle2D.Double rectangle;
  public final double width;
  public final double height;

  public BattleField(double width, double height) {
    rectangle = new Rectangle2D.Double(18, 18, width - 36, height - 36);
    this.width = width;
    this.height = height;
  }

  public Point2D.Double translateToField(Point2D.Double p) {
    return new Point2D.Double(DiaUtils.limit(18, p.x, width - 18),
                              DiaUtils.limit(18, p.y, height - 18));
  }

  public double orbitalWallDistance(Point2D.Double sourceLocation,
      Point2D.Double targetLocation, double bulletPower, int direction) {
    double absBearing =
        DiaUtils.absoluteBearing(sourceLocation, targetLocation);
    double distance = sourceLocation.distance(targetLocation);
    double maxEscapeAngle = Math.asin(8.0/(20-3.0*bulletPower));

    // 1.0 means the max range of orbital movement exactly reaches bounds
    // of battle field
    double wallDistance = 2.0;
    for (int x = 0; x < 200; x++) {
      if (!rectangle.contains(
          sourceLocation.x + (Math.sin(absBearing +
              (direction*(x/100.0)*maxEscapeAngle))*distance),
          sourceLocation.y + (Math.cos(absBearing +
              (direction*(x/100.0)*maxEscapeAngle))*distance))) {
        wallDistance = x/100.0;
        break;
      }
    }

    return wallDistance;
  }

  public double directToWallDistance(Point2D.Double targetLocation,
      double distance, double heading, double bulletPower) {
    int bulletTicks = DiaUtils.bulletTicksFromPower(distance, bulletPower);
    double wallDistance = 2.0;
    double sinHeading = Math.sin(heading);
    double cosHeading = Math.cos(heading);
    for (int x = 0; x < 2 * bulletTicks; x++) {
      if (!rectangle.contains(
              targetLocation.x + (sinHeading * 8.0 * x),
              targetLocation.y + (cosHeading * 8.0 * x))) {
        wallDistance = ((double) x)/bulletTicks;
        break;
      }
    }

    return wallDistance;
  }

  /**
   * Do some Voodoo and wall smooth in a very efficient way. (In terms of CPU
   * cycles, not amount of code.)
   */
  public double wallSmoothing(Point2D.Double startLocation, double startAngle,
      int orientation, double wallStick) {
    double wallDistanceX = Math.min(startLocation.x - 18,
                                    width - startLocation.x - 18);
    double wallDistanceY = Math.min(startLocation.y - 18,
                                    height - startLocation.y - 18);

    if (wallDistanceX > wallStick && wallDistanceY > wallStick) {
      return startAngle;
    }

    double angle = startAngle;
    double testX = startLocation.x + (Math.sin(angle) * wallStick);
    double testY = startLocation.y + (Math.cos(angle) * wallStick);
    double testDistanceX =
        Math.min(testX - 18, width - testX - 18);
    double testDistanceY =
        Math.min(testY - 18, height - testY - 18);

    double adjacent = 0;
    int g = 0; // shouldn't be needed, but infinite loop sanity check

    // TODO: rewrite w/o loop for clarity / sanity
    while ((testDistanceX < 0 || testDistanceY < 0) && g++ < 25) {
      if (testDistanceY < 0 && testDistanceY < testDistanceX) {
        // North or South wall
        angle = (testY < 18) ? Math.PI : 0;
        adjacent = wallDistanceY;
      } else if (testDistanceX < 0 && testDistanceX <= testDistanceY) {
        // East or West wall
        angle = (testX < 18) ? (3 * HALF_PI) : HALF_PI;
        adjacent = wallDistanceX;
      }

      if (adjacent < 0) {
        if (-adjacent > wallStick) {
          wallStick += -adjacent;
        }
        angle += Math.PI -
            orientation * (Math.abs(Math.acos(-adjacent/wallStick)) - 0.0005);
      } else {
        angle +=
            orientation * (Math.abs(Math.acos(adjacent/wallStick)) + 0.0005);
      }
      testX = startLocation.x + (Math.sin(angle) * wallStick);
      testY = startLocation.y + (Math.cos(angle) * wallStick);
      testDistanceX = Math.min(testX - 18, width - testX - 18);
      testDistanceY = Math.min(testY - 18, height - testY - 18);
    }

    return angle;
  }
}
