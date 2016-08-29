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

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import com.github.rinde.ecj.GPFunc;
import com.github.rinde.ecj.GPProgram;
import com.github.rinde.ecj.GPProgramParser;
import com.github.rinde.evo4mas.common.GlobalStateObjectFunctions.GpGlobal;
import com.github.rinde.gpem17.eval.Evaluate;
import com.github.rinde.gpem17.evo.FitnessEvaluator.Converter;
import com.github.rinde.gpem17.evo.FunctionSet;
import com.github.rinde.rinsim.io.FileProvider;

/**
 * 
 * @author Rinde van Lon
 */
public class TestEarlyStop {

  @Test
  public void test() {
    String prog =
      "(x (x (- 0.0 1.0) (neg insertiontraveltime)) (x (/ insertioncost insertioncost) (neg insertioncost)))";

    String prog2 =
      "(/ (neg (x (- 2.0 1.0) (neg insertiontardiness))) (min (x (/ insertiontardiness insertionovertime) (+ timeleft time)) (+ (- 10.0 insertionovertime) (min 1.0 insertionflexibility))))";

    Collection<GPFunc<GpGlobal>> funcs = new FunctionSet().create();

    List<GPProgram<GpGlobal>> progs = new ArrayList<>();
    for (String p : asList(prog, prog2)) {
      progs.add(GPProgramParser.parseProgramFunc(prog, funcs));
    }

    FileProvider.Builder files = FileProvider.builder()
      .add(Paths.get("files/train-dataset"))
      .filter("regex:.*0.50-20-1.00-0\\.scen");

    File parent = new File("files/test/results");
    Evaluate.execute(progs, false, files, parent, true, Converter.INSTANCE,
      "-g", "true", "-t", "1", "--repetitions", "1");

  }

}
