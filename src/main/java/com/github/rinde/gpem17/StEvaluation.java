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

import java.util.Arrays;
import java.util.List;

import com.github.rinde.ecj.GPFunc;
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
public class StEvaluation {

  public static void main(String[] args) {

    List<GPFunc<GpGlobal>> list =
      Arrays.<GPFunc<GpGlobal>>asList(
        new InsertionFlexibility(),
        new InsertionCost(),
        new InsertionTravelTime(),
        new InsertionTardiness(),
        new InsertionOverTime(),
        new Time(),
        new TimeLeft(),
        new Slack());

    Evaluator.evaluate(list, "glob:**0.50-20-1.00-[0-1]*[0-9].scen");
    System.out.println("Done.");
  }

}
