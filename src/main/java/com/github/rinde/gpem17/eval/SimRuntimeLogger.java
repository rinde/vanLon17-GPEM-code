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

import java.io.File;
import java.io.IOException;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;

import com.github.rinde.rinsim.experiment.Experiment.SimulationResult;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.experiment.ResultListener;
import com.github.rinde.rinsim.scenario.Scenario;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;

/**
 * 
 * @author Rinde van Lon
 */
public class SimRuntimeLogger implements ResultListener {
  final File progressFile;
  int receivedSims;
  int totalSims;

  SimRuntimeLogger(File dir) {
    progressFile = new File(dir, "progress.csv");
  }

  @Override
  public void startComputing(int numberOfSimulations,
      ImmutableSet<MASConfiguration> configurations,
      ImmutableSet<Scenario> scenarios, int repetitions, int seedRepetitions) {
    totalSims = numberOfSimulations;
  }

  @Override
  public void receive(SimulationResult result) {
    receivedSims++;

    RtExperimentInfo info = (RtExperimentInfo) result.getResultObject();
    StringBuilder sb = new StringBuilder();
    sb.append(receivedSims)
      .append("/")
      .append(totalSims)
      .append(",")
      .append(info.getStats().computationTime)
      .append(",")
      .append(PeriodFormat.getDefault()
        .print(new Period(info.getStats().computationTime)))
      .append(",")
      .append(result.getSimArgs().toShortString())
      .append(System.lineSeparator());
    try {
      Files.append(sb.toString(), progressFile, Charsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void doneComputing(ExperimentResults results) {}

}
