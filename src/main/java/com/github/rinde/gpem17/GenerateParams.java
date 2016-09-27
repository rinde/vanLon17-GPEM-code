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

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

/**
 * 
 * @author Rinde van Lon
 */
public class GenerateParams {

  static final ImmutableList<Long> SEEDS =
    ImmutableList.of(123L, 13568108L, 74621297L, 22575559L, 93668490L,
      14702894L, 77141177L, 90984713L, 45252814L, 11899292L);

  /**
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {

    for (long seed : SEEDS) {
      for (Map.Entry<String, String> regexEntry : GPEM17.EXPECTED_REGEXES
        .entrySet()) {

        String regex = regexEntry.getKey().replace("\\", "\\\\");
        String name = regexEntry.getValue();

        int numScensPerGen = 50;
        int numScensInLastGen = 250;
        if (name.equals("mixed")) {
          numScensPerGen = 54;
          numScensInLastGen = 270;
        }

        StringBuilder sb =
          new StringBuilder("parent.0 = ../gpem17common.params");
        sb.append(System.lineSeparator())
          .append(System.lineSeparator())
          .append("seed.0 = ")
          .append(seed)
          .append(System.lineSeparator())
          .append("eval.num_scenarios_per_gen = ")
          .append(Integer.toString(numScensPerGen))
          .append(System.lineSeparator())
          .append("eval.num_scenarios_in_last_gen = ")
          .append(Integer.toString(numScensInLastGen))
          .append(System.lineSeparator())
          .append("eval.scenarios_regex = ")
          .append(regex)
          .append(System.lineSeparator());

        File dir = new File("files/config/experiments");
        dir.mkdir();

        File f = new File(dir, seed + "-" + name + ".params");
        Files.write(sb.toString(), f, Charsets.UTF_8);

      }
    }

  }

}
