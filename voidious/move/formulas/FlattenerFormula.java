package voidious.move.formulas;

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

public class FlattenerFormula extends DistanceFormula {
  public FlattenerFormula() {
    weights = new double[]{3, 4, 3, 5, 1, 4, 3, 3, 2, 2, 2};
  }

  @Override
  public double[] dataPointFromWave(Wave w, boolean aiming) {
    return new double[]{
      (Math.min(91, w.targetDistance / w.bulletSpeed()) / 91),
      (((w.targetVelocitySign * w.targetVelocity) + 0.1) / 8.1),
      (Math.sin(w.targetRelativeHeading)),
      ((Math.cos(w.targetRelativeHeading) + 1) / 2),
      (((w.targetAccel / (w.targetAccel < 0
          ? Rules.DECELERATION : Rules.ACCELERATION)) + 1) / 2),
      (Math.min(1.0, w.targetWallDistance) / 1.0),
      (Math.min(1.0, w.targetRevWallDistance) / 1.0),
      (Math.min(1, ((double) w.targetVchangeTime)
          / (w.targetDistance/w.bulletSpeed()))),
      (w.targetDl8t / 64),
      (w.targetDl20t / 160),
      (w.targetDl40t / 320),
    };
  }
}
