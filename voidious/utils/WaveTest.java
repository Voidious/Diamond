package voidious.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static voidious.utils.DiaUtils.normalizeAngle;

import java.awt.geom.Point2D;

import org.junit.Test;

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

public class WaveTest {

  @Test
  public void testNewWave() {
    Point2D.Double sourceLocation = newLocation();
    Point2D.Double targetLocation = newLocation();
    Wave w = new Wave("Shadow", sourceLocation, targetLocation, 2, 36L, 1.95,
        0.2, 8.0, 1, newBattleField(), newPredictor());
    assertEquals("Shadow", w.botName);
    assertEquals(sourceLocation, w.sourceLocation);
    assertEquals(targetLocation, w.targetLocation);
    assertEquals(2, w.fireRound);
    assertEquals(36L, w.fireTime);
    assertEquals(1.95, w.bulletPower(), 0.001);
    assertEquals(14.15, w.bulletSpeed(), 0.001);
    assertEquals(0.2, w.targetHeading, 0.001);
    assertEquals(8.0, w.targetVelocity, 0.001);
    assertEquals(1, w.targetVelocitySign);
    assertEquals(DiaUtils.absoluteBearing(sourceLocation, targetLocation),
        w.absBearing, 0.001);
    assertFalse(w.processedBulletHit());
    assertFalse(w.firingWave);
    assertFalse(w.altWave);
    assertTrue(w.shadows.isEmpty());
    assertEquals(w.targetHeading, w.effectiveHeading(), 0.001);
  }

  @Test
  public void testSetBulletPower() {
    Wave w = newWave();
    w.setBulletPower(1.33);
    assertEquals(1.33, w.bulletPower(), 0.001);
    assertEquals(16.01, w.bulletSpeed(), 0.001);
    assertEquals(Math.asin(8.0 / 16.01), w.maxEscapeAngle(), 0.001);
  }

  @Test
  public void testEffectiveHeading() {
    Wave w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, 0.2, 8.0, 1, newBattleField(), newPredictor());
    assertEquals(0.2, w.effectiveHeading(), 0.001);
    w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, 0.2, 8.0, -1, newBattleField(), newPredictor());
    assertEquals(Math.PI + 0.2, w.effectiveHeading(), 0.001);
  }

  @Test
  public void testDistanceTraveled() {
    Wave w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, 0.2, 8.0, 1, newBattleField(), newPredictor());
    assertEquals(14.15, w.distanceTraveled(37L), 0.001);
    assertEquals(28.30, w.distanceTraveled(38L), 0.001);
    assertEquals(141.50, w.distanceTraveled(46L), 0.001);
    assertEquals(523.55, w.distanceTraveled(73L), 0.001);
    assertEquals(1400.85, w.distanceTraveled(135L), 0.001);
  }

  @Test
  public void testLateralVelocityFullSpeedForward() {
    Wave w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, Math.PI / 2, 8.0, 1, newBattleField(), newPredictor());
    assertEquals(8.0, w.lateralVelocity(), 0.001);
  }

  @Test
  public void testLateralVelocityFullSpeedReverse() {
    Wave w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, Math.PI / 2, -8.0, -1, newBattleField(), newPredictor());
    assertEquals(8.0, w.lateralVelocity(), 0.001);
  }

  @Test
  public void testLateralVelocityRamming() {
    Wave w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, Math.PI, 8.0, 1, newBattleField(), newPredictor());
    assertEquals(0, w.lateralVelocity(), 0.001);
  }

  @Test
  public void testLateralVelocityAngled() {
    Wave w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, 2.4980915, 8.0, 1, newBattleField(), newPredictor());
    assertEquals(4.8, w.lateralVelocity(), 0.001);
  }

  @Test
  public void testLateralVelocityOppositeVelocitySign() {
    Wave w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, Math.PI / 2, 0.1, -1, newBattleField(), newPredictor());
    assertEquals(-0.1, w.lateralVelocity(), 0.001);
  }

  @Test
  public void testOrbitDirection() {
    Wave w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, Math.PI / 2, 8.0, 1, newBattleField(), newPredictor());
    assertEquals(1, w.orbitDirection);
  }

  @Test
  public void testOrbitDirectionReverse() {
    Wave w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, Math.PI / 2, -8.0, -1, newBattleField(), newPredictor());
    assertEquals(-1, w.orbitDirection);
  }

  @Test
  public void testOrbitDirectionOppositeVelocitySign() {
    Wave w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, Math.PI / 2, 0.1, -1, newBattleField(), newPredictor());
    assertEquals(-1, w.orbitDirection);
  }

  @Test
  public void testVirtualitySinceLastFiringTime() {
    Wave w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, Math.PI / 2, 8.0, 1, newBattleField(), newPredictor());
    w.setLastBulletFiredTime(33L);
    w.setGunHeat(1.19);
    assertEquals(0.375, w.virtuality(), 0.001);
  }

  @Test
  public void testVirtualityFiringWave() {
    Wave w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, Math.PI / 2, 8.0, 1, newBattleField(), newPredictor());
    w.firingWave = true;
    assertEquals(0, w.virtuality(), 0.001);
  }

  @Test
  public void testVirtualityGunHeat() {
    Wave w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, Math.PI / 2, 8.0, 1, newBattleField(), newPredictor());
    w.setLastBulletFiredTime(22L);
    w.setGunHeat(0.15);
    assertEquals(0.25, w.virtuality(), 0.001);
  }

  @Test
  public void testVirtualityGunHeatCoolingRateMultiple() {
    Wave w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, Math.PI / 2, 8.0, 1, newBattleField(), newPredictor());
    w.setLastBulletFiredTime(22L);
    w.setGunHeat(0.2);
    assertEquals(0.25, w.virtuality(), 0.001);
  }

  @Test
  public void testFiringAngle() {
    Wave w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, Math.PI / 2, -8.0, -1, newBattleField(), newPredictor());
    assertEquals(
        w.absBearing, normalizeAngle(w.firingAngle(0), w.absBearing), 0.001);
  }

  @Test
  public void testFiringAnglePositiveGuessFactor() {
    Wave w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, Math.PI / 2, 8.0, 1, newBattleField(), newPredictor());
    assertEquals(w.absBearing + 0.210309,
        normalizeAngle(w.firingAngle(0.35), w.absBearing), 0.001);
  }

  @Test
  public void testFiringAngleNegativeGuessFactor() {
    Wave w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, Math.PI / 2, 8.0, 1, newBattleField(), newPredictor());
    assertEquals(w.absBearing - 0.16223841,
        normalizeAngle(w.firingAngle(-0.27), w.absBearing), 0.001);
  }

  @Test
  public void testFiringAnglePositiveGuessFactorReverseDirection() {
    Wave w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, Math.PI / 2, -8.0, -1, newBattleField(), newPredictor());
    assertEquals(w.absBearing - 0.210309,
        normalizeAngle(w.firingAngle(0.35), w.absBearing), 0.001);
  }

  @Test
  public void testFiringAngleNegativeGuessFactorReverseDirection() {
    Wave w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, Math.PI / 2, -8.0, -1, newBattleField(), newPredictor());
    assertEquals(w.absBearing + 0.16223841,
        normalizeAngle(w.firingAngle(-0.27), w.absBearing), 0.001);
  }

  @Test
  public void testDisplacementVector() {
    Wave w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, Math.PI / 2, 8.0, 1, newBattleField(), newPredictor());
    RobotState waveBreakState = RobotState.newBuilder()
        .setLocation(newLocation(513.1371, 513.1371))
        .setHeading(Math.PI / 4)
        .setVelocity(8.0)
        .setTime(56L)
        .build();
    Point2D.Double displacementVector = w.displacementVector(waveBreakState);
    assertEquals(-5.65685, displacementVector.x, 0.001);
    assertEquals(5.65685, displacementVector.y, 0.001);
  }

  @Test
  public void testDisplacementVectorReverseHeading() {
    Wave w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, Math.PI / 2, -8.0, -1, newBattleField(), newPredictor());
    RobotState waveBreakState = RobotState.newBuilder()
        .setLocation(newLocation(286.862915, 513.1371))
        .setHeading(Math.PI / 4)
        .setVelocity(8.0)
        .setTime(56L)
        .build();
    Point2D.Double displacementVector = w.displacementVector(waveBreakState);
    assertEquals(-5.65685, displacementVector.x, 0.001);
    assertEquals(5.65685, displacementVector.y, 0.001);
  }

  @Test
  public void testDisplacementVectorReverseHeadingBackwards() {
    Wave w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, Math.PI / 2, -8.0, -1, newBattleField(), newPredictor());
    RobotState waveBreakState = RobotState.newBuilder()
        .setLocation(newLocation(513.1371, 513.1371))
        .setHeading(Math.PI / 4)
        .setVelocity(8.0)
        .setTime(56L)
        .build();
    Point2D.Double displacementVector = w.displacementVector(waveBreakState);
    assertEquals(-5.65685, displacementVector.x, 0.001);
    assertEquals(-5.65685, displacementVector.y, 0.001);
  }

  @Test
  public void testProjectLocation() {
    Wave w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, Math.PI / 2, 8.0, 1, newBattleField(), newPredictor());
    Point2D.Double projectedLocation =
        w.projectLocation(newLocation(400, 100), newLocation(0, 8), 0);
    assertEquals(600, projectedLocation.x, 0.001);
    assertEquals(400, projectedLocation.y, 0.001);
  }

  @Test
  public void testProjectLocationRamming() {
    Wave w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, Math.PI, 8.0, 1, newBattleField(), newPredictor());
    Point2D.Double projectedLocation =
        w.projectLocation(newLocation(400, 100), newLocation(0, 8), 0);
    assertEquals(400, projectedLocation.x, 0.001);
    assertEquals(296, projectedLocation.y, 0.001);

    projectedLocation =
        w.projectLocation(newLocation(400, 100), newLocation(0, 7), 0);
    assertEquals(302, projectedLocation.y, 0.001);

    projectedLocation =
        w.projectLocation(newLocation(400, 100), newLocation(0, 6), 0);
    assertEquals(316, projectedLocation.y, 0.001);

    projectedLocation =
        w.projectLocation(newLocation(400, 100), newLocation(0, 5), 0);
    assertEquals(325, projectedLocation.y, 0.001);

    projectedLocation =
        w.projectLocation(newLocation(400, 100), newLocation(0, 4), 0);
    assertEquals(336, projectedLocation.y, 0.001);

    projectedLocation =
        w.projectLocation(newLocation(400, 100), newLocation(0, 3), 0);
    assertEquals(349, projectedLocation.y, 0.001);

    projectedLocation =
        w.projectLocation(newLocation(400, 100), newLocation(0, 2), 0);
    assertEquals(364, projectedLocation.y, 0.001);
  }

  @Test
  public void testProjectLocationFromDisplacementVector() {
    Wave w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, Math.PI / 2, 8.0, 1, newBattleField(), newPredictor());
    Point2D.Double projectedLocation =
        w.projectLocationFromDisplacementVector(newLocation(0, 8));
    assertEquals(600, projectedLocation.x, 0.001);
    assertEquals(400, projectedLocation.y, 0.001);
  }

  @Test
  public void testProjectLocationBlind() {
    Wave w = new Wave("Shadow", newLocation(400, 100), newLocation(400, 400), 2,
        36L, 1.95, Math.PI / 2, 8.0, 1, newBattleField(), newPredictor());
    Point2D.Double projectedLocation =
        w.projectLocationBlind(newLocation(500, 100), newLocation(0, 8), 39L);
    assertEquals(608, projectedLocation.x, 0.001);
    assertEquals(400, projectedLocation.y, 0.001);
  }

  @Test
  public void testGuessFactorZero() {
    Wave w = new Wave("Shadow", newLocation(), newLocation(), 2, 36L, 1.95,
        Math.random() * 2 * Math.PI, 8.0, 1, newBattleField(), newPredictor());
    assertEquals(0, w.guessFactor(w.absBearing), 0.001);
  }

  @Test
  public void testGuessFactorPositive() {
    Point2D.Double sourceLocation = newLocation();
    Point2D.Double targetLocation = newLocation();
    double heading = DiaUtils.absoluteBearing(sourceLocation, targetLocation)
        + (Math.PI / 2);
    Wave w = new Wave("Shadow", sourceLocation, targetLocation, 2, 36L, 1.95,
        heading, 8.0, 1, newBattleField(), newPredictor());
    assertEquals(
        0.5, w.guessFactor(w.absBearing + (Math.asin(8 / 14.15) / 2)), 0.001);
  }

  @Test
  public void testGuessFactorNegative() {
    Point2D.Double sourceLocation = newLocation();
    Point2D.Double targetLocation = newLocation();
    double heading = DiaUtils.absoluteBearing(sourceLocation, targetLocation)
        + (Math.PI / 2);
    Wave w = new Wave("Shadow", sourceLocation, targetLocation, 2, 36L, 1.95,
        heading, 8.0, 1, newBattleField(), newPredictor());
    assertEquals(
        -0.5, w.guessFactor(w.absBearing - (Math.asin(8 / 14.15) / 2)), 0.001);
  }

  @Test
  public void testGuessFactorPositiveReverseDirection() {
    Point2D.Double sourceLocation = newLocation();
    Point2D.Double targetLocation = newLocation();
    double heading = DiaUtils.absoluteBearing(sourceLocation, targetLocation)
        - (Math.PI / 2);
    Wave w = new Wave("Shadow", sourceLocation, targetLocation, 2, 36L, 1.95,
        heading, 8.0, 1, newBattleField(), newPredictor());
    assertEquals(
        0.5, w.guessFactor(w.absBearing - (Math.asin(8 / 14.15) / 2)), 0.001);
  }

  @Test
  public void testGuessFactorNegativeReverseDirection() {
    Point2D.Double sourceLocation = newLocation();
    Point2D.Double targetLocation = newLocation();
    double heading = DiaUtils.absoluteBearing(sourceLocation, targetLocation)
        + (Math.PI / 2);
    Wave w = new Wave("Shadow", sourceLocation, targetLocation, 2, 36L, 1.95,
        heading, -8.0, -1, newBattleField(), newPredictor());
    assertEquals(
        -0.5, w.guessFactor(w.absBearing + (Math.asin(8 / 14.15) / 2)), 0.001);
  }

  @Test
  public void testGuessFactorPreciseZero() {
    Wave w = new Wave("Shadow", newLocation(), newLocation(), 2, 36L, 1.95,
        Math.random() * 2 * Math.PI, 8.0, 1, newBattleField(), newPredictor());
    assertEquals(0, w.guessFactor(w.absBearing), 0.001);
  }

  @Test
  public void testGuessFactorPrecisePositive() {
    for (int x = 0; x < 50; x++) {
      Point2D.Double sourceLocation = newLocation();
      Point2D.Double targetLocation = newLocation();
      double heading = DiaUtils.absoluteBearing(sourceLocation, targetLocation)
          + (Math.PI / 2);
      Wave w = new Wave("Shadow", sourceLocation, targetLocation, 2, 36L, 1.95,
          heading, 8.0, 1, newBattleField(), newPredictor());
      double bearing = w.absBearing + 0.1;
      double preciseFactor = w.guessFactorPrecise(bearing);
      assertTrue("Precise GuessFactor: " + preciseFactor + " is not greater "
          + "than zero. Precise positive MEA: " + w.preciseEscapeAngle(true)
          + ", Source location: " + sourceLocation + ", Target location: "
          + targetLocation,        
          preciseFactor > 0);
      double guessFactor = w.guessFactor(bearing);
      assertTrue(preciseFactor > guessFactor);
    }
  }

  @Test
  public void testGuessFactorPreciseNegative() {
    for (int x = 0; x < 50; x++) {
      Point2D.Double sourceLocation = newLocation();
      Point2D.Double targetLocation = newLocation();
      double heading = DiaUtils.absoluteBearing(sourceLocation, targetLocation)
          + (Math.PI / 2);
      Wave w = new Wave("Shadow", sourceLocation, targetLocation, 2, 36L, 1.95,
          heading, 8.0, 1, newBattleField(), newPredictor());
      double negativeMea = w.preciseEscapeAngle(false);
      if (negativeMea > 0) {
        // if negative MEA < 0, all GFs will be considered positive until
        // if/when GF = 0 becomes the precise stop position
        double bearing = w.absBearing - 0.1;
        double preciseFactor = w.guessFactorPrecise(bearing);
        assertTrue("Precise GuessFactor: " + preciseFactor + " is not less than "
            + "zero. Precise negative MEA: " + w.preciseEscapeAngle(false)
            + ", Source location: " + sourceLocation + ", Target location: "
            + targetLocation,        
            preciseFactor < 0);
        double guessFactor = w.guessFactor(bearing);
        assertTrue(preciseFactor < guessFactor);
      }
    }
  }

  @Test
  public void testGuessFactorPrecisePositiveReverseDirection() {
    for (int x = 0; x < 50; x++) {
      Point2D.Double sourceLocation = newLocation();
      Point2D.Double targetLocation = newLocation();
      double heading = DiaUtils.absoluteBearing(sourceLocation, targetLocation)
          + (Math.PI / 2);
      Wave w = new Wave("Shadow", sourceLocation, targetLocation, 2, 36L, 1.95,
          heading, -8.0, -1, newBattleField(), newPredictor());
      double bearing = w.absBearing - 0.1;
      double preciseFactor = w.guessFactorPrecise(bearing);
      assertTrue("Precise GuessFactor: " + preciseFactor + " is not greater " +
          "than zero. Precise positive MEA: " + w.preciseEscapeAngle(true),
          preciseFactor > 0);
      double guessFactor = w.guessFactor(bearing);
      assertTrue(preciseFactor > guessFactor);
    }
  }

  @Test
  public void testGuessFactorPreciseNegativeReverseDirection() {
    for (int x = 0; x < 50; x++) {
      Point2D.Double sourceLocation = newLocation();
      Point2D.Double targetLocation = newLocation();
      double heading = DiaUtils.absoluteBearing(sourceLocation, targetLocation)
          - (Math.PI / 2);
      Wave w = new Wave("Shadow", sourceLocation, targetLocation, 2, 36L, 1.95,
          heading, 8.0, 1, newBattleField(), newPredictor());
      double negativeMea = w.preciseEscapeAngle(false);
      if (negativeMea > 0) {
        // if negative MEA < 0, all GFs will be considered positive until
        // if/when GF = 0 becomes the precise stop position
        double bearing = w.absBearing + 0.1;
        double preciseFactor = w.guessFactorPrecise(bearing);
        assertTrue("Precise GuessFactor: " + preciseFactor + " is not less than "
            + "zero. Precise negative MEA: " + w.preciseEscapeAngle(false)
            + ", Source location: " + sourceLocation + ", Target location: "
            + targetLocation,        
            preciseFactor < 0);
        double guessFactor = w.guessFactor(bearing);
        assertTrue(preciseFactor < guessFactor);
      }
    }
  }

  @Test
  public void testPreciseEscapeAngle() {
    Wave w = new Wave("Shadow", newLocation(), newLocation(), 2, 36L, 1.95,
        Math.random() * 2 * Math.PI, 8.0, 1, newBattleField(), newPredictor());
    double preciseMea = w.preciseEscapeAngle(true);
    assertTrue(preciseMea > 0);
    assertTrue(preciseMea < Math.asin(8.0 / 14.15));
  }

  @Test
  public void testPreciseEscapeAngleNegative() {
    Wave w = new Wave("Shadow", newLocation(), newLocation(), 2, 36L, 1.95,
        Math.random() * 2 * Math.PI, 8.0, 1, newBattleField(), newPredictor());
    double preciseMea = w.preciseEscapeAngle(false);
    assertTrue(-preciseMea < w.preciseEscapeAngle(true));
    assertTrue(preciseMea < Math.asin(8.0 / 14.15));
  }

  @Test
  public void testEscapeAngleRange() {
    fail("Not yet implemented");
  }

  @Test
  public void testCalculatePreciseEscapeAngle() {
    fail("Not yet implemented");
  }

  @Test
  public void testClearCachedPreciseEscapeAngles() {
    fail("Not yet implemented");
  }

  @Test
  public void testShadowed() {
    fail("Not yet implemented");
  }

  @Test
  public void testCastShadow() {
    fail("Not yet implemented");
  }

  @Test
  public void testShadowFactor() {
    fail("Not yet implemented");
  }

  @Test
  public void testPreciseIntersection() {
    fail("Not yet implemented");
  }

  @Test
  public void testCheckWavePositionRobotState() {
    fail("Not yet implemented");
  }

  @Test
  public void testCheckWavePositionRobotStateBoolean() {
    fail("Not yet implemented");
  }

  @Test
  public void testCheckWavePositionRobotStateWavePosition() {
    fail("Not yet implemented");
  }

  @Test
  public void testCheckWavePositionRobotStateBooleanWavePosition() {
    fail("Not yet implemented");
  }

  @Test
  public void testClone() {
    fail("Not yet implemented");
  }

  private Point2D.Double newLocation() {
    return newLocation(18 + Math.random() * 764, 18 + Math.random() * 564);
  }

  private Point2D.Double newLocation(double x, double y) {
    return new Point2D.Double(x, y);
  }

  private Wave newWave() {
    return new Wave("Shadow", newLocation(), newLocation(), 2, 36L, 1.95, 0.2,
        8.0, 1, newBattleField(), newPredictor());
  }

  private BattleField newBattleField() {
    return new BattleField(800, 600);
  }

  private MovementPredictor newPredictor() {
    return new MovementPredictor(new BattleField(800, 600));
  }
}
