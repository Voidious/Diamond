package voidious.gun.formulas;

import robocode.Rules;
import voidious.utils.DistanceFormula;
import voidious.utils.Wave;

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

public class TripHammerFormula extends DistanceFormula {
  public TripHammerFormula() {
    weights = new double[]
        {0.34, 10.0, 3.13, 2.58, 8.38, 7.91, 8.67, 5.12, 2.34, 0.38};
  }

  public double[] dataPointFromWave(Wave w, boolean aiming) {
    return new double[] {
      (Math.min(3, w.bulletPower()) / 3),
      (Math.min(91, w.targetDistance / w.bulletSpeed()) / 91),
      (((w.targetVelocitySign * w.targetVelocity) + 0.1) / 8.1),
      (Math.sin(w.targetRelativeHeading)),
      ((Math.cos(w.targetRelativeHeading) + 1) / 2),
      ((w.targetAccel + Rules.DECELERATION)
          / (Rules.DECELERATION + Rules.ACCELERATION)),
      (Math.min(1.25, w.targetWallDistance)),
      (Math.min(1.15, w.targetRevWallDistance)),
      (Math.min(1.0, ((double) w.targetVchangeTime)
          / (w.targetDistance/w.bulletSpeed())) / 1.0),
      (aiming ? 0 : w.virtuality())
    };
  }
}
