package voidious.move;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.awt.geom.Point2D;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import robocode.Bullet;
import robocode.ScannedRobotEvent;

import voidious.gfx.RoboGraphic;
import voidious.gun.FireListener.FiredBullet;
import voidious.test.RobocodeEventTestHelper;
import voidious.utils.BattleField;
import voidious.utils.MovementPredictor;
import voidious.utils.RobotState;
import voidious.utils.RobotStateLog;
import voidious.utils.Wave;

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

public class MoveDataManagerTest {
  private static final String WARNING_HIT_BY_BULLET_UNKNOWN =
      "WARNING (move): A bot shot me that I never knew existed!";
  private static final String WARNING_BULLET_HIT_BULLET_UNKNOWN =
      "WARNING (move): One of my bullets hit a bullet from a bot";
  private static final String WARNING_BULLET_HIT_UNKNOWN =
      "WARNING (move): One of my bullets hit a bot that I never ";

  private RobocodeEventTestHelper _eventHelper;
  private List<RoboGraphic> _renderables;
  private ByteArrayOutputStream _out;
  private ScannedRobotEvent _shadowSre;
  private ScannedRobotEvent _phoenixSre;
  private ScannedRobotEvent _portiaSre;

  @Before
  public void setUp() throws Exception {
    _eventHelper = new RobocodeEventTestHelper();
    _renderables = new ArrayList<RoboGraphic>();
    _out = new ByteArrayOutputStream();
    _shadowSre = _eventHelper.newScannedRobotEvent("Shadow");
    _phoenixSre = _eventHelper.newScannedRobotEvent("Phoenix");
    _portiaSre = _eventHelper.newScannedRobotEvent("Portia");
  }

  @Test
  public void testMoveDataManager() {
    newMoveDataManager();
  }

  @Test
  public void testInitRound() {
    MoveDataManager manager = newMoveDataManager();
    manager.addFiredBullet(new FiredBullet(1, newLocation(), 0, 0));
    manager.getMyStateLog().addState(RobotState.newBuilder()
        .setLocation(newLocation())
        .setTime(1)
        .build());
    manager.initRound();
    assertTrue(manager.getFiredBullets().isEmpty());
    assertTrue(manager.getMyStateLog().size() == 0);
  }

  @Test
  public void testExecuteMeleeDistanceSq() {
    MoveDataManager manager = newMoveDataManager();
    MoveEnemy shadowData =
        manager.newEnemy(_shadowSre, newLocation(0, 0), 0, 1, false);
    MoveEnemy phoenixData =
        manager.newEnemy(_phoenixSre, newLocation(0, 3), 0.3, 1, false);
    MoveEnemy portiaData =
        manager.newEnemy(_portiaSre, newLocation(4, 0), -0.21, 1, false);
    
    manager.execute(1, 50, newLocation(), 0, 8.0);
    assertEquals(9, shadowData.getBotDistanceSq("Phoenix"), 0.01);
    assertEquals(16, shadowData.getBotDistanceSq("Portia"), 0.01);
    assertEquals(9, phoenixData.getBotDistanceSq("Shadow"), 0.01);
    assertEquals(25, phoenixData.getBotDistanceSq("Portia"), 0.01);
    assertEquals(16, portiaData.getBotDistanceSq("Shadow"), 0.01);
    assertEquals(25, portiaData.getBotDistanceSq("Phoenix"), 0.01);
    assertEquals(Double.NaN, shadowData.getBotDistanceSq("SandboxDT"), 0.01);
    assertEquals(Double.NaN, phoenixData.getBotDistanceSq("SandboxDT"), 0.01);
    assertEquals(Double.NaN, portiaData.getBotDistanceSq("SandboxDT"), 0.01);
  }

  @Test
  public void testExecuteMeleeDamageFactors() {
    MoveDataManager manager = newMoveDataManager();
    MoveEnemy shadowData =
        manager.newEnemy(_shadowSre, newLocation(0, 0), 0, 1, false);
    MoveEnemy phoenixData =
        manager.newEnemy(_phoenixSre, newLocation(0, 3), 0.3, 1, false);
    MoveEnemy portiaData =
        manager.newEnemy(_portiaSre, newLocation(4, 0), -0.21, 1, false);
    shadowData.damageGiven = 5;
    shadowData.damageTaken = 10;
    phoenixData.damageGiven = 5;
    phoenixData.damageTaken = 3;
    portiaData.damageGiven = 1;
    portiaData.damageTaken = 10;
    manager.execute(1, 50, newLocation(), 0, 8.0);
    assertFalse(
        Math.abs(shadowData.damageFactor - phoenixData.damageFactor) < 0.001);
    assertFalse(
        Math.abs(shadowData.damageFactor - portiaData.damageFactor) < 0.001);
    assertFalse(
        Math.abs(portiaData.damageFactor - phoenixData.damageFactor) < 0.001);
  }

  @Test
  public void testExecute1v1() {
    MoveDataManager manager = newMoveDataManager();
    MoveEnemy shadowData = mock(MoveEnemy.class);
    shadowData.lastScanState = RobotState.newBuilder()
        .setLocation(newLocation())
        .setTime(49L)
        .build();
    manager.saveEnemy("Shadow", shadowData);
    manager.updateEnemy(
        _eventHelper.newScannedRobotEvent("Shadow"), newLocation(), 0, 1, true);
    Point2D.Double myLocation = newLocation();
    manager.execute(1, 50L, myLocation, 0, 8.0);
    verify(shadowData).execute1v1(1, 50L, myLocation);
  }

  @Test
  public void testExecuteMelee() {
    MoveDataManager manager = newMoveDataManager();
    MoveEnemy shadowData = mock(MoveEnemy.class);
    shadowData.lastScanState = RobotState.newBuilder()
        .setLocation(newLocation())
        .setTime(1)
        .build();
    manager.saveEnemy("Shadow", shadowData);
    MoveEnemy phoenixData = mock(MoveEnemy.class);
    phoenixData.lastScanState = RobotState.newBuilder()
        .setLocation(newLocation())
        .setTime(1)
        .build();
    manager.saveEnemy("Phoenix", phoenixData);
    MoveEnemy portiaData = mock(MoveEnemy.class);
    portiaData.lastScanState = RobotState.newBuilder()
        .setLocation(newLocation())
        .setTime(1)
        .build();
    manager.saveEnemy("Portia", portiaData);
    manager.updateEnemy(_eventHelper.newScannedRobotEvent("Shadow"),
        newLocation(), 0, 1, false);
    Point2D.Double myLocation = newLocation();
    manager.execute(1, 50L, myLocation, 0, 8.0);
    verify(shadowData, never()).execute1v1(
        anyInt(), anyLong(), (Point2D.Double) any());
    verify(phoenixData, never()).execute1v1(
        anyInt(), anyLong(), (Point2D.Double) any());
    verify(portiaData, never()).execute1v1(
        anyInt(), anyLong(), (Point2D.Double) any());
  }

  @Test
  public void testNewEnemy1v1() {
    testNewEnemy(true);
  }

  @Test
  public void testNewEnemyMelee() {
    testNewEnemy(false);
  }

  private void testNewEnemy(boolean is1v1) {
    MoveDataManager manager = newMoveDataManager();
    ScannedRobotEvent shadowSre =
        _eventHelper.newScannedRobotEvent("Shadow", 87, 0.15, 351, 1, 7.0);
    MoveEnemy shadowData =
        manager.newEnemy(shadowSre, newLocation(), 0.6, 1, is1v1);
    assertEquals("Shadow", shadowData.botName);
    assertEquals(87, shadowData.energy, 0.01);
    assertEquals(0.6, shadowData.absBearing, 0.01);
    assertEquals(351, shadowData.distance, 0.01);
    assertEquals(1, shadowData.lastScanState.heading, 0.01);
    assertEquals(7.0, shadowData.lastScanState.velocity, 0.01);
    if (is1v1) {
      assertEquals(shadowData, manager.duelEnemy());
    } else {
      assertNull(manager.duelEnemy());
    }
  }

  @Test
  public void testUpdateEnemy1v1() {
    testUpdateEnemy(true);
  }

  @Test
  public void testUpdateEnemyMelee() {
    testUpdateEnemy(true);
  }

  private void testUpdateEnemy(boolean is1v1) {
    MoveDataManager manager = newMoveDataManager();
    manager.newEnemy(_shadowSre, newLocation(), 2.2, 1, is1v1);
    ScannedRobotEvent shadowSre =
        _eventHelper.newScannedRobotEvent("Shadow", 87, 0.15, 351, 1, 7.0);
    MoveEnemy shadowData =
        manager.updateEnemy(shadowSre, newLocation(), 0.6, 1, is1v1);
    assertEquals("Shadow", shadowData.botName);
    assertEquals(87, shadowData.energy, 0.01);
    assertEquals(0.6, shadowData.absBearing, 0.01);
    assertEquals(351, shadowData.distance, 0.01);
    assertEquals(1, shadowData.lastScanState.heading, 0.01);
    assertEquals(7.0, shadowData.lastScanState.velocity, 0.01);
    if (is1v1) {
      assertEquals(shadowData, manager.duelEnemy());
    } else {
      assertNull(manager.duelEnemy());
    }
  }

  @Test
  public void testOnHitByBulletUnknownEnemy() {
    MoveDataManager manager = newMoveDataManager();
    manager.onHitByBullet(_eventHelper.newHitByBulletEvent(
        0, _eventHelper.newBullet("Shadow", "Portia", 3.0)), 1, 50L, false);
    assertTrue(_out.toString().contains(WARNING_HIT_BY_BULLET_UNKNOWN));
  }

  @Test
  public void testOnHitByBullet() {
    MoveDataManager manager = newMoveDataManager();
    MoveEnemy shadowData =
        manager.newEnemy(_shadowSre, newLocation(), 0, 1, true);
    shadowData.energy = 94.0;
    manager.onHitByBullet(_eventHelper.newHitByBulletEvent(
        0, _eventHelper.newBullet("Shadow", "Portia", 3.0)), 1, 50L, false);
    assertEquals(50L, shadowData.lastTimeHit);
    assertEquals(16, shadowData.damageTaken, 0.01);
    assertEquals(3.0, shadowData.totalBulletPower, 0.01);
    assertEquals(1, shadowData.totalTimesHit);
    assertEquals(103, shadowData.energy, 0.01);
  }

  @Test
  public void testOnHitByBulletHitWave() {
    MoveDataManager manager = newMoveDataManager();
    MoveEnemy shadowData = mock(MoveEnemy.class);
    manager.saveEnemy("Shadow", shadowData);
    Wave hitWave = mock(Wave.class);
    hitWave.targetDistance = 300;
    when(hitWave.escapeAngleRange()).thenReturn(0.98);
    when(shadowData.processBullet(
        (Bullet) any(), eq(1), eq(50L), anyBoolean()))
        .thenReturn(hitWave);
    manager.onHitByBullet(_eventHelper.newHitByBulletEvent(
        0, _eventHelper.newBullet("Shadow", "Portia", 3.0)), 1, 50L, false);
    assertTrue(shadowData.weighted1v1ShotsHit > 0);
    assertTrue(shadowData.weighted1v1ShotsHitThisRound > 0);
    assertEquals(1, shadowData.raw1v1ShotsHit, 0.01);
    assertEquals(1, shadowData.raw1v1ShotsHitThisRound, 0.01);
  }

  @Test
  public void testOnHitByBulletClearCache() {
    MoveDataManager manager = newMoveDataManager();
    MoveEnemy shadowData = mock(MoveEnemy.class);
    shadowData.lastScanState = RobotState.newBuilder()
        .setLocation(newLocation())
        .setTime(49L)
        .build();
    manager.saveEnemy("Shadow", shadowData);
    manager.updateEnemy(
        _eventHelper.newScannedRobotEvent("Shadow"), newLocation(), 0, 1, true);
    manager.onHitByBullet(_eventHelper.newHitByBulletEvent(
            0, _eventHelper.newBullet("Shadow", "Portia", 3.0)),
        1, 50L, false);
    verify(shadowData).clearNeighborCache();
  }

  @Test
  public void testOnBulletHitBulletUnknownEnemy() {
    MoveDataManager manager = newMoveDataManager();
    manager.onBulletHitBullet(
        _eventHelper.newBulletHitBulletEvent("Shadow", 3.0, "Portia", 1.9),
        1, 50L, false);
    assertTrue(_out.toString().contains(WARNING_BULLET_HIT_BULLET_UNKNOWN));
  }

  @Test
  public void testOnBulletHitBulletHitWave() {
    MoveDataManager manager = newMoveDataManager();
    MoveEnemy shadowData = mock(MoveEnemy.class);
    shadowData.raw1v1ShotsFired = 4;
    shadowData.raw1v1ShotsFiredThisRound = 3;
    manager.saveEnemy("Shadow", shadowData);
    Wave hitWave = mock(Wave.class);
    when(shadowData.processBullet(
        (Bullet) any(), eq(1), eq(50L), anyBoolean()))
        .thenReturn(hitWave);
    manager.onBulletHitBullet(
        _eventHelper.newBulletHitBulletEvent("Diamond", 3.0, "Shadow", 1.9),
        1, 50L, false);
    assertEquals(3, shadowData.raw1v1ShotsFired, 0.01);
    assertEquals(2, shadowData.raw1v1ShotsFiredThisRound, 0.01);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testOnBulletHitBulletClearCacheResetShadows() {
    MoveDataManager manager = newMoveDataManager();
    MoveEnemy shadowData = mock(MoveEnemy.class);
    shadowData.lastScanState = RobotState.newBuilder()
        .setLocation(newLocation())
        .setTime(49L)
        .build();
    manager.saveEnemy("Shadow", shadowData);
    manager.updateEnemy(
        _eventHelper.newScannedRobotEvent("Shadow"), newLocation(), 0, 1, true);
    manager.onBulletHitBullet(
        _eventHelper.newBulletHitBulletEvent("Diamond", 3.0, "Shadow", 1.9),
        1, 50L, false);
    verify(shadowData).clearNeighborCache();
    verify(shadowData).resetBulletShadows((List<FiredBullet>) any());
  }

  @Test
  public void testOnBulletHitUnknownEnemy() {
    MoveDataManager manager = newMoveDataManager();
    manager.onBulletHit(
        _eventHelper.newBulletHitEvent("Diamond", "Shadow", 3.0));
    assertTrue(_out.toString().contains(WARNING_BULLET_HIT_UNKNOWN));
  }

  @Test
  public void testOnBulletHit() {
    MoveDataManager manager = newMoveDataManager();
    ScannedRobotEvent shadowSre =
        _eventHelper.newScannedRobotEvent("Shadow", 87, 0.15, 351, 1, 7.0);
    MoveEnemy shadowData =
        manager.newEnemy(shadowSre, newLocation(), 0.6, 1, true);
    shadowData.energy = 91;
    shadowData.damageGiven = 11;
    manager.onBulletHit(
        _eventHelper.newBulletHitEvent("Diamond", "Shadow", 3.0));
    assertEquals(75, shadowData.energy, 0.01);
    assertEquals(27, shadowData.damageGiven, 0.01);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testUpdateEnemyWaves() {
    MoveDataManager manager = newMoveDataManager();
    MoveEnemy shadowData = mock(MoveEnemy.class);
    shadowData.wallHitDamage = new MoveEnemy.WallHitDamage(0, 0);
    shadowData.lastScanState = RobotState.newBuilder()
        .setLocation(newLocation())
        .setTime(31L)
        .build();
    manager.saveEnemy("Shadow", shadowData);
    shadowData.energy = 81.5;
    shadowData.raw1v1ShotsFired = 9;
    shadowData.raw1v1ShotsFiredThisRound = 3;
    shadowData.lastScanState = RobotState.newBuilder()
        .setLocation(newLocation(100, 100))
        .setHeading(0)
        .setVelocity(8)
        .setTime(31L)
        .build();
    when(shadowData.guessBulletPower(anyDouble())).thenReturn(1.5);
    Point2D.Double myLocation = newLocation();
    manager.updateEnemyWaves(
        myLocation, 82.0, "Shadow", 1, 32L, 99, 1, 7.0, 2);

    assertEquals(10, shadowData.raw1v1ShotsFired, 0.01);
    assertEquals(4, shadowData.raw1v1ShotsFiredThisRound, 0.01);
    verify(shadowData).newMoveWave(eq(newLocation(100, 108)), eq(myLocation),
        anyDouble(), eq(1), eq(33L), eq(1.5), eq(99D), eq(1D), eq(7.0), anyInt(),
        anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyLong(),
        anyLong());
    verify(shadowData).updateImaginaryWave(
        eq(32L), (RobotState) any(), eq(2), (MovementPredictor) any());
    verify(shadowData).updateFiringWave(
        eq(32L), eq(0.5), (RobotStateLog) any(), (List<FiredBullet>) any());
  }

  // TODO: test other paths through updateEnemyWaves

  private MoveDataManager newMoveDataManager() {
    BattleField battleField = new BattleField(800, 600);
    return new MoveDataManager(4, battleField,
        new MovementPredictor(battleField), _renderables, _out);
  }

  private Point2D.Double newLocation() {
    return newLocation(Math.random() * 800, Math.random() * 600);
  }

  private Point2D.Double newLocation(double x, double y) {
    return new Point2D.Double(x, y);
  }
}
