package com.github.rinde.gpem17;

import java.io.Serializable;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class AuctionStats implements Serializable {
  private static final long serialVersionUID = 1274657052180048653L;

  abstract int getNumParcels();

  abstract int getNumReauctions();

  abstract int getNumUnsuccesfulReauctions();

  abstract int getNumFailedReauctions();

  static AuctionStats create(int numP, int numR, int numUn, int numF) {
    return new AutoValue_AuctionStats(numP, numR, numUn,
      numF);
  }
}
