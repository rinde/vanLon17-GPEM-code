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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import com.github.rinde.ecj.GPFunc;
import com.github.rinde.ecj.GPProgram;
import com.github.rinde.ecj.GPProgramParser;
import com.github.rinde.evo4mas.common.GlobalStateObjectFunctions.GpGlobal;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.experiment.CommandLineProgress;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.SimulationProperty;
import com.github.rinde.rinsim.io.FileProvider;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.ScenarioIO;
import com.github.rinde.rinsim.scenario.StopConditions;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.io.Files;

/**
 * 
 * @author Rinde van Lon
 */
public class RtEvaluation {
  static final String DATASET_PATH = "files/vanLonHolvoet15/";
  static final String RESULTS_DIR = "files/results/realtime/";

  public static void main(String[] args) {
    final long startTime = System.currentTimeMillis();
    System.out.println(System.getProperty("java.vm.name") + ", "
      + System.getProperty("java.vm.vendor") + ", "
      + System.getProperty("java.vm.version") + " (runtime version: "
      + System.getProperty("java.runtime.version") + ")");
    System.out.println(System.getProperty("os.name") + " "
      + System.getProperty("os.version") + " "
      + System.getProperty("os.arch"));
    checkArgument(System.getProperty("java.vm.name").contains("Server"),
      "Experiments should be run in a JVM in server mode.");

    List<Path> ps = new ArrayList<>(FileProvider.builder()
      .add(Paths.get(DATASET_PATH))
      // .filter("regex:.*0\\.20-20-1\\.00-0\\.scen")
      .filter("regex:.*\\.scen")
      .build().get().asList());

    ResultWriter rw =
      new VanLonHolvoetResultWriter(new File(RESULTS_DIR), Evaluator.OBJ_FUNC);

    Experiment.Builder exp = Evaluator.experimentBuilder(false, ps, false);
    exp.setScenarioReader(
      ScenarioIO.readerAdapter(ScenarioConverter.TO_ONLINE_REALTIME_250))
      .usePostProcessor(new LogProcessor(Evaluator.OBJ_FUNC))
      .computeLocal()
      .withRandomSeed(123)
      .repeat(3)
      // SEED_REPS,REPS,SCENARIO,CONFIG
      .withOrdering(SimulationProperty.SEED_REPS,
        SimulationProperty.REPS,
        SimulationProperty.SCENARIO,
        SimulationProperty.CONFIG)
      .showGui(false)
      .withThreads((int) Math
        .floor((Runtime.getRuntime().availableProcessors() - 1) / 2d))
      .withWarmup(30000)
      .addResultListener(new CommandLineProgress(System.out))
      .addResultListener(rw);

    Collection<GPFunc<GpGlobal>> funcs = new FunctionSet().create();

    List<GPProgram<GpGlobal>> programs = asList(
      GPProgramParser.parseProgramFunc("(insertioncost)", funcs),
      GPProgramParser.parseProgramFunc(
        // best at gen 99
        "(- (pow insertioncost 2.0) (+ (- (if4 (if4 insertiontardiness (pow 0.0 timeleft) (neg (max 10.0 insertionflexibility)) (pow 0.0 timeleft)) (min time (if4 2.0 insertiontraveltime 2.0 time)) (if4 2.0 timeleft 10.0 10.0) (if4 insertiontardiness timeleft (if4 insertiontardiness (pow 0.0 timeleft) (if4 (pow 0.0 timeleft) (if4 0.0 (/ (+ insertionflexibility 10.0) (max (max insertionovertime (max (neg (if4 0.0 insertiontraveltime timeleft insertionovertime)) (x 2.0 2.0))) (+ insertionflexibility 10.0))) timeleft insertionovertime) (if4 insertiontardiness (pow (max 10.0 insertionflexibility) timeleft) (neg (max 10.0 insertionflexibility)) (if4 2.0 insertiontraveltime 2.0 (+ insertionflexibility (+ insertionflexibility insertioncost)))) (max insertionovertime insertionovertime)) (x 2.0 2.0)) (neg (if4 (max (max insertionovertime insertionovertime) (+ insertionflexibility 10.0)) (neg (if4 (- (max (if4 (if4 (min insertiontardiness time) timeleft (- 1.0 insertionflexibility) timeleft) (- (pow 0.0 timeleft) (min insertiontardiness time)) (- (if4 (pow 0.0 timeleft) (pow 0.0 timeleft) (+ insertioncost 2.0) (+ insertioncost 2.0)) (min insertiontardiness time)) (if4 2.0 timeleft 10.0 10.0)) (x 2.0 2.0)) (min insertiontardiness time)) insertiontraveltime timeleft insertionovertime)) (if4 (if4 0.0 insertiontraveltime timeleft insertionovertime) insertiontraveltime timeleft insertionovertime) (x 2.0 2.0))))) (- (if4 (if4 (+ (neg (max 10.0 insertionflexibility)) insertioncost) (pow 0.0 timeleft) (neg (neg (if4 (max (max insertionovertime insertionovertime) (+ insertionflexibility 10.0)) (neg (if4 0.0 insertiontraveltime timeleft insertionovertime)) (if4 (if4 0.0 insertiontraveltime timeleft insertionovertime) insertiontraveltime timeleft insertionovertime) (x 2.0 2.0)))) (if4 insertiontardiness timeleft (if4 insertiontardiness (pow 0.0 timeleft) (if4 (pow 0.0 timeleft) (if4 0.0 (neg (+ insertionflexibility (if4 insertiontardiness (- insertiontardiness 2.0) (neg (pow 0.0 timeleft)) (neg (if4 0.0 insertiontraveltime timeleft insertionovertime))))) timeleft insertionovertime) (if4 insertiontardiness (pow 0.0 timeleft) (neg (max 10.0 insertionflexibility)) (neg timeleft)) (max insertionovertime insertionovertime)) (min insertiontardiness time)) (neg (if4 (if4 2.0 insertiontraveltime 2.0 time) (neg (if4 0.0 insertiontraveltime timeleft insertionovertime)) (if4 (if4 0.0 insertiontraveltime timeleft insertionovertime) insertiontraveltime timeleft insertionovertime) (+ (pow 0.0 timeleft) insertionovertime))))) (max (neg (if4 0.0 insertiontraveltime timeleft insertionovertime)) (min time (if4 2.0 insertiontraveltime 2.0 time))) (- (max (if4 (if4 (min insertiontardiness time) timeleft (- 1.0 insertionflexibility) timeleft) (- (pow 0.0 timeleft) (min insertiontardiness time)) (- (if4 (neg (+ insertioncost 2.0)) (pow 0.0 timeleft) (+ insertioncost 2.0) (+ insertioncost 2.0)) (min insertiontardiness time)) (if4 2.0 timeleft 10.0 10.0)) (x 2.0 2.0)) (min insertiontardiness time)) (neg insertiontraveltime)) (max (if4 (if4 0.0 (+ insertionflexibility insertioncost) timeleft insertionovertime) (if4 2.0 insertiontraveltime 2.0 time) (if4 2.0 insertiontraveltime 2.0 time) (if4 insertiontardiness (pow 0.0 timeleft) (pow 0.0 timeleft) timeleft)) (x 2.0 2.0)))) (if4 (x (+ insertionflexibility 10.0) 10.0) (if4 2.0 insertiontraveltime 2.0 time) (if4 2.0 insertiontraveltime 2.0 time) (if4 insertiontardiness (pow 0.0 timeleft) (pow 0.0 timeleft) timeleft))))",
        funcs));

    int counter = 0;
    StringBuilder sb = new StringBuilder();
    for (GPProgram<GpGlobal> prog : programs) {
      String progId = "c" + counter++;
      sb.append(progId)
        .append(" = ")
        .append(prog.getId())
        .append(System.lineSeparator());

      exp.addConfiguration(Evaluator.createRtConfig(prog, progId));
    };

    try {
      Files.write(sb, new File(rw.getExperimentDirectory(), "configs.txt"),
        Charsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    final Optional<ExperimentResults> results = exp.perform(System.out, args);

    final long duration = System.currentTimeMillis() - startTime;
    if (!results.isPresent()) {
      return;
    }
    System.out.println("Done, computed " + results.get().getResults().size()
      + " simulations in " + duration / 1000d + "s");
  }

  enum ScenarioConverter implements Function<Scenario, Scenario> {
    /**
     * Changes ticksize to 250ms and adds stopcondition with maximum sim time of
     * 10 hours.
     */
    TO_ONLINE_REALTIME_250 {
      @Override
      public Scenario apply(@Nullable Scenario input) {
        final Scenario s = verifyNotNull(input);
        return Scenario.builder(s)
          .removeModelsOfType(TimeModel.AbstractBuilder.class)
          .addModel(TimeModel.builder().withTickLength(250).withRealTime())
          .setStopCondition(StopConditions.or(s.getStopCondition(),
            StopConditions.limitedTime(10 * 60 * 60 * 1000)))
          .build();
      }
    }
  }
}
