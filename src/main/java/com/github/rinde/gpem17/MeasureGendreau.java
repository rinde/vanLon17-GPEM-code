/*
 * Copyright (C) 2015-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;

import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06Parser;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06Scenario;
import com.github.rinde.rinsim.scenario.measure.Metrics;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;
import com.google.common.io.Files;

/**
 *
 * @author Rinde van Lon
 */
public final class MeasureGendreau {
  static final String PROPS_FILE = "files/gendreau-properties.csv";

  static final ImmutableMap<String, ImmutableMap<Property, String>> MAP =
    ImmutableMap.<String, ImmutableMap<Property, String>>builder()
      .put("1_240_24", ImmutableMap.<Property, String>builder()
        .put(Property.GENDR_ALG, "Tabu")
        .put(Property.GENDR_TT, "336")
        .put(Property.GENDR_TARD, "65")
        .put(Property.GENDR_OT, "55")
        .put(Property.GENDR_COST, "456")
        .build())
      .put("2_240_24", ImmutableMap.<Property, String>builder()
        .put(Property.GENDR_ALG, "Tabu")
        .put(Property.GENDR_TT, "386")
        .put(Property.GENDR_TARD, "68")
        .put(Property.GENDR_OT, "53")
        .put(Property.GENDR_COST, "506")
        .build())
      .put("3_240_24", ImmutableMap.<Property, String>builder()
        .put(Property.GENDR_ALG, "Adaptive_descent")
        .put(Property.GENDR_TT, "355")
        .put(Property.GENDR_TARD, "120")
        .put(Property.GENDR_OT, "86")
        .put(Property.GENDR_COST, "562")
        .build())
      .put("4_240_24", ImmutableMap.<Property, String>builder()
        .put(Property.GENDR_ALG, "Tabu")
        .put(Property.GENDR_TT, "359")
        .put(Property.GENDR_TARD, "38")
        .put(Property.GENDR_OT, "31")
        .put(Property.GENDR_COST, "428")
        .build())
      .put("5_240_24", ImmutableMap.<Property, String>builder()
        .put(Property.GENDR_ALG, "Tabu")
        .put(Property.GENDR_TT, "348")
        .put(Property.GENDR_TARD, "75")
        .put(Property.GENDR_OT, "52")
        .put(Property.GENDR_COST, "476")
        .build())
      .put("1_240_33", ImmutableMap.<Property, String>builder()
        .put(Property.GENDR_ALG, "Tabu")
        .put(Property.GENDR_TT, "473")
        .put(Property.GENDR_TARD, "4392")
        .put(Property.GENDR_OT, "699")
        .put(Property.GENDR_COST, "5564")
        .build())
      .put("2_240_33", ImmutableMap.<Property, String>builder()
        .put(Property.GENDR_ALG, "Tabu")
        .put(Property.GENDR_TT, "402")
        .put(Property.GENDR_TARD, "867")
        .put(Property.GENDR_OT, "297")
        .put(Property.GENDR_COST, "1566")
        .build())
      .put("3_240_33", ImmutableMap.<Property, String>builder()
        .put(Property.GENDR_ALG, "Tabu")
        .put(Property.GENDR_TT, "455")
        .put(Property.GENDR_TARD, "853")
        .put(Property.GENDR_OT, "303")
        .put(Property.GENDR_COST, "1611")
        .build())
      .put("4_240_33", ImmutableMap.<Property, String>builder()
        .put(Property.GENDR_ALG, "Adaptive_descent")
        .put(Property.GENDR_TT, "405")
        .put(Property.GENDR_TARD, "1031")
        .put(Property.GENDR_OT, "317")
        .put(Property.GENDR_COST, "1753")
        .build())
      .put("5_240_33", ImmutableMap.<Property, String>builder()
        .put(Property.GENDR_ALG, "Tabu")
        .put(Property.GENDR_TT, "495")
        .put(Property.GENDR_TARD, "4121")
        .put(Property.GENDR_OT, "687")
        .put(Property.GENDR_COST, "5303")
        .build())
      .put("1_450_24", ImmutableMap.<Property, String>builder()
        .put(Property.GENDR_ALG, "Tabu")
        .put(Property.GENDR_TT, "539")
        .put(Property.GENDR_TARD, "1")
        .put(Property.GENDR_OT, "0")
        .put(Property.GENDR_COST, "540")
        .build())
      .put("2_450_24", ImmutableMap.<Property, String>builder()
        .put(Property.GENDR_ALG, "Adaptive_descent")
        .put(Property.GENDR_TT, "567")
        .put(Property.GENDR_TARD, "3")
        .put(Property.GENDR_OT, "2")
        .put(Property.GENDR_COST, "572")
        .build())
      .put("3_450_24", ImmutableMap.<Property, String>builder()
        .put(Property.GENDR_ALG, "Tabu")
        .put(Property.GENDR_TT, "629")
        .put(Property.GENDR_TARD, "2")
        .put(Property.GENDR_OT, "1")
        .put(Property.GENDR_COST, "632")
        .build())
      .put("4_450_24", ImmutableMap.<Property, String>builder()
        .put(Property.GENDR_ALG, "Adaptive_descent")
        .put(Property.GENDR_TT, "695")
        .put(Property.GENDR_TARD, "2")
        .put(Property.GENDR_OT, "1")
        .put(Property.GENDR_COST, "698")
        .build())
      .put("5_450_24", ImmutableMap.<Property, String>builder()
        .put(Property.GENDR_ALG, "Tabu")
        .put(Property.GENDR_TT, "694")
        .put(Property.GENDR_TARD, "0")
        .put(Property.GENDR_OT, "0")
        .put(Property.GENDR_COST, "694")
        .build())
      .build();

  private MeasureGendreau() {}

  /**
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    final List<Gendreau06Scenario> scns =
      new ArrayList<>(Gendreau06Parser.parser()
        .addDirectory("files/gendreau2006/requests").parse());

    Collections.sort(scns, new Comparator<Gendreau06Scenario>() {
      @Override
      public int compare(Gendreau06Scenario o1, Gendreau06Scenario o2) {
        final int compare =
          o1.getProblemClass().getId().compareTo(o2.getProblemClass().getId());
        if (compare == 0) {
          return o1.getProblemInstanceId().compareTo(o2.getProblemInstanceId());
        }
        return compare;
      }
    });

    final List<Map<Property, Object>> propsList = new ArrayList<>();
    for (final Gendreau06Scenario scen : scns) {
      final StatisticalSummary urgency = Metrics.measureUrgency(scen);
      final Multiset<Class<?>> counts = Metrics.getEventTypeCounts(scen);

      final long scenarioLength = scen.getProblemClass().duration * 60000;
      final double dyn = Metrics.measureDynamism(scen, scenarioLength);

      final ImmutableMap<Property, Object> prop =
        ImmutableMap.<Property, Object>builder()
          .put(Property.PROBLEM_CLASS, scen.getProblemClass().getId())
          .put(Property.INSTANCE_ID, scen.getProblemInstanceId())
          .put(Property.DYNAMISM, dyn)
          .put(Property.URGENCY_MEAN, urgency.getMean() / 60000d)
          .put(Property.URGENCY_SD, urgency.getStandardDeviation() / 60000d)
          .put(Property.NUM_ORDERS, counts.count(AddParcelEvent.class))
          .put(Property.NUM_VEHICLES, counts.count(AddVehicleEvent.class))
          .putAll(MAP.get(
            scen.getProblemInstanceId() + scen.getProblemClass().getId()))
          .build();

      propsList.add(prop);
    }

    final File targetFile = new File(PROPS_FILE);
    write(propsList, targetFile, asList(Property.values()));
    System.out.println("Results written to " + targetFile.getAbsolutePath());
  }

  static <T extends Enum<?>> void write(
      Iterable<? extends Map<T, Object>> props, File dest, Iterable<T> keys)
          throws IOException {
    final StringBuilder sb =
      Joiner.on(",").appendTo(new StringBuilder(), keys)
        .append(System.lineSeparator());

    for (final Map<T, Object> row : props) {
      appendValuesTo(sb, row, keys)
        .append(System.lineSeparator());
    }
    Files.write(sb.toString(), dest, Charsets.UTF_8);
  }

  static <T extends Enum<?>> StringBuilder appendValuesTo(StringBuilder sb,
      Map<T, Object> props, Iterable<T> keys) {
    final List<Object> values = new ArrayList<>();
    for (final T p : keys) {
      checkArgument(props.containsKey(p));
      values.add(props.get(p));
    }
    Joiner.on(",").appendTo(sb, values);
    return sb;
  }

  static ImmutableList<ImmutableMap<Property, String>> read(File file)
      throws IOException {

    final List<String> lines = Files.readLines(file, Charsets.UTF_8);
    final List<Property> header =
      Property.valueOf(Arrays.asList(lines.get(0).split(",")));

    final ImmutableList.Builder<ImmutableMap<Property, String>> list =
      ImmutableList.builder();
    for (int i = 1; i < lines.size(); i++) {
      final ImmutableMap.Builder<Property, String> map = ImmutableMap.builder();
      final String[] values = lines.get(i).split(",");
      for (int j = 0; j < header.size(); j++) {
        final Property key = header.get(j);
        map.put(key, values[j]);
      }
      list.add(map.build());
    }
    return list.build();
  }

  enum Property {
    PROBLEM_CLASS,

    INSTANCE_ID,

    DYNAMISM,

    URGENCY_MEAN,

    URGENCY_SD,

    NUM_ORDERS,

    NUM_VEHICLES,

    GENDR_ALG,

    GENDR_COST,

    GENDR_TT,

    GENDR_TARD,

    GENDR_OT;

    @Override
    public String toString() {
      return name().toLowerCase();
    }

    public static List<Property> valueOf(Iterable<String> names) {
      final List<Property> list = new ArrayList<>();
      for (final String nm : names) {
        list.add(valueOf(nm.toUpperCase()));
      }
      return list;
    }
  }
}
