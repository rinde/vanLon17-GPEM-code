package com.github.rinde.gpem17.eval;

import java.io.Serializable;
import java.util.List;

import com.github.rinde.gpem17.AuctionStats;
import com.github.rinde.rinsim.core.model.time.RealtimeClockLogger.LogEntry;
import com.github.rinde.rinsim.core.model.time.RealtimeTickInfo;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

@AutoValue
public abstract class RtExperimentInfo implements Serializable {
  private static final long serialVersionUID = 6324066851233398736L;

  RtExperimentInfo() {}

  abstract List<LogEntry> getLog();

  abstract long getRtCount();

  abstract long getStCount();

  public abstract StatisticsDTO getStats();

  abstract ImmutableList<RealtimeTickInfo> getTickInfoList();

  public abstract Optional<AuctionStats> getAuctionStats();

  static RtExperimentInfo create(List<LogEntry> log, long rt, long st,
      StatisticsDTO stats, ImmutableList<RealtimeTickInfo> dev,
      Optional<AuctionStats> aStats) {
    return new AutoValue_RtExperimentInfo(log, rt, st, stats, dev, aStats);
  }
}
