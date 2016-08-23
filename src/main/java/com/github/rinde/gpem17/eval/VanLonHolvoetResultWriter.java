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

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.rinde.rinsim.experiment.Experiment.SimulationResult;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06ObjectiveFunction;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

/**
 *
 * @author Rinde van Lon
 */
public class VanLonHolvoetResultWriter extends ResultWriter {

  public VanLonHolvoetResultWriter(File target,
      Gendreau06ObjectiveFunction objFunc) {
    super(target, objFunc);
  }

  @Override
  public void receive(SimulationResult result) {
    final String configName = result.getSimArgs().getMasConfig().getName();
    final File targetFile = new File(experimentDirectory, configName + ".csv");

    if (!targetFile.exists()) {
      createCSVWithHeader(targetFile);
    }
    appendSimResult(result, targetFile);

    writeTimeLog(result);
  }

  @Override
  void appendSimResult(SimulationResult sr, File destFile) {
    final String pc = sr.getSimArgs().getScenario().getProblemClass().getId();
    final String id = sr.getSimArgs().getScenario().getProblemInstanceId();

    try {
      final String scenarioName = Joiner.on("-").join(pc, id);
      final List<String> propsStrings = Files.readLines(new File(
        Evaluate.DATASET_PATH + scenarioName
          + ".properties"),
        Charsets.UTF_8);
      final Map<String, String> properties = Splitter.on("\n")
        .withKeyValueSeparator(" = ")
        .split(Joiner.on("\n").join(propsStrings));

      final ImmutableMap.Builder<Enum<?>, Object> map =
        ImmutableMap.<Enum<?>, Object>builder()
          .put(OutputFields.SCENARIO_ID, scenarioName)
          .put(OutputFields.DYNAMISM, properties.get("dynamism_bin"))
          .put(OutputFields.URGENCY, properties.get("urgency"))
          .put(OutputFields.SCALE, properties.get("scale"))
          .put(OutputFields.NUM_ORDERS, properties.get("AddParcelEvent"))
          .put(OutputFields.NUM_VEHICLES, properties.get("AddVehicleEvent"))
          .put(OutputFields.RANDOM_SEED, sr.getSimArgs().getRandomSeed())
          .put(OutputFields.REPETITION, sr.getSimArgs().getRepetition());

      addSimOutputs(map, sr, objectiveFunction);

      final String line =
        appendValuesTo(new StringBuilder(), map.build(), getFields())
          .append(System.lineSeparator())
          .toString();
      Files.append(line, destFile, Charsets.UTF_8);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  Iterable<Enum<?>> getFields() {
    return ImmutableList.<Enum<?>>copyOf(OutputFields.values());
  }

  static <T extends Enum<?>> StringBuilder appendValuesTo(StringBuilder sb,
      Map<T, Object> props, Iterable<T> keys) {
    final List<Object> values = new ArrayList<>();
    for (final T p : keys) {
      checkArgument(props.containsKey(p));
      values.add(props.get(p));
    }
    Joiner.on(",").appendTo(sb, values);
    return sb;
  }
}
