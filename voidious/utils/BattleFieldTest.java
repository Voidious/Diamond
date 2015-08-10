package voidious.utils;

import static org.junit.Assert.*;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

import org.junit.Test;

import robocode.util.Utils;

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

public class BattleFieldTest {

  @Test
  public void testBattleField() {
    BattleField battleField = newBattleField(800, 600);
    assertEquals(800, battleField.width, 0.0001);
    assertEquals(600, battleField.height, 0.0001);
  }

  @Test
  public void testTranslateToFieldEast() {
    BattleField battleField = newBattleField(800, 600);
    Double p = battleField.translateToField(newLocation(835, 300));
    assertEquals(782, p.x, 0.0001);
    assertEquals(300, p.y, 0.0001);
  }

  @Test
  public void testTranslateToFieldWest() {
    BattleField battleField = newBattleField(800, 600);
    Double p = battleField.translateToField(newLocation(-99, 300));
    assertEquals(18, p.x, 0.0001);
    assertEquals(300, p.y, 0.0001);
  }

  @Test
  public void testTranslateToFieldNorth() {
    BattleField battleField = newBattleField(800, 600);
    Double p = battleField.translateToField(newLocation(300, 650));
    assertEquals(300, p.x, 0.0001);
    assertEquals(582, p.y, 0.0001);
  }

  @Test
  public void testTranslateToFieldSouth() {
    BattleField battleField = newBattleField(800, 600);
    Double p = battleField.translateToField(newLocation(300, 9));
    assertEquals(300, p.x, 0.0001);
    assertEquals(18, p.y, 0.0001);
  }

  @Test
  public void testTranslateToFieldNorthEast() {
    BattleField battleField = newBattleField(800, 600);
    Double p = battleField.translateToField(newLocation(850, 650));
    assertEquals(782, p.x, 0.0001);
    assertEquals(582, p.y, 0.0001);
  }

  @Test
  public void testTranslateToFieldSouthEast() {
    BattleField battleField = newBattleField(800, 600);
    Double p = battleField.translateToField(newLocation(850, -828));
    assertEquals(782, p.x, 0.0001);
    assertEquals(18, p.y, 0.0001);
  }

  @Test
  public void testTranslateToFieldSouthWest() {
    BattleField battleField = newBattleField(800, 600);
    Double p = battleField.translateToField(newLocation(-98, -828));
    assertEquals(18, p.x, 0.0001);
    assertEquals(18, p.y, 0.0001);
  }

  @Test
  public void testTranslateToFieldNorthWest() {
    BattleField battleField = newBattleField(800, 600);
    Double p = battleField.translateToField(newLocation(-8828, 599));
    assertEquals(18, p.x, 0.0001);
    assertEquals(582, p.y, 0.0001);
  }

  @Test
  public void testOrbitalWallDistance() {
    BattleField battleField = newBattleField(800, 600);
    double wallDistance = battleField.orbitalWallDistance(
        newLocation(400, 100), newLocation(400, 300), 2.0, 1);
    assertEquals(2.0, wallDistance, 0.0001);
  }

  @Test
  public void testOrbitalWallDistanceReverse() {
    BattleField battleField = newBattleField(800, 600);
    double wallDistance = battleField.orbitalWallDistance(
        newLocation(400, 100), newLocation(400, 300), 2.0, -1);
    assertEquals(2.0, wallDistance, 0.0001);
  }

  @Test
  public void testOrbitalWallDistanceNearWall() {
    BattleField battleField = newBattleField(800, 600);
    double wallDistance1 = battleField.orbitalWallDistance(
        newLocation(400, 100), newLocation(700, 100), 3.0, 1);
    double wallDistance2 = battleField.orbitalWallDistance(
        newLocation(400, 100), newLocation(700, 95), 3.0, 1);
    assertTrue(wallDistance1 < 1.0);
    assertTrue(wallDistance1 > wallDistance2);
  }

  @Test
  public void testOrbitalWallDistanceNearWallReverse() {
    BattleField battleField = newBattleField(800, 600);
    double wallDistance1 = battleField.orbitalWallDistance(
        newLocation(400, 100), newLocation(100, 100), 3.0, -1);
    double wallDistance2 = battleField.orbitalWallDistance(
        newLocation(400, 100), newLocation(100, 95), 3.0, -1);
    assertTrue(wallDistance1 < 1.0);
    assertTrue(wallDistance1 > wallDistance2);
  }

  @Test
  public void testDirectToWallDistanceStraightToWestWall() {
    BattleField battleField = newBattleField(800, 600);
    double wallDistance = battleField.directToWallDistance(
        newLocation(97, 300), 220, 3 * Math.PI / 2, 3.0);
    assertEquals(0.5, wallDistance, 0.0001);
  }

  @Test
  public void testDirectToWallDistanceStraightToNorthWall() {
    BattleField battleField = newBattleField(800, 600);
    double wallDistance = battleField.directToWallDistance(
        newLocation(400, 463), 280, 0, 2.0);
    assertEquals(0.75, wallDistance, 0.0001);
  }

  @Test
  public void testDirectToWallDistanceDiagonalToEastWall() {
    BattleField battleField = newBattleField(800, 600);
    double wallDistance = battleField.directToWallDistance(
        newLocation(622, 300), 280, Math.PI * 0.4, 2.0);
    assertTrue(wallDistance > 1.0 && wallDistance < 2.0);
  }

  @Test
  public void testDirectToWallDistanceStraightToSouthWall() {
    BattleField battleField = newBattleField(800, 600);
    double wallDistance = battleField.directToWallDistance(
        newLocation(400, 500), 140, Math.PI, 2.0);
    assertEquals(2.0, wallDistance, 0.0001);
  }

  @Test
  public void testWallSmoothingEastWall() {
    BattleField battleField = newBattleField(800, 600);
    Double botLocation = newLocation(700, 300);
    double goAngle = Utils.normalAbsoluteAngle(battleField.wallSmoothing(
        botLocation, Math.PI / 2, 1, 120));
    assertTrue(goAngle > Math.PI * 0.6 && goAngle < Math.PI);
    assertTrue(battleField.rectangle.contains(
        DiaUtils.project(botLocation, goAngle, 120)));
  }

  @Test
  public void testWallSmoothingWestWall() {
    BattleField battleField = newBattleField(800, 600);
    Double botLocation = newLocation(100, 300);
    double goAngle = Utils.normalAbsoluteAngle(battleField.wallSmoothing(
        botLocation, 3 * Math.PI / 2, -1, 120));
    assertTrue(goAngle < Math.PI * 1.4 && goAngle > Math.PI);
    assertTrue(battleField.rectangle.contains(
        DiaUtils.project(botLocation, goAngle, 120)));
  }

  @Test
  public void testWallSmoothingSouthWall() {
    BattleField battleField = newBattleField(800, 600);
    Double botLocation = newLocation(400, 100);
    double goAngle = Utils.normalAbsoluteAngle(battleField.wallSmoothing(
        botLocation, Math.PI, 1, 120));
    assertTrue(goAngle > Math.PI && goAngle < Math.PI * 1.5);
    assertTrue(battleField.rectangle.contains(
        DiaUtils.project(botLocation, goAngle, 120)));
  }

  @Test
  public void testWallSmoothingNorthWall() {
    BattleField battleField = newBattleField(800, 600);
    Double botLocation = newLocation(400, 700);
    double goAngle = Utils.normalAbsoluteAngle(battleField.wallSmoothing(
        botLocation, 0, -1, 120));
    assertTrue(goAngle < 2 * Math.PI && goAngle < 1.5 * Math.PI);
    assertTrue(battleField.rectangle.contains(
        DiaUtils.project(botLocation, goAngle, 120)));
  }

  @Test
  public void testWallSmoothingNorthWallNoSmoothing() {
    BattleField battleField = newBattleField(800, 600);
    Double botLocation = newLocation(400, 500);
    double goAngle = Utils.normalAbsoluteAngle(battleField.wallSmoothing(
        botLocation, 0, -1, 40));
    assertEquals(0, goAngle, 0.001);
    assertTrue(battleField.rectangle.contains(
        DiaUtils.project(botLocation, goAngle, 40)));
  }

  @Test
  public void testWallSmoothingSouthWallNoSmoothing() {
    BattleField battleField = newBattleField(800, 600);
    double heading = Math.PI * 0.8;
    Double botLocation = newLocation(400, 59);
    double goAngle = Utils.normalAbsoluteAngle(battleField.wallSmoothing(
        botLocation, heading, 1, 40));
    assertEquals(heading, goAngle, 0.001);
    assertTrue(battleField.rectangle.contains(
        DiaUtils.project(botLocation, goAngle, 40)));
  }

  @Test
  public void testWallSmoothingEastWallFromOutOfBounds() {
    BattleField battleField = newBattleField(800, 600);
    Double botLocation = newLocation(805, 300);
    double goAngle = Utils.normalAbsoluteAngle(battleField.wallSmoothing(
        botLocation, Math.PI / 2, 1, 120));
    assertTrue(goAngle > Math.PI && goAngle < Math.PI * 1.5);
    assertTrue(battleField.rectangle.contains(
        DiaUtils.project(botLocation, goAngle, 120)));
  }

  @Test
  public void testWallSmoothingEastWallFromOutOfBoundsReverse() {
    BattleField battleField = newBattleField(800, 600);
    Double botLocation = newLocation(805, 300);
    double goAngle = Utils.normalAbsoluteAngle(battleField.wallSmoothing(
        botLocation, Math.PI / 2, -1, 120));
    assertTrue(goAngle > Math.PI * 1.5 && goAngle < 2 * Math.PI);
    assertTrue(battleField.rectangle.contains(
        DiaUtils.project(botLocation, goAngle, 120)));
  }

  @Test
  public void testWallSmoothingEastWallFromWayOutOfBounds() {
    BattleField battleField = newBattleField(800, 600);
    Double botLocation = newLocation(1205, 300);
    double goAngle = Utils.normalAbsoluteAngle(battleField.wallSmoothing(
        botLocation, Math.PI / 2, 1, 120));
    assertTrue(goAngle > Math.PI && goAngle < Math.PI * 1.5);
  }

  private BattleField newBattleField(double width, double height) {
    return new BattleField(width, height);
  }

  private Point2D.Double newLocation(double x, double y) {
    return new Point2D.Double(x, y);
  }
}
