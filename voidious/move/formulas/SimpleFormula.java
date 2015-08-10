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

public class SimpleFormula extends DistanceFormula {
  public SimpleFormula() {
    weights = new double[]{1, 1, 1};
  }

  @Override
  public double[] dataPointFromWave(Wave w, boolean aiming) {
    return new double[]{
      (Math.min(91, w.targetDistance / w.bulletSpeed()) / 91),
      ((w.lateralVelocity() + 0.1) / 8.1),
      (((w.targetAccel / (w.targetAccel < 0
          ? Rules.DECELERATION : Rules.ACCELERATION)) + 1) / 2)
    };
  }
}
