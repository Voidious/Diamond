package voidious.gun;

import robocode.util.Utils;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import voidious.gun.GunDataManager.GunDataListener;
import voidious.utils.Wave;
import voidious.utils.DiaUtils;

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

public class VirtualGunsManager<T> implements GunDataListener {
  private static final double TYPICAL_ANGULAR_BOT_WIDTH = 0.1;
  private static final double TYPICAL_ESCAPE_RANGE = 0.9;

  private List<DuelGun<T>> _guns;
  private Map<DuelGun<T>, Map<String, GunStats>> _gunRatings;
  private PrintStream _out;

  public VirtualGunsManager(OutputStream out) {
    _guns = new ArrayList<DuelGun<T>>();
    _gunRatings = new HashMap<DuelGun<T>, Map<String, GunStats>>();
    _out = new PrintStream(out);
  }

  public void addGun(DuelGun<T> gun) {
    _guns.add(gun);
    _gunRatings.put(gun, new HashMap<String, GunStats>());
  }

  public List<DuelGun<T>> getGuns() {
    return _guns;
  }

  public boolean contains(DuelGun<T> gun) {
    return _guns.contains(gun);
  }

  public double getRating(DuelGun<T> gun, String botName) {
    if (_guns.contains(gun) && _gunRatings.get(gun).containsKey(botName)) {
      return _gunRatings.get(gun).get(botName).gunRating();
    }
    return 0;
  }

  public double getFormattedRating(DuelGun<T> gun, String botName) {
    return DiaUtils.round(getRating(gun, botName) * 100, 2);
  }

  public int getShotsFired(DuelGun<T> gun, String botName) {
    if (_guns.contains(gun) && _gunRatings.get(gun).containsKey(botName)) {
      return _gunRatings.get(gun).get(botName).shotsFired;
    }
    return 0;
  }

  public void fireVirtualBullets(Wave w) {
    for (DuelGun<T> gun : _guns) {
      GunStats stats;
      if (_gunRatings.get(gun).containsKey(w.botName)) {
        stats = _gunRatings.get(gun).get(w.botName);
      } else {
        stats = new GunStats();
        _gunRatings.get(gun).put(w.botName, stats);
      }

      double firingAngle = gun.aim(w, false);
      stats.virtualBullets.put(w, new VirtualBullet(firingAngle));
    }
  }

  public void registerWaveBreak(Wave w, double hitAngle, double tolerance) {
    for (DuelGun<T> gun : _guns) {
      GunStats stats = _gunRatings.get(gun).get(w.botName);
      VirtualBullet vb = stats.virtualBullets.get(w);

      double angularBotWidth = tolerance * 2;
      double hitWeight = (TYPICAL_ANGULAR_BOT_WIDTH / angularBotWidth)
          * (w.escapeAngleRange() / TYPICAL_ESCAPE_RANGE);
      double ux = Math.abs(Utils.normalRelativeAngle(vb.firingAngle - hitAngle))
          / tolerance;
      stats.shotsHit += hitWeight * Math.pow(1.6, -ux);

      stats.shotsFired++;
      stats.virtualBullets.remove(w);
    }
  }

  public void initRound() {
    for (DuelGun<T> gun : _guns) {
      for (GunStats stats : _gunRatings.get(gun).values()) {
        stats.virtualBullets.clear();
      }
      gun.clearCache();
    }
  }

  public DuelGun<T> bestGun(String botName) {
    DuelGun<T> bestGun = null;
    double bestRating = 0;

    for (DuelGun<T> gun : _guns) {
      double rating = 0;
      if (_gunRatings.get(gun).containsKey(botName)) {
        rating = _gunRatings.get(gun).get(botName).gunRating();
      }

      if (bestGun == null || rating > bestRating) {
        bestGun = gun;
        bestRating = rating;
      }
    }

    return bestGun;
  }

  public void printGunRatings(String botName) {
    _out.println("Virtual Gun ratings for " + botName + ":");
    for (DuelGun<T> gun : _guns) {
      if (_gunRatings.get(gun).containsKey(botName)) {
        double rating = _gunRatings.get(gun).get(botName).gunRating();
        _out.println("  " + gun.getLabel() + ": "
            + DiaUtils.round(rating * 100, 2));
      } else {
        _out.println("WARNING (gun): Never logged any Virtual Guns info for "
            + gun.getLabel());
      }
    }
  }

  @Override
  public void on1v1FiringWaveBreak(Wave w, double hitAngle, double tolerance) {
    registerWaveBreak(w, hitAngle, tolerance);
  }

  @Override
  public void onMarkFiringWave(Wave w) {
    fireVirtualBullets(w);
  }

  private static class GunStats {
    public int shotsFired;
    public double shotsHit;
    public Map<Wave, VirtualBullet> virtualBullets;

    public GunStats() {
      shotsFired = 0;
      shotsHit = 0;
      virtualBullets = new HashMap<Wave, VirtualBullet>();
    }

    public double gunRating() {
      return (shotsFired == 0) ? 0 : (shotsHit / shotsFired);
    }
  }

  private static class VirtualBullet {
    public final double firingAngle;

    public VirtualBullet(double firingAngle) {
      this.firingAngle = firingAngle;
    }
  }
}
