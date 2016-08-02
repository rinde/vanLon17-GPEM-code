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
package com.github.rinde.sec17;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.rinde.ecj.BaseEvaluator;
import com.github.rinde.ecj.GPBaseNode;
import com.github.rinde.ecj.GPProgram;
import com.github.rinde.ecj.GPProgramParser;
import com.github.rinde.evo4mas.common.PriorityHeuristicSolver;
import com.github.rinde.evo4mas.common.VehicleParcelContext;
import com.github.rinde.jppf.GPComputationResult;
import com.github.rinde.rinsim.central.Central;
import com.github.rinde.rinsim.central.SolverValidator;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.Experiment.SimulationResult;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.experiment.PostProcessors;
import com.github.rinde.rinsim.io.FileProvider;
import com.github.rinde.rinsim.pdptw.common.ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.scenario.ScenarioConverters;
import com.github.rinde.rinsim.scenario.ScenarioIO;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06ObjectiveFunction;
import com.google.auto.value.AutoValue;
import com.google.common.collect.SetMultimap;

import ec.EvolutionState;

/**
 * 
 * @author Rinde van Lon
 */
public class Evaluator extends BaseEvaluator {

  static final ObjectiveFunction OBJ_FUNC =
    Gendreau06ObjectiveFunction.instance(50d);

  @Override
  public void evaluatePopulation(EvolutionState state) {

    SetMultimap<GPNodeHolder, IndividualHolder> mapping =
      getGPFitnessMapping(state);

    Experiment.Builder expBuilder = Experiment.builder()
      .addScenarios(FileProvider.builder()
        .add(Paths.get("files/vanLonHolvoet15"))
        .filter("glob:**0.20-5-1.00-0.scen"))
      .setScenarioReader(
        ScenarioIO.readerAdapter(ScenarioConverters.toSimulatedtime()))
      .usePostProcessor(PostProcessors.statisticsPostProcessor(OBJ_FUNC));

    Map<MASConfiguration, GPNodeHolder> configGpMapping = new LinkedHashMap<>();
    for (GPNodeHolder node : mapping.keySet()) {
      final GPProgram<VehicleParcelContext> prog = GPProgramParser
        .convertToGPProgram(
          (GPBaseNode<VehicleParcelContext>) node.trees[0].child);

      MASConfiguration config = Central.solverConfiguration(
        SolverValidator.wrap(PriorityHeuristicSolver.supplier(prog)));
      configGpMapping.put(config, node);
      expBuilder.addConfiguration(config);
    }

    ExperimentResults results = expBuilder.perform();
    List<GPComputationResult> convertedResults = new ArrayList<>();

    for (SimulationResult sr : results.getResults()) {
      StatisticsDTO stats = (StatisticsDTO) sr.getResultObject();
      double cost = OBJ_FUNC.computeCost(stats);
      float fitness = (float) cost;
      if (!stats.simFinish) {
        fitness = Float.MAX_VALUE;
      }
      String id = configGpMapping.get(sr.getSimArgs().getMasConfig()).string;
      convertedResults.add(SingleResult.create((float) fitness, id));
    }

    processResults(state, mapping, convertedResults);
  }

  @AutoValue
  abstract static class SingleResult implements GPComputationResult {
    static SingleResult create(float fitness, String id) {
      return new AutoValue_Evaluator_SingleResult(fitness, id);
    }
  }

  @Override
  protected int expectedNumberOfResultsPerGPIndividual(EvolutionState state) {
    // TODO Auto-generated method stub
    return 0;
  }

}
