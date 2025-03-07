/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.samples.testbed.orders;

import java.util.ArrayList;

import com.ib.client.Order;
import com.ib.client.TagValue;

public class AvailableAlgoParams {

    //! [scale_params]
    public static void FillScaleParams(Order baseOrder, int scaleInitLevelSize, int scaleSubsLevelSize, boolean scaleRandomPercent,
                                       double scalePriceIncrement, double scalePriceAdjustValue, int scalePriceAdjustInterval, double scaleProfitOffset,
                                       boolean scaleAutoReset, int scaleInitPosition, int scaleInitFillQty) {

        baseOrder.scaleInitLevelSize(scaleInitLevelSize);   //Initial Component Size
        baseOrder.scaleSubsLevelSize(scaleSubsLevelSize);    //Subsequent Comp. Size
        baseOrder.scaleRandomPercent(scaleRandomPercent);   //Randomize size by +/-55%
        baseOrder.scalePriceIncrement(scalePriceIncrement);   //Price Increment

        /*Auto Price adjustment*/
        baseOrder.scalePriceAdjustValue(scalePriceAdjustValue);    //starting price by
        baseOrder.scalePriceAdjustInterval(scalePriceAdjustInterval);   // in seconds

        /*Profit Orders*/
        baseOrder.scaleProfitOffset(scaleProfitOffset);   //Create profit taking order Profit Offset
        baseOrder.scaleAutoReset(scaleAutoReset);      //Restore size after taking profit
        baseOrder.scaleInitPosition(scaleInitPosition);     //Initial Position
        baseOrder.scaleInitFillQty(scaleInitFillQty);      //Filled initial Component Size

    }
    //! [scale_params]

    //! [arrivalpx_params]
    public static void FillArrivalPriceParams(Order baseOrder, double maxPctVol, String riskAversion, String startTime,
                                              String endTime, boolean forceCompletion, boolean allowPastTime) {

        baseOrder.algoStrategy("ArrivalPx");
        baseOrder.algoParams(new ArrayList<>());
        baseOrder.algoParams().add(new TagValue("maxPctVol", String.valueOf(maxPctVol)));
        baseOrder.algoParams().add(new TagValue("riskAversion", riskAversion));
        baseOrder.algoParams().add(new TagValue("startTime", startTime));
        baseOrder.algoParams().add(new TagValue("endTime", endTime));
        baseOrder.algoParams().add(new TagValue("forceCompletion", forceCompletion ? "1" : "0"));
        baseOrder.algoParams().add(new TagValue("allowPastEndTime", allowPastTime ? "1" : "0"));

    }
    //! [arrivalpx_params]

    //! [darkice_params]
    public static void FillDarkIceParams(Order baseOrder, int displaySize, String startTime, String endTime,
                                         boolean allowPastEndTime) {

        baseOrder.algoStrategy("DarkIce");
        baseOrder.algoParams(new ArrayList<>());
        baseOrder.algoParams().add(new TagValue("displaySize", String.valueOf(displaySize)));
        baseOrder.algoParams().add(new TagValue("startTime", startTime));
        baseOrder.algoParams().add(new TagValue("endTime", endTime));
        baseOrder.algoParams().add(new TagValue("allowPastEndTime", allowPastEndTime ? "1" : "0"));

    }
    //! [darkice_params]

    //! [pctvol_params]
    public static void FillPctVolParams(Order baseOrder, double pctVol, String startTime, String endTime, boolean noTakeLiq) {

        baseOrder.algoStrategy("PctVol");
        baseOrder.algoParams(new ArrayList<>());
        baseOrder.algoParams().add(new TagValue("pctVol", String.valueOf(pctVol)));
        baseOrder.algoParams().add(new TagValue("startTime", startTime));
        baseOrder.algoParams().add(new TagValue("endTime", endTime));
        baseOrder.algoParams().add(new TagValue("noTakeLiq", noTakeLiq ? "1" : "0"));

    }
    //! [pctvol_params]

    //! [twap_params]
    public static void FillTwapParams(Order baseOrder, String strategyType, String startTime, String endTime,
                                      boolean allowPastEndTime) {

        baseOrder.algoStrategy("Twap");
        baseOrder.algoParams(new ArrayList<>());
        baseOrder.algoParams().add(new TagValue("strategyType", strategyType));
        baseOrder.algoParams().add(new TagValue("startTime", startTime));
        baseOrder.algoParams().add(new TagValue("endTime", endTime));
        baseOrder.algoParams().add(new TagValue("allowPastEndTime", allowPastEndTime ? "1" : "0"));

    }
    //! [twap_params]

    //! [vwap_params]
    public static void FillVwapParams(Order baseOrder, double maxPctVol, String startTime, String endTime,
                                      boolean allowPastEndTime, boolean noTakeLiq, boolean speedUp) {

        baseOrder.algoStrategy("Vwap");
        baseOrder.algoParams(new ArrayList<>());
        baseOrder.algoParams().add(new TagValue("maxPctVol", String.valueOf(maxPctVol)));
        baseOrder.algoParams().add(new TagValue("startTime", startTime));
        baseOrder.algoParams().add(new TagValue("endTime", endTime));
        baseOrder.algoParams().add(new TagValue("allowPastEndTime", allowPastEndTime ? "1" : "0"));
        baseOrder.algoParams().add(new TagValue("noTakeLiq", noTakeLiq ? "1" : "0"));
        baseOrder.algoParams().add(new TagValue("speedUp", speedUp ? "1" : "0"));


    }
    //! [vwap_params]

    //! [ad_params]
    public static void FillAccumulateDistributeParams(Order baseOrder, int componentSize, int timeBetweenOrders, boolean randomizeTime20, boolean randomizeSize55,
                                                      int giveUp, boolean catchUp, boolean waitForFill, String startTime, String endTime) {

        baseOrder.algoStrategy("AD");
        baseOrder.algoParams(new ArrayList<>());
        baseOrder.algoParams().add(new TagValue("componentSize", String.valueOf(componentSize)));
        baseOrder.algoParams().add(new TagValue("timeBetweenOrders", String.valueOf(timeBetweenOrders)));
        baseOrder.algoParams().add(new TagValue("randomizeTime20", randomizeTime20 ? "1" : "0"));
        baseOrder.algoParams().add(new TagValue("randomizeSize55", randomizeSize55 ? "1" : "0"));
        baseOrder.algoParams().add(new TagValue("giveUp", String.valueOf(giveUp)));
        baseOrder.algoParams().add(new TagValue("catchUp", catchUp ? "1" : "0"));
        baseOrder.algoParams().add(new TagValue("waitForFill", waitForFill ? "1" : "0"));
        baseOrder.algoParams().add(new TagValue("activeTimeStart", startTime));
        baseOrder.algoParams().add(new TagValue("activeTimeEnd", endTime));

    }
    //! [ad_params]

    //! [balanceimpactrisk_params]
    public static void FillBalanceImpactRiskParams(Order baseOrder, double maxPctVol, String riskAversion, boolean forceCompletion) {

        baseOrder.algoStrategy("BalanceImpactRisk");
        baseOrder.algoParams(new ArrayList<>());
        baseOrder.algoParams().add(new TagValue("maxPctVol", String.valueOf(maxPctVol)));
        baseOrder.algoParams().add(new TagValue("riskAversion", riskAversion));
        baseOrder.algoParams().add(new TagValue("forceCompletion", forceCompletion ? "1" : "0"));

    }
    //! [balanceimpactrisk_params]

    //! [minimpact_params]
    public static void FillMinImpactParams(Order baseOrder, double maxPctVol) {

        baseOrder.algoStrategy("BalanceImpactRisk");
        baseOrder.algoParams(new ArrayList<>());
        baseOrder.algoParams().add(new TagValue("maxPctVol", String.valueOf(maxPctVol)));

    }
    //! [minimpact_params]

    //! [adaptive_params]
    public static void FillAdaptiveParams(Order baseOrder, String priority) {

        baseOrder.algoStrategy("Adaptive");
        baseOrder.algoParams(new ArrayList<>());
        baseOrder.algoParams().add(new TagValue("adaptivePriority", priority));

    }
    //! [adaptive_params]

    //! [closepx_params]
    public static void FillClosePriceParams(Order baseOrder, double maxPctVol, String riskAversion, String startTime,
                                            boolean forceCompletion) {

        baseOrder.algoStrategy("ClosePx");
        baseOrder.algoParams(new ArrayList<>());
        baseOrder.algoParams().add(new TagValue("maxPctVol", String.valueOf(maxPctVol)));
        baseOrder.algoParams().add(new TagValue("riskAversion", riskAversion));
        baseOrder.algoParams().add(new TagValue("startTime", startTime));
        baseOrder.algoParams().add(new TagValue("forceCompletion", forceCompletion ? "1" : "0"));

    }
    //! [closepx_params]

    //! [pctvolpx_params]
    public static void FillPriceVariantPctVolParams(Order baseOrder, double pctVol, double deltaPctVol, double minPctVol4Px,
                                                    double maxPctVol4Px, String startTime, String endTime, boolean noTakeLiq) {

        baseOrder.algoStrategy("PctVolPx");
        baseOrder.algoParams(new ArrayList<>());
        baseOrder.algoParams().add(new TagValue("pctVol", String.valueOf(pctVol)));
        baseOrder.algoParams().add(new TagValue("deltaPctVol", String.valueOf(deltaPctVol)));
        baseOrder.algoParams().add(new TagValue("minPctVol4Px", String.valueOf(minPctVol4Px)));
        baseOrder.algoParams().add(new TagValue("maxPctVol4Px", String.valueOf(maxPctVol4Px)));
        baseOrder.algoParams().add(new TagValue("startTime", startTime));
        baseOrder.algoParams().add(new TagValue("endTime", endTime));
        baseOrder.algoParams().add(new TagValue("noTakeLiq", noTakeLiq ? "1" : "0"));

    }
    //! [pctvolpx_params]

    //! [pctvolsz_params]
    public static void FillSizeVariantPctVolParams(Order baseOrder, double startPctVol, double endPctVol,
                                                   String startTime, String endTime, boolean noTakeLiq) {

        baseOrder.algoStrategy("PctVolSz");
        baseOrder.algoParams(new ArrayList<>());
        baseOrder.algoParams().add(new TagValue("startPctVol", String.valueOf(startPctVol)));
        baseOrder.algoParams().add(new TagValue("endPctVol", String.valueOf(endPctVol)));
        baseOrder.algoParams().add(new TagValue("startTime", startTime));
        baseOrder.algoParams().add(new TagValue("endTime", endTime));
        baseOrder.algoParams().add(new TagValue("noTakeLiq", noTakeLiq ? "1" : "0"));

    }
    //! [pctvolsz_params]

    //! [pctvoltm_params]
    public static void FillTimeVariantPctVolParams(Order baseOrder, double startPctVol, double endPctVol,
                                                   String startTime, String endTime, boolean noTakeLiq) {

        baseOrder.algoStrategy("PctVolTm");
        baseOrder.algoParams(new ArrayList<>());
        baseOrder.algoParams().add(new TagValue("startPctVol", String.valueOf(startPctVol)));
        baseOrder.algoParams().add(new TagValue("endPctVol", String.valueOf(endPctVol)));
        baseOrder.algoParams().add(new TagValue("startTime", startTime));
        baseOrder.algoParams().add(new TagValue("endTime", endTime));
        baseOrder.algoParams().add(new TagValue("noTakeLiq", noTakeLiq ? "1" : "0"));

    }
    //! [pctvoltm_params]

    //! [csfb_params]
    public static void FillCSFBParams(Order baseOrder, double startPctVol, double endPctVol,
                                      String startTime, String endTime, boolean noTakeLiq) {

        // must be direct-routed to "CSFB"

        baseOrder.algoStrategy("PctVolTm");
        baseOrder.algoParams(new ArrayList<>());
        baseOrder.algoParams().add(new TagValue("startPctVol", String.valueOf(startPctVol)));
        baseOrder.algoParams().add(new TagValue("endPctVol", String.valueOf(endPctVol)));
        baseOrder.algoParams().add(new TagValue("startTime", startTime));
        baseOrder.algoParams().add(new TagValue("endTime", endTime));
        baseOrder.algoParams().add(new TagValue("noTakeLiq", noTakeLiq ? "1" : "0"));

    }
    //! [csfb_params]

    //! [jefferies_vwap_params]
    public static void FillJefferiesVWAPParams(Order baseOrder, String startTime, String endTime, double relativeLimit,
                                               double maxVolumeRate, String excludeAuctions, double triggerPrice, double wowPrice, int minFillSize, double wowOrderPct,
                                               String wowMode, boolean isBuyBack, String wowReference) {

        // must be direct-routed to "JEFFALGO"

        baseOrder.algoStrategy("VWAP");

        baseOrder.algoParams(new ArrayList<>());
        baseOrder.algoParams().add(new TagValue("StartTime", startTime));
        baseOrder.algoParams().add(new TagValue("EndTime", endTime));
        baseOrder.algoParams().add(new TagValue("RelativeLimit", String.valueOf(relativeLimit)));
        baseOrder.algoParams().add(new TagValue("MaxVolumeRate", String.valueOf(maxVolumeRate)));
        baseOrder.algoParams().add(new TagValue("ExcludeAuctions", excludeAuctions));
        baseOrder.algoParams().add(new TagValue("TriggerPrice", String.valueOf(triggerPrice)));
        baseOrder.algoParams().add(new TagValue("WowPrice", String.valueOf(wowPrice)));
        baseOrder.algoParams().add(new TagValue("MinFillSize", String.valueOf(minFillSize)));
        baseOrder.algoParams().add(new TagValue("WowOrderPct", String.valueOf(wowOrderPct)));
        baseOrder.algoParams().add(new TagValue("WowMode", wowMode));
        baseOrder.algoParams().add(new TagValue("IsBuyBack", isBuyBack ? "1" : "0"));
        baseOrder.algoParams().add(new TagValue("WowReference", wowReference));

    }
    //! [jefferies_vwap_params]

    //! [csfb_inline_params]
    public static void FillCSFBInlineParams(Order baseOrder, String startTime, String endTime, String execStyle, int minPercent,
                                            int maxPercent, int displaySize, String auction, boolean blockFinder, double blockPrice,
                                            int minBlockSize, int maxBlockSize, double iWouldPrice) {

        // must be direct-routed to "CSFBALGO"

        baseOrder.algoStrategy("INLINE");

        baseOrder.algoParams(new ArrayList<>());
        baseOrder.algoParams().add(new TagValue("StartTime", startTime));
        baseOrder.algoParams().add(new TagValue("EndTime", endTime));
        baseOrder.algoParams().add(new TagValue("ExecStyle", execStyle));
        baseOrder.algoParams().add(new TagValue("MinPercent", String.valueOf(minPercent)));
        baseOrder.algoParams().add(new TagValue("MaxPercent", String.valueOf(maxPercent)));
        baseOrder.algoParams().add(new TagValue("DisplaySize", String.valueOf(displaySize)));
        baseOrder.algoParams().add(new TagValue("Auction", auction));
        baseOrder.algoParams().add(new TagValue("BlockFinder", blockFinder ? "1" : "0"));
        baseOrder.algoParams().add(new TagValue("BlockPrice", String.valueOf(blockPrice)));
        baseOrder.algoParams().add(new TagValue("MinBlockSize", String.valueOf(minBlockSize)));
        baseOrder.algoParams().add(new TagValue("MaxBlockSize", String.valueOf(maxBlockSize)));
        baseOrder.algoParams().add(new TagValue("IWouldPrice", String.valueOf(iWouldPrice)));

    }
    //! [csfb_inline_params]

    //! [qbalgo_strobe_params]
    public static void FillQBAlgoInlineParams(Order baseOrder, String startTime, String endTime, double duration, String benchmark,
                                              double percentVolume, boolean noCleanUp) {

        baseOrder.algoStrategy("STROBE");

        baseOrder.algoParams(new ArrayList<>());
        baseOrder.algoParams().add(new TagValue("StartTime", startTime));
        baseOrder.algoParams().add(new TagValue("EndTime", endTime));
        //This example uses endTime instead of duration
        //baseOrder.algoParams().add(new TagValue("Duration", String.valueOf(duration)));
        baseOrder.algoParams().add(new TagValue("Benchmark", benchmark));
        baseOrder.algoParams().add(new TagValue("PercentVolume", String.valueOf(percentVolume)));
        baseOrder.algoParams().add(new TagValue("NoCleanUp", noCleanUp ? "1" : "0"));

    }
    //! [qbalgo_strobe_params]

}
