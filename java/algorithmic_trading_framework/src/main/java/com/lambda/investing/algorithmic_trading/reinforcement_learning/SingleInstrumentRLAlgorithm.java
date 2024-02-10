package com.lambda.investing.algorithmic_trading.reinforcement_learning;

import com.google.common.primitives.Doubles;
import com.lambda.investing.ArrayUtils;
import com.lambda.investing.Configuration;
import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.AlgorithmState;
import com.lambda.investing.algorithmic_trading.PnlSnapshot;
import com.lambda.investing.algorithmic_trading.SingleInstrumentAlgorithm;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.action.AbstractAction;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.state.AbstractState;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.market_data_connector.parquet_file_reader.ParquetMarketDataConnectorPublisher;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.rl_gym.StepOutput;
import com.lambda.investing.trading_engine_connector.ordinary.OrdinaryTradingEngine;
import lombok.Getter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.lambda.investing.ArrayUtils.PrintDoubleArrayString;
import static com.lambda.investing.Configuration.DELTA_REWARD_REINFORCEMENT_LEARNING;
import static com.lambda.investing.Configuration.DISCOUNT_REWARD_NO_TRADE;
import static com.lambda.investing.algorithmic_trading.AlgorithmState.STOPPED;
import static com.lambda.investing.algorithmic_trading.reinforcement_learning.SingleInstrumentRLReplier.GetStateColumnsAlgorithm;

@Getter
public abstract class SingleInstrumentRLAlgorithm extends SingleInstrumentAlgorithm {
    protected static int DEFAULT_LAST_Q = -1;//action 0
    protected static long WARNING_WAITING_SPAN_MS = 300;
    //locker for training
    protected CountDownLatch latchIteration;
    protected final Object lockLatchIteration = new Object();
    protected final Object stepLockIteration = new Object();

    private double lastReward;
    private double cumDayReward;

    private double diffReward;
    private double[] lastState;
    public double[] lastAction;

    protected boolean isTraining = false;


    public AbstractState state;
    @Getter
    public AbstractAction action;
    protected AtomicInteger iterations = new AtomicInteger(0);
    protected AtomicBoolean isActionReceived = new AtomicBoolean(false);
    protected long timestampStartStep = DEFAULT_LAST_Q;
    protected int lastStepMillisElapsed = 0;
    protected long msWaitingCounter = 0;
    protected ReinforcementLearningActionType reinforcementLearningActionType;

    protected ScoreEnum scoreEnum;
    protected double rewardStartStep = DEFAULT_LAST_Q;
    protected int tradesStartStep = DEFAULT_LAST_Q;
    protected double midStartStep = DEFAULT_LAST_Q;

    protected List<double[]> actionHistoricalList = new ArrayList<>();

    protected int rlPort = -1;
    protected String rlHost = null;

    protected SingleInstrumentRLReplier replier;

    public void setActionReceived(boolean actionReceived) {
        isActionReceived.set(actionReceived);
    }

    public SingleInstrumentRLAlgorithm(AlgorithmConnectorConfiguration algorithmConnectorConfiguration, String algorithmInfo, Map<String, Object> parameters) {
        super(algorithmConnectorConfiguration, algorithmInfo, parameters);
        waitDone = true;
        //not close the proccess
    }


    public Set<Instrument> getInstruments() {
        Set<Instrument> instruments = super.getInstruments();
        instruments.add(instrument);
        String[] multiMarketOtherInstruments = getParameterArrayString(parameters, "otherInstrumentsStates");
        if (multiMarketOtherInstruments != null && multiMarketOtherInstruments.length > 0) {
            for (String instrument : multiMarketOtherInstruments) {
                Instrument instrument1 = Instrument.getInstrument(instrument);
                if (!instruments.contains(instrument1)) {
                    instruments.add(Instrument.getInstrument(instrument));
                }
            }
        }
        this.instruments = instruments;
        return instruments;
    }

    @Override
    public void setParameters(Map<String, Object> parameters) {
        super.setParameters(parameters);
        setLiveEngine(parameters);
        reinforcementLearningActionType = ReinforcementLearningActionType.valueOf(
                getParameterStringOrDefault(parameters, "reinforcementLearningActionType",
                        ReinforcementLearningActionType.discrete.toString()));
        logger.info("set reinforcementLearningActionType= {}", reinforcementLearningActionType);

    }

    protected void setFilteredState() {
        if (state.isFiltered()) {
            List<String> statesList = state.getFilteredColumns();
            //set the states fitlered because private states
            this.state.setColumnsFilter(statesList.toArray(new String[0]));
        }

    }


    protected void printState() {
        int states = this.state.getNumberOfColumns();
        int actions = this.action.getNumberActions();
        if (this.reinforcementLearningActionType.equals(ReinforcementLearningActionType.continuous)) {
            actions = this.action.getNumberActionColumns();
        }


        System.out.println(Configuration
                .formatLog("{}    states: {} actions: {}", algorithmInfo, states,
                        actions));
        logger.info(Configuration
                .formatLog("{}    states: {} actions: {}", algorithmInfo, states,
                        actions));

        List<String> statesList = state.getFilteredColumns();

        String listStates = ArrayUtils.PrintArrayListString(statesList, ",");
        logger.info("states : {}", listStates);

        System.out.println(Configuration
                .formatLog("states(JAVA) [{}] : {}", statesList.size(), listStates));
        System.out.println(Configuration
                .formatLog("actions(JAVA) [{}] : {}", actions, this.reinforcementLearningActionType.toString()));


    }

    protected void setLiveEngine(Map<String, Object> parameters) {
        //live connector
        if (isBacktest) {
            return;
        }

        this.rlPort = getParameterIntOrDefault(parameters, "rlPort", "rl_port", -1);
        this.rlHost = getParameterStringOrDefault(parameters, "rlHost", "rl_host", null);
        boolean isLive = this.rlPort > 0 && this.rlHost != null;
        if (isLive) {
            setTraining(false);

            String message = String.format("RL LIVE ENGINE: %s:%d", this.rlHost, this.rlPort);
            logger.info(message);
            System.out.println(message);
            ZeroMqConfiguration replierConfiguration = new ZeroMqConfiguration(this.rlHost, this.rlPort, "");
            try {
                replier = SingleInstrumentRLReplier.GetInstance(replierConfiguration, this);
            } catch (IOException e) {
                String errorMessage = "Error creating replier " + e.getMessage();
                logger.error(errorMessage, e);
                System.err.println(message);
                System.exit(-1);

            }

        }
    }

    protected abstract void notifyParameters();

    public boolean isTraining() {
        return isTraining;
    }

    public void setTraining(boolean training) {
        isTraining = training;
        setExitOnStop(!isTraining);
        if (isTraining) {
            OrdinaryTradingEngine.DEFAULT_TIMEOUT_TERMINATION_POOL_MS = 5000;//reduce timeout
        }
    }

    @Override
    public void stop() {
        super.stop();

        //just in case
//        unlockLatch();

    }

    public void resetAlgorithm() {
        logger.info("reset SingleInstrumentRLAlgorithm");
        long currentTime = System.currentTimeMillis();
        while (algorithmState != null && !algorithmState.equals(STOPPED)) {
            //waiting stopped completely
            Thread.onSpinWait();
            if (System.currentTimeMillis() - currentTime > 10000) {
                logger.warn("Waiting for stop algorithm timeout");
                break;
            }
        }
        cumDayReward = 0;
        diffReward = 0;
        lastReward = 0;
        lastState = null;
        lastAction = null;
        setActionReceived(false);
        timestampStartStep = DEFAULT_LAST_Q;
        iterations.set(0);
//        waitDone = true;
        super.resetAlgorithm();
        actionHistoricalList.clear();
    }

    public void endOfBacktest() {
        timestepFinish();
    }


    private void waitTimestep() {
        synchronized (lockLatchIteration) {
            if (latchIteration == null || latchIteration.getCount() == 0) {
                latchIteration = new CountDownLatch(1);
            }
        }
        try {
            latchIteration.await();
        } catch (Exception e) {
            ;
        }
    }

    private boolean isWaitingTimestep() {
        synchronized (lockLatchIteration) {
            if (latchIteration == null || latchIteration.getCount() == 0) {
                return false;
            }
        }
        return true;
    }

    private void timestepFinish() {
        synchronized (lockLatchIteration) {
            if (latchIteration != null) {
                latchIteration.countDown();
            }
        }
    }


    protected String getLastActionString() {
        if (lastAction == null)
            return "";

        String actionString = PrintDoubleArrayString(lastAction, ",");
        if (reinforcementLearningActionType == ReinforcementLearningActionType.discrete)
            actionString = String.valueOf(Math.round((float) lastAction[0]));
        else {
            actionString = "[" + actionString + "]";
        }
        return actionString;
    }

    public void setAction(double[] actionValues) {
        setActionReceived(true);
        iterations.incrementAndGet();
        lastAction = actionValues;
        timestampStartStep = getCurrentTimestamp();

        //we have to override it to change params!
    }

    protected double[] GetActionValues(double[] actionIndex) {
        if (reinforcementLearningActionType == ReinforcementLearningActionType.continuous) {
            return actionIndex;
        } else {
            // in discrete action is an index we have to transform to an array of values
            double[] actionValues = new double[0];
            int actionIndexValue = Math.round((float) actionIndex[0]);
            try {
                actionValues = this.action.getAction(actionIndexValue);
            } catch (Exception e) {
                logger.error("Error getting action values for action index {} actionValues {} ", actionIndexValue, Doubles.join(",", actionValues));
                System.err.println("Error getting action values for action index " + actionIndexValue + " actionValues " + Doubles.join(",", actionValues));
            }
            return actionValues;
        }
    }

    private void setCumDiffReward(double reward) {
        if (!DELTA_REWARD_REINFORCEMENT_LEARNING) {
            cumDayReward = reward;
            diffReward = reward - lastReward;
        } else {
            cumDayReward += reward;
            diffReward = reward;
        }
    }

    public boolean isDone() {
        boolean isStopped = getAlgorithmState() == AlgorithmState.STOPPED;
        boolean isOutOfTime = isEndOfBacktestDay();
        return isStopped && isOutOfTime;
    }

    /**
     * @param msElapsed
     * @param reward:   is coming from getCurrentReward
     * @param state
     * @return
     */
    protected boolean onFinishedIteration(long msElapsed, double reward, double[] state) {
        boolean isWaitingTimestep = isWaitingTimestep();
        if (iterations.get() > 0 && !isWaitingTimestep) {
            boolean isDone = isDone();
            if (!isDone) {
                //
                logger.warn("[{}][iteration {}] onFinishedIteration  when we are not waiting timestep during {} ms , since {} pause: {} isLockedLatch: {} isDone: {}", getCurrentTime(), iterations.get() - 1, msElapsed, new Date(timestampStartStep), ParquetMarketDataConnectorPublisher.isPause(), isWaitingTimestep, isDone);
                return false;
            } else {
                logger.info("[{}][iteration {}] onFinishedIteration  when we are not waiting timestep during {} ms but isDone: {}", getCurrentTime(), iterations.get() - 1, msElapsed, isDone);
            }

        }

        if (!isActionReceived.get()) {
            //already finished

            if (iterations.get() > 1 && msElapsed > WARNING_WAITING_SPAN_MS && msElapsed / WARNING_WAITING_SPAN_MS > msWaitingCounter) {
                msWaitingCounter++;
                logger.warn("[{}][iteration {}] onFinishedIteration  when isActionReceived is false during {} ms , since {} and pause: {} isWaitingTimestep: {} ... waiting python action ", getCurrentTime(), iterations.get() - 1, msElapsed, new Date(timestampStartStep), ParquetMarketDataConnectorPublisher.isPause(), isWaitingTimestep);
            }
            return false;
        }

        lastStepMillisElapsed = (int) msElapsed;
        msWaitingCounter = 0;
        setCumDiffReward(reward);
        lastReward = reward;


        if (state == null || !this.state.isReady()) {
            logger.warn("state is null or state is not ready -> return all zero");
            int stateColumns = this.state.getColumns().size();
            state = new double[stateColumns];
        }

        lastState = state;
        timestepFinish();//unlock waiting
        return true;
    }


    protected double getLastMidPrice() {
        return getLastDepth(getInstrument()).getMidPrice();
    }

    protected abstract double getQuantityMean();

    protected double getCurrentReward() {
        PnlSnapshot lastPnlSnapshot = getLastPnlSnapshot(this.instrument.getPrimaryKey());
        if (lastPnlSnapshot == null) {
            logger.warn("no lastPnlSnapshot found for {} ", this.instrument.getPrimaryKey());
            return 0;
        }
        double reward = ScoreUtils.getReward(scoreEnum, lastPnlSnapshot);
        double qtyMean = getQuantityMean();
        if (DELTA_REWARD_REINFORCEMENT_LEARNING) {
            double initialReward = rewardStartStep == DEFAULT_LAST_Q ? 0 : rewardStartStep;
            int initialNumberTrades = tradesStartStep == DEFAULT_LAST_Q ? 0 : tradesStartStep;
            double initialMid = midStartStep == DEFAULT_LAST_Q ? getLastMidPrice() : midStartStep;

            rewardStartStep = reward;
            tradesStartStep = lastPnlSnapshot.numberOfTrades.get();
            midStartStep = lastPnlSnapshot.getLastDepth().get(getInstrument()).getMidPrice();

            reward = reward - initialReward;
            if (DISCOUNT_REWARD_NO_TRADE && reward == 0 && initialNumberTrades == tradesStartStep && initialMid != 0.0) {
                reward = -Math.abs(midStartStep - initialMid);
                return reward;
            }
        }
        return reward / qtyMean;//to avoid differences on uptades based on qty!

    }

    @Override
    protected void waitDoneState() {
        int msElapsed = (int) (getCurrentTimestamp() - timestampStartStep);
        onFinishedIteration(msElapsed, getCurrentReward(), state.getCurrentStateRounded());
        super.waitDoneState();
    }

    @Override
    public boolean isReady() {
        return super.isReady() && state.isReady();
    }

    public StepOutput step(double[] action) {
        synchronized (stepLockIteration) {
            //called from OrdinaryBacktestRLGym (training)+ SingleInstrumentRLReplier(testing)
            String actionStr = ArrayUtils.PrintDoubleArrayString(action, ",");


            setAction(action);//set the action  and then wait for the iteration to finish

            waitTimestep();//is going to unlock after iteration - 5seconds or whatever
            //// Here the action is finished
            String messagePrintHead = Configuration.formatLog("[{}][iteration {} end] step finished", getCurrentTime(), iterations.get() - 1);
            String messagePrint = "";
            if (DELTA_REWARD_REINFORCEMENT_LEARNING) {
                messagePrint = String.format(" after %d ms action:%s  reward: %.2f  cumDayReward: %.2f", lastStepMillisElapsed, actionStr, lastReward, cumDayReward);
            } else {
                messagePrint = String.format(" after %d ms  action:%s  reward: %.2f  diffReward: %.2f", lastStepMillisElapsed, actionStr, lastReward, diffReward);
            }
            logger.info(messagePrintHead + messagePrint);

            double[] stateToSend = lastState;
            if (stateToSend == null) {
                int stateColumns = GetStateColumnsAlgorithm(this);
                logger.warn("state is null -> return {} all columns zero", stateColumns);
                stateToSend = new double[stateColumns];
                Arrays.fill(stateToSend, 0);
            }
            lastStepMillisElapsed = 0;
            setActionReceived(false);
            if (iterations.get() <= 1) {
                //first iteration -> reward must be 0 because of the initialization period is longer timestep defined
                return new StepOutput(stateToSend, 0);
            }

            return new StepOutput(stateToSend, lastReward);
        }
    }

    protected void printSummaryResults() {
        int iterations = Math.max(this.iterations.get() - 1, 0);
        summaryResultsAppend = String.format("iterations:%d cumDayReward:%.3f lastReward:%.3f", iterations, cumDayReward, lastReward);
        super.printSummaryResults();
    }

    public Map<String, String> getInfo() {
        Map<String, String> info = new HashMap<>();
        boolean isReady = false;
        try {
            isReady = isReady();
        } catch (Exception e) {
            logger.error("Error checking isReady in getInfo", e);
        }
        String message = isReady ? "" : "not_ready";
        info.put("message", message);
        info.put("timestamp", String.valueOf(getCurrentTimestamp()));
        info.put("cumDayReward", String.valueOf(cumDayReward));
        info.put("diffReward", String.valueOf(diffReward));
        info.put("reward", String.valueOf(lastReward));
        info.put("iterations", String.valueOf(iterations.get()));
        double totalPnl = 0;
        double realizedPnl = 0;
        double unrealizedPnl = 0;

        for (Instrument instrument : getInstruments()) {
            if (instrument == null) {
                continue;
            }

            PnlSnapshot pnlSnapshot = portfolioManager.getLastPnlSnapshot(instrument.getPrimaryKey());
            if (pnlSnapshot != null && pnlSnapshot.getNumberOfTrades().get() > 0) {
                totalPnl += pnlSnapshot.getTotalPnl();
                realizedPnl += pnlSnapshot.getRealizedPnl();
                unrealizedPnl += pnlSnapshot.getUnrealizedPnl();
                info.put("position_" + instrument, String.valueOf(pnlSnapshot.getNetPosition()));
                info.put("totalPnl_" + instrument, String.valueOf(pnlSnapshot.getTotalPnl()));
                info.put("realizedPnl_" + instrument, String.valueOf(pnlSnapshot.getRealizedPnl()));
                info.put("unrealizedPnl_" + instrument, String.valueOf(pnlSnapshot.getUnrealizedPnl()));
            }

            Depth depth = getLastDepth(instrument);
            double bid = 0.0;
            double ask = 0.0;
            if (depth != null) {
                bid = depth.getBestBid();
                ask = depth.getBestAsk();
            }
            info.put("bid_" + instrument, String.valueOf(bid));
            info.put("ask_" + instrument, String.valueOf(ask));
        }

        info.put("totalPnl", String.valueOf(totalPnl));
        info.put("realizedPnl", String.valueOf(realizedPnl));
        info.put("unrealizedPnl", String.valueOf(unrealizedPnl));
        info.put("is_success", String.valueOf(totalPnl > 0));

        return info;
    }


}
