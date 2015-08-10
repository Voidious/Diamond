package voidious.move;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.geom.Point2D;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import robocode.Bullet;
import voidious.Diamond;
import voidious.gfx.RoboGraphic;
import voidious.gun.FireListener.FiredBullet;
import voidious.move.SurfMover.SurfOption;
import voidious.test.RobocodeEventTestHelper;
import voidious.utils.BattleField;
import voidious.utils.DiaUtils;
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

public class SurfMoverTest {
  @Mock
  private Diamond diamond;

  private RobocodeEventTestHelper _eventHelper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    _eventHelper = new RobocodeEventTestHelper();
  }

  @Test
  public void testNewSurfMover() {
    newSurfMover();
  }

  @Test
  public void testInitRound() {
    // TODO: test for side effects of a statel lastSurfDestination somehow
  }

  @Test
  public void testMove() {
    SurfMover mover = newSurfMover();
    RobotState myRobotState = RobotState.newBuilder()
        .setLocation(newLocation())
        .setHeading(0)
        .setVelocity(8.0)
        .setTime(10L)
        .build();

    mover.move(myRobotState, newDuelEnemy(), 2, false);
    verifySomeRobotMovement();
  }

  @Test
  public void testOrbit() {
    SurfMover mover = newSurfMover();
    mover.orbit(newLocation(400, 300), newDuelEnemy(newLocation(400, 500)));

    verifySomeRobotMovement();
  }

  @Test
  public void testOrbitAwayFromWestWall() {
    SurfMover mover = newSurfMover();
    when(diamond.getHeadingRadians()).thenReturn(Math.PI / 2);
    mover.orbit(newLocation(100, 200), newDuelEnemy(newLocation(100, 550)));

    verify(diamond).setMaxVelocity(8.0);
    verify(diamond).setAhead(anyDouble());
    verify(diamond).setTurnRightRadians(anyDouble());
  }

  @Test
  public void testOrbitAwayFromEastWall() {
    SurfMover mover = newSurfMover();
    when(diamond.getHeadingRadians()).thenReturn(Math.PI / 2);
    mover.orbit(newLocation(700, 200), newDuelEnemy(newLocation(700, 550)));

    verify(diamond).setMaxVelocity(8.0);
    verify(diamond).setBack(anyDouble());
    verify(diamond).setTurnLeftRadians(anyDouble());
  }

  @Test
  public void testOrbitAwayFromNorthWall() {
    SurfMover mover = newSurfMover();
    when(diamond.getHeadingRadians()).thenReturn(0D);
    mover.orbit(newLocation(300, 550), newDuelEnemy(newLocation(700, 550)));

    verify(diamond).setMaxVelocity(8.0);
    verify(diamond).setBack(anyDouble());
    verify(diamond).setTurnRightRadians(anyDouble());
  }

  @Test
  public void testOrbitAwayFromSouthWall() {
    SurfMover mover = newSurfMover();
    when(diamond.getHeadingRadians()).thenReturn(0D);
    mover.orbit(newLocation(300, 50), newDuelEnemy(newLocation(700, 50)));

    verify(diamond).setMaxVelocity(8.0);
    verify(diamond).setAhead(anyDouble());
    verify(diamond).setTurnLeftRadians(anyDouble());
  }

  @Test
  public void testOrbitAwayFromCorner() {
    SurfMover mover = newSurfMover();
    when(diamond.getHeadingRadians()).thenReturn(0D);
    mover.orbit(newLocation(50, 50), newDuelEnemy(newLocation(300, 100)));

    verify(diamond).setMaxVelocity(8.0);
    verify(diamond).setAhead(anyDouble());
    verify(diamond).setTurnLeftRadians(anyDouble());
  }

  @Test
  public void testSurf() {
    SurfMover mover = newSurfMover();
    MoveEnemy duelEnemy = newDuelEnemy();

    Point2D.Double sourceLocation1 = newLocation(700, 500);
    Point2D.Double targetLocation1 = newLocation(200, 150);
    Wave wave1 = duelEnemy.newMoveWave(sourceLocation1, targetLocation1,
        DiaUtils.absoluteBearing(sourceLocation1, targetLocation1), 1, 50L,
        1.95, 100.0, Math.PI / 2, 8.0, 1, 0, 50, 150, 250, 9L, 3L);
    wave1.firingWave = true;

    Point2D.Double sourceLocation2 = newLocation(650, 530);
    Point2D.Double targetLocation2 = newLocation(240, 130);
    Wave wave2 = duelEnemy.newMoveWave(sourceLocation2, targetLocation2,
        DiaUtils.absoluteBearing(sourceLocation2, targetLocation2), 1, 65L,
        1.95, 100.0, Math.PI / 2, 8.0, 1, 0, 50, 150, 250, 9L, 3L);
    wave2.firingWave = true;

    RobotState myRobotState = RobotState.newBuilder()
        .setLocation(targetLocation2)
        .setHeading(Math.PI / 2)
        .setVelocity(8.0)
        .setTime(67L)
        .build();
    mover.surf(myRobotState, duelEnemy, wave1, 2, false);
    verifySomeRobotMovement();
  }

  private void verifySomeRobotMovement() {
    verify(diamond, Mockito.atMost(1)).setAhead(anyDouble());
    verify(diamond, Mockito.atMost(1)).setBack(anyDouble());
    verify(diamond, Mockito.atMost(1)).setTurnRightRadians(anyDouble());
    verify(diamond, Mockito.atMost(1)).setTurnLeftRadians(anyDouble());
    verify(diamond).setMaxVelocity(anyDouble());
  }

  @Test
  public void testGetSortedSurfOptions() {
    SurfMover mover = newSurfMover();
    Map<SurfOption, Double> surfOptionDangers = mover.getSurfOptionDangers();
    surfOptionDangers.put(SurfOption.CLOCKWISE, 3.0);
    surfOptionDangers.put(SurfOption.COUNTER_CLOCKWISE, 1.0);
    surfOptionDangers.put(SurfOption.STOP, 2.0);

    List<SurfOption> sortedSurfOptions = mover.getSortedSurfOptions();
    assertEquals(SurfOption.COUNTER_CLOCKWISE, sortedSurfOptions.get(0));
    assertEquals(SurfOption.STOP, sortedSurfOptions.get(1));
    assertEquals(SurfOption.CLOCKWISE, sortedSurfOptions.get(2));
  }

  @Test
  public void testGetSortedSurfOptions2() {
    SurfMover mover = newSurfMover();
    Map<SurfOption, Double> surfOptionDangers = mover.getSurfOptionDangers();
    surfOptionDangers.put(SurfOption.CLOCKWISE, 1.0);
    surfOptionDangers.put(SurfOption.COUNTER_CLOCKWISE, 2.0);
    surfOptionDangers.put(SurfOption.STOP, 3.0);

    List<SurfOption> sortedSurfOptions = mover.getSortedSurfOptions();
    assertEquals(SurfOption.CLOCKWISE, sortedSurfOptions.get(0));
    assertEquals(SurfOption.COUNTER_CLOCKWISE, sortedSurfOptions.get(1));
    assertEquals(SurfOption.STOP, sortedSurfOptions.get(2));
  }

  @Test
  public void testGetSortedSurfOptions3() {
    SurfMover mover = newSurfMover();
    Map<SurfOption, Double> surfOptionDangers = mover.getSurfOptionDangers();
    surfOptionDangers.put(SurfOption.CLOCKWISE, 3.0);
    surfOptionDangers.put(SurfOption.COUNTER_CLOCKWISE, 2.0);
    surfOptionDangers.put(SurfOption.STOP, 1.0);

    List<SurfOption> sortedSurfOptions = mover.getSortedSurfOptions();
    assertEquals(SurfOption.STOP, sortedSurfOptions.get(0));
    assertEquals(SurfOption.COUNTER_CLOCKWISE, sortedSurfOptions.get(1));
    assertEquals(SurfOption.CLOCKWISE, sortedSurfOptions.get(2));
  }

  @Test
  public void testCheckDangerBulletHit() {
    SurfMover mover = newSurfMover();
    MoveEnemy duelEnemy = newDuelEnemy();
    setupHitWave(duelEnemy);
    Bullet hitBullet = _eventHelper.newBullet(
        Math.PI / 8, 559.8, 476.02, 3.0, "Shadow", "Diamond", true, 22);
    duelEnemy.processBullet(hitBullet, 1, 68L, false);
    setupSurfWave(duelEnemy);

    RobotState startState = RobotState.newBuilder()
        .setLocation(newLocation(410, 500))
        .setHeading(Math.PI / 2)
        .setVelocity(8.0)
        .setTime(210L)
        .build();
    when(diamond.getTime()).thenReturn(210L);
    double clockwiseDanger = mover.checkDanger(newRobotState(400, 500), duelEnemy,
        startState, SurfOption.CLOCKWISE, true, Wave.FIRST_WAVE, 1,
        Double.POSITIVE_INFINITY, new RobotStateLog());
    double counterDanger = mover.checkDanger(newRobotState(400, 500), duelEnemy,
        startState, SurfOption.COUNTER_CLOCKWISE, true, Wave.FIRST_WAVE, 1,
        Double.POSITIVE_INFINITY, new RobotStateLog());
    assertTrue(clockwiseDanger > counterDanger * 10);
  }

  @Test
  public void testCheckDangerFlattenerVisit() {
    SurfMover mover = newSurfMover();
    MoveEnemy duelEnemy = newDuelEnemy();
    setupHitWave(duelEnemy);
    duelEnemy.execute1v1(1, 61L, newLocation(550, 500));
    duelEnemy.execute1v1(1, 67L, newLocation(600, 500));
    duelEnemy.execute1v1(1, 85L, newLocation(700, 480));
    duelEnemy.execute1v1(1, 185L, newLocation(500, 520));
    setupSurfWave(duelEnemy);
    // TODO: rig hit % high enough to enable flattener

    RobotState startState = RobotState.newBuilder()
        .setLocation(newLocation(410, 500))
        .setHeading(Math.PI / 2)
        .setVelocity(8.0)
        .setTime(210L)
        .build();
    when(diamond.getTime()).thenReturn(210L);
    double clockwiseDanger = mover.checkDanger(newRobotState(400, 500), duelEnemy,
        startState, SurfOption.CLOCKWISE, true, Wave.FIRST_WAVE, 1,
        Double.POSITIVE_INFINITY, new RobotStateLog());
    double counterDanger = mover.checkDanger(newRobotState(400, 500), duelEnemy,
        startState, SurfOption.COUNTER_CLOCKWISE, true, Wave.FIRST_WAVE, 1,
        Double.POSITIVE_INFINITY, new RobotStateLog());
    assertTrue(clockwiseDanger > counterDanger * 10);
  }

  private Wave setupHitWave(MoveEnemy duelEnemy) {
    Wave w = duelEnemy.newMoveWave(newLocation(400, 100), newLocation(400, 500),
        0, 1, 31L, 3.0, 100, Math.PI * .6, 5.0, 1, 1, 50, 150, 250, 6, 0);
    duelEnemy.setRobotState(RobotState.newBuilder()
        .setLocation(newLocation(398, 95))
        .setHeading(0)
        .setVelocity(3.0)
        .setTime(30L)
        .build());
    duelEnemy.setRobotState(RobotState.newBuilder()
        .setLocation(newLocation(400, 100))
        .setHeading(0)
        .setVelocity(3.0)
        .setTime(31L)
        .build());
    RobotStateLog myStateLog = new RobotStateLog();
    myStateLog.addState(RobotState.newBuilder()
        .setLocation(newLocation(395, 498))
        .setHeading(0)
        .setVelocity(3.0)
        .setTime(30L)
        .build());
    duelEnemy.updateFiringWave(
        32L, 3.0, myStateLog, new ArrayList<FiredBullet>());
    return w;
  }

  private Wave setupSurfWave(MoveEnemy duelEnemy) {
    RobotStateLog myStateLog = new RobotStateLog();
    myStateLog.addState(RobotState.newBuilder()
        .setLocation(newLocation(395, 498))
        .setHeading(0)
        .setVelocity(3.0)
        .setTime(207L)
        .build());
    duelEnemy.setRobotState(RobotState.newBuilder()
        .setLocation(newLocation(398, 95))
        .setHeading(0)
        .setVelocity(3.0)
        .setTime(207L)
        .build());
    duelEnemy.setRobotState(RobotState.newBuilder()
        .setLocation(newLocation(400, 100))
        .setHeading(0)
        .setVelocity(3.0)
        .setTime(208L)
        .build());
    Wave w = duelEnemy.newMoveWave(newLocation(400, 101), newLocation(396, 500),
        0, 1, 208L, 3.0, 100, Math.PI * .59, 5.0, 1, 1, 49, 152, 239, 6, 0);
    duelEnemy.updateFiringWave(
        209L, 3.0, myStateLog, new ArrayList<FiredBullet>());
    return w;
  }

  @Test
  public void testReplaySurfStates() {
    SurfMover mover = newSurfMover();
    MoveEnemy duelEnemy = newDuelEnemy();
    Point2D.Double sourceLocation = newLocation(150, 300);
    Point2D.Double targetLocation = newLocation(600, 359);
    Wave surfWave = duelEnemy.newMoveWave(sourceLocation, targetLocation,
        DiaUtils.absoluteBearing(sourceLocation, targetLocation), 1, 100L, 2.0,
        100.0, 0, -8.0, -1, 0, 60, 150, 220, 15L, 5L);
    RobotState startState = RobotState.newBuilder()
        .setLocation(newLocation(580, 200))
        .setHeading(Math.PI * .03)
        .setVelocity(-8)
        .setTime(140L)
        .build();
    RobotStateLog stateLog = new RobotStateLog();
    for (long time = 139L; time > 126L; time--) {
      RobotState oldState = RobotState.newBuilder()
          .setLocation(DiaUtils.project(startState.location,
              startState.heading + Math.PI,
              -startState.velocity * (140L - time)))
          .setHeading(startState.heading)
          .setVelocity(startState.velocity)
          .setTime(time)
          .build();
      stateLog.addState(oldState);
    }
    List<RobotState> dangerStates =
        mover.replaySurfStates(surfWave, stateLog, startState);
    assertFalse(dangerStates.isEmpty());
    assertTrue(dangerStates.size() <= 4);
  }

  // TODO: fix these tests
//  @Test
//  public void testPredictSurfLocation() {
//    SurfMover mover = newSurfMover();
//    Wave surfWave = newSurfWave();
//    RobotState surfState = RobotState.newBuilder()
//        .setLocation(newLocation(440, 350))
//        .setHeading(Math.PI / 2)
//        .setVelocity(8.0)
//        .setTime(105L)
//        .build();
//    RobotState nextSurfLocation =
//        mover.predictSurfLocation(surfWave, surfState, true, 8);
//    assertTrue(nextSurfLocation.location.distance(newLocation(448, 351)) < 1);
//  }
//
//  @Test
//  public void testPredictSurfLocationStop() {
//    SurfMover mover = newSurfMover();
//    Wave surfWave = newSurfWave();
//    RobotState surfState = RobotState.newBuilder()
//        .setLocation(newLocation(440, 350))
//        .setHeading(Math.PI / 2)
//        .setVelocity(8.0)
//        .setTime(105L)
//        .build();
//    RobotState nextSurfLocation =
//        mover.predictSurfLocation(surfWave, surfState, true, 0);
//    assertTrue(nextSurfLocation.location.distance(newLocation(446, 351)) < 1);
//  }
//
//  @Test
//  public void testPredictSurfLocationFromStop() {
//    SurfMover mover = newSurfMover();
//    Wave surfWave = newSurfWave();
//    RobotState surfState = RobotState.newBuilder()
//        .setLocation(newLocation(440, 350))
//        .setHeading(Math.PI / 2)
//        .setVelocity(0.0)
//        .setTime(110L)
//        .build();
//    RobotState nextSurfLocation =
//        mover.predictSurfLocation(surfWave, surfState, true, 8);
//    assertTrue(nextSurfLocation.location.distance(newLocation(441, 350)) < 0.5);
//  }
//
//  @Test
//  public void testPredictSurfLocationStopFromStop() {
//    SurfMover mover = newSurfMover();
//    Wave surfWave = newSurfWave();
//    RobotState surfState = RobotState.newBuilder()
//        .setLocation(newLocation(440, 350))
//        .setHeading(Math.PI / 2)
//        .setVelocity(0.0)
//        .setTime(110L)
//        .build();
//    RobotState nextSurfLocation =
//        mover.predictSurfLocation(surfWave, surfState, true, 0);
//    assertTrue(
//        nextSurfLocation.location.distance(newLocation(440, 350)) < 0.001);
//  }
//
//  @Test
//  public void testPredictSurfLocationNearWall() {
//    SurfMover mover = newSurfMover();
//    Wave surfWave = newSurfWave();
//    RobotState surfState = RobotState.newBuilder()
//        .setLocation(newLocation(750, 350))
//        .setHeading(Math.PI / 2)
//        .setVelocity(8.0)
//        .setTime(105L)
//        .build();
//    RobotState nextSurfLocation =
//        mover.predictSurfLocation(surfWave, surfState, true, 8);
//    assertTrue(nextSurfLocation.location.distance(newLocation(758, 349)) < 1);
//  }

  @Test
  public void testDistancingDangerFirstWaveFar() {
    SurfMover mover = newSurfMover();
    MoveEnemy duelEnemy = newDuelEnemy(newLocation(400, 100));
    Wave surfWave = newSurfWave(duelEnemy);
    double danger = mover.distancingDanger(newLocation(400, 500),
        newLocation(500, 500), duelEnemy.lastScanState.location);
    assertTrue(danger < 1);
  }

  @Test
  public void testDistancingDangerFirstWaveClose() {
    SurfMover mover = newSurfMover();
    MoveEnemy duelEnemy = newDuelEnemy(newLocation(400, 100));
    Wave surfWave = newSurfWave(duelEnemy);
    double danger = mover.distancingDanger(newLocation(400, 500),
        newLocation(400, 200), duelEnemy.lastScanState.location);
    assertTrue(danger > 4);
  }


  @Test
  public void testDistancingDangerFirstWaveEnemyCloser() {
    SurfMover mover = newSurfMover();
    MoveEnemy duelEnemy = newDuelEnemy(newLocation(501, 501));
    Wave surfWave = newSurfWave(duelEnemy);
    double danger = mover.distancingDanger(newLocation(400, 500), 
        newLocation(500, 500), duelEnemy.lastScanState.location);
    assertTrue(danger > 10);
  }

  @Test
  public void testDistancingDangerSecondWave() {
    SurfMover mover = newSurfMover();
    MoveEnemy duelEnemy = newDuelEnemy();
    Wave surfWave = newSurfWave(duelEnemy);
    double danger = mover.distancingDanger(newLocation(400, 500),
        newLocation(400, 350), duelEnemy.lastScanState.location);
    assertEquals(1, danger, 0.001);
  }

  private Wave newSurfWave() {
    return newSurfWave(newDuelEnemy());
  }

  private Wave newSurfWave(MoveEnemy duelEnemy) {
    Point2D.Double sourceLocation = newLocation(400, 100);
    Point2D.Double targetLocation = newLocation(400, 350);
    return duelEnemy.newMoveWave(sourceLocation, targetLocation,
        DiaUtils.absoluteBearing(sourceLocation, targetLocation), 1, 100L, 2.0,
        100.0, Math.PI / 2, 8.0, 1, 0, 60, 150, 220, 15L, 5L);
  }

  @Test
  public void testGetDangerScore() {
    SurfMover mover = newSurfMover();
    MoveEnemy duelEnemy = newDuelEnemy();
    Wave hitWave = setupHitWave(duelEnemy);
    Point2D.Double hitLocation =
        DiaUtils.project(newLocation(400, 100), Math.PI / 9, 11 * 37);
    Bullet hitBullet = _eventHelper.newBullet(Math.PI / 9,
        hitLocation.x, hitLocation.y, 3.0, "Shadow", "Diamond", true, 22);
    duelEnemy.processBullet(hitBullet, 1, 68L, false);
    Wave surfWave = setupSurfWave(duelEnemy);
    
    double angle = (Math.PI / 9) - hitWave.absBearing + surfWave.absBearing;
    Wave.Intersection intersection = new Wave.Intersection(angle, 0.05);
    Wave.Intersection intersection2 = new Wave.Intersection(angle + 0.01, 0.05);
    Wave.Intersection intersection3 = new Wave.Intersection(angle - 0.01, 0.05);
    double d1 = mover.getDangerScore(
        duelEnemy, surfWave, intersection, Wave.FIRST_WAVE);
    double d2 = mover.getDangerScore(
        duelEnemy, surfWave, intersection2, Wave.FIRST_WAVE);
    double d3 = mover.getDangerScore(
        duelEnemy, surfWave, intersection3, Wave.FIRST_WAVE);
    assertTrue(d1 > d2);
    assertTrue(d1 > d3);
  }

  private SurfMover newSurfMover() {
    return new SurfMover(diamond, new BattleField(800, 600),
        new ArrayList<RoboGraphic>(), new ByteArrayOutputStream());
  }

  private Point2D.Double newLocation() {
    return newLocation(Math.random() * 800, Math.random() * 600);
  }

  private Point2D.Double newLocation(double x, double y) {
    return new Point2D.Double(x, y);
  }

  private RobotState newRobotState(double x, double y) {
    return RobotState.newBuilder().setLocation(newLocation(x, y)).build();
  }

  private MoveEnemy newDuelEnemy() {
    return newDuelEnemy(newLocation());
  }

  private MoveEnemy newDuelEnemy(Point2D.Double enemyLocation) {
    return new MoveEnemy("Shadow", 400, 100, enemyLocation, 0, 0, Math.PI, 1,
        10L, new ArrayList<RoboGraphic>(), new BattleField(800, 600),
        new MovementPredictor(new BattleField(800, 600)),
        new PrintStream(new ByteArrayOutputStream()));
  }
}
