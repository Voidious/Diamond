package voidious.gun;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import robocode.Bullet;
import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.DeathEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;

import robocode.util.Utils;
import voidious.Diamond;
import voidious.gfx.ColoredValueSet;
import voidious.gfx.RoboGraphic;
import voidious.gfx.RoboPainter;
import voidious.gun.FireListener.FiredBullet;
import voidious.utils.BattleField;
import voidious.utils.DiaUtils;
import voidious.utils.MovementPredictor;
import voidious.utils.TimestampedFiringAngle;
import voidious.utils.Wave;

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

public class DiamondFist implements RoboPainter {
  private static final boolean ENABLE_DEBUGGING_GRAPHICS = true;
  private static final int KNN_DATA_THRESHOLD = 9;

  private Diamond _robot;
  private boolean _tcMode;
  private GunDataManager _gunDataManager;
  private VirtualGunsManager<TimestampedFiringAngle> _virtualGuns;
  private DuelGun<TimestampedFiringAngle> _perceptualGun;
  private DuelGun<TimestampedFiringAngle> _currentGun;
  private MeleeGun _meleeGun;
  private int _enemiesTotal;
  private double _aimedBulletPower;
  private List<FireListener> _fireListeners;
  private BattleField _battleField;
  private MovementPredictor _predictor;

  private boolean _startedDuel;
  private Collection<RoboGraphic> _renderables;
  private boolean _painting;
  private boolean _robocodePainting;
  private PrintStream _out;
  private boolean _drawVictory;

  public DiamondFist(Diamond robot, boolean isTcMode) {
    this(robot, isTcMode,
        new BattleField(robot.getBattleFieldWidth(),
            robot.getBattleFieldHeight()),
        new ArrayList<RoboGraphic>(), System.out);
  }

  DiamondFist(Diamond robot, boolean isTcMode, BattleField battleField,
      Collection<RoboGraphic> renderables, OutputStream out) {
    this(robot, isTcMode, battleField, new MovementPredictor(battleField),
        renderables, out);
  }

  DiamondFist(Diamond robot, boolean isTcMode, BattleField battleField,
      MovementPredictor predictor, Collection<RoboGraphic> renderables,
      OutputStream out) {
    this(robot, isTcMode,
        new GunDataManager(robot.getOthers(), battleField, predictor,
            renderables, out),
        new VirtualGunsManager<TimestampedFiringAngle>(out), battleField,
        renderables, out);
  }

  DiamondFist(Diamond robot, boolean isTcMode, GunDataManager gunDataManager,
      VirtualGunsManager<TimestampedFiringAngle> virtualGunsManager,
      BattleField battleField, Collection<RoboGraphic> renderables,
      OutputStream out) {
    if (robot == null || gunDataManager == null || virtualGunsManager == null) {
      throw new NullPointerException();
    }

    _robot = robot;
    _tcMode = isTcMode;
    _gunDataManager = gunDataManager;
    _virtualGuns = virtualGunsManager;
    _battleField = battleField;
    _renderables = renderables;
    _out = new PrintStream(out);
    _predictor = new MovementPredictor(battleField);
    _enemiesTotal = _robot.getOthers();
    _gunDataManager.addListener(_virtualGuns);

    _fireListeners = new ArrayList<FireListener>();
    _painting = false;
    _robocodePainting = false;

    initGuns();
  }

  void initGuns() {
    DuelGun<TimestampedFiringAngle> mainGun;
    if (_enemiesTotal > 1) {
      _perceptualGun = null;
      mainGun = new MainGun(_gunDataManager, _battleField, _renderables);
      _currentGun = mainGun;
    } else {
      _perceptualGun = new PerceptualGun<TimestampedFiringAngle>();
      _currentGun = _perceptualGun;
      mainGun = new TripHammerKnnGun(_gunDataManager, _renderables);
    }
    _virtualGuns.addGun(mainGun);

    DuelGun<TimestampedFiringAngle> antiSurfGun = new AntiSurferGun(
        _gunDataManager, _battleField, _renderables);
    _virtualGuns.addGun(antiSurfGun);

    _meleeGun = new MeleeGun(_gunDataManager, _battleField, _renderables);
  }

  void initGunViews(GunEnemy gunData) {
    for (DuelGun<TimestampedFiringAngle> gun : _virtualGuns.getGuns()) {
      gunData.addViews(gun.newDataViews());
    }
    if (isMelee()) {
      gunData.addViews(_meleeGun.newDataViews());
    }
  }

  public void initRound(Diamond robot) {
    _robot = robot;
    _gunDataManager.initRound();
    _virtualGuns.initRound();

    _startedDuel = false;
    _drawVictory = false;
    _renderables.clear();
  }

  public void execute() {
    _gunDataManager.execute(_robot.getRoundNum(), _robot.getTime(),
        calculateBulletPower(), _robot.getGunHeat(), myLocation(), is1v1(),
        paintStatus());
    if (is1v1()) {
      GunEnemy duelEnemy = _gunDataManager.duelEnemy();
      if (duelEnemy != null) {
        aimAndFire(duelEnemy);
        if (!_startedDuel) {
          _startedDuel = true;
          printCurrentGun(duelEnemy);
        }
      }
    } else {
      aimAndFireAtEveryone();
    }
    if (paintStatus() && _drawVictory) {
      _gunDataManager.drawVictory(_robot.getTime());
      _drawVictory = false;
    }
  }

  private void aimAndFire(GunEnemy gunData) {
    if (gunData != null) {
      fireIfGunTurned(_aimedBulletPower);

      Wave aimWave = gunData.lastWaveFired;
      _aimedBulletPower = aimWave.bulletPower();
      Point2D.Double myNextLocation = _predictor.nextLocation(_robot);
      double firingAngle;
      if (gunData.energy == 0 || ticksUntilGunCool() > 3) {
        firingAngle =
            DiaUtils.absoluteBearing(myNextLocation, aimWave.targetLocation);
        evaluateVirtualGuns(gunData);
      } else {
        firingAngle = _currentGun.aim(aimWave, paintStatus());
      }
      _robot.setTurnGunRightRadians(Utils.normalRelativeAngle(
          firingAngle - _robot.getGunHeadingRadians()));
    }
  }

  private void aimAndFireAtEveryone() {
    GunEnemy closestBot = _gunDataManager.getClosestLivingBot(myLocation());
    if (closestBot != null) {
      fireIfGunTurned(_aimedBulletPower);
      Point2D.Double myNextLocation = _predictor.nextLocation(_robot);
      long ticksUntilFire = ticksUntilGunCool();

      if (ticksUntilFire % 2 == 0 || ticksUntilFire <= 4) {
        _aimedBulletPower = calculateBulletPower();
        double firingAngle = _meleeGun.aimAtEveryone(myNextLocation,
            _robot.getTime(), _robot.getOthers(), _aimedBulletPower,
            closestBot, paintStatus());
        _robot.setTurnGunRightRadians(Utils.normalRelativeAngle(
            firingAngle - _robot.getGunHeadingRadians()));
      }
    }
  }

  protected Point2D.Double myLocation() {
    return new Point2D.Double(_robot.getX(), _robot.getY());
  }

  double calculateBulletPower() {
    GunEnemy gunData = _gunDataManager.getClosestLivingBot(myLocation());
    double bulletPower = 3;
    if (gunData != null) {
      double myEnergy = _robot.getEnergy();
      if (_tcMode) {
        bulletPower = Math.min(myEnergy, 3);
      } else if (is1v1()) {
        bulletPower = 1.95;

        if (gunData.distance < 150 || gunData.isRammer()) {
          bulletPower = 2.95;
        }

        if (gunData.distance > 325) {
          double powerDownPoint =
              DiaUtils.limit(35, 63 + ((gunData.energy - myEnergy) * 4), 63);
          if (myEnergy < powerDownPoint) {
            bulletPower = Math.min(bulletPower,
                DiaUtils.cube(myEnergy / powerDownPoint) * 1.95);
          }
        }

        bulletPower = Math.min(bulletPower, gunData.energy / 4);
        bulletPower = Math.max(bulletPower, 0.1);
        bulletPower = Math.min(bulletPower, myEnergy);
      } else {
        double avgEnemyEnergy = _gunDataManager.getAverageEnergy();

        bulletPower = 2.999;

        int enemiesAlive = _robot.getOthers();
        if (enemiesAlive <= 3) {
          bulletPower = 1.999;
        }

        if (enemiesAlive <= 5 && gunData.distance > 500) {
          bulletPower = 1.499;
        }

        if ((myEnergy < avgEnemyEnergy && enemiesAlive <= 5
                && gunData.distance > 300)
            || gunData.distance > 700) {
          bulletPower = 0.999;
        }

        if (myEnergy < 20 && myEnergy < avgEnemyEnergy) {
          bulletPower =
              Math.min(bulletPower, 2 - ((20 - myEnergy) / 11));
        }

        bulletPower = Math.max(bulletPower, 0.1);
        bulletPower = Math.min(bulletPower, myEnergy);
      }
    }

    return bulletPower;
  }

  private void evaluateVirtualGuns(GunEnemy gunData) {
    int dataPoints = _gunDataManager.getDuelDataSize();
    if (dataPoints < KNN_DATA_THRESHOLD) {
      _currentGun = _perceptualGun;
    } else {
      if (_perceptualGun != null) {
        _out.println(
            "Disabling " + _perceptualGun.getLabel() + " @ " + _robot.getTime());
        _perceptualGun = null;
      }
  
      DuelGun<TimestampedFiringAngle> bestGun =
          _virtualGuns.bestGun(gunData.botName);
      if (_currentGun != bestGun) {
        _currentGun = bestGun;
        _out.println("Switching to " + _currentGun.getLabel() + " ("
            + _virtualGuns.getFormattedRating(_currentGun, gunData.botName)
            + ")");
      }
    }
  }

  void fireIfGunTurned(double bulletPower) {
    if (_robot.getGunHeat() == 0 && _robot.getGunTurnRemaining() == 0) {
      Bullet realBullet = null;
      if (_tcMode) {
        realBullet = _robot.setFireBullet(3);
      } else if (_robot.getEnergy() > bulletPower) {
        realBullet = setFireBulletLogged(bulletPower);
      }

      if (realBullet != null) {
        _gunDataManager.markFiringWaves(_robot.getTime(), is1v1());
      }
    }
  }

  public void onScannedRobot(ScannedRobotEvent e) {
    String botName = e.getName();
    double absBearing = e.getBearingRadians() + _robot.getHeadingRadians();
    Point2D.Double enemyLocation = DiaUtils.project(
        myLocation(), Utils.normalAbsoluteAngle(absBearing), e.getDistance());

    GunEnemy gunData;
    if (_gunDataManager.hasEnemy(botName)) {
      gunData = _gunDataManager.updateEnemy(
          e, enemyLocation, absBearing, _robot.getRoundNum(), is1v1());
    } else {
      gunData = _gunDataManager.newEnemy(
          e, enemyLocation, absBearing, _robot.getRoundNum(), is1v1());
      initGunViews(gunData);
    }

    _gunDataManager.fireNextTickWave(_predictor.nextLocation(_robot),
        enemyLocation, botName, _robot.getRoundNum(), _robot.getTime(),
        calculateBulletPower(), _robot.getEnergy(), _robot.getGunHeat(),
        _robot.getHeadingRadians(), _robot.getVelocity(), _robot.getOthers());
  }

  public void onRobotDeath(RobotDeathEvent e) {
    _gunDataManager.onRobotDeath(e);
  }

  public void onWin(WinEvent e) {
    _drawVictory = true;
    roundOver();
  }

  public void onDeath(DeathEvent e) {
    roundOver();
  }

  protected void roundOver() {
    if (_tcMode) {
      _out.println("TC score: "
          + (_gunDataManager.getDamageGiven() / (_robot.getRoundNum() + 1)));
    }
    GunEnemy duelEnemy = _gunDataManager.duelEnemy();
    if (is1v1() && duelEnemy != null) {
      _virtualGuns.printGunRatings(duelEnemy.botName);
    }
  }

  public void onBulletHit(BulletHitEvent e) {
    _gunDataManager.onBulletHit(e, _robot.getTime());
  }

  public void onBulletHitBullet(BulletHitBulletEvent e) {
    _gunDataManager.onBulletHitBullet(e, _robot.getTime());
  }

  public void onPaint(Graphics2D g) {
    // TODO: factor _renderables into something else
    if (paintStatus()) {
      for (RoboGraphic r : _renderables) {
        r.render(g);
      }
      _renderables.clear();
    }
  }

  private long ticksUntilGunCool() {
    return Math.round(Math.ceil(
        _robot.getGunHeat() / _robot.getGunCoolingRate()));
  }

  private boolean is1v1() {
    return (_robot.getOthers() <= 1);
  }

  private boolean isMelee() {
    return (_robot.getOthers() > 1);
  }

  public int getEnemiesAlive() {
    return _robot.getOthers();
  }

  public void addFireListener(FireListener listener) {
    _fireListeners.add(listener);
  }

  private Bullet setFireBulletLogged(double bulletPower) {
    Bullet bullet = _robot.setFireBullet(bulletPower);
    if (bullet != null) {
      for (FireListener listener : _fireListeners) {
        listener.bulletFired(new FiredBullet(_robot.getTime(), myLocation(),
            _robot.getGunHeadingRadians(), (20 - (3 * bulletPower))));
      }
    }

    return bullet;
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
    return "Gun";
  }

  public boolean paintStatus() {
    return (_painting && _robocodePainting);
  }

  protected static void drawGunAngles(Collection<RoboGraphic> renderables,
    Wave wave, ColoredValueSet cvs, double bestAngle, double bandwidth) {

    double arrowLength = DiaUtils.limit(
        50, wave.sourceLocation.distance(wave.targetLocation) - 75, 200);

    for (ColoredValueSet.ColoredValue cv : cvs.getColoredValues()) {
      Color c = cv.grayToWhiteColor();
      Point2D.Double angleHead = DiaUtils.project(
          wave.sourceLocation, cv.firingAngle, arrowLength);

      renderables.add(RoboGraphic.drawLine(wave.sourceLocation, angleHead, c));
      renderables.addAll(Arrays.asList(
          RoboGraphic.drawArrowHead(angleHead, 10, cv.firingAngle, c)));
    }

    renderables.add(RoboGraphic.drawPoint(DiaUtils.project(
        wave.sourceLocation, bestAngle, arrowLength + 5), Color.red));
  }

  private void printCurrentGun(GunEnemy gunData) {
    _out.println("Current gun: " + _currentGun.getLabel() + " ("
        + _virtualGuns.getFormattedRating(_currentGun, gunData.botName)+ ")");
  }

  Diamond getRobot() {
    return _robot;
  }

  VirtualGunsManager<TimestampedFiringAngle> getVirtualGunsManager() {
    return _virtualGuns;
  }

  Collection<RoboGraphic> getRenderables() {
    return _renderables;
  }
}
