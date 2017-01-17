/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.gpem17.eval;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verifyNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.joda.time.Duration;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.format.PeriodFormat;

import com.github.rinde.ecj.GPFunc;
import com.github.rinde.ecj.GPProgram;
import com.github.rinde.ecj.GPProgramParser;
import com.github.rinde.evo4mas.common.GpGlobal;
import com.github.rinde.gpem17.GPEM17;
import com.github.rinde.gpem17.GPEM17.ReauctOpt;
import com.github.rinde.gpem17.GPEM17.RpOpt;
import com.github.rinde.gpem17.evo.FunctionSet;
import com.github.rinde.logistics.pdptw.mas.TruckFactory.DefaultTruckFactory;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionCommModel;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionStopConditions;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionTimeStatsLogger;
import com.github.rinde.logistics.pdptw.mas.comm.DoubleBid;
import com.github.rinde.logistics.pdptw.mas.comm.RtSolverBidder;
import com.github.rinde.logistics.pdptw.mas.comm.RtSolverBidder.BidFunction;
import com.github.rinde.logistics.pdptw.mas.comm.RtSolverBidder.BidFunctions;
import com.github.rinde.logistics.pdptw.mas.route.RoutePlannerStatsLogger;
import com.github.rinde.logistics.pdptw.mas.route.RtSolverRoutePlanner;
import com.github.rinde.logistics.pdptw.solver.optaplanner.OptaplannerSolvers;
import com.github.rinde.rinsim.central.rt.RtSolverModel;
import com.github.rinde.rinsim.core.model.time.RealtimeClockLogger;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.experiment.CommandLineProgress;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.experiment.PostProcessor.FailureStrategy;
import com.github.rinde.rinsim.experiment.SimulationProperty;
import com.github.rinde.rinsim.io.FileProvider;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.pdptw.common.ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.ScenarioIO;
import com.github.rinde.rinsim.scenario.StopConditions;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06ObjectiveFunction;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.io.Files;

/**
 * 
 * @author Rinde van Lon
 */
public class Evaluate {
  static final String DATASET_PATH = "files/vanLonHolvoet15/";
  static final String RT_RESULTS_DIR = "files/results/realtime/";
  static final String ST_RESULTS_DIR = "files/results/simtime/";

  public static void main(String[] args) {
    System.out.println(System.getProperty("java.vm.name") + ", "
      + System.getProperty("java.vm.vendor") + ", "
      + System.getProperty("java.vm.version") + " (runtime version: "
      + System.getProperty("java.runtime.version") + ")");
    System.out.println(System.getProperty("os.name") + " "
      + System.getProperty("os.version") + " "
      + System.getProperty("os.arch"));
    checkArgument(System.getProperty("java.vm.name").contains("Server"),
      "Experiments should be run in a JVM in server mode.");
    checkArgument(args.length >= 1
      && (args[0].equals("simtime") || args[0].equals("realtime")),
      "The first argument should be 'simtime' or 'realtime', found '%s'.",
      args[0]);
    boolean realtime = args[0].equals("realtime");
    checkArgument(args.length >= 2,
      "The second argument should be a path to a txt file containing the "
        + "programs to evaluate.");

    Collection<GPFunc<GpGlobal>> funcs = new FunctionSet().create();
    File programPath = new File(args[1]);
    checkArgument(programPath.exists(), "%s does not exist.", programPath);
    List<String> lines;
    try {
      lines = Files.readLines(programPath, Charsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed reading " + programPath, e);
    }
    List<GPProgram<GpGlobal>> programs = new ArrayList<>();
    for (String line : lines) {
      programs.add(GPProgramParser.parseProgramFunc(line, funcs));
    }

    checkArgument(args.length >= 3
      && (args[2].equals("EVO") || args[2].equals("CIH")),
      "The third argument should be 'EVO' or 'CIH', found '%s'.", args[2]);
    ReauctOpt reauctOpt = ReauctOpt.valueOf(args[2]);

    checkArgument(args.length >= 4,
      "The fourth argument should be the objective function weights: "
        + "'tt-td-ot'.");
    Gendreau06ObjectiveFunction objectiveFunction =
      GPEM17.parseObjFuncWeights(args[3]);

    checkArgument(args.length >= 5
      && (args[4].equals("OptaPlanner") || args[4].equals("CIH")),
      "The fifth argument should be 'CIH' or 'OptaPlanner', found '%s'.",
      args[4]);

    RpOpt rpOpt;
    if (args[4].equals("OptaPlanner")) {
      rpOpt = RpOpt.OPTA_PLANNER;
    } else {
      rpOpt = RpOpt.CIH;
    }

    checkArgument(args.length >= 6
      && (args[5].equals("EnableTimeMeasurements")
        || args[5].equals("DisableTimeMeasurements")),
      "The fifth argument should be 'EnableTimeMeasurements' or "
        + "'DisableTimeMeasurements', found '%s'.",
      args[5]);
    boolean enableTimeMeasurements = args[5].equals("EnableTimeMeasurements");

    Pattern compDelayRegex = Pattern.compile("heuristic-comp-delay:(\\d+)ms");
    Matcher matcher = compDelayRegex.matcher(args[6]);
    checkArgument(args.length >= 7 && matcher.matches(),
      "The sixth argument should match regex: '%s'.", compDelayRegex.pattern());
    long heuristicCompDelay = Long.parseLong(matcher.group(1));

    Pattern optaPlannerRegex =
      Pattern.compile("run-optaplanner-mas:(true|false)");
    Matcher optaPlannerMatcher = optaPlannerRegex.matcher(args[7]);
    checkArgument(args.length >= 8 && optaPlannerMatcher.matches(),
      "The seventh argument should match regex: '%s'.",
      optaPlannerRegex.pattern());
    boolean useOptaPlannerMAS =
      Boolean.parseBoolean(optaPlannerMatcher.group(1));

    final String[] expArgs = new String[args.length - 8];
    System.arraycopy(args, 8, expArgs, 0, args.length - 8);
    File resDir =
      realtime ? new File(RT_RESULTS_DIR) : new File(ST_RESULTS_DIR);

    FileProvider.Builder files = FileProvider.builder()
      .add(Paths.get(DATASET_PATH))
      .filter("regex:.*\\.scen");

    Function<Scenario, Scenario> conv =
      realtime ? null : ScenarioConverter.TO_ONLINE_SIMULATED_250;
    execute(programs, realtime, files, resDir, true, conv, true, reauctOpt,
      objectiveFunction, rpOpt, enableTimeMeasurements, useOptaPlannerMAS,
      heuristicCompDelay,
      expArgs);

  }

  // objFunc is only used at runtime, not for analysis
  public static ExperimentResults execute(
      Iterable<GPProgram<GpGlobal>> programs,
      boolean realtime,
      FileProvider.Builder scenarioFiles,
      File resDir,
      boolean createTimeStampedResDir,
      @Nullable Function<Scenario, Scenario> scenarioConverter,
      boolean createTmpFiles,
      ReauctOpt reauctOpt,
      Gendreau06ObjectiveFunction objFuncUsedAtRuntime,
      RpOpt routePlanner,
      boolean enableTimeMeasurements,
      boolean addOptaPlannerMAS,
      long heuristicComputationDelay,
      String... expArgs) {
    checkArgument(realtime ^ scenarioConverter != null);
    final long startTime = System.currentTimeMillis();

    if (createTimeStampedResDir) {
      resDir = createExperimentDir(resDir);
    } else {
      resDir.mkdirs();
    }

    boolean evolution = scenarioConverter != null;

    ResultWriter rw = new VanLonHolvoetResultWriter(resDir, GPEM17.OBJ_FUNC,
      scenarioFiles.build().get().iterator().next().getParent().toString(),
      realtime, true, createTmpFiles, evolution);
    Experiment.Builder exp = Experiment.builder()
      .addScenarios(scenarioFiles)
      .showGui(GPEM17.gui())
      .showGui(false)
      .usePostProcessor(
        new GpemPostProcessor(GPEM17.OBJ_FUNC, evolution
          ? FailureStrategy.INCLUDE : FailureStrategy.RETRY, false))
      .computeLocal()
      .withRandomSeed(123)
      .repeat(3)
      // SEED_REPS,REPS,SCENARIO,CONFIG
      .withOrdering(
        SimulationProperty.SEED_REPS,
        SimulationProperty.REPS,
        SimulationProperty.SCENARIO,
        SimulationProperty.CONFIG)

      .addResultListener(rw);

    if (!realtime) {
      exp.addResultListener(new SimRuntimeLogger(resDir));
    }
    if (realtime) {
      exp.setScenarioReader(
        ScenarioIO.readerAdapter(ScenarioConverter.TO_ONLINE_REALTIME_250))
        .withWarmup(30000)
        .addResultListener(new CommandLineProgress(System.out))
        .withThreads((int) Math
          .floor((Runtime.getRuntime().availableProcessors() - 1) / 2d));
    } else if (scenarioConverter == null) {
      exp.setScenarioReader(
        ScenarioIO.readerAdapter(ScenarioConverter.TO_ONLINE_SIMULATED_250))
        .addResultListener(new CommandLineProgress(System.out));
    } else {
      exp.setScenarioReader(
        ScenarioIO.readerAdapter(scenarioConverter));
    }

    if (addOptaPlannerMAS) {
      exp.addConfiguration(createOptaPlanner(enableTimeMeasurements));
    }

    int counter = 0;
    StringBuilder sb = new StringBuilder();
    for (GPProgram<GpGlobal> prog : programs) {
      String progId = "c" + counter++;
      sb.append(progId)
        .append(" = ")
        .append(prog.getId())
        .append(System.lineSeparator());

      if (realtime) {
        exp.addConfiguration(
          GPEM17.createRtConfig(prog, progId, reauctOpt, objFuncUsedAtRuntime,
            routePlanner, enableTimeMeasurements, heuristicComputationDelay));
      } else {
        exp.addConfiguration(
          GPEM17.createStConfig(prog, progId, reauctOpt, objFuncUsedAtRuntime,
            enableTimeMeasurements));
      }
    }

    try {
      Files.write(sb, new File(rw.getExperimentDirectory(), "configs.txt"),
        Charsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    final Optional<ExperimentResults> results =
      exp.perform(System.out, expArgs);
    final long duration = System.currentTimeMillis() - startTime;
    if (!results.isPresent()) {
      return null;
    }

    final Duration dur = new Duration(startTime, System.currentTimeMillis());
    System.out.println("Done, computed " + results.get().getResults().size()
      + " simulations in " + duration / 1000d + "s ("
      + PeriodFormat.getDefault().print(dur.toPeriod()) + ")");
    return results.get();
  }

  /**
   * @param enableTimeMeasurements
   * @return
   */
  private static MASConfiguration createOptaPlanner(
      boolean enableTimeMeasurements) {

    // using settings from JAAMAS paper
    final long rpMs = 100L;
    final long bMs = 20L;
    final long maxAuctionDurationSoft = 10000L;
    final String masSolverName =
      "Step-counting-hill-climbing-with-entity-tabu-and-strategic-oscillation";

    OptaplannerSolvers.Builder b = OptaplannerSolvers.builder()
      .withSolverXmlResource("com/github/rinde/jaamas16/jaamas-solver.xml")
      .withObjectiveFunction(GPEM17.OBJ_FUNC)
      .withName(masSolverName);

    return createMAS(b, GPEM17.OBJ_FUNC, rpMs, bMs, maxAuctionDurationSoft,
      false, 0L, enableTimeMeasurements);

    // MASConfiguration.Builder builder = MASConfiguration.pdptwBuilder()
    // .setName(
    // "ReAuction-FFD-" + masSolverName + "-RP-" + rpMs + "-BID-" + bMs
    // + "-" + bf)
    // .addEventHandler(AddVehicleEvent.class,
    // DefaultTruckFactory.builder()
    // .setRoutePlanner(
    // // RtSolverRoutePlanner.supplier(RtStAdapters.toRealtime(
    // // CheapestInsertionHeuristic.supplier(GPEM17.OBJ_FUNC)))
    //
    // RtSolverRoutePlanner.supplier(
    // b.withUnimprovedMsLimit(rpMs)
    // .buildRealtimeSolverSupplier())
    // //
    // )
    // .setCommunicator(RtSolverBidder.realtimeBuilder(GPEM17.OBJ_FUNC,
    // b.withUnimprovedMsLimit(bMs)
    // .withTimeMeasurementsEnabled(enableTimeMeasurements)
    // .buildRealtimeSolverSupplier())
    // .withBidFunction(bf)
    // .withReauctionCooldownPeriod(0))
    //
    // .setLazyComputation(false)
    // .setRouteAdjuster(RouteFollowingVehicle.delayAdjuster())
    // .build())
    // .addModel(AuctionCommModel.builder(DoubleBid.class)
    // .withStopCondition(
    // AuctionStopConditions.and(
    // AuctionStopConditions.<DoubleBid>atLeastNumBids(2),
    // AuctionStopConditions.<DoubleBid>or(
    // AuctionStopConditions.<DoubleBid>allBidders(),
    // AuctionStopConditions.<DoubleBid>maxAuctionDuration(5000))))
    // .withMaxAuctionDuration(30 * 60 * 1000L))
    // .addModel(RtSolverModel.builder()
    // .withThreadPoolSize(3)
    // .withThreadGrouping(true))
    // .addModel(RealtimeClockLogger.builder());
    //
    // if (enableTimeMeasurements) {
    // builder.addModel(AuctionTimeStatsLogger.builder());
    // }
    // return builder.build();
  }

  static MASConfiguration createMAS(OptaplannerSolvers.Builder opFfdFactory,
      ObjectiveFunction objFunc, long rpMs, long bMs,
      long maxAuctionDurationSoft, boolean enableReauctions,
      long reauctCooldownPeriodMs, boolean computationsLogging) {
    final BidFunction bf = BidFunctions.BALANCED_HIGH;
    final String masSolverName =
      "Step-counting-hill-climbing-with-entity-tabu-and-strategic-oscillation";

    final String suffix;
    if (false == enableReauctions) {
      suffix = "-NO-REAUCT";
    } else if (reauctCooldownPeriodMs > 0) {
      suffix = "-reauctCooldownPeriod-" + reauctCooldownPeriodMs;
    } else {
      suffix = "";
    }

    MASConfiguration.Builder b = MASConfiguration.pdptwBuilder()
      .setName(
        "ReAuction-FFD-" + masSolverName + "-RP-" + rpMs + "-BID-" + bMs + "-"
          + bf + suffix)
      .addEventHandler(AddVehicleEvent.class,
        DefaultTruckFactory.builder()
          .setRoutePlanner(RtSolverRoutePlanner.supplier(
            opFfdFactory.withSolverKey(masSolverName)
              .withUnimprovedMsLimit(rpMs)
              .withTimeMeasurementsEnabled(computationsLogging)
              .buildRealtimeSolverSupplier()))
          .setCommunicator(

            RtSolverBidder.realtimeBuilder(objFunc,
              opFfdFactory.withSolverKey(masSolverName)
                .withUnimprovedMsLimit(bMs)
                .withTimeMeasurementsEnabled(computationsLogging)
                .buildRealtimeSolverSupplier())
              .withBidFunction(bf)
              .withReauctionsEnabled(enableReauctions)
              .withReauctionCooldownPeriod(reauctCooldownPeriodMs))
          .setLazyComputation(false)
          .setRouteAdjuster(RouteFollowingVehicle.delayAdjuster())
          .build())
      .addModel(AuctionCommModel.builder(DoubleBid.class)
        .withStopCondition(
          AuctionStopConditions.and(
            AuctionStopConditions.<DoubleBid>atLeastNumBids(2),
            AuctionStopConditions.<DoubleBid>or(
              AuctionStopConditions.<DoubleBid>allBidders(),
              AuctionStopConditions
                .<DoubleBid>maxAuctionDuration(maxAuctionDurationSoft))))
        .withMaxAuctionDuration(30 * 60 * 1000L))
      .addModel(RtSolverModel.builder()
        .withThreadPoolSize(3)
        .withThreadGrouping(true))
      .addModel(RealtimeClockLogger.builder());

    if (computationsLogging) {
      b = b.addModel(AuctionTimeStatsLogger.builder())
        .addModel(RoutePlannerStatsLogger.builder());
    }

    return b.build();
  }

  static File createExperimentDir(File target) {
    final String timestamp = ISODateTimeFormat.dateHourMinuteSecond()
      .print(System.currentTimeMillis()).replace(":", "");
    final File experimentDirectory = new File(target, timestamp);
    experimentDirectory.mkdirs();

    final File latest = new File(target, "latest/");
    if (latest.exists()) {
      checkState(latest.delete());
    }
    try {
      java.nio.file.Files.createSymbolicLink(
        latest.toPath(),
        experimentDirectory.getAbsoluteFile().toPath());
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
    return experimentDirectory;
  }

  enum ScenarioConverter implements Function<Scenario, Scenario> {
    /**
     * Changes ticksize to 250ms and adds stopcondition with maximum sim time of
     * 10 hours.
     */
    TO_ONLINE_REALTIME_250 {
      @Override
      public Scenario apply(@Nullable Scenario input) {
        final Scenario s = verifyNotNull(input);
        return Scenario.builder(s)
          .removeModelsOfType(TimeModel.AbstractBuilder.class)
          .addModel(TimeModel.builder().withTickLength(250).withRealTime())
          .setStopCondition(s.getStopCondition()).build();
      }
    },
    TO_ONLINE_SIMULATED_250 {
      @Override
      public Scenario apply(@Nullable Scenario input) {
        final Scenario s = verifyNotNull(input);
        return Scenario.builder(s)
          .removeModelsOfType(TimeModel.AbstractBuilder.class)
          .addModel(TimeModel.builder().withTickLength(250))
          .setStopCondition(StopConditions.or(s.getStopCondition(),
            StopConditions.limitedTime(10 * 60 * 60 * 1000)))
          .build();
      }
    }
  }
}
