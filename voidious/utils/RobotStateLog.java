package voidious.utils;

import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

public class RobotStateLog implements Cloneable {
  private Map<Long, RobotState> _robotStates;

  public RobotStateLog() {
    _robotStates = new HashMap<Long, RobotState>();
  }

  public void clear() {
    _robotStates.clear();
  }

  public void addState(RobotState state) {
    _robotStates.put(state.time, state);
  }

  public RobotState getState(long time) {
    return getState(time, true);
  }

  public RobotState getState(long time, boolean interpolate) {
    if (_robotStates.containsKey(time)) {
      RobotState robotState = _robotStates.get(time);
      return (interpolate || !robotState.interpolated) ? robotState : null;
    } else if (interpolate) {
      RobotState beforeState = null;
      RobotState afterState = null;
      for (RobotState state : _robotStates.values()) {
        if (!state.interpolated) {
          if (state.time < time) {
            if (beforeState == null || state.time > beforeState.time) {
              beforeState = state;
            }
          }
          if (state.time > time) {
            if (afterState == null || state.time < afterState.time) {
              afterState = state;
            }
          }
        }
      }

      if (beforeState == null || afterState == null) {
        return null;
      }

      Interpolator interpolator =
          new Interpolator(time, beforeState.time, afterState.time);
      RobotState interpolatedRobotState = RobotState.newBuilder()
          .setLocation(interpolator.getLocation(
              beforeState.location, afterState.location))
          .setHeading(
              interpolator.getHeading(beforeState.heading, afterState.heading))
          .setVelocity(
              interpolator.avg(beforeState.velocity, afterState.velocity))
          .setTime(time)
          .setInterpolated(true)
          .build();
      _robotStates.put(time, interpolatedRobotState);
      return interpolatedRobotState;
    } else {
      return null;
    }
  }

  public double getDisplacementDistance(
      Point2D.Double location, long currentTime, long ticksAgo) {
    RobotState pastState = getState(currentTime - ticksAgo);
    pastState = (pastState == null) ? getOldestState() : pastState;
    return location.distance(pastState.location);
  }

  private RobotState getOldestState() {
    return _robotStates.get(Collections.min(_robotStates.keySet()));
  }

  public void forAllStates(AllStateListener listener) {
    for (RobotState state : _robotStates.values()) {
      listener.onRobotState(state);
    }
  }

  public int size() {
    return _robotStates.size();
  }

  public Object clone() {
    RobotStateLog newLog = new RobotStateLog();
    for (Long time : _robotStates.keySet()) {
      newLog.addState(_robotStates.get(time));
    }
    return newLog;
  }

  public interface AllStateListener {
    void onRobotState(RobotState state);
  }
}
