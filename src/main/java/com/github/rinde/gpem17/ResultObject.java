package com.github.rinde.gpem17;

import java.io.Serializable;

import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;

@AutoValue
abstract class ResultObject implements Serializable {
  private static final long serialVersionUID = -8066819797938008092L;

  abstract StatisticsDTO getStats();

  abstract Optional<AuctionStats> getAuctionStats();

  static ResultObject create(StatisticsDTO simulationStats,
      Optional<AuctionStats> auctionStats) {
    return new AutoValue_ResultObject(simulationStats,
      auctionStats);
  }
}
