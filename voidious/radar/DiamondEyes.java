package voidious.radar;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import voidious.Diamond;
import voidious.gfx.RoboGraphic;
import voidious.gfx.RoboPainter;
import voidious.utils.BattleField;
import voidious.utils.DiaUtils;
import voidious.utils.MovementPredictor;

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

public class DiamondEyes implements RoboPainter {
  private static final boolean ENABLE_DEBUGGING_GRAPHICS = true;
  private static final double MAX_RADAR_TRACKING_AMOUNT = Math.PI / 4;
  private static final long BOT_NOT_FOUND = -1;
  private static final double BOT_WIDTH = 36;

  private Diamond _robot;
  private Map<String, RadarScan> _scans;
  private Point2D.Double _myLocation;
  private String _targetName = null;
  private boolean _lockMode = false;
  private long _resetTime;
  private Point2D.Double _centerField;
  private int _radarDirection;
  private double _lastRadarHeading;
  private MovementPredictor _predictor;
  private int _startDirection;

  private List<RoboGraphic> _renderables;
  private boolean _painting;
  private boolean _robocodePainting;
  private PrintStream _out;

  public DiamondEyes(Diamond robot, OutputStream out) {
    _robot = robot;
    _out = new PrintStream(out);
    _scans = new HashMap<String, RadarScan>();
    _centerField = new Point2D.Double(_robot.getBattleFieldWidth() / 2,
                                      _robot.getBattleFieldHeight() / 2);
    _predictor = new MovementPredictor(
        new BattleField(_robot.getBattleFieldWidth(),
                        _robot.getBattleFieldHeight()));
    _radarDirection = 1;
    _renderables = new ArrayList<RoboGraphic>();
    _painting = false;
    _robocodePainting = false;
  }

  public void initRound(Diamond robot) {
    _myLocation = new Point2D.Double(_robot.getX(), _robot.getY());
    _robot = robot;
    _scans.clear();
    _lockMode = false;
    _resetTime = 0;
    _lastRadarHeading = _robot.getRadarHeadingRadians();
    _renderables.clear();
    _startDirection = getStartRadarDirection();
  }

  public void execute() {
    _myLocation = new Point2D.Double(_robot.getX(), _robot.getY());
    checkScansIntegrity();
    if (_robot.getOthers() == 1 && !_lockMode && !_scans.isEmpty()) {
      setRadarLock((String)_scans.keySet().toArray()[0]);
    }
    directRadar();

    _lastRadarHeading = _robot.getRadarHeadingRadians();
    _myLocation = _predictor.nextLocation(_robot);
  }

  public void onScannedRobot(ScannedRobotEvent e) {
    Point2D.Double enemyLocation = DiaUtils.project(_myLocation,
        e.getBearingRadians() + _robot.getHeadingRadians(), e.getDistance());

    _scans.put(e.getName(), new RadarScan(_robot.getTime(), enemyLocation));
  }

  public void onRobotDeath(RobotDeathEvent e) {
    _scans.remove(e.getName());
    if (_targetName != null && _targetName.equals(e.getName())) {
      _lockMode = false;
    }
  }

  public void onPaint(Graphics2D g) {
    if (paintStatus()) {
      drawLastKnownBotLocations();
      for (RoboGraphic r : _renderables) {
        r.render(g);
      }
      _renderables.clear();
    }
  }

  public void directRadar() {
    if (_lockMode && !_scans.containsKey(_targetName)) {
      _out.println("WARNING: Radar locked onto dead or non-existent bot, "
          + "releasing lock.");
      _lockMode = false;
    }

    double radarTurnAmount;
    if (_lockMode &&
      _scans.get(_targetName).lastScanTime == _robot.getTime()) {
      radarTurnAmount = Utils.normalRelativeAngle(DiaUtils.absoluteBearing(
              _myLocation, _scans.get(_targetName).lastLocation)
          - _robot.getRadarHeadingRadians());
      _radarDirection = DiaUtils.nonZeroSign(radarTurnAmount);
      radarTurnAmount += _radarDirection * (MAX_RADAR_TRACKING_AMOUNT / 2);
    } else {
      _radarDirection = nextRadarDirection();
      radarTurnAmount = _radarDirection * MAX_RADAR_TRACKING_AMOUNT;
    }
    _robot.setTurnRadarRightRadians(radarTurnAmount);
  }

  public void setRadarLock(String botName) {
    if (_scans.containsKey(botName)) {
      _targetName = botName;
      _lockMode = true;
    }
  }

  public void releaseRadarLock() {
    _lockMode = false;
  }

  public long minTicksToScan(String botName) {
    if (!_scans.containsKey(botName)) { return BOT_NOT_FOUND; }

    double absBearing =
        DiaUtils.absoluteBearing(_myLocation, _scans.get(botName).lastLocation);
    double shortestAngleToScan = Math.abs(Utils.normalRelativeAngle(
        absBearing - _robot.getRadarHeadingRadians()));
    long minTicks = Math.round(
        Math.ceil(shortestAngleToScan / MAX_RADAR_TRACKING_AMOUNT));

    return minTicks;
  }

  private int getStartRadarDirection() {
    return directionToBearing(
        DiaUtils.absoluteBearing(_myLocation, _centerField));
  }

  private int nextRadarDirection() {
    if (_scans.isEmpty() || _scans.size() < _robot.getOthers()) {
      return _startDirection;
    }

    String stalestBot = findStalestBotName();
    Point2D.Double radarTarget;
    if (minTicksToScan(stalestBot) == 4) {
      radarTarget = _centerField;
    } else {
      radarTarget = _scans.get(findStalestBotName()).lastLocation;
    }

    double absBearingRadarTarget =
        DiaUtils.absoluteBearing(_myLocation, radarTarget);

    if (justScannedThatSpot(absBearingRadarTarget)) {
      return _radarDirection;
    }

    return directionToBearing(absBearingRadarTarget);
  }

  private int directionToBearing(double bearing) {
    if (Utils.normalRelativeAngle(
            bearing - _robot.getRadarHeadingRadians()) > 0) {
      return 1;
    } else {
      return -1;
    }
  }

  private String findStalestBotName() {
    long oldestTime = Long.MAX_VALUE;
    String botName = null;

    for (String name : _scans.keySet()) {
      if (_scans.get(name).lastScanTime < oldestTime) {
        oldestTime = _scans.get(name).lastScanTime;
        botName = name;
      }
    }

    return botName;
  }

  private void checkScansIntegrity() {
    if (_scans.size() != _robot.getOthers() &&
      _robot.getTime() - _resetTime > 25 &&
      _robot.getOthers() > 0) {
      _scans.clear();
      _lockMode = false;
      _resetTime = _robot.getTime();
      _out.println("WARNING: Radar integrity failure detected (time = "
          + _resetTime + "), resetting.");
    }
  }

  public boolean justScannedThatSpot(double absBearing) {
    if ((DiaUtils.nonZeroSign(Utils.normalRelativeAngle(
            absBearing - _lastRadarHeading)) == _radarDirection)
        && (DiaUtils.nonZeroSign(Utils.normalRelativeAngle(
            _robot.getRadarHeadingRadians() - absBearing))
                == _radarDirection)) {
      return true;
    } else {
      return false;
    }
  }

  public void drawLastKnownBotLocations() {
    for (RadarScan scan : _scans.values()) {
      _renderables.addAll(Arrays.asList(
          RoboGraphic.drawRectangle(
              scan.lastLocation, BOT_WIDTH, BOT_WIDTH, Color.gray)));
    }
  }

  public void paintOn() {
    _painting = ENABLE_DEBUGGING_GRAPHICS;
  }

  public void paintOff() {
    _renderables.clear();
    _painting = false;
  }

  public void robocodePaintOn() {
    _robocodePainting = true;
  }

  public void robocodePaintOff() {
    _renderables.clear();
    _robocodePainting = false;
  }

  public String paintLabel() {
    return "Radar";
  }

  public boolean paintStatus() {
    return (_painting && _robocodePainting);
  }

  public class RadarScan {
    public final long lastScanTime;
    public final Point2D.Double lastLocation;

    public RadarScan(long lastScanTime, Point2D.Double lastLocation) {
      this.lastScanTime = lastScanTime;
      this.lastLocation = lastLocation;
    }
  }
}
