package voidious.utils;

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

abstract public class Timestamped implements Comparable<Timestamped>  {
  public final int round;
  public final long time;

  public Timestamped(int roundNum, long time) {
    this.round = roundNum;
    this.time = time;
  }

  @Override
  public int compareTo(Timestamped that) {
    if (this.round < that.round) {
      return -1;
    } else if (this.round == that.round) {
      if (this.time < that.time) {
        return -1;
      } else if (this.time == that.time) {
        return 0;
      } else {
        return 1;
      }
    } else {
      return 1;
    }
  }
}
