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

import java.util.Collection;

import com.github.rinde.ecj.GPFunc;
import com.github.rinde.ecj.GPFuncSet;
import com.github.rinde.ecj.GenericFunctions.Add;
import com.github.rinde.ecj.GenericFunctions.Constant;
import com.github.rinde.ecj.GenericFunctions.Div;
import com.github.rinde.ecj.GenericFunctions.If4;
import com.github.rinde.ecj.GenericFunctions.Mul;
import com.github.rinde.ecj.GenericFunctions.Pow;
import com.github.rinde.ecj.GenericFunctions.Sub;
import com.github.rinde.evo4mas.common.VehicleParcelContext;

/**
 * 
 * @author Rinde van Lon
 */
public class FunctionSet extends GPFuncSet<VehicleParcelContext> {

  @Override
  public Collection<GPFunc<VehicleParcelContext>> create() {
    return asList(
      /* GENERIC FUNCTIONS */
      new If4<VehicleParcelContext>(), /* */
      new Add<VehicleParcelContext>(), /* */
      new Sub<VehicleParcelContext>(), /* */
      new Div<VehicleParcelContext>(), /* */
      new Mul<VehicleParcelContext>(), /* */
      new Pow<VehicleParcelContext>(),
      /* CONSTANTS */
      new Constant<VehicleParcelContext>(1), /* */
      new Constant<VehicleParcelContext>(0), /* */
      /* PROBLEM SPECIFIC VARIABLES */
      new Urgency());
  }

  public static class Urgency extends GPFunc<VehicleParcelContext> {
    public double execute(double[] input, VehicleParcelContext context) {
      if (context.isPickup()) {
        return context.parcel().getPickupTimeWindow().end() - context.time();
      }
      return context.parcel().getDeliveryTimeWindow().end() - context.time();
    }
  }
}
