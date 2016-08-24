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
import static com.google.common.base.Verify.verifyNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.joda.time.Duration;
import org.joda.time.format.PeriodFormat;

import com.github.rinde.ecj.GPFunc;
import com.github.rinde.ecj.GPProgram;
import com.github.rinde.ecj.GPProgramParser;
import com.github.rinde.evo4mas.common.GlobalStateObjectFunctions.GpGlobal;
import com.github.rinde.gpem17.GPEM17;
import com.github.rinde.gpem17.evo.FunctionSet;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.experiment.CommandLineProgress;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.PostProcessor.FailureStrategy;
import com.github.rinde.rinsim.experiment.SimulationProperty;
import com.github.rinde.rinsim.io.FileProvider;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.ScenarioIO;
import com.github.rinde.rinsim.scenario.StopConditions;
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

    final String[] expArgs = new String[args.length - 2];
    System.arraycopy(args, 2, expArgs, 0, args.length - 2);
    File resDir =
      realtime ? new File(RT_RESULTS_DIR) : new File(ST_RESULTS_DIR);

    FileProvider.Builder files = FileProvider.builder()
      .add(Paths.get(DATASET_PATH))
      .filter("regex:.*\\.scen");

    execute(programs, realtime, files, resDir, null, expArgs);

  }

  public static ExperimentResults execute(
      Iterable<GPProgram<GpGlobal>> programs,
      boolean realtime,
      FileProvider.Builder scenarioFiles,
      File resDir,
      @Nullable Function<Scenario, Scenario> scenarioConverter,
      String... expArgs) {
    checkArgument(realtime ^ scenarioConverter != null);
    final long startTime = System.currentTimeMillis();

    ResultWriter rw = new VanLonHolvoetResultWriter(resDir, GPEM17.OBJ_FUNC);
    Experiment.Builder exp = Experiment.builder()
      .addScenarios(scenarioFiles)
      .showGui(GPEM17.gui())
      .showGui(false)
      .usePostProcessor(
        new LogProcessor(GPEM17.OBJ_FUNC, scenarioConverter == null
          ? FailureStrategy.RETRY : FailureStrategy.INCLUDE, false))
      .computeLocal()
      .withRandomSeed(123)
      .repeat(3)
      // SEED_REPS,REPS,SCENARIO,CONFIG
      .withOrdering(
        SimulationProperty.SEED_REPS,
        SimulationProperty.REPS,
        SimulationProperty.SCENARIO,
        SimulationProperty.CONFIG)
      .addResultListener(new CommandLineProgress(System.out))
      .addResultListener(rw);

    if (realtime) {
      exp.setScenarioReader(
        ScenarioIO.readerAdapter(ScenarioConverter.TO_ONLINE_REALTIME_250))
        .withWarmup(30000)
        .withThreads((int) Math
          .floor((Runtime.getRuntime().availableProcessors() - 1) / 2d));
    } else if (scenarioConverter == null) {
      exp.setScenarioReader(
        ScenarioIO.readerAdapter(ScenarioConverter.TO_ONLINE_SIMULATED_250));
    } else {
      exp.setScenarioReader(
        ScenarioIO.readerAdapter(scenarioConverter));
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
        exp.addConfiguration(GPEM17.createRtConfig(prog, progId));
      } else {
        exp.addConfiguration(GPEM17.createStConfig(prog, progId));
      }
    };

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
          .setStopCondition(StopConditions.or(s.getStopCondition(),
            StopConditions.limitedTime(10 * 60 * 60 * 1000)))
          .build();
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
