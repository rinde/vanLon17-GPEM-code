package com.github.rinde.gpem17.eval;

import static com.google.common.base.Preconditions.checkState;

import java.io.Serializable;
import java.util.ArrayList;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rinde.gpem17.AuctionStats;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionCommModel;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionTimeStatsLogger;
import com.github.rinde.logistics.pdptw.mas.comm.AuctionCommModel.AuctionEvent;
import com.github.rinde.logistics.pdptw.mas.comm.Bidder;
import com.github.rinde.rinsim.central.SolverTimeMeasurement;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.time.RealtimeClockLogger;
import com.github.rinde.rinsim.core.model.time.RealtimeClockLogger.LogEntry;
import com.github.rinde.rinsim.core.model.time.RealtimeTickInfo;
import com.github.rinde.rinsim.experiment.Experiment.SimArgs;
import com.github.rinde.rinsim.experiment.PostProcessor;
import com.github.rinde.rinsim.pdptw.common.ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.pdptw.common.StatsTracker;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;

class GpemPostProcessor
    implements PostProcessor<RtExperimentInfo>, Serializable {
  static final Logger LOGGER = LoggerFactory.getLogger(GpemPostProcessor.class);

  private static final long serialVersionUID = 5997690791395717045L;
  final ObjectiveFunction objectiveFunction;
  final boolean reportErrs;
  final FailureStrategy failureStrategy;

  GpemPostProcessor(ObjectiveFunction objFunc, FailureStrategy strategy,
      boolean reportErrors) {
    objectiveFunction = objFunc;
    failureStrategy = strategy;
    reportErrs = reportErrors;
  }

  @Override
  public RtExperimentInfo collectResults(Simulator sim, SimArgs args) {
    // sim stats should always be present
    final StatisticsDTO stats =
      sim.getModelProvider().getModel(StatsTracker.class).getStatistics();
    if (failureStrategy == FailureStrategy.ABORT_EXPERIMENT_RUN) {
      checkState(objectiveFunction.isValidResult(stats),
        "The simulation did not result in a valid result: %s, SimArgs: %s.",
        stats, args);
    }
    LOGGER.info("success: {}", args);
    // optional stats below

    @Nullable
    final AuctionCommModel<?> auctionModel =
      sim.getModelProvider().tryGetModel(AuctionCommModel.class);
    final Optional<AuctionStats> aStats;
    if (auctionModel == null) {
      aStats = Optional.absent();
    } else {
      final int parcels = auctionModel.getNumParcels();
      final int reauctions = auctionModel.getNumAuctions() - parcels;
      final int unsuccessful = auctionModel.getNumUnsuccesfulAuctions();
      final int failed = auctionModel.getNumFailedAuctions();
      aStats = Optional
        .of(AuctionStats.create(parcels, reauctions, unsuccessful, failed));
    }

    @Nullable
    final AuctionTimeStatsLogger auctionLogger =
      sim.getModelProvider().tryGetModel(AuctionTimeStatsLogger.class);
    ImmutableList<AuctionEvent> finishEvents;
    ImmutableListMultimap<Bidder<?>, SolverTimeMeasurement> timeMeasurements;
    if (auctionLogger != null) {
      finishEvents = auctionLogger.getAuctionFinishEvents();
      timeMeasurements =
        (ImmutableListMultimap<Bidder<?>, SolverTimeMeasurement>) auctionLogger
          .getTimeMeasurements();
    } else {
      finishEvents = ImmutableList.of();
      timeMeasurements = ImmutableListMultimap.of();
    }

    @Nullable
    final RealtimeClockLogger logger =
      sim.getModelProvider().tryGetModel(RealtimeClockLogger.class);
    if (logger == null) {
      return RtExperimentInfo.create(new ArrayList<LogEntry>(),
        0,
        sim.getCurrentTime() / sim.getTimeStep(),
        stats,
        ImmutableList.<RealtimeTickInfo>of(),
        aStats,
        finishEvents,
        timeMeasurements);
    }

    return RtExperimentInfo.create(logger.getLog(), logger.getRtCount(),
      logger.getStCount(), stats, logger.getTickInfoList(), aStats,
      finishEvents, timeMeasurements);
  }

  @Override
  public FailureStrategy handleFailure(Exception e, Simulator sim,
      SimArgs args) {

    if (reportErrs) {
      System.out.println("Fail: " + args);
      e.printStackTrace();
    }
    return failureStrategy;
  }
}
