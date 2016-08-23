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
package com.github.rinde.gpem17.evo;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.joda.time.format.ISODateTimeFormat;

import com.github.rinde.ecj.GPComputationResult;
import com.github.rinde.ecj.GPStats;
import com.github.rinde.gpem17.AuctionStats;
import com.github.rinde.gpem17.GPEM17;
import com.github.rinde.rinsim.experiment.Experiment.SimulationResult;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

import ec.EvolutionState;
import ec.Individual;
import ec.util.Parameter;

/**
 * 
 * @author Rinde van Lon
 */
public class StatsLogger extends GPStats {
  static final String RESULTS_MAIN_DIR = "files/results/evo/";
  static final Joiner DASH_JOINER = Joiner.on("-");
  static final Joiner COMMA_JOINER = Joiner.on(",");
  final File experimentDirectory;
  final File statsLog;

  public StatsLogger() {
    experimentDirectory = createExperimentDir(new File(RESULTS_MAIN_DIR));
    statsLog = new File(experimentDirectory, "best-stats.csv");

    // create best-stats.csv with header
    createHeader(statsLog);
  }

  static void createHeader(File dest) {
    try {
      Files.createParentDirs(dest);
      Files.append(
        Joiner.on(",").appendTo(new StringBuilder(), CsvFields.values())
          .append(System.lineSeparator()),
        dest,
        Charsets.UTF_8);
    } catch (final IOException e1) {
      throw new IllegalStateException(e1);
    }
  }

  public void setup(final EvolutionState state, final Parameter base) {
    super.setup(state, base);
  }

  static void appendResults(Iterable<SimulationResult> results, File dest,
      String generationId) {
    StringBuilder sb = new StringBuilder();
    for (SimulationResult sr : results) {
      ResultObject ro = (ResultObject) sr.getResultObject();
      StatisticsDTO stats = ro.getStats();

      final String pc = sr.getSimArgs().getScenario().getProblemClass().getId();
      final String id = sr.getSimArgs().getScenario().getProblemInstanceId();
      final String scenarioName = DASH_JOINER.join(pc, id);
      boolean isValid = GPEM17.OBJ_FUNC.isValidResult(stats);
      double cost = GPEM17.OBJ_FUNC.computeCost(stats);
      final ImmutableMap.Builder<Enum<?>, Object> map =
        ImmutableMap.<Enum<?>, Object>builder()
          .put(CsvFields.GENERATION, generationId)
          .put(CsvFields.SCENARIO_ID, scenarioName)
          .put(CsvFields.RANDOM_SEED, sr.getSimArgs().getRandomSeed())
          .put(CsvFields.COST, cost)
          .put(CsvFields.TRAVEL_TIME, GPEM17.OBJ_FUNC.travelTime(stats))
          .put(CsvFields.TARDINESS, GPEM17.OBJ_FUNC.tardiness(stats))
          .put(CsvFields.OVER_TIME, GPEM17.OBJ_FUNC.overTime(stats))
          .put(CsvFields.IS_VALID, isValid)
          .put(CsvFields.NUM_ORDERS, stats.totalParcels)
          .put(CsvFields.NUM_VEHICLES, stats.totalVehicles)
          .put(CsvFields.COST_PER_PARCEL,
            isValid ? cost / (double) stats.totalParcels : "invalid");

      if (ro.getAuctionStats().isPresent()) {
        final AuctionStats aStats = ro.getAuctionStats().get();
        map.put(CsvFields.NUM_REAUCTIONS, aStats.getNumReauctions())
          .put(CsvFields.NUM_UNSUC_REAUCTIONS,
            aStats.getNumUnsuccesfulReauctions())
          .put(CsvFields.NUM_FAILED_REAUCTIONS,
            aStats.getNumFailedReauctions());
      } else {
        map.put(CsvFields.NUM_REAUCTIONS, -1)
          .put(CsvFields.NUM_UNSUC_REAUCTIONS, -1)
          .put(CsvFields.NUM_FAILED_REAUCTIONS, -1);
      }
      appendValuesTo(sb, map.build(), CsvFields.values())
        .append(System.lineSeparator());
    }

    try {
      Files.append(sb.toString(), dest, Charsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public void printMore(EvolutionState state, Individual best,
      List<GPComputationResult> bestResults) {

    List<SimulationResult> results = new ArrayList<>();
    for (GPComputationResult res : bestResults) {
      results.add(((SingleResult) res).getSimulationResult());
    }

    appendResults(results, statsLog, Integer.toString(state.generation));

    File programFile = new File(experimentDirectory,
      "programs/best-individual-" + state.generation + ".txt");

    try {
      Files.createParentDirs(programFile);
      Files.append(bestResults.get(0).getTaskDataId(), programFile,
        Charsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  static <T extends Enum<?>> StringBuilder appendValuesTo(StringBuilder sb,
      Map<T, Object> props, T[] keys) {
    final List<Object> values = new ArrayList<>();
    for (final T p : keys) {
      checkArgument(props.containsKey(p));
      values.add(props.get(p));
    }
    COMMA_JOINER.appendTo(sb, values);
    return sb;
  }

  public void finalStatistics(final EvolutionState state, final int result) {
    super.finalStatistics(state, result);
  }

  static File createExperimentDir(File target) {
    final String timestamp = ISODateTimeFormat.dateHourMinuteSecond()
      .print(System.currentTimeMillis());
    final File experimentDirectory = new File(target, timestamp);
    experimentDirectory.mkdirs();

    final Path latest = Paths.get(target.getAbsolutePath(), "latest/");

    try {
      java.nio.file.Files.deleteIfExists(latest);
      java.nio.file.Files.createSymbolicLink(
        latest,
        experimentDirectory.getAbsoluteFile().toPath());
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
    return experimentDirectory;
  }

  enum CsvFields {
    GENERATION,

    COST_PER_PARCEL,

    COST,

    TRAVEL_TIME,

    TARDINESS,

    OVER_TIME,

    IS_VALID,

    SCENARIO_ID,

    RANDOM_SEED,

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
