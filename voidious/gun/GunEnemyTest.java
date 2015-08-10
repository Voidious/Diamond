package voidious.gun;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import robocode.Bullet;
import voidious.gfx.RoboGraphic;
import voidious.gun.GunDataManager.GunDataListener;
import voidious.gun.formulas.AntiSurferFormula;
import voidious.gun.formulas.MainGunFormula;
import voidious.test.RobocodeEventTestHelper;
import voidious.utils.BattleField;
import voidious.utils.DiaUtils;
import voidious.utils.KnnView;
import voidious.utils.MovementPredictor;
import voidious.utils.RobotState;
import voidious.utils.TimestampedFiringAngle;
import voidious.utils.Wave;
import voidious.utils.WaveManager;

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

public class GunEnemyTest {
  private RobocodeEventTestHelper _eventHelper;
  private Point2D.Double _enemyLocation;
  private KnnView<TimestampedFiringAngle> _mainView;
  private KnnView<TimestampedFiringAngle> _asView;
  
  @Before
  public void setUp() throws Exception {
    _eventHelper = new RobocodeEventTestHelper();
    _enemyLocation = new Point2D.Double(100, 100);
    _mainView = new KnnView<TimestampedFiringAngle>(new MainGunFormula(1))
        .meleeOn()
        .visitsOn()
        .virtualWavesOn();
    _asView = new KnnView<TimestampedFiringAngle>(new AntiSurferFormula())
        .bulletHitsOn();
  }

  @Test
  public void testGunEnemy() {
    newGunEnemy();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testAddViews() {
    GunEnemy gunData = newGunEnemy();
    gunData.addViews(Arrays.asList(_mainView, _asView));
    assertEquals(2, gunData.views.size());
  }

  @Test
  public void testAddViewsEmptyList() {
    GunEnemy gunData = newGunEnemy();
    gunData.addViews(new ArrayList<KnnView<TimestampedFiringAngle>>());
    assertTrue(gunData.views.isEmpty());
  }

  @Test
  public void testGetBotDistanceSq() {
    GunEnemy gunData = newGunEnemy();
    gunData.setBotDistanceSq("Shadow", 100);
    assertEquals(100, gunData.getBotDistanceSq("Shadow"), 0.01);
  }

  @Test
  public void testGetBotDistanceSqNaN() {
    GunEnemy gunData = newGunEnemy();
    assertEquals(Double.NaN, gunData.getBotDistanceSq("Shadow"), 0.01);
  }

  @Test
  public void testClearDistancesSq() {
    GunEnemy gunData = newGunEnemy();
    gunData.setBotDistanceSq("Shadow", 100);
    gunData.clearDistancesSq();
    assertEquals(Double.NaN, gunData.getBotDistanceSq("Shadow"), 0.01);
  }

  @Test
  public void testRemoveDistanceSq() {
    GunEnemy gunData = newGunEnemy();
    gunData.setBotDistanceSq("Shadow", 100);
    gunData.removeDistanceSq("Shadow");
    assertEquals(Double.NaN, gunData.getBotDistanceSq("Shadow"), 0.01);
  }

  @Test
  public void testRemoveDistanceSqAnotherBot() {
    GunEnemy gunData = newGunEnemy();
    gunData.setBotDistanceSq("Shadow", 100);
    gunData.setBotDistanceSq("Portia", 200);
    gunData.removeDistanceSq("Portia");
    assertEquals(100, gunData.getBotDistanceSq("Shadow"), 0.01);
  }

  @Test
  public void testInitRound() {
    GunEnemy gunData = newGunEnemy();
    gunData.energy = 10;
    gunData.alive = false;
    gunData.setRobotState(RobotState.newBuilder()
        .setLocation(newLocation())
        .setTime(1)
        .build());
    gunData.initRound();
    assertEquals(100, gunData.energy, 0.01);
    assertTrue(gunData.alive);
    assertTrue(gunData.stateLog.size() == 0);
  }

  @Test
  public void testProcessBulletHit() {
    testProcessBulletHit(521, 1);
  }

  @Test
  public void testProcessBulletHitNoWaveFound() {
    testProcessBulletHit(420, 0);
  }
  
  @SuppressWarnings("unchecked")
  private void testProcessBulletHit(double y, int hitSize) {
    GunEnemy gunData = newGunEnemy();
    gunData.addViews(Arrays.asList(_mainView, _asView));
    Wave w = gunData.newGunWave(new Point2D.Double(400, 400),
        new Point2D.Double(400, 521), 1, 100, 84, 3.0, 100, 0.0, 1, 0, 50, 100,
        200, false);
    w.firingWave = true;
    Bullet hitBullet = _eventHelper.newBullet(
        Math.PI, 400, y, 3.0, "Diamond", "Shadow", true, 1);
    gunData.processBulletHit(hitBullet, 111, true, true);
    assertEquals(0, _mainView.size());
    assertEquals(hitSize, _asView.size());
  }

  // TODO: test processBulletHit logWave == false doesn't log it

  @Test
  public void testExecuteCallsWaveManagerCheckCurrentWaves() {
    WaveManager waveManager = mock(WaveManager.class);
    GunEnemy gunData = newGunEnemy("Shadow", waveManager);
    gunData.execute(50L, 38L, 3.0, 0.1, newLocation(), true, 1, null, false);
    verify(waveManager).checkCurrentWaves(
        eq(50L), (WaveManager.CurrentWaveListener) any());
  }

  @Test
  public void testExecuteCallsWaveManagerCheckActiveWaves() {
    WaveManager waveManager = mock(WaveManager.class);
    GunEnemy gunData = newGunEnemy("Shadow", waveManager);
    gunData.execute(50L, 38L, 3.0, 0.1, newLocation(), true, 1, null, false);
    verify(waveManager).checkActiveWaves(
        eq(50L), (RobotState) any(), (WaveManager.WaveBreakListener) any());
  }

  @Test
  public void testUpdateWave() {
    GunEnemy gunData = newGunEnemy();
    Wave w = gunData.newGunWave(newLocation(), newLocation(),
        1, 100, 87, 3.0, 100.0, 0.1, 1, 0, 50, 100, 200, false);
    GunDataListener mockListener = mock(GunDataListener.class);
    gunData.updateWave(
        w, newLocation(), 3.0, 87, 0.0, true, Arrays.asList(mockListener));
  
    assertEquals(0.0, w.gunHeat, 0.01);
    assertEquals(87, w.lastBulletFiredTime);
    assertEquals(3.0, w.bulletPower(), 0.01);
  }
  
  @Test
  public void testUpdateWaveBulletPowerChanged() {
    GunEnemy gunData = newGunEnemy();
    Wave w = gunData.newGunWave(newLocation(), newLocation(),
        1, 100, 87, 2.9, 100.0, 0.1, 1, 0, 50, 100, 200, false);
    GunDataListener mockListener = mock(GunDataListener.class);
    gunData.updateWave(
        w, newLocation(), 3.0, 87, 0.0, true, Arrays.asList(mockListener));

    assertEquals(0.0, w.gunHeat, 0.01);
    assertEquals(87, w.lastBulletFiredTime);
    assertEquals(3.0, w.bulletPower(), 0.01);
  }

  @Test
  public void testUpdateWaveFireTimeWave() {
    GunEnemy gunData = newGunEnemy();
    Wave w = gunData.newGunWave(newLocation(), newLocation(),
        1, 100, 100, 3.0, 100.0, 0.1, 1, 0, 50, 100, 200, false);
    GunDataListener mockListener = mock(GunDataListener.class);
    Point2D.Double myLocation = newLocation();
    gunData.setRobotState(RobotState.newBuilder()
        .setLocation(newLocation())
        .setTime(100L)
        .build());
    gunData.updateWave(
        w, myLocation, 3.0, 100, 0.0, true, Arrays.asList(mockListener));

    assertEquals(0.0, w.gunHeat, 0.01);
    assertEquals(100, w.lastBulletFiredTime);
    assertEquals(myLocation, w.sourceLocation);
    assertEquals(gunData.lastScanState.location, w.targetLocation);
  }

  @Test
  public void testUpdateWavesAltWave() {
    GunEnemy gunData = newGunEnemy();
    Wave w = gunData.newGunWave(newLocation(), newLocation(),
        1, 100, 87, 1.9, 100.0, 0.1, 1, 0, 50, 100, 200, true);
    GunDataListener mockListener = mock(GunDataListener.class);
    gunData.updateWave(
        w, newLocation(), 3.0, 100, 0.0, true, Arrays.asList(mockListener));

    assertEquals(0.1, w.gunHeat, 0.01);
    assertEquals(87, w.lastBulletFiredTime);
    assertEquals(1.9, w.bulletPower(), 0.01);
  }

  // TODO: fix these tests, either proper wave break states or mock the wave

//  @Test
//  @SuppressWarnings("unchecked")
//  public void testProcessWaveBreak1v1FiringWave() {
//    GunEnemy gunData = newGunEnemy();
//    gunData.addViews(Arrays.asList(_mainView, _asView));
//    Wave w = gunData.newGunWave(newLocation(), newLocation(),
//        1, 100, 87, 1.0, 100.0, 0.0, 1, 0, 50, 100, 200, false);
//    GunDataListener mockListener = mock(GunDataListener.class);
//    w.firingWave = true;
//
//    gunData.processWaveBreak(w, newWaveBreakStates(), 100, 1, mockListener);
//    assertEquals(1, _mainView.size());
//    verify(mockListener).on1v1FiringWaveBreak(eq(w), anyDouble(), anyDouble());
//  }
//
//  @Test
//  @SuppressWarnings("unchecked")
//  public void testProcessWaveBreakNonFiringWave() {
//    GunEnemy gunData = newGunEnemy();
//    gunData.addViews(Arrays.asList(_mainView, _asView));
//    Wave w = gunData.newGunWave(newLocation(), newLocation(),
//        1, 100, 87, 1.0, 100.0, 0.0, 1, 0, 50, 100, 200, false);
//    GunDataListener mockListener = mock(GunDataListener.class);
//    assertFalse(w.firingWave);
//
//    gunData.processWaveBreak(w, newWaveBreakStates(), 100, 1, mockListener);
//    assertEquals(1, _mainView.size());
//    verify(mockListener, never())
//        .on1v1FiringWaveBreak(eq(w), anyDouble(), anyDouble());
//  }
//
//  @Test
//  @SuppressWarnings("unchecked")
//  public void testProcessWaveBreakAltWave() {
//    GunEnemy gunData = newGunEnemy();
//    gunData.addViews(Arrays.asList(_mainView, _asView));
//    Wave w = gunData.newGunWave(newLocation(), newLocation(),
//        1, 100, 87, 1.0, 100.0, 0.0, 1, 0, 50, 100, 200, true);
//    GunDataListener mockListener = mock(GunDataListener.class);
//    w.firingWave = true;
//
//    gunData.processWaveBreak(w, newWaveBreakStates(), 100, 1, mockListener);
//    assertEquals(1, _mainView.size());
//    verify(mockListener, never())
//        .on1v1FiringWaveBreak(eq(w), anyDouble(), anyDouble());
//  }
//
//  @Test
//  @SuppressWarnings("unchecked")
//  public void testProcessWaveBreakMelee() {
//    GunEnemy gunData = newGunEnemy();
//    gunData.addViews(Arrays.asList(_mainView, _asView));
//    Wave w = gunData.newGunWave(newLocation(), newLocation(),
//        1, 100, 87, 1.0, 100.0, 0.0, 3, 0, 50, 100, 200, false);
//    GunDataListener mockListener = mock(GunDataListener.class);
//    w.firingWave = true;
//
//    gunData.processWaveBreak(w, newWaveBreakStates(), 100, 1, mockListener);
//    assertEquals(1, _mainView.size());
//    verify(mockListener, never())
//        .on1v1FiringWaveBreak(eq(w), anyDouble(), anyDouble());
//  }

  private List<RobotState> newWaveBreakStates() {
    List<RobotState> waveBreakStates = new ArrayList<RobotState>();
    Point2D.Double location = newLocation();
    waveBreakStates.add(RobotState.newBuilder()
        .setLocation(location)
        .setTime(120)
        .build());
    Point2D.Double location2 =
        DiaUtils.project(location, Math.random() * 2 * Math.PI, 8);
    waveBreakStates.add(RobotState.newBuilder()
        .setLocation(location2)
        .setTime(121)
        .build());
    Point2D.Double location3 =
        DiaUtils.project(location2, Math.random() * 2 * Math.PI, 8);
    waveBreakStates.add(RobotState.newBuilder()
        .setLocation(location3)
        .setTime(122)
        .build());
    return waveBreakStates;
  }

  @Test
  public void testLogWave() {
    // TODO: write this test, pretty trivial tho
  }

  @Test
  public void testMarkFiringWaves() {
    GunEnemy gunData = newGunEnemy();
    Wave w = gunData.newGunWave(newLocation(100, 100),
        gunData.lastScanState.location, 1, 100, 87, 1.0, 100.0, 0.0, 1, 0, 50,
        100, 200, false);
    GunDataListener mockListener = mock(GunDataListener.class);
    gunData.markFiringWaves(100, true, Arrays.asList(mockListener));
    assertTrue(w.firingWave);
    verify(mockListener).onMarkFiringWave(w);
  }

  @Test
  public void testMarkFiringWavesNonFiringWave() {
    GunEnemy gunData = newGunEnemy();
    Wave w = gunData.newGunWave(newLocation(100, 100),
        gunData.lastScanState.location, 1, 100, 87, 1.0, 100.0, 0.0, 1, 0, 50,
        100, 200, false);
    GunDataListener mockListener = mock(GunDataListener.class);
    gunData.markFiringWaves(101, true, Arrays.asList(mockListener));
    assertFalse(w.firingWave);
    verify(mockListener, never()).onMarkFiringWave((Wave) any());
  }

  @Test
  public void testMarkFiringWavesFiringAndNonFiringWave() {
    GunEnemy gunData = newGunEnemy();
    Wave w1 = gunData.newGunWave(newLocation(100, 100),
        gunData.lastScanState.location, 1, 100, 87, 1.0, 100.0, 0.0, 1, 0, 50,
        100, 200, false);
    Wave w2 = gunData.newGunWave(newLocation(100, 100),
        gunData.lastScanState.location, 1, 115, 87, 1.0, 100.0, 0.0, 1, 0, 50,
        100, 200, false);
    GunDataListener mockListener = mock(GunDataListener.class);
    gunData.markFiringWaves(115, true, Arrays.asList(mockListener));
    assertFalse(w1.firingWave);
    assertTrue(w2.firingWave);
    verify(mockListener).onMarkFiringWave(w2);
  }

  @Test
  public void testMarkFiringWavesNoWaves() {
    GunEnemy gunData = newGunEnemy();
    GunDataListener mockListener = mock(GunDataListener.class);
    gunData.markFiringWaves(115, true, Arrays.asList(mockListener));
    verify(mockListener, never()).onMarkFiringWave((Wave) any());
  }

  @Test
  public void testNewGunWave() {
    WaveManager waveManager = mock(WaveManager.class);
    GunEnemy gunData = newGunEnemy("Shadow", waveManager);
    Point2D.Double myLocation = newLocation(100, 100);
    Wave w = gunData.newGunWave(myLocation, gunData.lastScanState.location,
        1, 100, 87, 1.9, 100.0, 0.1, 4, 0.22, 54, 104, 204, false);
    assertEquals("Shadow", w.botName);
    assertEquals(myLocation, w.sourceLocation);
    assertEquals(gunData.lastScanState.location, w.targetLocation);
    assertEquals(1, w.fireRound);
    assertEquals(100, w.fireTime);
    assertEquals(87, w.lastBulletFiredTime);
    assertEquals(1.9, w.bulletPower(), 0.01);
    assertEquals(100.0, w.sourceEnergy, 0.01);
    assertEquals(0.1, w.gunHeat, 0.01);
    assertEquals(4, w.enemiesAlive);
    assertEquals(0.22, w.targetAccel, 0.01);
    assertEquals(54, w.targetDl8t, 0.01);
    assertEquals(104, w.targetDl20t, 0.01);
    assertEquals(204, w.targetDl40t, 0.01);
    assertEquals(false, w.altWave);
    verify(waveManager).addWave(w);
  }

  @Test
  public void testNewGunWaveAltWave() {
    GunEnemy gunData = newGunEnemy("Shadow");
    java.awt.geom.Point2D.Double myLocation = newLocation(100, 100);
    Wave w = gunData.newGunWave(myLocation, gunData.lastScanState.location,
        1, 100, 87, 1.9, 100.0, 0.1, 4, 0.22, 54, 104, 204, true);
    assertEquals(true, w.altWave);
  }

  @Test
  public void testNewGunWaveZeroEnergy() {
    WaveManager waveManager = mock(WaveManager.class);
    GunEnemy gunData = newGunEnemy("Shadow", waveManager);
    java.awt.geom.Point2D.Double myLocation = newLocation(100, 100);
    gunData.newGunWave(myLocation, gunData.lastScanState.location,
        1, 100, 87, 1.9, 0.0, 0.1, 4, 0.22, 54, 104, 204, true);
    verify(waveManager, never()).addWave((Wave) any());
  }

  private GunEnemy newGunEnemy() {
    return newGunEnemy("Shadow");
  }

  private GunEnemy newGunEnemy(String botName) {
    BattleField battleField = new BattleField(800, 600);
    return new GunEnemy(botName, 400, 100, _enemyLocation, 1, 50, 8.0, 0, 0,
        battleField, new MovementPredictor(battleField),
        new ArrayList<RoboGraphic>());
  }

  private GunEnemy newGunEnemy(String botName, WaveManager waveManager) {
    BattleField battleField = new BattleField(800, 600);
    return new GunEnemy(botName, 400, 100, _enemyLocation, 1, 50, 8.0, 0, 0,
        battleField, new MovementPredictor(battleField),
        new ArrayList<RoboGraphic>(), waveManager);
  }

  private Point2D.Double newLocation() {
    return newLocation(Math.random() * 800, Math.random() * 600);
  }

  private Point2D.Double newLocation(double x, double y) {
    return new Point2D.Double(x, y);
  }
}
