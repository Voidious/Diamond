package voidious.gun;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import voidious.utils.KnnView;
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

public abstract class DuelGun<T> {
  private Map<Wave, Double> _firingAngles;

  public DuelGun() {
    _firingAngles = new HashMap<Wave, Double>();
  }

  public void clearCache() {
    _firingAngles.clear();
  }

  public double aim(Wave w, boolean painting) {
    if (!_firingAngles.containsKey(w)) {
      _firingAngles.put(w, aimInternal(w, painting));
    }
    return _firingAngles.get(w);
  }

  abstract public String getLabel();

  abstract protected double aimInternal(Wave w, boolean painting);

  abstract public List<KnnView<T>> newDataViews();
}
