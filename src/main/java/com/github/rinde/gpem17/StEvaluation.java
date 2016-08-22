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

import java.nio.file.Paths;

import com.github.rinde.ecj.GPProgram;
import com.github.rinde.ecj.GPProgramParser;
import com.github.rinde.evo4mas.common.GlobalStateObjectFunctions.GpGlobal;
import com.github.rinde.rinsim.io.FileProvider;

/**
 * 
 * @author Rinde van Lon
 */
public class StEvaluation {

  public static void main(String[] args) {

    // List<GPFunc<GpGlobal>> list =
    // Arrays.<GPFunc<GpGlobal>>asList(
    // new InsertionFlexibility(),
    // new InsertionCost(),
    // new InsertionTravelTime(),
    // new InsertionTardiness(),
    // new InsertionOverTime(),
    // new Time(),
    // new TimeLeft(),
    // new Slack());

    GPProgram<GpGlobal> prog = GPProgramParser.parseProgramFunc(
      "(max (+ (neg (if4 (x slack insertioncost) (x insertionflexibility timeleft) (if4 slack insertionflexibility timeleft timeleft) (neg insertiontraveltime))) (max (- (pow insertioncost 2.0) (/ (max (- 1.0 1.0) (x 0.0 10.0)) (/ (max (max insertionovertime insertionovertime) (- 1.0 insertionflexibility)) (min insertiontardiness time)))) (/ (+ (- 1.0 1.0) (neg 0.0)) (max (max insertionovertime insertionovertime) (- 1.0 insertionflexibility))))) (if4 (pow insertioncost 2.0) (/ (+ (min insertiontardiness (/ 0.0 slack)) (pow 0.0 10.0)) (if4 (min insertiontardiness time) (x 1.0 insertionflexibility) (max (- 1.0 insertionflexibility) (if4 (- insertiontardiness 2.0) (/ (max (- 1.0 1.0) (x 0.0 10.0)) (if4 (if4 timeleft 2.0 2.0 time) (/ insertiontardiness timeleft) (neg 2.0) (pow insertioncost 0.0))) (x 10.0 10.0) (x 2.0 2.0))) (- insertionflexibility 2.0))) (pow (neg insertionovertime) (pow (x 1.0 0.0) (x insertiontardiness insertionflexibility))) (neg (/ (/ (- (max (neg (if4 (x (neg insertioncost) (if4 0.0 insertiontraveltime timeleft insertionovertime)) (x insertionflexibility timeleft) (if4 slack insertionflexibility timeleft timeleft) (/ insertiontardiness timeleft))) (/ (+ (- 1.0 insertionflexibility) (neg 0.0)) (max (max (/ (pow 0.0 10.0) (if4 (x 0.0 10.0) (x 1.0 insertionflexibility) (neg insertionovertime) (- insertionflexibility 2.0))) insertionovertime) (- 1.0 insertionflexibility)))) (+ (- (if4 insertionovertime (/ 10.0 (min insertiontardiness time)) (if4 2.0 timeleft 10.0 (if4 insertiontardiness (max (neg insertionflexibility) (- 1.0 insertionflexibility)) timeleft insertioncost)) (x (x 1.0 0.0) (- (if4 (if4 timeleft 2.0 2.0 time) (/ insertiontardiness timeleft) (neg 2.0) (pow insertioncost 2.0)) insertionflexibility))) (pow (min (if4 insertionovertime time insertioncost insertiontraveltime) (neg insertionovertime)) (+ timeleft insertioncost))) 10.0)) 10.0) (- 10.0 insertionflexibility)))))",
      new FunctionSet().create());

    Evaluator.evaluate(asList(prog),
      FileProvider.builder()
        .add(Paths.get(Evaluator.TRAINSET_PATH))
        // .filter("regex:.*0\\.50-20-1\\.00-[01]?[0-9]\\.scen")
        .filter("regex:.*0\\.50-20-1\\.00-129\\.scen")
        .build().get(),
      false);
    System.out.println("Done.");
  }

}
