package com.github.rinde.gpem17;

import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;

@AutoValue
abstract class ResultObject {

  abstract StatisticsDTO getStats();

  abstract Optional<AuctionStats> getAuctionStats();

  static ResultObject create(StatisticsDTO simulationStats,
      Optional<AuctionStats> auctionStats) {
    return new AutoValue_ResultObject(simulationStats,
      auctionStats);
  }
}
