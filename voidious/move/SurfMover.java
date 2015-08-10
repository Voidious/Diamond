package voidious.move;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import robocode.AdvancedRobot;
import robocode.Rules;
import robocode.util.Utils;
import voidious.gfx.RoboGraphic;
import voidious.utils.BattleField;
import voidious.utils.DiaUtils;
import voidious.utils.KnnView;
import voidious.utils.MaxEscapeTarget;
import voidious.utils.MovementPredictor;
import voidious.utils.RobotState;
import voidious.utils.RobotStateLog;
import voidious.utils.RobotStateLog.AllStateListener;
import voidious.utils.Timestamped;
import voidious.utils.TimestampedGuessFactor;
import voidious.utils.Wave;
import voidious.utils.Wave.BulletShadow;
import voidious.utils.Wave.WavePosition;
import ags.utils.KdTree;

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

public class SurfMover {
  private static final double DESIRED_DISTANCE = 650;
  private static final double WALL_STICK = 160;
  private static final double MEA_WALL_STICK = 100;
  private static final double DISTANCING_DANGER_BASE = 2.5;
  private static final double MAX_ATTACK_ANGLE = Math.PI * .45;
  private static final double BASE_DANGER_FACTOR = 1.0;

  private static final Color SHADOW_COLOR = new Color(50, 255, 50);
  private static final int WAVES_TO_DRAW = 5;

  private AdvancedRobot _robot;
  private BattleField _battleField;
  private Collection<RoboGraphic> _renderables;
  private PrintStream _out;

  private SurfOption _lastSurfOption = SurfOption.CLOCKWISE;
  private Point2D.Double _lastSurfDestination;
  private Point2D.Double _stopDestination;
  private Map<SurfOption, Double> _surfOptionDangers;
  private Map<SurfOption, Point2D.Double> _surfOptionDestinations;
  private DistanceController _distancer;
  private Wave _lastWaveSurfed;
  private MovementPredictor _predictor;

  public SurfMover(AdvancedRobot robot, BattleField battleField,
      Collection<RoboGraphic> renderables, OutputStream out) {
    _robot = robot;
    _battleField = battleField;
    _renderables = renderables;
    _out = new PrintStream(out);

    _predictor = new MovementPredictor(battleField);
    _surfOptionDangers = new HashMap<SurfOption, Double>();
    _surfOptionDestinations = new HashMap<SurfOption, Point2D.Double>();
    _distancer = new DistanceController();
  }

  public void initRound() {
    _lastSurfDestination = null;
    _stopDestination = null;
  }

  public void move(RobotState myRobotState, MoveEnemy duelEnemy,
      int wavesToSurf, boolean painting) {
    if (duelEnemy == null) {
      return;
    }

    Point2D.Double myLocation = myRobotState.location;
    Wave surfWave = duelEnemy.findSurfableWave(Wave.FIRST_WAVE, myRobotState);

    if (surfWave == null) {
      orbit(myLocation, duelEnemy);
    } else {
      surf(myRobotState, duelEnemy, surfWave, wavesToSurf, painting);
    }
  }

  void orbit(Point2D.Double myLocation, MoveEnemy duelEnemy) {
    _robot.setMaxVelocity(8);
    RobotState enemyState = duelEnemy.lastScanState;
    double orbitAbsBearing =
        DiaUtils.absoluteBearing(enemyState.location, myLocation);
    double retreatAngle =
        _distancer.orbitAttackAngle(myLocation.distance(enemyState.location));
    double counterGoAngle = orbitAbsBearing
        + (SurfOption.COUNTER_CLOCKWISE.getDirection()
            * ((Math.PI / 2) + retreatAngle));
    counterGoAngle =
        wallSmoothing(myLocation, counterGoAngle, SurfOption.COUNTER_CLOCKWISE);

    double clockwiseGoAngle = orbitAbsBearing
        + (SurfOption.CLOCKWISE.getDirection()
            * ((Math.PI / 2) + retreatAngle));
    clockwiseGoAngle =
        wallSmoothing(myLocation, clockwiseGoAngle, SurfOption.CLOCKWISE);

    double goAngle;
    if (Math.abs(
            Utils.normalRelativeAngle(clockwiseGoAngle - orbitAbsBearing))
        < Math.abs(
            Utils.normalRelativeAngle(counterGoAngle - orbitAbsBearing))) {
      _lastSurfOption = SurfOption.CLOCKWISE;
      goAngle = clockwiseGoAngle;
    } else {
      _lastSurfOption = SurfOption.COUNTER_CLOCKWISE;
      goAngle = counterGoAngle;
    }

    DiaUtils.setBackAsFront(_robot, goAngle);
  }

  void surf(RobotState myRobotState, MoveEnemy duelEnemy, Wave surfWave,
      int wavesToSurf, boolean painting) {
    if (surfWave != _lastWaveSurfed) {
      duelEnemy.clearNeighborCache();
      _lastWaveSurfed = surfWave;
      _lastSurfDestination = null;
      _stopDestination = null;
    }

    boolean goingClockwise = (_lastSurfOption == SurfOption.CLOCKWISE);
    updateSurfDangers(myRobotState, duelEnemy, wavesToSurf, goingClockwise);
    double counterDanger = _surfOptionDangers.get(SurfOption.COUNTER_CLOCKWISE);
    double stopDanger = _surfOptionDangers.get(SurfOption.STOP);
    double clockwiseDanger = _surfOptionDangers.get(SurfOption.CLOCKWISE);

    Point2D.Double surfDestination;
    if (stopDanger <= counterDanger && stopDanger <= clockwiseDanger) {
      if (_stopDestination == null) {
        _stopDestination = _surfOptionDestinations.get(_lastSurfOption);
      }
      surfDestination = _stopDestination;
      _robot.setMaxVelocity(0);
      _lastSurfDestination = null;
    } else {
      _robot.setMaxVelocity(8);
      _lastSurfOption = (clockwiseDanger < counterDanger)
          ? SurfOption.CLOCKWISE : SurfOption.COUNTER_CLOCKWISE;
      surfDestination = _surfOptionDestinations.get(_lastSurfOption);
      _lastSurfDestination = surfDestination;
      _stopDestination = null;
    }

    double goAngle =
        DiaUtils.absoluteBearing(myRobotState.location, surfDestination);
    goAngle = wallSmoothing(myRobotState.location, goAngle, _lastSurfOption);

    if (painting) {
      duelEnemy.drawRawWaves(_robot.getTime());
      for (int x = 0; x < WAVES_TO_DRAW; x++) {
        drawWaveDangers(myRobotState, duelEnemy, x);
        drawWaveShadows(myRobotState, duelEnemy, x);
      }
    }

    DiaUtils.setBackAsFront(_robot, goAngle);
  }

  private void updateSurfDangers(RobotState myRobotState, MoveEnemy duelEnemy,
      int wavesToSurf, boolean goingClockwise) {
    List<SurfOption> surfOptions = getSortedSurfOptions();
    double bestSurfDanger = Double.POSITIVE_INFINITY;
    for (SurfOption testOption : surfOptions) {
      double testDanger = checkDanger(myRobotState, duelEnemy,
          myRobotState, testOption, goingClockwise, Wave.FIRST_WAVE,
          wavesToSurf, bestSurfDanger, new RobotStateLog());
      _surfOptionDangers.put(testOption, testDanger);
      bestSurfDanger = Math.min(bestSurfDanger, testDanger);
    }
  }

  List<SurfOption> getSortedSurfOptions() {
    List<SurfOption> surfOptions = Arrays.asList(SurfOption.values());
    for (int x = 0; x < surfOptions.size(); x++) {
      double lowestDanger = getSurfOptionDanger(surfOptions.get(x));
      for (int y = x + 1; y < surfOptions.size(); y++) {
        if (getSurfOptionDanger(surfOptions.get(y)) < lowestDanger) {
          lowestDanger = getSurfOptionDanger(surfOptions.get(y));
          SurfOption temp = surfOptions.get(x);
          surfOptions.set(x, surfOptions.get(y));
          surfOptions.set(y, temp);
        }
      }
    }
    return surfOptions;
  }

  private double getSurfOptionDanger(SurfOption surfOption) {
    if (_surfOptionDangers.containsKey(surfOption)) {
      return _surfOptionDangers.get(surfOption);
    } else {
      return 0;
    }
  }

  // TODO: make previouslyMovingClockwise either SurfOption or int
  double checkDanger(RobotState myRobotState, MoveEnemy duelEnemy,
      RobotState startState, SurfOption surfOption,
      boolean previouslyMovingClockwise, int surfWaveIndex, int numWavesToSurf,
      double cutoffDanger, RobotStateLog predictedStateLog) {
    Wave surfWave = duelEnemy.findSurfableWave(surfWaveIndex, myRobotState);
    if (surfWave == null) {
      return 0;
    }

    List<RobotState> dangerStates = new ArrayList<RobotState>();
    WavePosition startWavePosition = surfWave.checkWavePosition(startState);
    if (surfWaveIndex > Wave.FIRST_WAVE
        && startWavePosition != WavePosition.MIDAIR) {
      dangerStates.addAll(
          replaySurfStates(surfWave, predictedStateLog, startState));
    }

    if (startWavePosition == WavePosition.GONE && dangerStates.isEmpty()) {
      return 0;
    }

    boolean predictClockwise =
        predictClockwise(surfOption, previouslyMovingClockwise);

    RobotState predictedState = startState;
    RobotState passedState = startState;

    boolean wavePassed = false;
    boolean waveHit = false;
    double maxVelocity;
    SurfOption smoothingSurfOption;
    if (surfOption == SurfOption.STOP) {
      maxVelocity = 0;
      smoothingSurfOption = predictClockwise
          ? SurfOption.CLOCKWISE : SurfOption.COUNTER_CLOCKWISE;
    } else {
      maxVelocity = 8;
      smoothingSurfOption = surfOption;
    }
    Point2D.Double surfDestination;
    if (surfWaveIndex == Wave.FIRST_WAVE && surfOption == SurfOption.STOP
        && _stopDestination != null) {
      surfDestination = _stopDestination;
    } else {
      surfDestination = surfDestination(
          surfWave, surfWaveIndex, startState, smoothingSurfOption);
    }
    if (surfWaveIndex == Wave.FIRST_WAVE) {
      _surfOptionDestinations.put(surfOption, surfDestination);
    }

    do {
      if (!waveHit && surfWave.checkWavePosition(
              predictedState, WavePosition.BREAKING_FRONT)
                  == WavePosition.BREAKING_FRONT) {
        RobotState dangerState = predictedState;
        do {
          dangerStates.add(dangerState);
          dangerState = predictSurfLocation(
              dangerState, surfDestination, 0, smoothingSurfOption);
        } while (
            surfWave.checkWavePosition(dangerState, true) != WavePosition.GONE);
        waveHit = true;
      }

      WavePosition wavePosition =
          surfWave.checkWavePosition(predictedState, true);
      if (wavePosition == WavePosition.BREAKING_CENTER
          || wavePosition == WavePosition.GONE) {
        passedState = predictedState;
        wavePassed = true;
      } else {
        predictedStateLog.addState(predictedState);
        predictedState = predictSurfLocation(
            predictedState, surfDestination, maxVelocity, smoothingSurfOption);
      }
    } while (!wavePassed);

    Wave.Intersection intersection = surfWave.preciseIntersection(dangerStates);
    double baseDangerScore =
        normalizedEnemyHitRate(duelEnemy) * BASE_DANGER_FACTOR;
    double danger = baseDangerScore + getDangerScore(
        duelEnemy, surfWave, intersection, surfWaveIndex);
    danger *= surfWave.shadowFactor(intersection);
    danger *= Rules.getBulletDamage(surfWave.bulletPower());
    double currentDistanceToWaveSource =
        myRobotState.location.distance(surfWave.sourceLocation);
    double currentDistanceToWave = currentDistanceToWaveSource
        - surfWave.distanceTraveled(_robot.getTime());
    double timeToImpact =
        Math.max(1, currentDistanceToWave / surfWave.bulletSpeed());
    danger /= timeToImpact;

    danger *= distancingDanger(startState.location, passedState.location,
        duelEnemy.lastScanState.location);

    if (surfWaveIndex + 1 < numWavesToSurf && danger < cutoffDanger) {
      double nextCounterClockwiseDanger = checkDanger(myRobotState, duelEnemy,
          passedState, SurfOption.COUNTER_CLOCKWISE, predictClockwise,
          surfWaveIndex + 1, numWavesToSurf, cutoffDanger,
          (RobotStateLog) predictedStateLog.clone());
      double nextStopDanger = checkDanger(myRobotState, duelEnemy, passedState,
          SurfOption.STOP, predictClockwise, surfWaveIndex + 1, numWavesToSurf,
          cutoffDanger, (RobotStateLog) predictedStateLog.clone());
      double nextClockwiseDanger = checkDanger(myRobotState, duelEnemy,
          passedState, SurfOption.CLOCKWISE, predictClockwise,
          surfWaveIndex + 1, numWavesToSurf, cutoffDanger,
          (RobotStateLog) predictedStateLog.clone());

      danger += Math.min(nextCounterClockwiseDanger,
                         Math.min(nextStopDanger, nextClockwiseDanger));
    }

    return danger;
  }

  private boolean predictClockwise(
      SurfOption surfOption, boolean previouslyMovingClockwise) {
    if (surfOption == SurfOption.STOP) {
      return previouslyMovingClockwise;
    } else {
      return (surfOption == SurfOption.CLOCKWISE);
    }
  }

  List<RobotState> replaySurfStates(final Wave surfWave,
      RobotStateLog predictedStateLog, RobotState startState) {
    final List<RobotState> dangerStates = new ArrayList<RobotState>();
    predictedStateLog.forAllStates(new AllStateListener() {
      @Override
      public void onRobotState(RobotState state) {
        WavePosition pastWavePosition = surfWave.checkWavePosition(state);
        if (pastWavePosition.isBreaking()) {
          dangerStates.add(state);
        }
      }
    });
    return dangerStates;
  }

  private Point2D.Double surfDestination(Wave surfWave,
      int surfWaveIndex, RobotState startState, SurfOption surfOption) {
    if (surfWaveIndex == Wave.FIRST_WAVE && _lastSurfOption == surfOption
        && _lastSurfDestination != null) {
      return _lastSurfDestination;
    }

    double attackAngle = _distancer.surfAttackAngle(
        surfWave.sourceLocation.distance(startState.location));
    MaxEscapeTarget meaTarget = _predictor.preciseEscapeAngle(
        surfOption.getDirection(), surfWave.sourceLocation, surfWave.fireTime,
        surfWave.bulletSpeed(), startState, attackAngle, MEA_WALL_STICK);
    return meaTarget.location;
  }

  RobotState predictSurfLocation(RobotState robotState,
      Point2D.Double surfDestination, double maxVelocity,
      SurfOption smoothingSurfOption) {
    double goAngle = wallSmoothing(robotState.location,
        DiaUtils.absoluteBearing(robotState.location, surfDestination),
        smoothingSurfOption);
    return _predictor.nextLocation(robotState, maxVelocity, goAngle, false);
  }

  private double wallSmoothing(Point2D.Double startLocation,
      double goAngleRadians, SurfOption surfOption) {
    return _battleField.wallSmoothing(
        startLocation, goAngleRadians, surfOption.getDirection(), WALL_STICK);
  }

  double distancingDanger(Point2D.Double startLocation,
      Point2D.Double predictedLocation, Point2D.Double enemyLocation) {
    double distanceToEnemy = enemyLocation.distance(startLocation);
    double predictedDistanceToEnemy = enemyLocation.distance(predictedLocation);

    double distanceQuotient = distanceToEnemy / predictedDistanceToEnemy;

    return Math.pow(DISTANCING_DANGER_BASE, distanceQuotient)
        / DISTANCING_DANGER_BASE;
  }

  double getDangerScore(MoveEnemy duelEnemy, Wave w,
        Point2D.Double dangerLocation, int surfWaveIndex) {
    Wave.Intersection intersection = new Wave.Intersection(
        DiaUtils.absoluteBearing(w.sourceLocation, dangerLocation), 0.05);
    return getDangerScore(duelEnemy, w, intersection, surfWaveIndex);
  }

  double getDangerScore(MoveEnemy duelEnemy, Wave w,
      Wave.Intersection intersection, int surfWaveIndex) {
    double dangerAngle = intersection.angle;
    double bandwidth = intersection.bandwidth;
    double totalDanger = 0;
    double totalScanWeight = 0;
    int enabledSize = 0;
    double hitPercentage = normalizedEnemyHitPercentage(duelEnemy);
    double marginOfError = hitPercentageMarginOfError(duelEnemy);
    for (KnnView<TimestampedGuessFactor> view : duelEnemy.views.values()) {
      if (view.enabled(hitPercentage, marginOfError)) {
        enabledSize += view.size();
        List<KdTree.Entry<TimestampedGuessFactor>> nearestNeighbors =
            getNearestNeighbors(view, w, surfWaveIndex);
        Map<Timestamped, Double> weightMap =
            view.getDecayWeights(nearestNeighbors);

        double density = 0;
        double viewScanWeight = 0;
        int numScans = nearestNeighbors.size();
        for (int x = 0; x < numScans; x++) {
          TimestampedGuessFactor tsgf = nearestNeighbors.get(x).value;
          double scanWeight =
              weightMap.get(tsgf) / Math.sqrt(nearestNeighbors.get(x).distance);
          double xFiringAngle = DiaUtils.normalizeAngle(
              w.firingAngle(tsgf.guessFactor), dangerAngle);
          if (!w.shadowed(xFiringAngle)) {
            double ux = (xFiringAngle - dangerAngle) / bandwidth;
            density += scanWeight * Math.pow(2, -Math.abs(ux));
          }
          viewScanWeight += scanWeight;
        }
        totalScanWeight += viewScanWeight * view.weight;
        totalDanger += view.weight * density;
      }
    }

    if (enabledSize == 0) {
      return defaultDanger(w, intersection);
    }

    return totalDanger / totalScanWeight;
  }

  private List<KdTree.Entry<TimestampedGuessFactor>> getNearestNeighbors(
      KnnView<TimestampedGuessFactor> view, Wave w, int surfWaveIndex) {
    if (!view.cachedNeighbors.containsKey(surfWaveIndex)) {
      view.cachedNeighbors.put(surfWaveIndex, view.nearestNeighbors(w, false));
    }
    return view.cachedNeighbors.get(surfWaveIndex);
  }

  // TODO: move these to MoveEnemy
  double normalizedEnemyHitRate(MoveEnemy duelEnemy) {
    return (duelEnemy == null || duelEnemy.raw1v1ShotsFired == 0)
        ? 0 : (((double) duelEnemy.weighted1v1ShotsHit)
            / duelEnemy.raw1v1ShotsFired);
  }

  double normalizedEnemyHitPercentage(MoveEnemy duelEnemy) {
    return 100 * normalizedEnemyHitRate(duelEnemy);
  }

  double rawEnemyHitPercentage(MoveEnemy duelEnemy) {
    return (duelEnemy == null || duelEnemy.raw1v1ShotsFired == 0)
        ? 0 : ((((double) duelEnemy.raw1v1ShotsHit)
            / duelEnemy.raw1v1ShotsFired) * 100.0);
  }

  double hitPercentageMarginOfError(MoveEnemy duelEnemy) {
    if (duelEnemy == null) {
      return 100;
    } else {
      double hitProbability = normalizedEnemyHitRate(duelEnemy);
      return 100 *
          DiaUtils.marginOfError(hitProbability, duelEnemy.raw1v1ShotsFired);
    }
  }

  private static double defaultDanger(Wave w, Wave.Intersection intersection) {
    double[] guessFactors = new double[]{0, 0.85};
    double[] weights = new double[]{3, 1};
    double danger = 0;
    for (int x = 0; x < guessFactors.length; x++) {
      double firingAngle = w.firingAngle(guessFactors[x]);
      double ux = (firingAngle
          - DiaUtils.normalizeAngle(intersection.angle, firingAngle))
              / intersection.bandwidth;
      danger += weights[x] * Math.pow(2, -Math.abs(ux));
    }
    return danger;
  }

  public void roundOver(MoveEnemy duelEnemy) {
    _out.println("Enemy normalized hit %: "
        + DiaUtils.round(normalizedEnemyHitPercentage(duelEnemy), 2) + "\n"
        + "Enemy raw hit %: "
        + DiaUtils.round(rawEnemyHitPercentage(duelEnemy), 2));
  }

  Map<SurfOption, Double> getSurfOptionDangers() {
    return _surfOptionDangers;
  }

  private void drawWaveDangers(
      RobotState myRobotState, MoveEnemy duelEnemy, int surfWaveIndex) {
    Wave surfWave = duelEnemy.findSurfableWave(surfWaveIndex, myRobotState);
    if (surfWave == null) {
      return;
    }

    int numBins = 51;
    double halfBins = (double)(numBins - 1) / 2;

    double[] gfDangers = new double[numBins];
    boolean[] gfShadowed = new boolean[numBins];
    double minDanger = Double.POSITIVE_INFINITY;

    long currentTime = _robot.getTime();
    for (int x = 0; x <= numBins - 1; x++) {
      double gf = (x - halfBins) / halfBins;
      double bearingOffset =
          surfWave.orbitDirection * (gf * surfWave.maxEscapeAngle());
      double absFiringAngle = surfWave.absBearing + bearingOffset;
      Point2D.Double dangerLocation = DiaUtils.project(
          surfWave.sourceLocation, absFiringAngle,
          surfWave.distanceTraveled(currentTime));
      if (surfWave.shadowed(absFiringAngle)) {
        gfShadowed[x] = true;
        gfDangers[x] = 0;
      } else {
        gfShadowed[x] = false;
        gfDangers[x] = getDangerScore(
            duelEnemy, surfWave, dangerLocation, surfWaveIndex);
      }

      if (gfDangers[x] < minDanger) {
        minDanger = gfDangers[x];
      }
    }

    double avg = DiaUtils.average(gfDangers);
    double stDev = DiaUtils.standardDeviation(gfDangers);

    for (int x = 0; x <= numBins - 1; x++) {
      double gf = (x - halfBins) / halfBins;
      double bearingOffset =
          surfWave.orbitDirection * (gf * surfWave.maxEscapeAngle());
      Point2D.Double drawLocation = DiaUtils.project(
          surfWave.sourceLocation, surfWave.absBearing + bearingOffset,
          surfWave.distanceTraveled(currentTime + 1) - 15);

      Color binColor = RoboGraphic.riskColor(
          gfDangers[x] - minDanger, avg - minDanger, stDev, false, 1);
      _renderables.add(RoboGraphic.drawCircleFilled(drawLocation,
          gfShadowed[x] ? SHADOW_COLOR : binColor, 2));
    }
  }

  private void drawWaveShadows(
      RobotState myRobotState, MoveEnemy duelEnemy, int surfWaveIndex) {
    Wave surfWave = duelEnemy.findSurfableWave(surfWaveIndex, myRobotState);
    if (surfWave == null) {
      return;
    }

    long currentTime = _robot.getTime();
    for (BulletShadow shadow : surfWave.shadows) {
      Point2D.Double p1 = DiaUtils.project(surfWave.sourceLocation,
          shadow.minAngle, surfWave.distanceTraveled(currentTime + 1));
      Point2D.Double p2 = DiaUtils.project(surfWave.sourceLocation,
          shadow.maxAngle, surfWave.distanceTraveled(currentTime + 1));
      _renderables.add(RoboGraphic.drawLine(p1, p2, SHADOW_COLOR));
    }
  }

  enum SurfOption {
    COUNTER_CLOCKWISE(-1),
    STOP(0),
    CLOCKWISE(1);

    private int _direction;

    private SurfOption(int direction) {
      _direction = direction;
    }

    public int getDirection() {
      return _direction;
    }
  }

  private static class DistanceController {
    public double surfAttackAngle(double currentDistance) {
      return attackAngle(currentDistance, 0.6);
    }

    public double orbitAttackAngle(double currentDistance) {
      return attackAngle(currentDistance, 1.65);
    }

    private double attackAngle(
        double currentDistance, double offsetMultiplier) {
      double distanceFactor =
          (currentDistance - DESIRED_DISTANCE) / DESIRED_DISTANCE;
      return DiaUtils.limit(-MAX_ATTACK_ANGLE,
                            distanceFactor * offsetMultiplier,
                            MAX_ATTACK_ANGLE);
    }
  }
}
