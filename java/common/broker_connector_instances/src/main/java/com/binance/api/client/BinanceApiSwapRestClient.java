package com.binance.api.client;

import com.binance.api.client.domain.SwapRemoveType;
import com.binance.api.client.domain.account.*;
import retrofit2.Call;
import retrofit2.http.Query;

import java.util.List;

public interface BinanceApiSwapRestClient {

	/**
	 * Get metadata about all swap pools.
	 *
	 * @return
	 */
	List<Pool> listAllSwapPools();

	/**
	 * Get liquidity information and user share of a pool.
	 *
	 * @param poolId
	 * @return
	 */
	Liquidity getPoolLiquidityInfo(String poolId);

	/**
	 * Add liquidity to a pool.
	 *
	 * @param poolId
	 * @param asset
	 * @param quantity
	 * @return
	 */
	LiquidityOperationRecord addLiquidity(String poolId, String asset, String quantity);

	/**
	 * Remove liquidity from a pool, type include SINGLE and COMBINATION, asset is mandatory for single asset removal
	 *
	 * @param poolId
	 * @param type
	 * @param asset
	 * @param shareAmount
	 * @return
	 */
	LiquidityOperationRecord removeLiquidity(String poolId, SwapRemoveType type, List<String> asset,
			String shareAmount);

	/**
	 * Get liquidity operation (add/remove) records of a pool
	 *
	 * @param poolId
	 * @param limit
	 * @return
	 */
	List<LiquidityOperationRecord> getPoolLiquidityOperationRecords(String poolId, Integer limit);

	/**
	 * Get liquidity operation (add/remove) record.
	 *
	 * @param operationId
	 * @return
	 */
	LiquidityOperationRecord getLiquidityOperationRecord(String operationId);

	/**
	 * Request a quote for swap quote asset (selling asset) for base asset (buying asset), essentially price/exchange rates.
	 *
	 * @param quoteAsset
	 * @param baseAsset
	 * @param quoteQty
	 * @return
	 */
	SwapQuote requestQuote(String quoteAsset, String baseAsset, String quoteQty);

	/**
	 * Swap quoteAsset for baseAsset
	 *
	 * @param quoteAsset
	 * @param baseAsset
	 * @param quoteQty
	 * @return
	 */
	SwapRecord swap(String quoteAsset, String baseAsset, String quoteQty);

	SwapHistory getSwapHistory(String swapId);
}
