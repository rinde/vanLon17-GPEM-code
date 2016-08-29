/*
 * Copyright (C) 2015-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import com.github.rinde.gpem17.AuctionStats;
import com.github.rinde.rinsim.core.model.time.RealtimeTickInfo;
import com.github.rinde.rinsim.experiment.Experiment.SimArgs;
import com.github.rinde.rinsim.experiment.Experiment.SimulationResult;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.experiment.PostProcessor.FailureStrategy;
import com.github.rinde.rinsim.experiment.ResultListener;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06ObjectiveFunction;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;

abstract class ResultWriter implements ResultListener {
  final File experimentDirectory;
  final File timeDeviationsDirectory;
  final Gendreau06ObjectiveFunction objectiveFunction;
  final boolean realtime;
  private final boolean createFinalFiles;

  ResultWriter(File target, Gendreau06ObjectiveFunction objFunc, boolean rt,
      boolean finalFiles) {
    experimentDirectory = target;
    objectiveFunction = objFunc;
    realtime = rt;
    createFinalFiles = finalFiles;
    if (rt) {
      timeDeviationsDirectory =
        new File(experimentDirectory, "time-deviations");
      timeDeviationsDirectory.mkdirs();
    } else {
      timeDeviationsDirectory = null;
    }
  }

  public File getExperimentDirectory() {
    return experimentDirectory;
  }

  @Override
  public void startComputing(int numberOfSimulations,
      ImmutableSet<MASConfiguration> configurations,
      ImmutableSet<Scenario> scenarios,
      int repetitions,
      int seedRepetitions) {

    final StringBuilder sb = new StringBuilder("Experiment summary");
    sb.append(System.lineSeparator())
      .append("Number of simulations: ")
      .append(numberOfSimulations)
      .append(System.lineSeparator())
      .append("Number of configurations: ")
      .append(configurations.size())
      .append(System.lineSeparator())
      .append("Number of scenarios: ")
      .append(scenarios.size())
      .append(System.lineSeparator())
      .append("Number of repetitions: ")
      .append(repetitions)
      .append(System.lineSeparator())
      .append("Number of seed repetitions: ")
      .append(seedRepetitions)
      .append(System.lineSeparator())
      .append("Configurations:")
      .append(System.lineSeparator());

    for (final MASConfiguration config : configurations) {
      sb.append(config.getName())
        .append(System.lineSeparator());
    }

    final File setup = new File(experimentDirectory, "experiment-setup.txt");
    try {
      setup.createNewFile();
      Files.write(sb.toString(), setup, Charsets.UTF_8);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void doneComputing(ExperimentResults results) {
    if (createFinalFiles) {
      final Multimap<MASConfiguration, SimulationResult> groupedResults =
        LinkedHashMultimap.create();
      for (final SimulationResult sr : results.sortedResults()) {
        groupedResults.put(sr.getSimArgs().getMasConfig(), sr);
      }

      for (final MASConfiguration config : groupedResults.keySet()) {
        final Collection<SimulationResult> group = groupedResults.get(config);

        final File configResult =
          new File(experimentDirectory, config.getName() + "-final.csv");

        // deletes the file in case it already exists
        configResult.delete();
        createCSVWithHeader(configResult);
        for (final SimulationResult sr : group) {
          appendSimResult(sr, configResult);
        }
      }
    }
  }

  abstract Iterable<Enum<?>> getFields();

  abstract void appendSimResult(SimulationResult sr, File destFile);

  void createCSVWithHeader(File f) {
    try {
      Files.createParentDirs(f);
      Files.append(
        Joiner.on(",").appendTo(new StringBuilder(), getFields())
          .append(System.lineSeparator()),
        f,
        Charsets.UTF_8);
    } catch (final IOException e1) {
      throw new IllegalStateException(e1);
    }
  }

  static void appendTimeLogSummary(SimulationResult sr, File target) {
    if (sr.getResultObject() instanceof RtExperimentInfo) {
      final RtExperimentInfo info = (RtExperimentInfo) sr.getResultObject();

      final int tickInfoListSize = info.getTickInfoList().size();
      long sumIatNs = 0;
      for (final RealtimeTickInfo md : info.getTickInfoList()) {
        sumIatNs += md.getInterArrivalTime();
      }

      try {
        Files.append(Joiner.on(',').join(
          sr.getSimArgs().getScenario().getProblemClass().getId(),
          sr.getSimArgs().getScenario().getProblemInstanceId(),
          sr.getSimArgs().getMasConfig().getName(),
          sr.getSimArgs().getRandomSeed(),
          sr.getSimArgs().getRepetition(),
          tickInfoListSize,
          tickInfoListSize == 0 ? 0
            : sumIatNs / tickInfoListSize,
          info.getRtCount(),
          info.getStCount() + "\n"), target, Charsets.UTF_8);
      } catch (final IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  static void createTimeLogSummaryHeader(File target) {
    try {
      Files.append(Joiner.on(',').join(
        "problem-class",
        "instance",
        "config",
        "random-seed",
        "repetition",
        "rt-tick-infos",
        "avg-interarrival-time",
        "rt-count",
        "st-count\n"), target, Charsets.UTF_8);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  static void addSimOutputs(ImmutableMap.Builder<Enum<?>, Object> map,
      SimulationResult sr, Gendreau06ObjectiveFunction objFunc) {
    if (sr.getResultObject() instanceof FailureStrategy) {
      map.put(OutputFields.COST, -1)
        .put(OutputFields.TRAVEL_TIME, -1)
        .put(OutputFields.TARDINESS, -1)
        .put(OutputFields.OVER_TIME, -1)
        .put(OutputFields.IS_VALID, false)
        .put(OutputFields.COMP_TIME, -1)
        .put(OutputFields.NUM_REAUCTIONS, -1)
        .put(OutputFields.NUM_UNSUC_REAUCTIONS, -1)
        .put(OutputFields.NUM_FAILED_REAUCTIONS, -1);
    } else {
      final RtExperimentInfo ei = (RtExperimentInfo) sr.getResultObject();
      final StatisticsDTO stats = ei.getStats();
      map.put(OutputFields.COST, objFunc.computeCost(stats))
        .put(OutputFields.TRAVEL_TIME, objFunc.travelTime(stats))
        .put(OutputFields.TARDINESS, objFunc.tardiness(stats))
        .put(OutputFields.OVER_TIME, objFunc.overTime(stats))
        .put(OutputFields.IS_VALID, objFunc.isValidResult(stats))
        .put(OutputFields.COMP_TIME, stats.computationTime);

      if (ei.getAuctionStats().isPresent()) {
        final AuctionStats aStats = ei.getAuctionStats().get();
        map.put(OutputFields.NUM_REAUCTIONS, aStats.getNumReauctions())
          .put(OutputFields.NUM_UNSUC_REAUCTIONS,
            aStats.getNumUnsuccesfulReauctions())
          .put(OutputFields.NUM_FAILED_REAUCTIONS,
            aStats.getNumFailedReauctions());
      } else {
        map.put(OutputFields.NUM_REAUCTIONS, 0)
          .put(OutputFields.NUM_UNSUC_REAUCTIONS, 0)
          .put(OutputFields.NUM_FAILED_REAUCTIONS, 0);
      }

      // if (!objFunc.isValidResult(stats)) {
      // System.err.println("WARNING: FOUND AN INVALID RESULT: ");
      // System.err.println(map.build());
      // }
    }
  }

  void writeTimeLog(SimulationResult result) {
    final String configName = result.getSimArgs().getMasConfig().getName();
    final File timeLogSummaryFile =
      new File(experimentDirectory, configName + "-timelog-summary.csv");

    if (!timeLogSummaryFile.exists()) {
      createTimeLogSummaryHeader(timeLogSummaryFile);
    }
    appendTimeLogSummary(result, timeLogSummaryFile);
    createTimeLog(result, timeDeviationsDirectory);
  }

  static void createTimeLog(SimulationResult sr, File experimentDir) {
    if (!(sr.getResultObject() instanceof RtExperimentInfo)) {
      return;
    }
    final SimArgs simArgs = sr.getSimArgs();
    final Scenario scenario = simArgs.getScenario();

    final String id = Joiner.on("-").join(
      simArgs.getMasConfig().getName(),
      scenario.getProblemClass().getId(),
      scenario.getProblemInstanceId(),
      simArgs.getRandomSeed(),
      simArgs.getRepetition());

    final File iatFile = new File(experimentDir, id + "-interarrivaltimes.csv");
    final RtExperimentInfo info = (RtExperimentInfo) sr.getResultObject();
    try (FileWriter writer = new FileWriter(iatFile)) {
      iatFile.createNewFile();
      for (final RealtimeTickInfo md : info.getTickInfoList()) {
        writer.write(Long.toString(md.getInterArrivalTime()));
        writer.write(System.lineSeparator());
      }
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  enum OutputFields {
    DYNAMISM,

    URGENCY,

    SCALE,

    COST,

    TRAVEL_TIME,

    TARDINESS,

    OVER_TIME,

    IS_VALID,

    SCENARIO_ID,

    RANDOM_SEED,

    REPETITION,

    COMP_TIME,

    NUM_VEHICLES,

    NUM_ORDERS,

    NUM_REAUCTIONS,

    NUM_UNSUC_REAUCTIONS,

    NUM_FAILED_REAUCTIONS;

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

}
