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

import java.util.Arrays;
import java.util.Collection;

import com.github.rinde.ecj.GPFunc;
import com.github.rinde.ecj.GPFuncSet;
import com.github.rinde.ecj.GenericFunctions.Add;
import com.github.rinde.ecj.GenericFunctions.Constant;
import com.github.rinde.ecj.GenericFunctions.Div;
import com.github.rinde.ecj.GenericFunctions.If4;
import com.github.rinde.ecj.GenericFunctions.Max;
import com.github.rinde.ecj.GenericFunctions.Min;
import com.github.rinde.ecj.GenericFunctions.Mul;
import com.github.rinde.ecj.GenericFunctions.Neg;
import com.github.rinde.ecj.GenericFunctions.Pow;
import com.github.rinde.ecj.GenericFunctions.Sub;
import com.github.rinde.evo4mas.common.GlobalStateObjectFunctions.GpGlobal;
import com.github.rinde.evo4mas.common.GlobalStateObjectFunctions.InsertionCost;
import com.github.rinde.evo4mas.common.GlobalStateObjectFunctions.InsertionFlexibility;
import com.github.rinde.evo4mas.common.GlobalStateObjectFunctions.InsertionOverTime;
import com.github.rinde.evo4mas.common.GlobalStateObjectFunctions.InsertionTardiness;
import com.github.rinde.evo4mas.common.GlobalStateObjectFunctions.InsertionTravelTime;
import com.github.rinde.evo4mas.common.GlobalStateObjectFunctions.Slack;
import com.github.rinde.evo4mas.common.GlobalStateObjectFunctions.Time;
import com.github.rinde.evo4mas.common.GlobalStateObjectFunctions.TimeLeft;

/**
 * 
 * @author Rinde van Lon
 */
public class FunctionSet extends GPFuncSet<GpGlobal> {

  @Override
  public Collection<GPFunc<GpGlobal>> create() {
    return Arrays.asList(
      /* GENERIC FUNCTIONS */
      new If4<GpGlobal>(),
      new Add<GpGlobal>(),
      new Sub<GpGlobal>(),
      new Div<GpGlobal>(),
      new Mul<GpGlobal>(),
      new Pow<GpGlobal>(),
      new Neg<GpGlobal>(),
      new Min<GpGlobal>(),
      new Max<GpGlobal>(),
      /* CONSTANTS */
      new Constant<GpGlobal>(10),
      new Constant<GpGlobal>(2),
      new Constant<GpGlobal>(1),
      new Constant<GpGlobal>(0),
      new InsertionFlexibility(),
      new InsertionCost(),
      new InsertionTravelTime(),
      new InsertionTardiness(),
      new InsertionOverTime(),
      new Time(),
      new TimeLeft(),
      new Slack());
  }

}
