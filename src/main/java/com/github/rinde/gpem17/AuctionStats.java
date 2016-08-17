package com.github.rinde.gpem17;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class AuctionStats {
  abstract int getNumParcels();

  abstract int getNumReauctions();

  abstract int getNumUnsuccesfulReauctions();

  abstract int getNumFailedReauctions();

  static AuctionStats create(int numP, int numR, int numUn, int numF) {
    return new AutoValue_AuctionStats(numP, numR, numUn,
      numF);
  }
}
