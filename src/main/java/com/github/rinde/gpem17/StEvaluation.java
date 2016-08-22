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
      "(x (+ (neg (- (max (- (- (max (- (pow insertioncost 2.0) (max (neg 10.0) insertiontardiness)) (/ (/ insertioncost (min insertiontardiness time)) (max (/ insertionflexibility (if4 1.0 insertioncost insertionflexibility insertionflexibility)) (- (x 10.0 10.0) (max (max (x timeleft insertioncost) 0.0) insertioncost))))) (+ (- (if4 (- insertiontardiness 2.0) (pow 0.0 timeleft) (if4 2.0 timeleft 10.0 10.0) (x 2.0 2.0)) (pow (neg 1.0) (+ timeleft insertioncost))) (if4 (x (+ insertionflexibility 10.0) (x 10.0 10.0)) (min insertioncost 0.0) (neg (if4 0.0 insertiontraveltime timeleft insertionovertime)) insertiontardiness))) (pow 0.0 timeleft)) (/ (+ (pow 0.0 timeleft) (neg 0.0)) (max (max insertionovertime insertionovertime) (- 1.0 insertionflexibility)))) (+ (- (if4 (- (x 2.0 2.0) 2.0) (pow 0.0 timeleft) (if4 2.0 timeleft 10.0 10.0) (+ (if4 2.0 insertiontraveltime 2.0 time) (if4 (x (+ insertionflexibility 10.0) (x 10.0 (/ insertionflexibility insertionovertime))) (/ (/ insertionflexibility insertionovertime) (min insertiontardiness time)) (neg (if4 0.0 insertiontraveltime timeleft insertionovertime)) (/ (/ insertionflexibility insertionovertime) (/ 0.0 slack))))) (pow (/ 0.0 slack) (+ timeleft insertioncost))) (if4 (x (+ insertionflexibility 10.0) (x 10.0 10.0)) (max (max 10.0 insertionflexibility) (neg (+ insertionflexibility insertioncost))) (neg (if4 0.0 insertiontraveltime timeleft insertionovertime)) (/ (x (+ insertionflexibility 10.0) (x 10.0 10.0)) (min insertiontardiness (pow 0.0 timeleft))))))) (neg insertioncost)) (neg (max (x timeleft insertioncost) time)))",
      new FunctionSet().create());

    Evaluator.evaluate(asList(prog),
      FileProvider.builder()
        .add(Paths.get(Evaluator.TRAINSET_PATH))
        // .filter("regex:.*0\\.50-20-1\\.00-[01]?[0-9]\\.scen")
        .filter("regex:.*0\\.50-20-1\\.00-80\\.scen")
        .build().get(),
      false);
    System.out.println("Done.");
  }

}
