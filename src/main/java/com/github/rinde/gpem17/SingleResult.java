package com.github.rinde.gpem17;

import com.github.rinde.ecj.GPComputationResult;
import com.github.rinde.rinsim.experiment.Experiment.SimulationResult;
import com.google.auto.value.AutoValue;

@AutoValue
abstract class SingleResult implements GPComputationResult {
  abstract SimulationResult getSimulationResult();

  static SingleResult create(float fitness, String id, SimulationResult sr) {
    return new AutoValue_SingleResult(fitness, id, sr);
  }
}
