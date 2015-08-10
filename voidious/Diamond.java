package voidious;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

import robocode.AdvancedRobot;
import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.DeathEvent;
import robocode.HitByBulletEvent;
import robocode.HitWallEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.SkippedTurnEvent;
import robocode.WinEvent;

import voidious.gfx.DiamondColors;
import voidious.gun.DiamondFist;
import voidious.move.DiamondWhoosh;
import voidious.radar.DiamondEyes;
import voidious.utils.ErrorLogger;

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

/**
 * Diamond - a robot by Voidious
 *
 * A Melee and 1v1 bot.
 *
 * In Melee it uses:
 *   - Minimum Risk Movement
 *   - Dynamic Clustering
 *   - Displacement Vectors
 *   - "Shadow" Melee Gun
 *
 * In 1v1 it uses:
 *   - Wave Surfing movement with Dynamic Clustering
 *   - Dynamic Clustering / GuessFactors for the two guns: one tuned for
 *     surfers, the other tuned for non-adaptive movements.
 *
 * For more details, see: http://robowiki.net?Diamond
 */
public class Diamond extends AdvancedRobot {
  private static final boolean _TC = false;
  private static final boolean _MC = false;
  private static final boolean _LOG_ERRORS = true;

  protected static DiamondEyes _radar;
  protected static DiamondWhoosh _move;
  private static DiamondFist _gun;
  protected static DiamondColors _gfx;
  private static double _randColors = Math.random();

  private double _maxVelocity;

  static {
    ErrorLogger.enabled = _LOG_ERRORS;
  }

  public void run() {
    try {
      ErrorLogger.init(this);
      initComponents();
      initColors();

      setAdjustGunForRobotTurn(true);
      setAdjustRadarForGunTurn(true);

      while (true) {
        _gfx.updatePaintProcessing();
        if (!_TC) {
          _move.execute();
        }
        if (!_MC) {
          _gun.execute();
        }
        _radar.execute();
        execute();
      }
    } catch (RuntimeException re) {
      logAndRethrowException(re);
    }
  }

  private void initComponents() {
    if (_radar == null) {
      _radar = new DiamondEyes(this, System.out);
    }
    if (_move == null) {
      _move = new DiamondWhoosh(this, System.out);
    }
    if (_gun == null) {
      _gun = new DiamondFist(this, _TC);
      _gun.addFireListener(_move);
    }
    if (_gfx == null) {
      _gfx = new DiamondColors(this, _radar, _move, _gun, _TC, _MC);
      _gfx.registerPainter("r", _radar);
      _gfx.registerPainter("g", _gun);
      _gfx.registerPainter("m", _move);
    }

    _radar.initRound(this);
    _move.initRound(this);
    _gun.initRound(this);
  }

  private void initColors() {
    if (_randColors < .05) {
      setBlueStreakColors();
    } else if (_randColors < .1) {
      setMillenniumGuardColors();
    } else {
      setDiamondColors();
    }
  }

  private void setDiamondColors() {
    Color diamondYellow = new Color(255, 255, 170);
    setColors(Color.black, Color.black, diamondYellow);
  }

  private void setMillenniumGuardColors() {
    if (getRoundNum() == 0) {
      System.out.println("Activating Millennium Guard colors.");
    }

    Color bloodRed = new Color(120, 0, 0);
    Color gold = new Color(240, 235, 170);
    setColors(bloodRed, gold, bloodRed);
  }

  private void setBlueStreakColors() {
    if (getRoundNum() == 0) {
      System.out.println("Activating Blue Streak colors.");
    }

    Color denimGrey = new Color(101, 108, 128);
    Color gloveRed = new Color(150, 20, 30);
    setColors(denimGrey, Color.white, gloveRed);
  }

  public void onScannedRobot(ScannedRobotEvent e) {
    try {
      _radar.onScannedRobot(e);
      if (!_TC) {
        _move.onScannedRobot(e);
      }
      if (!_MC) {
        _gun.onScannedRobot(e);
      }
    } catch (RuntimeException re) {
      logAndRethrowException(re);
    }
  }

  public void onRobotDeath(RobotDeathEvent e) {
    try {
      _radar.onRobotDeath(e);
      if (!_TC) {
        _move.onRobotDeath(e);
      }
      if (!_MC) {
        _gun.onRobotDeath(e);
      }
    } catch (RuntimeException re) {
      logAndRethrowException(re);
    }
  }

  public void onHitByBullet(HitByBulletEvent e) {
    try {
      if (!_TC) {
        _move.onHitByBullet(e);
      }
    } catch (RuntimeException re) {
      logAndRethrowException(re);
    }
  }

  public void onBulletHit(BulletHitEvent e) {
    try {
      if (!_TC) {
        _move.onBulletHit(e);
      }
      if (!_MC) {
        _gun.onBulletHit(e);
      }
    } catch (RuntimeException re) {
      logAndRethrowException(re);
    }
  }

  public void onBulletHitBullet(BulletHitBulletEvent e) {
    try {
      if (!_TC) {
        _move.onBulletHitBullet(e);
      }
      if (!_MC) {
        _gun.onBulletHitBullet(e);
      }
    } catch (RuntimeException re) {
      logAndRethrowException(re);
    }
  }

  public void onHitWall(HitWallEvent e) {
    System.out.println("WARNING: I hit a wall (" + getTime() + ").");
  }

  public void onWin(WinEvent e) {
    try {
      if (!_MC) {
        _gun.onWin(e);
      }
      if (!_TC) {
        _move.onWin(e);
      }
    } catch (RuntimeException re) {
      logAndRethrowException(re);
    }
  }

  public void onDeath(DeathEvent e) {
    try {
      if (!_MC) {
        _gun.onDeath(e);
      }
      if (!_TC) {
        _move.onDeath(e);
      }
    } catch (RuntimeException re) {
      logAndRethrowException(re);
    }
  }

  public void onPaint(Graphics2D g) {
    try {
      _gfx.onPaint(g);
    } catch (RuntimeException re) {
      logAndRethrowException(re);
    }
  }

  public void onKeyPressed(KeyEvent e) {
    try {
      _gfx.onKeyPressed(e);
    } catch (RuntimeException re) {
      logAndRethrowException(re);
    }
  }

  public void onSkippedTurn(SkippedTurnEvent e) {
    System.out.println("WARNING: Turn skipped at: " + e.getTime());
  }

  protected void logAndRethrowException(RuntimeException e) {
    String moreInfo = "getOthers(): " + getOthers() + "\n"
        + "getEnemiesAlive(): " + _gun.getEnemiesAlive() + "\n"
        + "getRoundNum(): " + getRoundNum() + "\n" + "getTime(): " + getTime();
    ErrorLogger.getInstance().logException(e, moreInfo);

    throw e;
  }

  public void setMaxVelocity(double maxVelocity) {
    super.setMaxVelocity(maxVelocity);
    _maxVelocity = maxVelocity;
  }

  public double getMaxVelocity() {
    return _maxVelocity;
  }
}
