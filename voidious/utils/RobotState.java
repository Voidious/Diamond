package voidious.utils;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

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

public class RobotState {
  private static final double BOT_HALF_WIDTH = 18;

  public final Point2D.Double location;
  public final double heading;
  public final double velocity;
  public final long time;
  public final boolean interpolated;
  private List<Point2D.Double> _botCorners;
  private Rectangle2D.Double _botRectangle;
  private List<Line2D.Double> _botSides;

  private RobotState(Point2D.Double location, double heading, double velocity,
      long time, boolean interpolated) {
    this.location = location;
    this.heading = heading;
    this.velocity = velocity;
    this.time = time;
    this.interpolated = interpolated;

    _botCorners = null;
    _botRectangle = null;
    _botSides = null;
  }

  public List<Point2D.Double> botCorners() {
    if (_botCorners == null) {
      _botCorners = new ArrayList<Point2D.Double>();
      _botCorners.add(new Point2D.Double(location.x - BOT_HALF_WIDTH,
                                         location.y - BOT_HALF_WIDTH));
      _botCorners.add(new Point2D.Double(location.x - BOT_HALF_WIDTH,
                                         location.y + BOT_HALF_WIDTH));
      _botCorners.add(new Point2D.Double(location.x + BOT_HALF_WIDTH,
                                         location.y - BOT_HALF_WIDTH));
      _botCorners.add(new Point2D.Double(location.x + BOT_HALF_WIDTH,
                                         location.y + BOT_HALF_WIDTH));
    }
    return _botCorners;
  }

  public Rectangle2D.Double botRectangle() {
    if (_botRectangle == null) {
      _botRectangle =
          new Rectangle2D.Double(location.x - 18, location.y - 18, 36, 36);
    }
    return _botRectangle;
  }

  public List<Line2D.Double> botSides() {
    if (_botSides == null) {
      _botSides = new ArrayList<Line2D.Double>();
      _botSides.add(new Line2D.Double(location.x - 18,
          location.y - 18, location.x + 18, location.y - 18));
      _botSides.add(new Line2D.Double(location.x + 18,
          location.y - 18, location.x + 18, location.y + 18));
      _botSides.add(new Line2D.Double(location.x + 18,
          location.y + 18, location.x - 18, location.y + 18));
      _botSides.add(new Line2D.Double(location.x - 18,
          location.y + 18, location.x - 18, location.y - 18));
    }
    return _botSides;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private Point2D.Double location;
    private double heading;
    private double velocity;
    private long time;
    private boolean interpolated;

    public Builder() {
      location = null;
      heading = 0;
      velocity = 0;
      time = -1;
      interpolated = false;
    }

    public Builder setLocation(Point2D.Double location) {
      this.location = location;
      return this;
    }

    public Builder setHeading(double heading) {
      this.heading = heading;
      return this;
    }

    public Builder setVelocity(double velocity) {
      this.velocity = velocity;
      return this;
    }

    public Builder setTime(long time) {
      this.time = time;
      return this;
    }

    public Builder setInterpolated(boolean interpolated) {
      this.interpolated = interpolated;
      return this;
    }

    public RobotState build() {
      return new RobotState(location, heading, velocity, time, interpolated);
    }
  }
}
