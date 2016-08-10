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
import com.github.rinde.ecj.GPProgram;
import com.github.rinde.ecj.GPProgramParser;
import com.github.rinde.evo4mas.common.PriorityHeuristicSolver;
import com.github.rinde.evo4mas.common.VehicleParcelContext;
import com.github.rinde.logistics.pdptw.mas.TruckFactory.DefaultTruckFactory;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionCommModel;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionPanel;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionStopConditions;
import com.github.rinde.logistics.pdptw.mas.comm.DoubleBid;
import com.github.rinde.logistics.pdptw.mas.comm.RtSolverBidder;
import com.github.rinde.logistics.pdptw.mas.comm.RtSolverBidder.BidFunction;
import com.github.rinde.logistics.pdptw.mas.comm.RtSolverBidder.BidFunctions;
import com.github.rinde.logistics.pdptw.mas.route.RtSolverRoutePlanner;
import com.github.rinde.rinsim.central.Solver;
import com.github.rinde.rinsim.central.SolverModel;
import com.github.rinde.rinsim.central.SolverValidator;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.Experiment.SimArgs;
import com.github.rinde.rinsim.experiment.Experiment.SimulationResult;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.experiment.PostProcessor;
import com.github.rinde.rinsim.experiment.PostProcessors;
import com.github.rinde.rinsim.io.FileProvider;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import com.github.rinde.rinsim.pdptw.common.RoutePanel;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.pdptw.common.TimeLinePanel;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.ScenarioIO;
import com.github.rinde.rinsim.scenario.StopConditions;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06ObjectiveFunction;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PDPModelRenderer;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.SetMultimap;

import ec.EvolutionState;

/**
 * 
 * @author Rinde van Lon
 */
public class Evaluator extends BaseEvaluator {

  static final Gendreau06ObjectiveFunction OBJ_FUNC =
    Gendreau06ObjectiveFunction.instance(50d);

  @Override
  public void evaluatePopulation(EvolutionState state) {

    SetMultimap<GPNodeHolder, IndividualHolder> mapping =
      getGPFitnessMapping(state);

    Experiment.Builder expBuilder = Experiment.builder()
      .addScenarios(FileProvider.builder()
        .add(Paths.get("files/vanLonHolvoet15"))
        .filter("glob:**0.50-5-5.00-0.scen"))
      .setScenarioReader(
        ScenarioIO.readerAdapter(Converter.INSTANCE))
      // .withThreads(1)
      .showGui(View.builder()
        .withAutoPlay()
        .with(PlaneRoadModelRenderer.builder())
        .with(PDPModelRenderer.builder())
        .with(AuctionPanel.builder())
        .with(RoutePanel.builder())
        .with(TimeLinePanel.builder()))
      .showGui(false)
      .usePostProcessor(AuctionPostProcessor.INSTANCE);

    Map<MASConfiguration, GPNodeHolder> configGpMapping = new LinkedHashMap<>();
    for (GPNodeHolder node : mapping.keySet()) {
      final GPProgram<VehicleParcelContext> prog = GPProgramParser
        .convertToGPProgram(
          (GPBaseNode<VehicleParcelContext>) node.trees[0].child);

      MASConfiguration config = createConfig(
        SolverValidator.wrap(PriorityHeuristicSolver.supplier(prog)));
      configGpMapping.put(config, node);
      expBuilder.addConfiguration(config);
    }

    ExperimentResults results = expBuilder.perform();
    List<GPComputationResult> convertedResults = new ArrayList<>();

    for (SimulationResult sr : results.getResults()) {
      StatisticsDTO stats = ((ResultObject) sr.getResultObject()).getStats();
      double cost = OBJ_FUNC.computeCost(stats);
      float fitness = (float) cost;
      if (!stats.simFinish) {
        fitness = Float.MAX_VALUE;
      }
      String id = configGpMapping.get(sr.getSimArgs().getMasConfig()).string;
      convertedResults.add(SingleResult.create((float) fitness, id, sr));
    }

    processResults(state, mapping, convertedResults);
  }

  @AutoValue
  abstract static class SingleResult implements GPComputationResult {
    abstract SimulationResult getSimulationResult();

    static SingleResult create(float fitness, String id, SimulationResult sr) {
      return new AutoValue_Evaluator_SingleResult(fitness, id, sr);
    }
  }

  @Override
  protected int expectedNumberOfResultsPerGPIndividual(EvolutionState state) {
    return 1;
  }

  static MASConfiguration createConfig(
      StochasticSupplier<? extends Solver> solver) {
    final BidFunction bf = BidFunctions.BALANCED_HIGH;
    return MASConfiguration.pdptwBuilder()
      .setName("ReAuction-RP-EVO-BID-EVO-" + bf)
      .addEventHandler(AddVehicleEvent.class,
        DefaultTruckFactory.builder()
          .setRoutePlanner(RtSolverRoutePlanner.simulatedTimeSupplier(solver))
          .setCommunicator(RtSolverBidder.simulatedTimeBuilder(OBJ_FUNC, solver)
            .withBidFunction(bf)
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
            StopConditions.limitedTime(8 * 60 * 60 * 1000)))
          .build();
      }
    }
  }

  @AutoValue
  abstract static class ResultObject {

    abstract StatisticsDTO getStats();

    abstract Optional<AuctionStats> getAuctionStats();

    static ResultObject create(StatisticsDTO simulationStats,
        Optional<AuctionStats> auctionStats) {
      return new AutoValue_Evaluator_ResultObject(simulationStats,
        auctionStats);
    }
  }

  @AutoValue
  abstract static class AuctionStats {
    abstract int getNumParcels();

    abstract int getNumReauctions();

    abstract int getNumUnsuccesfulReauctions();

    abstract int getNumFailedReauctions();

    static AuctionStats create(int numP, int numR, int numUn, int numF) {
      return new AutoValue_Evaluator_AuctionStats(numP, numR, numUn,
        numF);
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
          PostProcessors.statisticsPostProcessor(OBJ_FUNC)
            .collectResults(sim, args);
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
