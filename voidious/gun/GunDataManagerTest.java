package voidious.gun;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.awt.geom.Point2D;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.ScannedRobotEvent;
import voidious.gfx.RoboGraphic;
import voidious.gun.GunDataManager.GunDataListener;
import voidious.test.RobocodeEventTestHelper;
import voidious.utils.BattleField;
import voidious.utils.MovementPredictor;
import voidious.utils.RobotState;
import voidious.utils.RobotStateLog;

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

public class GunDataManagerTest {
  private RobocodeEventTestHelper _eventHelper;
  private ScannedRobotEvent _shadowSre;
  private ScannedRobotEvent _phoenixSre;
  private ScannedRobotEvent _portiaSre;
  private Point2D.Double _myLocation;
  private Point2D.Double _enemyLocation;
  private ByteArrayOutputStream _out;

  @Before
  public void setUp() throws Exception {
    _eventHelper = new RobocodeEventTestHelper();
    _shadowSre = _eventHelper.newScannedRobotEvent("Shadow");
    _phoenixSre = _eventHelper.newScannedRobotEvent("Phoenix");
    _portiaSre = _eventHelper.newScannedRobotEvent("Portia");
    _myLocation = new Point2D.Double(20, 20);
    _enemyLocation = new Point2D.Double(0, 0);
    _out = new ByteArrayOutputStream();
  }
  
  @Test
  public void testConstruction() {
    newGunDataManager();
  }

  @Test
  public void testGetGunData() {
    GunDataManager gdm = newGunDataManager();
    gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, true);
    GunEnemy gunData = gdm.getEnemyData("Shadow");
    assertNotNull(gunData);
  }

  @Test
  public void testGetGunDataEmpty() {
    GunDataManager gdm = newGunDataManager();
    assertNull(gdm.getEnemyData("Shadow"));
  }

  @Test
  public void testGetGunDataNotFound() {
    GunDataManager gdm = newGunDataManager();
    gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, true);
    assertNull(gdm.getEnemyData("Ascendant"));
  }

  @Test
  public void testGetAllGunData() {
    GunDataManager gdm = newGunDataManager(3);
    gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, false);
    gdm.newEnemy(_phoenixSre, _enemyLocation, 0, 1, false);
    gdm.newEnemy(_portiaSre, _enemyLocation, 0, 1, false);

    Collection<GunEnemy> allGunData = gdm.getAllEnemyData();
    assertEquals(3, allGunData.size());

    boolean foundShadow = false;
    boolean foundPhoenix = false;
    boolean foundPortia = false;
    for (GunEnemy gunData : allGunData) {
      if (gunData.botName.equals("Shadow")) {
        foundShadow = true;
      }
      if (gunData.botName.equals("Phoenix")) {
        foundPhoenix = true;
      }
      if (gunData.botName.equals("Portia")) {
        foundPortia = true;
      }
    }
    assertTrue(foundShadow);
    assertTrue(foundPhoenix);
    assertTrue(foundPortia);
  }

  @Test
  public void testGetAllGunDataOne() {
    GunDataManager gdm = newGunDataManager();
    gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, true);

    Collection<GunEnemy> allGunData = gdm.getAllEnemyData();
    assertEquals(1, allGunData.size());
    assertEquals("Shadow", allGunData.iterator().next().botName);
  }

  @Test
  public void testGetAllGunDataNone() {
    GunDataManager gdm = newGunDataManager();
    Collection<GunEnemy> allGunData = gdm.getAllEnemyData();
    assertTrue(allGunData.isEmpty());
  }

  // TODO: move these to EnemyDataManagerTest
  @Test
  public void testInitRound() {
    // This is a little deep into GunEnemy to test in the manager, but these
    // aspects of GunEnemy are pretty unlikely to change. If it starts seeming
    // brittle, delete it and just leave test that GunEnemy.initRound is being
    // called.
    GunDataManager gdm = newGunDataManager(3);
    GunEnemy shadowData = gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, false);
    shadowData.alive = false;
    GunEnemy phoenixData = gdm.newEnemy(_phoenixSre, _enemyLocation, 0, 1, false);
    GunEnemy portiaData = gdm.newEnemy(_portiaSre, _enemyLocation, 0, 1, false);
    portiaData.alive = false;
    portiaData.setRobotState(RobotState.newBuilder()
        .setLocation(new Point2D.Double(50, 90))
        .setTime(1)
        .build());
    gdm.initRound();

    assertTrue(shadowData.alive);
    assertTrue(phoenixData.alive);
    assertTrue(portiaData.alive);
    assertTrue(shadowData.stateLog.size() == 0);
    assertTrue(phoenixData.stateLog.size() == 0);
    assertTrue(portiaData.stateLog.size() == 0);
  }

  @Test
  public void testInitRoundOne() {
    GunDataManager gdm = newGunDataManager();
    gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, true);
    gdm.initRound();
  }

  @Test
  public void testInitRoundNone() {
    GunDataManager gdm = newGunDataManager();
    gdm.initRound();
  }

  @Test
  public void testInitRoundCallsGunEnemyInitRound() {
    GunEnemy shadowData = mock(GunEnemy.class);
    GunEnemy phoenixData = mock(GunEnemy.class);
    GunEnemy portiaData = mock(GunEnemy.class);
    
    GunDataManager gdm = newGunDataManager(3);
    gdm.saveEnemy("Shadow", shadowData);
    gdm.saveEnemy("Phoenix", phoenixData);
    gdm.saveEnemy("Portia", portiaData);
    gdm.initRound();

    verify(shadowData).initRound();
    verify(phoenixData).initRound();
    verify(portiaData).initRound();
  }

  @Test
  public void testExecuteMeleeDistanceSq() {
    GunDataManager gdm = newGunDataManager(3);
    GunEnemy shadowData =
        gdm.newEnemy(_shadowSre, new Point2D.Double(0, 0), 0, 1, false);
    GunEnemy phoenixData =
        gdm.newEnemy(_phoenixSre, new Point2D.Double(0, 3), 0, 1, false);
    GunEnemy portiaData =
        gdm.newEnemy(_portiaSre, new Point2D.Double(4, 0), 0, 1, false);
    
    gdm.execute(1, 50, 3.0, 1.0, _myLocation, false, false);
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
  public void testExecute1v1() {
    GunDataManager gdm = newGunDataManager();
    GunEnemy shadowData = gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, true);
    shadowData.setRobotState(RobotState.newBuilder()
        .setLocation(newLocation())
        .setTime(5L)
        .build());
    assertEquals(2, shadowData.stateLog.size());
    
    gdm.execute(1, 5, 3.0, 1.0, _myLocation, false, true);
    assertEquals(Double.NaN, shadowData.getBotDistanceSq("SandboxDT"), 0.01);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testExecute() {
    GunDataManager gdm = newGunDataManager(2);
    GunEnemy shadowData = mock(GunEnemy.class);
    shadowData.alive = true;
    shadowData.lastScanState = RobotState.newBuilder()
        .setLocation(newLocation())
        .setTime(1)
        .build();
    GunEnemy phoenixData = mock(GunEnemy.class);
    phoenixData.alive = true;
    phoenixData.lastScanState = RobotState.newBuilder()
        .setLocation(newLocation())
        .setTime(1)
        .build();
    GunEnemy portiaData = mock(GunEnemy.class);
    portiaData.alive = true;
    portiaData.lastScanState = RobotState.newBuilder()
        .setLocation(newLocation())
        .setTime(1)
        .build();
    gdm.saveEnemy("Shadow", shadowData);
    gdm.saveEnemy("Phoenix", phoenixData);
    gdm.saveEnemy("Portia", portiaData);

    gdm.execute(1, 50, 3.0, 1.0, _myLocation, false, false);
    verify(shadowData).execute(eq(50L), anyLong(), eq(3.0), eq(1.0),
        eq(_myLocation), eq(false), eq(2), (List<GunDataListener>) any(), eq(false));
    verify(phoenixData).execute(eq(50L), anyLong(), eq(3.0), eq(1.0),
        eq(_myLocation), eq(false), eq(2), (List<GunDataListener>) any(), eq(false));
    verify(portiaData, never()).execute(eq(50L), anyLong(), eq(1.0), eq(3.0),
        eq(_myLocation), eq(false), eq(2), (List<GunDataListener>) any(), eq(false));
  }

  @Test
  public void testHasEnemyTrue() {
    GunDataManager gdm = newGunDataManager();
    gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, true);
    assertTrue(gdm.hasEnemy("Shadow"));
  }

  @Test
  public void testHasEnemyFalse() {
    GunDataManager gdm = newGunDataManager();
    gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, true);
    assertFalse(gdm.hasEnemy("Portia"));
  }

  @Test
  public void testHasEnemyEmpty() {
    GunDataManager gdm = newGunDataManager();
    assertFalse(gdm.hasEnemy("Shadow"));
  }

  @Test
  public void testNewEnemy() {
    GunDataManager gdm = newGunDataManager();
    GunEnemy shadowData = gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, true);
    assertEquals(_shadowSre.getName(), shadowData.botName);
    assertEquals(_shadowSre.getEnergy(), shadowData.energy, 0.01);
    assertEquals(_shadowSre.getDistance(), shadowData.distance, 0.01);
    assertEquals(_shadowSre.getHeading(),
                 shadowData.lastScanState.heading, 0.01);
    assertEquals(_shadowSre.getVelocity(),
                 shadowData.lastScanState.velocity, 0.01);
    assertEquals(_shadowSre.getTime(), shadowData.lastScanState.time);
    assertEquals(_enemyLocation, shadowData.lastScanState.location);
  }

  @Test
  public void testNewEnemyExists() {
    GunDataManager gdm = newGunDataManager();
    gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, true);
    try {
      gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, true);
      fail();
    } catch (IllegalArgumentException iae) {
      // expected
    }
  }

  @Test
  public void testUpdateEnemy() {
    GunDataManager gdm = newGunDataManager();
    gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, true);
    Point2D.Double newLocation = new Point2D.Double(500, 500);
    GunEnemy shadowData = gdm.updateEnemy(
        _eventHelper.newScannedRobotEvent("Shadow", 93.0, 2, 50, 3.0, -2.0),
        newLocation, 0, 1, true);
    assertEquals(_shadowSre.getName(), shadowData.botName);
    assertEquals(93.0, shadowData.energy, 0.01);
    assertEquals(50, shadowData.distance, 0.01);
    assertEquals(3.0, shadowData.lastScanState.heading, 0.01);
    assertEquals(-2.0, shadowData.lastScanState.velocity, 0.01);
    assertEquals(newLocation, shadowData.lastScanState.location);
  }

  // TODO: move to EnemyDataManagerTest
//  @Test
//  public void testOnRobotDeath() {
//    GunDataManager gdm = newGunDataManager();
//    GunEnemy shadowData = gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, true);
//    assertTrue(shadowData.alive);
//    gdm.onRobotDeath(_eventHelper.newRobotDeathEvent("Shadow"));
//    assertFalse(shadowData.alive);
//    assertFalse(_out.toString().contains(
//        GunDataManager.WARNING_ROBOT_DEATH_UNKNOWN));
//  }
//
//  @Test
//  public void testOnRobotDeathUnknownBot() {
//    GunDataManager gdm = newGunDataManager();
//    assertTrue(gdm.getAllEnemyData().isEmpty());
//    gdm.onRobotDeath(_eventHelper.newRobotDeathEvent("Shadow"));
//    assertTrue(_out.toString().contains(
//        GunDataManager.WARNING_ROBOT_DEATH_UNKNOWN));
//  }

  @Test
  public void testOnBulletHit() {
    GunDataManager gdm = newGunDataManager();
    GunEnemy shadowData = gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, true);
    assertTrue(shadowData.energy > 16.0);
    verifyBulletHit(gdm, shadowData, 16.0);
  }

  @Test
  public void testOnBulletHitLowEnergy() {
    GunDataManager gdm = newGunDataManager();
    GunEnemy shadowData = gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, true);
    shadowData.energy = 2;
    verifyBulletHit(gdm, shadowData, 2.0);
  }

  private void verifyBulletHit(
      GunDataManager gdm, GunEnemy shadowData, double damageGiven) {
    double initialDamageGiven = shadowData.damageGiven;
    BulletHitEvent bulletHitEvent =
        _eventHelper.newBulletHitEvent("Diamond", "Shadow", 3.0);
    gdm.onBulletHit(bulletHitEvent, 50);
    assertEquals(damageGiven, shadowData.damageGiven - initialDamageGiven, 0.01);
    assertFalse(_out.toString().contains(
        GunDataManager.WARNING_BULLET_HIT_UNKNOWN));
  }

  @Test
  public void testOnBulletHitUnknownBot() {
    GunDataManager gdm = newGunDataManager();
    assertTrue(gdm.getAllEnemyData().isEmpty());
    BulletHitEvent bulletHitEvent =
        _eventHelper.newBulletHitEvent("Diamond", "Shadow", 3.0);
    gdm.onBulletHit(bulletHitEvent, 50);
    assertTrue(_out.toString().contains(
        GunDataManager.WARNING_BULLET_HIT_UNKNOWN));
  }

  @Test
  public void testOnBulletHitCallsGunEnemyProcessBulletHit() {
    GunDataManager gdm = newGunDataManager();
    GunEnemy shadowData = mock(GunEnemy.class);
    gdm.saveEnemy("Shadow", shadowData);

    BulletHitEvent bulletHitEvent =
        _eventHelper.newBulletHitEvent("Diamond", "Shadow", 3.0);
    gdm.onBulletHit(bulletHitEvent, 50);
    verify(shadowData)
        .processBulletHit(bulletHitEvent.getBullet(), 50, true, true);
  }

  // TODO: test hitEnemy

  @Test
  public void testOnBulletHitBullet() {
    GunDataManager gdm = newGunDataManager();
    gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, true);
    BulletHitBulletEvent bulletHitBulletEvent =
        _eventHelper.newBulletHitBulletEvent("Diamond", 3.0, "Shadow", 3.0);
    gdm.onBulletHitBullet(bulletHitBulletEvent, 50);
    assertFalse(_out.toString().contains(
        GunDataManager.WARNING_BULLET_HIT_BULLET_UNKNOWN));
  }

  @Test
  public void testOnBulletHitBulletUnknownBot() {
    GunDataManager gdm = newGunDataManager();
    assertTrue(gdm.getAllEnemyData().isEmpty());
    BulletHitBulletEvent bulletHitBulletEvent =
        _eventHelper.newBulletHitBulletEvent("Diamond", 3.0, "Shadow", 3.0);
    gdm.onBulletHitBullet(bulletHitBulletEvent, 50);
    assertTrue(_out.toString().contains(
        GunDataManager.WARNING_BULLET_HIT_BULLET_UNKNOWN));
  }

  @Test
  public void testOnBulletHitBulletCallsGunEnemyProcessBulletHit() {
    GunDataManager gdm = newGunDataManager();
    GunEnemy shadowData = mock(GunEnemy.class);
    shadowData.botName = "Shadow";
    gdm.saveEnemy("Shadow", shadowData);

    BulletHitBulletEvent bulletHitBulletEvent =
      _eventHelper.newBulletHitBulletEvent("Diamond", 3.0, "Shadow", 3.0);
    gdm.onBulletHitBullet(bulletHitBulletEvent, 50);
    verify(shadowData).processBulletHit(
        bulletHitBulletEvent.getBullet(), 50, true, true);
  }

  @Test
  public void testGetDamageGiven() {
    GunDataManager gdm = newGunDataManager();
    GunEnemy shadowData = gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, false);
    GunEnemy phoenixData = gdm.newEnemy(_phoenixSre, _enemyLocation, 0, 1, false);
    GunEnemy portiaData = gdm.newEnemy(_portiaSre, _enemyLocation, 0, 1, false);
    shadowData.damageGiven = 0;
    phoenixData.damageGiven = 10;
    portiaData.damageGiven = 25.5;
    assertEquals(35.5, gdm.getDamageGiven(), 0.01);
  }

  @Test
  public void testGetDamageGivenOneEnemy() {
    GunDataManager gdm = newGunDataManager();
    GunEnemy shadowData = gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, true);
    shadowData.damageGiven = 12.2;
    assertEquals(12.2, gdm.getDamageGiven(), 0.01);
  }

  @Test
  public void testGetDamageGivenDeadEnemies() {
    GunDataManager gdm = newGunDataManager();
    GunEnemy shadowData = gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, false);
    GunEnemy phoenixData = gdm.newEnemy(_phoenixSre, _enemyLocation, 0, 1, false);
    GunEnemy portiaData = gdm.newEnemy(_portiaSre, _enemyLocation, 0, 1, false);
    shadowData.damageGiven = 5;
    shadowData.alive = false;
    phoenixData.damageGiven = 10;
    portiaData.damageGiven = 25.5;
    assertEquals(40.5, gdm.getDamageGiven(), 0.01);
  }

  @Test
  public void testGetDamageGivenNoEnemies() {
    GunDataManager gdm = newGunDataManager();
    assertEquals(0, gdm.getAllEnemyData().size());
    assertEquals(0, gdm.getDamageGiven(), 0.01);
  }

  @Test
  public void testGetAverageEnergy() {
    GunDataManager gdm = newGunDataManager();
    GunEnemy shadowData = gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, false);
    GunEnemy phoenixData = gdm.newEnemy(_phoenixSre, _enemyLocation, 0, 1, false);
    GunEnemy portiaData = gdm.newEnemy(_portiaSre, _enemyLocation, 0, 1, false);
    shadowData.energy = 50;
    phoenixData.energy = 25;
    portiaData.energy = 75;
    assertEquals(50, gdm.getAverageEnergy(), 0.01);
  }

  @Test
  public void testGetAverageEnergyOneEnemy() {
    GunDataManager gdm = newGunDataManager();
    GunEnemy shadowData = gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, true);
    shadowData.energy = 20;
    assertEquals(20, gdm.getAverageEnergy(), 0.01);
  }

  @Test
  public void testGetAverageEnergyDeadEnemies() {
    GunDataManager gdm = newGunDataManager();
    GunEnemy shadowData = gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, false);
    GunEnemy phoenixData = gdm.newEnemy(_phoenixSre, _enemyLocation, 0, 1, false);
    GunEnemy portiaData = gdm.newEnemy(_portiaSre, _enemyLocation, 0, 1, false);
    shadowData.energy = 50;
    phoenixData.energy = 25;
    phoenixData.alive = false;
    portiaData.energy = 60;
    assertEquals(55, gdm.getAverageEnergy(), 0.01);
  }

  @Test
  public void testGetAverageEnergyAllDeadEnemies() {
    GunDataManager gdm = newGunDataManager();
    GunEnemy shadowData = gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, false);
    GunEnemy phoenixData = gdm.newEnemy(_phoenixSre, _enemyLocation, 0, 1, false);
    GunEnemy portiaData = gdm.newEnemy(_portiaSre, _enemyLocation, 0, 1, false);
    shadowData.energy = 50;
    shadowData.alive = false;
    phoenixData.energy = 25;
    phoenixData.alive = false;
    portiaData.energy = 60;
    portiaData.alive = false;
    assertEquals(0, gdm.getAverageEnergy(), 0.01);
  }

  @Test
  public void testGetAverageEnergyNoEnemies() {
    GunDataManager gdm = newGunDataManager();
    assertEquals(0, gdm.getAverageEnergy(), 0.01);
  }

  @Test
  public void testMarkFiringWaves() {
    GunEnemy shadowData = mock(GunEnemy.class);
    shadowData.alive = true;
    GunEnemy phoenixData = mock(GunEnemy.class);
    phoenixData.alive = true;
    GunEnemy portiaData = mock(GunEnemy.class);
    portiaData.alive = true;
    
    GunDataManager gdm = newGunDataManager(3);
    gdm.saveEnemy("Shadow", shadowData);
    gdm.saveEnemy("Phoenix", phoenixData);
    gdm.saveEnemy("Portia", portiaData);
    GunDataListener listener = mock(GunDataListener.class);
    gdm.addListener(listener);
    gdm.markFiringWaves(1, false);

    verify(shadowData).markFiringWaves(1, false, Arrays.asList(listener));
    verify(phoenixData).markFiringWaves(1, false, Arrays.asList(listener));
    verify(portiaData).markFiringWaves(1, false, Arrays.asList(listener));
  }

  @Test
  public void testMarkFiringWavesListenerNull() {
    GunEnemy shadowData = mock(GunEnemy.class);
    
    GunDataManager gdm = newGunDataManager();
    gdm.saveEnemy("Shadow", shadowData);
    gdm.markFiringWaves(1, true);

    verify(shadowData).markFiringWaves(
        1, true, new ArrayList<GunDataListener>());
  }

  @Test
  public void testFireNextTickWave() {
    GunDataManager gdm = newGunDataManager();
    GunEnemy shadowData = gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, true);
    shadowData.lastWaveFired = null;
    gdm.fireNextTickWave(_myLocation, _enemyLocation, shadowData.botName, 1, 50,
        3.0, 100, 0.0, 0, 0, 1);
    assertNotNull(shadowData.lastWaveFired);
  }

  @Test
  public void testFireNextTickWaveMelee() {
    GunDataManager gdm = newGunDataManager(3);
    GunEnemy shadowData = gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, false);
    gdm.newEnemy(_phoenixSre, _enemyLocation, 0, 1, false);
    gdm.newEnemy(_portiaSre, _enemyLocation, 0, 1, false);
    shadowData.lastWaveFired = null;
    gdm.fireNextTickWave(_myLocation, _enemyLocation, shadowData.botName, 1, 50,
        3.0, 100, 0.0, 0, 0, 1);
    assertNotNull(shadowData.lastWaveFired);
  }

  // TODO: test gun wave interpolation

  @Test
  public void testFireNextTickWaveMeleeCallsGunEnemyNewGunWave() {
    testFireNextTickWaveMeleeCallsGunEnemyNewGunWave(true, 2);
  }

  @Test
  public void testFireNextTickWaveMeleeCallsGunEnemyNewGunWaveDeadEnemy() {
    testFireNextTickWaveMeleeCallsGunEnemyNewGunWave(false, 1);
  }
  
  private void testFireNextTickWaveMeleeCallsGunEnemyNewGunWave(
      boolean portiaAlive, int altWaves) {
    GunDataManager gdm = newGunDataManager(3);
    GunEnemy shadowData = mock(GunEnemy.class);
    shadowData.alive = true;
    RobotState shadowState = RobotState.newBuilder()
        .setLocation(newLocation())
        .setTime(1)
        .build();
    RobotStateLog stateLog = new RobotStateLog();
    stateLog.addState(shadowState);
    shadowData.lastScanState = shadowState;
    shadowData.stateLog = stateLog;
    shadowData.botName = "Shadow";
    gdm.saveEnemy("Shadow", shadowData);
    gdm.newEnemy(_phoenixSre, _enemyLocation, 0, 1, false);
    GunEnemy portiaData = gdm.newEnemy(_portiaSre, _enemyLocation, 0, 1, false);
    portiaData.alive = portiaAlive;
    gdm.fireNextTickWave(_myLocation, _enemyLocation, "Shadow", 1, 50, 3.0, 100,
        0.0, 0, 0, 3);
    verify(shadowData).newGunWave((Point2D.Double) any(),
        (Point2D.Double) any(), eq(1), eq(51L), anyLong(), eq(3.0), eq(100.0),
        eq(0.0), eq(3), anyDouble(), anyDouble(), anyDouble(), anyDouble(),
        eq(false));
    verify(shadowData, times(altWaves)).newGunWave((Point2D.Double) any(),
        (Point2D.Double) any(), eq(1), eq(51L), anyLong(), eq(3.0), anyDouble(),
        anyDouble(), eq(3), anyDouble(), anyDouble(), anyDouble(), anyDouble(),
        eq(true));
  }

  @Test
  public void testGetClosestLivingBot() {
    testGetClosestLivingBot(true, true, true, "Shadow");
  }

  @Test
  public void testGetClosestLivingBotDeadEnemy() {
    testGetClosestLivingBot(false, true, true, "Phoenix");
  }

  @Test
  public void testGetClosestLivingBotDeadEnemy2() {
    testGetClosestLivingBot(false, false, true, "Portia");
  }

  @Test
  public void testGetClosestLivingBotDeadEnemy3() {
    testGetClosestLivingBot(true, false, true, "Shadow");
  }

  @Test
  public void testGetClosestLivingBotAllDeadEnemies() {
    testGetClosestLivingBot(false, false, false, null);
  }

  private void testGetClosestLivingBot(boolean shadowAlive,
      boolean phoenixAlive, boolean portiaAlive, String closestLivingBotName) {
    GunDataManager gdm = newGunDataManager(3);
    GunEnemy shadowData = gdm.newEnemy(_shadowSre, _enemyLocation, 0, 1, false);
    GunEnemy phoenixData = gdm.newEnemy(_phoenixSre, _enemyLocation, 0, 1, false);
    GunEnemy portiaData = gdm.newEnemy(_portiaSre, _enemyLocation, 0, 1, false);
    shadowData.setRobotState(RobotState.newBuilder()
        .setLocation(newLocation(200, 100))
        .setTime(1)
        .build());
    shadowData.alive = shadowAlive;
    phoenixData.setRobotState(RobotState.newBuilder()
        .setLocation(newLocation(100, 201))
        .setTime(1)
        .build());
    phoenixData.alive = phoenixAlive;
    portiaData.setRobotState(RobotState.newBuilder()
        .setLocation(newLocation(180, 180))
        .setTime(1)
        .build());
    portiaData.alive = portiaAlive;

    GunEnemy closestLivingBot =
        gdm.getClosestLivingBot(new Point2D.Double(100, 100));
    if (closestLivingBotName == null) {
      assertNull(closestLivingBot);
    } else {
      assertEquals(gdm.getEnemyData(closestLivingBotName), closestLivingBot);
    }
  }

  private GunDataManager newGunDataManager() {
    return newGunDataManager(1);
  }

  private GunDataManager newGunDataManager(int enemiesTotal) {
    BattleField battleField = new BattleField(800, 600);
    return new GunDataManager(enemiesTotal, battleField,
        new MovementPredictor(battleField), new ArrayList<RoboGraphic>(), _out);
  }

  private Point2D.Double newLocation() {
    return newLocation(Math.random() * 800, Math.random() * 600);
  }

  private Point2D.Double newLocation(double x, double y) {
    return new Point2D.Double(x, y);
  }
}
