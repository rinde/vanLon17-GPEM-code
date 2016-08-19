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
package com.github.rinde.gpem17;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.github.rinde.ecj.BaseEvaluator;
import com.github.rinde.ecj.GPBaseNode;
import com.github.rinde.ecj.GPComputationResult;
import com.github.rinde.ecj.GPFunc;
import com.github.rinde.ecj.GPFuncNode;
import com.github.rinde.ecj.GPProgram;
import com.github.rinde.ecj.GPProgramParser;
import com.github.rinde.ecj.PriorityHeuristic;
import com.github.rinde.evo4mas.common.EvoBidder;
import com.github.rinde.evo4mas.common.GlobalStateObjectFunctions.GpGlobal;
import com.github.rinde.logistics.pdptw.mas.TruckFactory.DefaultTruckFactory;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionCommModel;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionPanel;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionStopConditions;
import com.github.rinde.logistics.pdptw.mas.comm.DoubleBid;
import com.github.rinde.logistics.pdptw.mas.comm.RtSolverBidder.BidFunction;
import com.github.rinde.logistics.pdptw.mas.comm.RtSolverBidder.BidFunctions;
import com.github.rinde.logistics.pdptw.mas.route.RtSolverRoutePlanner;
import com.github.rinde.logistics.pdptw.solver.CheapestInsertionHeuristic;
import com.github.rinde.rinsim.central.SolverModel;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.Experiment.SimArgs;
import com.github.rinde.rinsim.experiment.Experiment.SimulationResult;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.experiment.PostProcessor;
import com.github.rinde.rinsim.io.FileProvider;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import com.github.rinde.rinsim.pdptw.common.RoutePanel;
import com.github.rinde.rinsim.pdptw.common.RouteRenderer;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.pdptw.common.StatsTracker;
import com.github.rinde.rinsim.pdptw.common.TimeLinePanel;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.ScenarioIO;
import com.github.rinde.rinsim.scenario.StopConditions;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06ObjectiveFunction;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PDPModelRenderer;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.SetMultimap;

import ec.EvolutionState;

/**
 * 
 * @author Rinde van Lon
 */
public class Evaluator extends BaseEvaluator {

  static final long MAX_SIM_TIME = 8 * 60 * 60 * 1000L;

  static final Gendreau06ObjectiveFunction OBJ_FUNC =
    Gendreau06ObjectiveFunction.instance(50d);

  static Experiment.Builder experimentBuilder(boolean showGui,
      String scenarioFileFilter) {
    return Experiment.builder()
      .addScenarios(FileProvider.builder()
        .add(Paths.get("files/train-dataset"))
        .filter(scenarioFileFilter))// "glob:**0.50-20-1.00-0.scen").)
      .setScenarioReader(
        ScenarioIO.readerAdapter(Converter.INSTANCE))
      // .withThreads(1)
      .showGui(View.builder()
        .withAutoPlay()
        .withSpeedUp(64)
        .withAutoClose()
        .withResolution(1280, 768)
        .with(PlaneRoadModelRenderer.builder())
        .with(PDPModelRenderer.builder())
        .with(AuctionPanel.builder())
        .with(RoutePanel.builder())
        .with(RouteRenderer.builder())
        .with(TimeLinePanel.builder()))
      .showGui(showGui)
      .usePostProcessor(AuctionPostProcessor.INSTANCE);
  }

  static void evaluate(Iterable<GPFunc<GpGlobal>> funcs,
      String scenarioFileFilter) {
    Experiment.Builder expBuilder =
      experimentBuilder(false, scenarioFileFilter);

    Map<MASConfiguration, String> map = new LinkedHashMap<>();
    for (GPFunc<GpGlobal> func : funcs) {
      GPProgram<GpGlobal> prog = new GPProgram<>(new GPFuncNode<>(func));
      // GPProgramParser.convertToGPProgram(new GPBaseNode<>(func));

      MASConfiguration config = createConfig(prog);
      map.put(config, prog.getId());
      expBuilder.addConfiguration(config);
    }

    ExperimentResults results = expBuilder.perform();

    File dest = new File("files/results/test.csv");

    StatsLogger.createHeader(dest);
    for (SimulationResult sr : results.sortedResults()) {
      StatsLogger.appendResults(asList(sr), dest,
        map.get(sr.getSimArgs().getMasConfig()));
    }
  }

  @Override
  public void evaluatePopulation(EvolutionState state) {

    SetMultimap<GPNodeHolder, IndividualHolder> mapping =
      getGPFitnessMapping(state);

    Experiment.Builder expBuilder =
      experimentBuilder(false, "glob:**0.50-20-1.00-0.scen");

    Map<MASConfiguration, GPNodeHolder> configGpMapping = new LinkedHashMap<>();
    for (GPNodeHolder node : mapping.keySet()) {

      // GPProgram<GpGlobal> prog =
      // GPProgramParser.parseProgramFunc("(insertioncost)",
      // (new FunctionSet()).create());

      final GPProgram<GpGlobal> prog = GPProgramParser
        .convertToGPProgram((GPBaseNode<GpGlobal>) node.trees[0].child);

      MASConfiguration config = createConfig(prog);
      configGpMapping.put(config, node);
      expBuilder.addConfiguration(config);
    }

    ExperimentResults results = expBuilder.perform();
    List<GPComputationResult> convertedResults = new ArrayList<>();

    for (SimulationResult sr : results.getResults()) {
      StatisticsDTO stats = ((ResultObject) sr.getResultObject()).getStats();
      double cost = OBJ_FUNC.computeCost(stats);
      float fitness = (float) cost;
      if (!OBJ_FUNC.isValidResult(stats)) {
        fitness = Float.MAX_VALUE;
      }
      String id = configGpMapping.get(sr.getSimArgs().getMasConfig()).string;
      convertedResults.add(SingleResult.create((float) fitness, id, sr));
    }

    processResults(state, mapping, convertedResults);
  }

  @Override
  protected int expectedNumberOfResultsPerGPIndividual(EvolutionState state) {
    return 1;
  }

  static MASConfiguration createConfig(PriorityHeuristic<GpGlobal> solver) {
    final BidFunction bf = BidFunctions.BALANCED_HIGH;
    return MASConfiguration.pdptwBuilder()
      .setName("ReAuction-RP-EVO-BID-EVO-" + bf)
      .addEventHandler(AddVehicleEvent.class,
        DefaultTruckFactory.builder()
          .setRoutePlanner(
            RtSolverRoutePlanner.simulatedTimeSupplier(
              CheapestInsertionHeuristic.supplier(OBJ_FUNC)))
          .setCommunicator(EvoBidder.simulatedTimeBuilder(solver, OBJ_FUNC)
            .withReauctionCooldownPeriod(60000))
          .setLazyComputation(false)
          .setRouteAdjuster(RouteFollowingVehicle.delayAdjuster())
          .build())
      .addModel(AuctionCommModel.builder(DoubleBid.class)
        .withStopCondition(
          AuctionStopConditions.and(
            AuctionStopConditions.<DoubleBid>atLeastNumBids(2),
            AuctionStopConditions.<DoubleBid>or(
              AuctionStopConditions.<DoubleBid>allBidders(),
              AuctionStopConditions.<DoubleBid>maxAuctionDuration(5000))))
        .withMaxAuctionDuration(30 * 60 * 1000L))
      .addModel(SolverModel.builder())
      // .addEventHandler(AddParcelEvent.class, AddParcelEvent.namedHandler())
      .build();
  }

  enum Converter implements Function<Scenario, Scenario> {
    INSTANCE {
      @Override
      public Scenario apply(Scenario input) {
        return Scenario.builder(input)
          .removeModelsOfType(TimeModel.AbstractBuilder.class)
          .addModel(TimeModel.builder().withTickLength(250))
          .setStopCondition(StopConditions.or(input.getStopCondition(),
            StopConditions.limitedTime(MAX_SIM_TIME),
            EvoStopCondition.INSTANCE))
          .build();
      }
    }
  }

  enum AuctionPostProcessor
    implements PostProcessor<ResultObject>,Serializable {

    INSTANCE {
      @Override
      public ResultObject collectResults(Simulator sim, SimArgs args) {

        @Nullable
        final AuctionCommModel<?> auctionModel =
          sim.getModelProvider().tryGetModel(AuctionCommModel.class);

        final Optional<AuctionStats> aStats;
        if (auctionModel == null) {
          aStats = Optional.absent();
        } else {
          final int parcels = auctionModel.getNumParcels();
          final int reauctions = auctionModel.getNumAuctions() - parcels;
          final int unsuccessful = auctionModel.getNumUnsuccesfulAuctions();
          final int failed = auctionModel.getNumFailedAuctions();
          aStats = Optional
            .of(AuctionStats.create(parcels, reauctions, unsuccessful, failed));
        }

        final StatisticsDTO stats =
          sim.getModelProvider().getModel(StatsTracker.class).getStatistics();

        return ResultObject.create(stats, aStats);
      }

      @Override
      public FailureStrategy handleFailure(Exception e, Simulator sim,
          SimArgs args) {
        return FailureStrategy.ABORT_EXPERIMENT_RUN;
      }
    }
  }
}
