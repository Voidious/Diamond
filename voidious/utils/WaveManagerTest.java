package voidious.utils;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.awt.geom.Point2D;

import org.junit.Test;

import voidious.utils.Wave.WavePosition;
import voidious.utils.WaveManager.CurrentWaveListener;

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

public class WaveManagerTest {

  @Test
  public void testWaveManager() {
    newWaveManager();
  }

  @Test
  public void testInitRound() {
    WaveManager waveManager = newWaveManager();
    waveManager.initRound();
    // TODO: do a checkCurrentWaves style test and make sure nothing happens
  }

  @Test
  public void testAddWave() {
    WaveManager waveManager = newWaveManager();
    waveManager.addWave(mock(Wave.class));
  }

  @Test
  public void testCheckCurrentWaves() {
    WaveManager waveManager = newWaveManager();
    Wave w = newWave(newLocation(), newLocation(), 88L, 3.0);
    waveManager.addWave(w);
    Wave w2 = newWave(newLocation(), newLocation(), 100L, 3.0);
    waveManager.addWave(w2);
    CurrentWaveListener listener = mock(CurrentWaveListener.class);
    waveManager.checkCurrentWaves(100L, listener);
    verify(listener, never()).onCurrentWave(w);
    verify(listener).onCurrentWave(w2);
  }

  @Test
  public void testCheckCurrentWavesNoWaves() {
    WaveManager waveManager = newWaveManager();
    CurrentWaveListener listener = mock(CurrentWaveListener.class);
    waveManager.checkCurrentWaves(100L, listener);
    verify(listener, never()).onCurrentWave((Wave) any());
  }

  // TODO: test checkActiveWaves as a whole?

  // TODO: move these to WaveTest
  @Test
  public void testCheckWavePositionMidair() {
    Wave w = newWave(newLocation(100, 100), newLocation(500, 500), 100L, 3.0);
    RobotState currentState = RobotState.newBuilder()
        .setLocation(newLocation(500, 500))
        .setTime(105L)
        .build();
    WavePosition wavePosition = w.checkWavePosition(currentState);
    assertEquals(WavePosition.MIDAIR, wavePosition);
  }

  @Test
  public void testCheckWavePositionMidairBarely() {
    testCheckWavePositionAlongAxis(19, WavePosition.MIDAIR);
  }

  @Test
  public void testCheckWavePositionBreakingFront() {
    testCheckWavePositionAlongAxis(17, WavePosition.BREAKING_FRONT);
  }

  @Test
  public void testCheckWavePositionBreakingFrontBarely() {
    testCheckWavePositionAlongAxis(1, WavePosition.BREAKING_FRONT);
  }

  @Test
  public void testCheckWavePositionBreakingCenter() {
    testCheckWavePositionAlongAxis(-1, WavePosition.BREAKING_CENTER);
  }

  @Test
  public void testCheckWavePositionBreakingCenterBarely() {
    testCheckWavePositionAlongAxis(-17, WavePosition.BREAKING_CENTER);
  }

  @Test
  public void testCheckWavePositionGone() {
    testCheckWavePositionAlongAxis(-31, WavePosition.GONE);
  }

  private void testCheckWavePositionAlongAxis(
      double offset, WavePosition expectedWavePosition) {
    double bulletPower = 3;
    double myX = 100;
    double y = 300;
    Wave w =
        newWave(newLocation(myX, y), newLocation(250, y), 100L, bulletPower);
    int bulletTicks = 10;
    double enemyX = myX + ((bulletTicks + 1) * w.bulletSpeed()) + offset;
    RobotState currentState = RobotState.newBuilder()
        .setLocation(newLocation(enemyX, y))
        .setTime(100L + bulletTicks)
        .build();
    WavePosition wavePosition = w.checkWavePosition(currentState);
    assertEquals(expectedWavePosition, wavePosition);
  }

  @Test
  public void testCheckWavePositionMidairBarelyDiagonal() {
    testCheckWavePositionDiagonal(26, WavePosition.MIDAIR);
  }

  @Test
  public void testCheckWavePositionBreakingFrontDiagonal() {
    testCheckWavePositionDiagonal(25, WavePosition.BREAKING_FRONT);
  }

  @Test
  public void testCheckWavePositionBreakingFrontBarelyDiagonal() {
    testCheckWavePositionDiagonal(1, WavePosition.BREAKING_FRONT);
  }

  @Test
  public void testCheckWavePositionBreakingCenterDiagonal() {
    testCheckWavePositionDiagonal(-1, WavePosition.BREAKING_CENTER);
  }

  @Test
  public void testCheckWavePositionBreakingCenterBarelyDiagonal() {
    testCheckWavePositionDiagonal(-25, WavePosition.BREAKING_CENTER);
  }

  @Test
  public void testCheckWavePositionGoneDiagonal() {
    testCheckWavePositionDiagonal(-37, WavePosition.GONE);
  }

  // TODO: consolidate some of this with testCheckWavePositionAlongAxis
  private void testCheckWavePositionDiagonal(
      double offset, WavePosition expectedWavePosition) {
    double bulletPower = 3;
    Point2D.Double myLocation = newLocation(100, 100);
    Wave w = newWave(myLocation, newLocation(200, 200), 100L, bulletPower);
    int bulletTicks = 10;
    Point2D.Double enemyLocation = DiaUtils.project(myLocation, Math.PI / 4,
        ((bulletTicks + 1) * w.bulletSpeed()) + offset);
    RobotState currentState = RobotState.newBuilder()
        .setLocation(enemyLocation)
        .setTime(100L + bulletTicks)
        .build();
    WavePosition wavePosition = w.checkWavePosition(currentState);
    assertEquals(expectedWavePosition, wavePosition);
  }

  @Test
  public void testFindClosestWave() {
    WaveManager waveManager = new WaveManager();
    Wave w = newWave(newLocation(100, 100), newLocation(100, 300), 100L, 1.0);
    waveManager.addWave(w);
    waveManager.addWave(
        newWave(newLocation(500, 500), newLocation(550, 550), 100L, 1.0));
    waveManager.addWave(
        newWave(newLocation(100, 100), newLocation(100, 200), 100L, 1.09));
    Wave closestWave = waveManager.findClosestWave(
        newLocation(130, 140), 103L, false, null, 1.0);
    assertEquals(w, closestWave);
  }

  // TODO: more permutations of findClosestWave

  private WaveManager newWaveManager() {
    return new WaveManager();
  }

  private Wave newWave(Point2D.Double sourceLocation,
      Point2D.Double targetLocation, long fireTime, double bulletPower) {
    return new Wave("Shadow", sourceLocation, targetLocation,
        1, fireTime, bulletPower, 0, 0, 1, mock(BattleField.class),
        mock(MovementPredictor.class));
  }

//  private Wave newWave() {
//    return new Wave("Shadow", newLocation(), newLocation(),
//        1, 100L, 3.0, 0, 0, 1, mock(BattleField.class),
//        mock(MovementPredictor.class));
//  }

  private Point2D.Double newLocation() {
    return newLocation(Math.random() * 800, Math.random() * 600);
  }

  private Point2D.Double newLocation(double x, double y) {
    return new Point2D.Double(x, y);
  }

  @Test
  public void testWaveGuessFactorToFiringAngle() {
    Wave w = newWave(newLocation(100, 100), newLocation(500, 300), 1L, 3.0);
    for (int x = 0; x < 100; x++) {
      double gf = ((double) x - 50) / 50.0;
      assertEquals(gf, w.guessFactor(w.firingAngle(gf)), 0.001);
    }
  }
}
