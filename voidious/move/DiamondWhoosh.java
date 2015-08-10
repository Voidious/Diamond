package voidious.move;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;

import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.DeathEvent;
import robocode.HitByBulletEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;
import robocode.util.Utils;

import voidious.Diamond;
import voidious.gfx.RoboGraphic;
import voidious.gfx.RoboPainter;
import voidious.gun.FireListener;
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

public class DiamondWhoosh implements RoboPainter, FireListener {
  private static final boolean ENABLE_DEBUGGING_GRAPHICS = true;
  private static final int WAVES_TO_SURF = 2;

  private Diamond _robot;
  private MoveDataManager _moveDataManager;
  private MeleeMover _meleeMover;
  private SurfMover _surfMover;

  private Collection<RoboGraphic> _renderables;
  private boolean _painting;
  private boolean _robocodePainting;

  public DiamondWhoosh(Diamond robot, OutputStream out) {
    this(robot, new BattleField(robot.getBattleFieldWidth(),
                                robot.getBattleFieldHeight()),
         new ArrayList<RoboGraphic>(), out);
  }

  DiamondWhoosh(Diamond robot, BattleField battleField,
      Collection<RoboGraphic> renderables, OutputStream out) {
    this(robot, battleField, new MovementPredictor(battleField), renderables,
        out);
  }

  DiamondWhoosh(Diamond robot, BattleField battleField,
      MovementPredictor predictor, Collection<RoboGraphic> renderables,
      OutputStream out) {
    this(robot,
        new MoveDataManager(robot.getOthers(), battleField, predictor,
            renderables, out),
        new MeleeMover(robot, battleField, renderables),
        new SurfMover(robot, battleField, renderables, out),
        battleField, renderables, out);
  }

  public DiamondWhoosh(Diamond robot, MoveDataManager moveDataManager,
      MeleeMover meleeMover, SurfMover surfMover, BattleField battleField,
      Collection<RoboGraphic> renderables, OutputStream out) {
    _robot = robot;
    _moveDataManager = moveDataManager;
    _meleeMover = meleeMover;
    _surfMover = surfMover;
    _renderables = renderables;

    _painting = false;
    _robocodePainting = false;
  }

  public void initRound(Diamond robot) {
    _robot = robot;
    _moveDataManager.initRound();
    _meleeMover.initRound(robot, myLocation());
    _surfMover.initRound();
    _renderables.clear();
  }

  public void execute() {
    _moveDataManager.execute(_robot.getRoundNum(), _robot.getTime(),
        myLocation(), _robot.getHeadingRadians(), _robot.getVelocity());
    move();
  }

  private void move() {
    if (is1v1()) {
      _surfMover.move(_moveDataManager.myStateLog().getState(_robot.getTime()),
          _moveDataManager.duelEnemy(), WAVES_TO_SURF, paintStatus());
    } else {
      _meleeMover.move(myLocation(), _moveDataManager.getAllEnemyData(),
          _moveDataManager.getClosestLivingBot(myLocation()), paintStatus());
    }
  }

  public void onScannedRobot(ScannedRobotEvent e) {
    Point2D.Double myLocation = myLocation();
    String botName = e.getName();
    double absBearing = Utils.normalAbsoluteAngle(
        e.getBearingRadians() + _robot.getHeadingRadians());
    Point2D.Double enemyLocation =
        DiaUtils.project(myLocation, absBearing, e.getDistance());

    double previousEnemyEnergy;
    if (_moveDataManager.hasEnemy(botName)) {
      previousEnemyEnergy = _moveDataManager.getEnemyData(botName).energy;
      _moveDataManager.updateEnemy(
          e, enemyLocation, absBearing, _robot.getRoundNum(), is1v1());
    } else {
      previousEnemyEnergy = e.getEnergy();
      _moveDataManager.newEnemy(
          e, enemyLocation, absBearing, _robot.getRoundNum(), is1v1());
    }

    if (is1v1() && _moveDataManager.duelEnemy() != null) {
      _moveDataManager.updateEnemyWaves(myLocation(), previousEnemyEnergy,
          botName, _robot.getRoundNum(), _robot.getTime(), _robot.getEnergy(),
          _robot.getHeadingRadians(), _robot.getVelocity(), WAVES_TO_SURF);
    }
  }

  public void onRobotDeath(RobotDeathEvent e) {
    _moveDataManager.onRobotDeath(e);
  }

  public void onHitByBullet(HitByBulletEvent e) {
    _moveDataManager.onHitByBullet(
        e, _robot.getRoundNum(), _robot.getTime(), paintStatus());
  }

  public void onBulletHitBullet(BulletHitBulletEvent e) {
    _moveDataManager.onBulletHitBullet(e, _robot.getRoundNum(),
        _robot.getTime(), paintStatus());
  }

  public void onBulletHit(BulletHitEvent e) {
    _moveDataManager.onBulletHit(e);
  }

  public void onWin(WinEvent e) {
    roundOver();
  }

  public void onDeath(DeathEvent e) {
    roundOver();
  }

  void roundOver() {
    MoveEnemy duelEnemy = _moveDataManager.duelEnemy();
    if (is1v1() && duelEnemy != null) {
      _surfMover.roundOver(duelEnemy);
    }
  }

  public void onPaint(Graphics2D g) {
    if (paintStatus()) {
      for (RoboGraphic renderable : _renderables) {
        renderable.render(g);
      }
      _renderables.clear();
    }
  }

  private Point2D.Double myLocation() {
    return new Point2D.Double(_robot.getX(), _robot.getY());
  }

  private boolean is1v1() {
    return (_robot.getOthers() <= 1);
  }

  @Override
  public void bulletFired(FiredBullet bullet) {
    _moveDataManager.addFiredBullet(bullet);
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
    return "Movement";
  }

  public boolean paintStatus() {
    return (_painting && _robocodePainting);
  }

  Diamond getRobot() {
    return _robot;
  }

  Collection<RoboGraphic> getRenderables() {
    return _renderables;
  }
}
