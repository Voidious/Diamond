package voidious.utils;

import static org.junit.Assert.*;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

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

public class RobotStateTest {

  @Test
  public void testNewRobotState() {
    Point2D.Double botLocation = newLocation(312, 939);
    RobotState state = RobotState.newBuilder()
        .setLocation(botLocation)
        .setHeading(2.1)
        .setVelocity(7.5)
        .setTime(39L)
        .setInterpolated(true)
        .build();
    assertEquals(botLocation, state.location);
    assertEquals(2.1, state.heading, 0.001);
    assertEquals(7.5, state.velocity, 0.001);
    assertEquals(39L, state.time);
    assertTrue(state.interpolated);
  }

  @Test
  public void testNewRobotStateNotInterpolated() {
    RobotState state = RobotState.newBuilder()
        .setInterpolated(false)
        .build();
    assertFalse(state.interpolated);
  }

  @Test
  public void testNewRobotStateBlank() {
    RobotState state = RobotState.newBuilder()
        .build();
    assertNull(state.location);
    assertEquals(0, state.heading, 0.001);
    assertEquals(0, state.velocity, 0.001);
    assertEquals(-1L, state.time);
    assertFalse(state.interpolated);
  }

  @Test
  public void testBotCorners() {
    Point2D.Double botLocation = newLocation(312, 939);
    RobotState state = RobotState.newBuilder()
        .setLocation(botLocation)
        .build();
    List<Point2D.Double> botCorners = state.botCorners();
    assertTrue(listContainsPoint(botCorners, newLocation(330, 957)));
    assertTrue(listContainsPoint(botCorners, newLocation(330, 921)));
    assertTrue(listContainsPoint(botCorners, newLocation(294, 921)));
    assertTrue(listContainsPoint(botCorners, newLocation(294, 957)));
    assertEquals(botCorners, state.botCorners());
  }

  private boolean listContainsPoint(
      List<Point2D.Double> pointList, Point2D.Double p) {
    for (Point2D.Double p2 : pointList) {
      if (p2.distanceSq(p) < 0.001) {
        return true;
      }
    }
    return false;
  }

  @Test
  public void testBotRectangle() {
    Point2D.Double botLocation = newLocation(155, 720);
    RobotState state = RobotState.newBuilder()
        .setLocation(botLocation)
        .build();
    Rectangle2D.Double botRectangle = state.botRectangle();
    assertEquals(36, botRectangle.width, 0.001);
    assertEquals(36, botRectangle.height, 0.001);
    assertEquals(137, botRectangle.getMinX(), 0.001);
    assertEquals(173, botRectangle.getMaxX(), 0.001);
    assertEquals(702, botRectangle.getMinY(), 0.001);
    assertEquals(738, botRectangle.getMaxY(), 0.001);
    assertEquals(155, botRectangle.getCenterX(), 0.001);
    assertEquals(720, botRectangle.getCenterY(), 0.001);
    assertEquals(botRectangle, state.botRectangle());
  }

  @Test
  public void testBotSides() {
    Point2D.Double botLocation = newLocation(348, 741);
    RobotState state = RobotState.newBuilder()
        .setLocation(botLocation)
        .build();
    List<Line2D.Double> botSides = state.botSides();
    assertTrue(
        listContainsLine(botSides, new Line2D.Double(366, 723, 366, 759)));
    assertTrue(
        listContainsLine(botSides, new Line2D.Double(366, 759, 330, 759)));
    assertTrue(
        listContainsLine(botSides, new Line2D.Double(330, 759, 330, 723)));
    assertTrue(
        listContainsLine(botSides, new Line2D.Double(330, 723, 366, 723)));
    assertEquals(botSides, state.botSides());
  }

  private boolean listContainsLine(
      List<Line2D.Double> lineList, Line2D.Double l) {
    for (Line2D.Double l2 : lineList) {
      if ((l.getP1().distanceSq(l2.getP1()) < 0.001
              && l.getP2().distanceSq(l2.getP2()) < 0.001)
          || (l.getP1().distanceSq(l2.getP2()) < 0.001
              && l.getP2().distanceSq(l2.getP1()) < 0.001)) {
        return true;
      }
    }
    return false;
  }

  private Point2D.Double newLocation(double x, double y) {
    return new Point2D.Double(x, y);
  }
}
