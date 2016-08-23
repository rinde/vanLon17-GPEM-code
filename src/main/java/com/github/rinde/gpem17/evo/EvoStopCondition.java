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

import com.github.rinde.rinsim.core.model.time.Clock;
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
    private final long MAX_TIME = 2 * 60 * 60 * 1000L;

    @Override
    public ImmutableSet<Class<?>> getTypes() {
      return ImmutableSet.of(Clock.class, StatisticsProvider.class);
    }

    // if no more than half the vehicles have moved after two hours, we should
    // better stop
    @Override
    public boolean evaluate(TypeProvider provider) {
      long time = provider.get(Clock.class).getCurrentTime();
      if (time > MAX_TIME) {
        StatisticsDTO stats =
          provider.get(StatisticsProvider.class).getStatistics();

        return stats.movedVehicles <= stats.totalVehicles / 2;
      }
      return false;
    }
  }
}
