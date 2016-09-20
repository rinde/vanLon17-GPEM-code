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

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.rinde.ecj.GPProgram;
import com.github.rinde.ecj.GPProgramParser;
import com.github.rinde.evo4mas.common.GpGlobal;
import com.github.rinde.gpem17.GPEM17.ReauctOpt;
import com.github.rinde.gpem17.eval.Evaluate;
import com.github.rinde.rinsim.experiment.Experiment.SimulationResult;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.io.FileProvider;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06ObjectiveFunction;

/**
 * 
 * @author Rinde van Lon
 */
public class CihReference {

  static final Gendreau06ObjectiveFunction LESS_TT_OBJ_FUNC =
    Gendreau06ObjectiveFunction.instance(50d, .25, 1d, 1d);

  public static void main(String[] args) {
    final int generations = 50;
    final int numScensInGen = 50;
    final int numScensInLastGen = 250;
    int totalScens = ((generations - 1) * numScensInGen) + numScensInLastGen;
    final String regex = ".*0\\.50-20-1\\.00-.*\\.scen";

    GPProgram<GpGlobal> prog =
      GPProgramParser.parseProgramFunc("(insertioncost)",
        new FunctionSet().create());

    List<Path> paths =
      FitnessEvaluator.getScenarioPaths("files/dataset5000", regex).subList(0,
        totalScens);

    File resDir = new File("files/results/cih/");

    ExperimentResults results = Evaluate.execute(
      asList(prog),
      false,
      FileProvider.builder().add(paths),
      resDir,
      true,
      FitnessEvaluator.Converter.INSTANCE,
      false,
      ReauctOpt.CIH,
      LESS_TT_OBJ_FUNC,
      new String[] {"--repetitions", "1"});

    File statsLog = new File(resDir, "best-stats.csv");
    StatsLogger.createHeader(statsLog);

    List<SimulationResult> list = results.getResults().asList();

    // sort the scenarios in the exact same order as the files were sorted in
    Map<String, SimulationResult> map = new LinkedHashMap<>();
    for (SimulationResult sr : list) {
      String key = sr.getSimArgs().getScenario().getProblemClass().getId()
        + "-"
        + sr.getSimArgs().getScenario().getProblemInstanceId()
        + ".scen";
      checkState(!map.containsKey(key));
      map.put(key, sr);
    }
    List<SimulationResult> sortedList = new ArrayList<>();
    for (Path p : paths) {
      sortedList.add(map.get(p.getFileName().toString()));
    }

    int i;
    for (i = 0; i < sortedList.size() - numScensInLastGen; i += numScensInGen) {
      StatsLogger.appendResults(sortedList.subList(i, i + numScensInGen),
        statsLog, Integer.toString(i / numScensInGen));
    }
    StatsLogger.appendResults(sortedList.subList(i, i + numScensInLastGen),
      statsLog, Integer.toString(i / numScensInGen));

  }

}
