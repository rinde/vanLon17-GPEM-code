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
import com.github.rinde.evo4mas.common.GlobalStateObjectFunctions.GpGlobal;
import com.github.rinde.logistics.pdptw.mas.TruckFactory.DefaultTruckFactory;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionCommModel;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionPanel;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionStopConditions;
import com.github.rinde.logistics.pdptw.mas.comm.Communicator;
import com.github.rinde.logistics.pdptw.mas.comm.DoubleBid;
import com.github.rinde.logistics.pdptw.mas.route.RoutePlanner;
import com.github.rinde.logistics.pdptw.mas.route.RtSolverRoutePlanner;
import com.github.rinde.logistics.pdptw.solver.CheapestInsertionHeuristic;
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

/**
 * 
 * @author Rinde van Lon
 */
public class GPEM17 {

  public static final Gendreau06ObjectiveFunction OBJ_FUNC =
    Gendreau06ObjectiveFunction.instance(50d);

  public static View.Builder gui() {
    return View.builder()
      .withAutoPlay()
      .withSpeedUp(64)
      .withAutoClose()
      .withResolution(1280, 768)
      .with(PlaneRoadModelRenderer.builder())
      .with(PDPModelRenderer.builder())
      .with(RoadUserRenderer.builder().withToStringLabel())
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
      String name) {
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
    // .addEventHandler(AddParcelEvent.class, AddParcelEvent.namedHandler())
    return builder.build();
  }

  public static MASConfiguration createRtConfig(
      PriorityHeuristic<GpGlobal> solver,
      String id) {
    StochasticSupplier<RoutePlanner> rp =
      RtSolverRoutePlanner.supplier(
        RtStAdapters
          .toRealtime(CheapestInsertionHeuristic.supplier(OBJ_FUNC)));

    StochasticSupplier<? extends Communicator> cm =
      EvoBidder.realtimeBuilder(solver, OBJ_FUNC)
        .withReauctionCooldownPeriod(60000)
        .withPriorityHeuristicForReauction();

    String name = "ReAuction-RP-EVO-BID-EVO-" + id;
    return GPEM17.createConfig(solver, rp, cm, true, name);
  }

  public static MASConfiguration createStConfig(
      PriorityHeuristic<GpGlobal> solver,
      String id) {
    StochasticSupplier<RoutePlanner> rp =
      RtSolverRoutePlanner.simulatedTimeSupplier(
        CheapestInsertionHeuristic.supplier(OBJ_FUNC));

    StochasticSupplier<? extends Communicator> cm =
      EvoBidder.simulatedTimeBuilder(solver, OBJ_FUNC)
        .withReauctionCooldownPeriod(60000)
        .withPriorityHeuristicForReauction();

    String name = "ReAuction-RP-EVO-BID-EVO-" + id;

    return createConfig(solver, rp, cm, false, name);
  }

}
