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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.joda.time.Period;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.format.PeriodFormat;

import com.github.rinde.rinsim.experiment.Experiment.SimulationResult;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.experiment.ResultListener;
import com.github.rinde.rinsim.scenario.Scenario;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import com.google.common.math.DoubleMath;

/**
 * 
 * @author Rinde van Lon
 */
public class SimRuntimeLogger implements ResultListener {
  final File progressFile;
  int receivedSims;
  int totalSims;

  long lastWrite;

  List<SimulationResult> receivedResults;

  SimRuntimeLogger(File dir) {
    progressFile = new File(dir, "progress.csv");
    receivedResults = new ArrayList<>();
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
    receivedResults.add(result);
    if (System.currentTimeMillis() - lastWrite >= 60000
      || receivedSims == totalSims) {
      write();
    }
  }

  void write() {
    lastWrite = System.currentTimeMillis();
    StringBuilder sb = new StringBuilder();
    String timestamp =
      ISODateTimeFormat.dateHourMinuteSecond().print(lastWrite);

    long sum = 0;
    double[] arr = new double[receivedResults.size()];
    for (int i = 0; i < receivedResults.size(); i++) {
      SimResult info =
        (SimResult) receivedResults.get(i).getResultObject();
      sum += info.getStats().computationTime;
      arr[i] = info.getStats().computationTime;
    }
    double mean = sum / receivedResults.size();
    long sd = DoubleMath.roundToLong(
      new StandardDeviation().evaluate(arr, mean), RoundingMode.HALF_DOWN);
    long longMean = DoubleMath.roundToLong(mean, RoundingMode.HALF_DOWN);

    sb.append(timestamp)
      .append(",")
      .append(receivedSims)
      .append("/")
      .append(totalSims)
      .append(", Received ")
      .append(receivedResults.size())
      .append(" results in last minute, avg comp time,")
      .append(PeriodFormat.getDefault().print(new Period(longMean)))
      .append(", standard deviation,")
      .append(PeriodFormat.getDefault().print(new Period(sd)))
      .append(System.lineSeparator());
    try {
      Files.append(sb.toString(), progressFile, Charsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    receivedResults.clear();
  }

  @Override
  public void doneComputing(ExperimentResults results) {}

}
