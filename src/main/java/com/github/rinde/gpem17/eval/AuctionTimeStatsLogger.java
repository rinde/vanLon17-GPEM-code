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

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.List;

import com.github.rinde.evo4mas.common.EvoBidder;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionCommModel;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionCommModel.AuctionEvent;
import com.github.rinde.logistics.pdptw.mas.comm.Bidder;
import com.github.rinde.logistics.pdptw.mas.comm.DoubleBid;
import com.github.rinde.logistics.pdptw.mas.comm.RtSolverBidder;
import com.github.rinde.rinsim.central.Solvers.SolverTimeMeasurement;
import com.github.rinde.rinsim.central.rt.MeasurableRealtimeSolver;
import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * 
 * @author Rinde van Lon
 */
public class AuctionTimeStatsLogger extends AbstractModel<Bidder<DoubleBid>> {
  private final List<AuctionEvent> finishEvents;
  private final List<Bidder> bidders;

  AuctionTimeStatsLogger(AuctionCommModel<?> model) {

    finishEvents = new ArrayList<>();
    bidders = new ArrayList<>();

    model.getEventAPI().addListener(new Listener() {
      @Override
      public void handleEvent(Event e) {
        finishEvents.add((AuctionEvent) e);
      }
    }, AuctionCommModel.EventType.FINISH_AUCTION);
  }

  ImmutableListMultimap<Bidder<?>, SolverTimeMeasurement> getTimeMeasurements() {
    ListMultimap<Bidder<?>, SolverTimeMeasurement> map =
      ArrayListMultimap.create();
    for (Bidder<?> b : bidders) {
      if (b instanceof EvoBidder) {
        EvoBidder eb = (EvoBidder) b;
        checkState(!eb.getTimeMeasurements().isEmpty());
        map.putAll(eb, eb.getTimeMeasurements());
      } else if (b instanceof RtSolverBidder) {
        RtSolverBidder sb = (RtSolverBidder) b;
        if (sb.getSolver() instanceof MeasurableRealtimeSolver) {
          map.putAll(sb,
            ((MeasurableRealtimeSolver) sb.getSolver()).getTimeMeasurements());
        }
      }
    }
    return ImmutableListMultimap.copyOf(map);
  }

  ImmutableList<AuctionEvent> getAuctionFinishEvents() {
    return ImmutableList.copyOf(finishEvents);
  }

  public static Builder builder() {
    return new AutoValue_AuctionTimeStatsLogger_Builder();
  }

  @AutoValue
  abstract static class Builder
      extends AbstractModelBuilder<AuctionTimeStatsLogger, Bidder<DoubleBid>> {

    private static final long serialVersionUID = 5417079781123182630L;

    Builder() {
      setDependencies(AuctionCommModel.class);
    }

    @Override
    public AuctionTimeStatsLogger build(DependencyProvider dependencyProvider) {
      return new AuctionTimeStatsLogger(
        dependencyProvider.get(AuctionCommModel.class));
    }
  }

  @Override
  public boolean register(Bidder<DoubleBid> element) {
    bidders.add(element);
    return true;
  }

  @Override
  public boolean unregister(Bidder<DoubleBid> element) {
    return false;
  }
}
