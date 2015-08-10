package voidious.gun;

import java.awt.geom.Point2D;

/**
 * Copyright (c) 2011-2012 - Voidious
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

public interface FireListener {
  public void bulletFired(FiredBullet bullet);

  public static class FiredBullet {
    public final long fireTime;
    public final Point2D.Double sourceLocation;
    public final double firingAngle;
    public final double bulletSpeed;
    public final double dx;
    public final double dy;
    public long deathTime;

    public FiredBullet(long fireTime, Point2D.Double sourceLocation,
        double firingAngle, double bulletSpeed) {
      this.fireTime = fireTime;
      this.sourceLocation = sourceLocation;
      this.firingAngle = firingAngle;
      this.bulletSpeed = bulletSpeed;
      dx = Math.sin(firingAngle) * bulletSpeed;
      dy = Math.cos(firingAngle) * bulletSpeed;
      deathTime = Long.MAX_VALUE;
    }

    public double distanceTraveled(long currentTime) {
      return (currentTime - fireTime) * bulletSpeed;
    }

    public Point2D.Double position(long currentTime) {
      return new Point2D.Double(
          sourceLocation.x + (dx * (currentTime - fireTime)),
          sourceLocation.y + (dy * (currentTime - fireTime)));
    }
  }
}
