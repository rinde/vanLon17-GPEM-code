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

import com.github.rinde.ecj.PriorityHeuristic;
import com.github.rinde.evo4mas.common.EvoBidder;
import com.github.rinde.evo4mas.common.GpGlobal;
import com.github.rinde.logistics.pdptw.mas.TruckFactory.DefaultTruckFactory;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionCommModel;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionPanel;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionStopConditions;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionTimeStatsLogger;
import com.github.rinde.logistics.pdptw.mas.comm.Communicator;
import com.github.rinde.logistics.pdptw.mas.comm.DoubleBid;
import com.github.rinde.logistics.pdptw.mas.route.RoutePlanner;
import com.github.rinde.logistics.pdptw.mas.route.RtSolverRoutePlanner;
import com.github.rinde.logistics.pdptw.solver.CheapestInsertionHeuristic;
import com.github.rinde.logistics.pdptw.solver.optaplanner.OptaplannerSolvers;
import com.github.rinde.rinsim.central.SolverModel;
import com.github.rinde.rinsim.central.rt.RtSolverModel;
import com.github.rinde.rinsim.central.rt.RtStAdapters;
import com.github.rinde.rinsim.core.model.time.RealtimeClockLogger;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import com.github.rinde.rinsim.pdptw.common.RoutePanel;
import com.github.rinde.rinsim.pdptw.common.RouteRenderer;
import com.github.rinde.rinsim.pdptw.common.TimeLinePanel;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06ObjectiveFunction;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PDPModelRenderer;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.google.common.collect.ImmutableMap;

/**
 * 
 * @author Rinde van Lon
 */
public class GPEM17 {

  public static final Gendreau06ObjectiveFunction OBJ_FUNC =
    Gendreau06ObjectiveFunction.instance(50d);

  public static final ImmutableMap<String, String> EXPECTED_REGEXES =
    ImmutableMap.of(
      ".*0\\.50-20-1\\.00-\\d*\\.scen", "50-20-1",
      ".*0\\.80-5-1\\.00-\\d*\\.scen", "80-5-1",
      ".*0\\.20-35-1\\.00-\\d*\\.scen", "20-35-1",
      ".*-1\\.00-\\d*\\.scen", "mixed");

  public static View.Builder gui() {
    return View.builder()
      .withAutoPlay()
      .withSpeedUp(64)
      .withAutoClose()
      .withResolution(1280, 768)
      .with(PlaneRoadModelRenderer.builder())
      .with(PDPModelRenderer.builder())
      .with(RoadUserRenderer.builder())// .withToStringLabel())
      .with(AuctionPanel.builder())
      .with(RoutePanel.builder())
      .with(RouteRenderer.builder())
      .with(TimeLinePanel.builder());
  }

  public static MASConfiguration createConfig(
      PriorityHeuristic<GpGlobal> solver,
      StochasticSupplier<? extends RoutePlanner> rp,
      StochasticSupplier<? extends Communicator> cm,
      boolean rt,
      String name,
      boolean enableTimeMeasurements) {
    MASConfiguration.Builder builder = MASConfiguration.pdptwBuilder()
      .setName(name)
      .addEventHandler(AddVehicleEvent.class,
        DefaultTruckFactory.builder()
          .setRoutePlanner(rp)
          .setCommunicator(cm)
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
        .withMaxAuctionDuration(30 * 60 * 1000L));

    if (rt) {
      builder
        .addModel(RtSolverModel.builder()
          .withThreadPoolSize(3)
          .withThreadGrouping(true))
        .addModel(RealtimeClockLogger.builder());
    } else {
      builder.addModel(SolverModel.builder());
    }
    if (enableTimeMeasurements) {
      builder.addModel(AuctionTimeStatsLogger.builder());
    }

    // .addEventHandler(AddParcelEvent.class, AddParcelEvent.namedHandler())
    return builder.build();
  }

  public enum ReauctOpt {
    CIH, EVO;
  }

  public enum RpOpt {
    CIH {
      StochasticSupplier<RoutePlanner> create() {
        return RtSolverRoutePlanner.supplier(RtStAdapters.toRealtime(
          CheapestInsertionHeuristic.supplier(GPEM17.OBJ_FUNC)));
      }
    },

    OPTA_PLANNER {
      StochasticSupplier<RoutePlanner> create() {
        final long rpMs = 2500L;
        final String masSolverName =
          "Step-counting-hill-climbing-with-entity-tabu-and-strategic-oscillation";
        return RtSolverRoutePlanner.supplier(
          OptaplannerSolvers.builder()
            .withSolverXmlResource(
              "com/github/rinde/jaamas16/jaamas-solver.xml")
            .withObjectiveFunction(GPEM17.OBJ_FUNC)
            .withName(masSolverName)
            .withUnimprovedMsLimit(rpMs)
            .buildRealtimeSolverSupplier());
      }
    };

    abstract StochasticSupplier<RoutePlanner> create();

  }

  public static MASConfiguration createRtConfig(
      PriorityHeuristic<GpGlobal> solver,
      String id,
      ReauctOpt reauctOpt,
      Gendreau06ObjectiveFunction objFunc,
      RpOpt rpOpt,
      boolean enableTimeMeasurements,
      long computationDelay) {

    EvoBidder.Builder cm = EvoBidder.realtimeBuilder(solver, objFunc)
      .withTimeMeasurement(enableTimeMeasurements)
      .withComputationDelay(computationDelay)
      .withReauctionCooldownPeriod(60000);

    if (reauctOpt == ReauctOpt.CIH) {
      cm = cm.withCheapestInsertionHeuristicForReauction();
    } else {
      cm = cm.withPriorityHeuristicForReauction();
    }
    String name =
      "RTMAS-RP-" + rpOpt.name() + "-BID-EVO-REAUCT-" + reauctOpt + "-" + id;
    return createConfig(solver, rpOpt.create(), cm, true, name,
      enableTimeMeasurements);
  }

  public static MASConfiguration createStConfig(
      PriorityHeuristic<GpGlobal> solver,
      String id, ReauctOpt reauctOpt,
      Gendreau06ObjectiveFunction objFunc,
      boolean enableTimeMeasurements) {
    StochasticSupplier<RoutePlanner> rp =
      RtSolverRoutePlanner.simulatedTimeSupplier(
        CheapestInsertionHeuristic.supplier(objFunc));
    EvoBidder.Builder cm = EvoBidder.simulatedTimeBuilder(solver, objFunc)
      .withTimeMeasurement(enableTimeMeasurements)
      .withReauctionCooldownPeriod(60000);

    if (reauctOpt == ReauctOpt.CIH) {
      cm = cm.withCheapestInsertionHeuristicForReauction();
    } else {
      cm = cm.withPriorityHeuristicForReauction();
    }
    String name = "STMAS-RP-CIH-BID-EVO-REAUCT-" + reauctOpt + "-" + id;
    return createConfig(solver, rp, cm, false, name, enableTimeMeasurements);
  }

  public static Gendreau06ObjectiveFunction parseObjFuncWeights(
      String objFuncWeights) {
    String[] parts = objFuncWeights.split("-");
    double tt = Double.parseDouble(parts[0]);
    double td = Double.parseDouble(parts[1]);
    double ot = Double.parseDouble(parts[2]);
    System.out.println(
      "Using objective function weights: tt:" + tt + " td:" + td + " ot:" + ot);
    return Gendreau06ObjectiveFunction.instance(50d, tt, td, ot);
  }

}
