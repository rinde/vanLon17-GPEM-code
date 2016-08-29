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

import java.util.Set;

import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.pdptw.common.StatisticsProvider;
import com.github.rinde.rinsim.scenario.StopCondition;
import com.google.common.collect.ImmutableSet;

/**
 * 
 * @author Rinde van Lon
 */
enum EvoStopCondition implements StopCondition {
  INSTANCE {
    private final long MAX_TIME = 1 * 60 * 60 * 1000L;
    private final int PARCEL_THRESHOLD = 20;

    @Override
    public ImmutableSet<Class<?>> getTypes() {
      return ImmutableSet.of(Clock.class, StatisticsProvider.class,
        RoadModel.class);
    }

    // if no more than half the vehicles have moved after two hours, we should
    // better stop
    @Override
    public boolean evaluate(TypeProvider provider) {
      long time = provider.get(Clock.class).getCurrentTime();

      // only check every minute
      if (time % 60000 == 0) {
        StatisticsDTO stats =
          provider.get(StatisticsProvider.class).getStatistics();

        RoadModel rm = provider.get(RoadModel.class);
        Set<RouteFollowingVehicle> vehicles =
          rm.getObjectsOfType(RouteFollowingVehicle.class);

        checkState(vehicles.size() == stats.totalVehicles);

        int routeSizeLimit = (int) Math.ceil(.5 * ((stats.totalParcels * 2d)));
        int numNonEmptyRoutes = 0;
        for (RouteFollowingVehicle v : vehicles) {
          if (!v.getRoute().isEmpty()) {
            numNonEmptyRoutes++;

            if (stats.totalParcels > 20
              && v.getRoute().size() > routeSizeLimit) {
              return true;
            }
          }
        }

        // only one vehicle has a route and only one has moved
        // if (numNonEmptyRoutes == 1
        // && stats.movedVehicles == 1
        // && stats.totalParcels > PARCEL_THRESHOLD) {
        // return true;
        // }
        //
        // if (time > MAX_TIME && stats.totalParcels > PARCEL_THRESHOLD) {
        // return stats.movedVehicles <= stats.totalVehicles / 2;
        // }

      }
      return false;
    }
  }
}
