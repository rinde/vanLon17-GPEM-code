package com.github.rinde.gpem17;

import java.io.Serializable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class AuctionStats implements Serializable {
  private static final long serialVersionUID = 1274657052180048653L;

  public abstract int getNumParcels();

  public abstract int getNumReauctions();

  public abstract int getNumUnsuccesfulReauctions();

  public abstract int getNumFailedReauctions();

  public static AuctionStats create(int numP, int numR, int numUn, int numF) {
    return new AutoValue_AuctionStats(numP, numR, numUn,
      numF);
  }
}
