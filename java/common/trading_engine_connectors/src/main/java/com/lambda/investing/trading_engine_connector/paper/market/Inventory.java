package com.lambda.investing.trading_engine_connector.paper.market;

import java.util.HashMap;
import java.util.Map;

public class Inventory {

	private Map<String, Double> inventoryPerIsin;

	public Inventory() {
		inventoryPerIsin = new HashMap<>();
	}

	public void updateInventory(String isin, double quantityFilled) {
		double currentPosition = getPosition(isin);
		double newPosition = quantityFilled + currentPosition;
		inventoryPerIsin.put(isin, newPosition);
	}

	public double getPosition(String isin) {
		return inventoryPerIsin.getOrDefault(isin, 0.);
	}

	public Map<String, Object> getFields(String bookId, String refdataId, String isin) {
		//generate map to send as from ion bus!
		Map<String, Object> output = new HashMap<>();
		//		System.out.println("RANDOM POSITON-> FORCE STOP");
		//		double position=Math.random()*10;
		double position = getPosition(isin);
		output.put("Id", bookId + "_" + refdataId);
		output.put("BookId", bookId);
		output.put("InstrumentId", refdataId);
		output.put("NetTradingPos", position);//in Millions
		output.put("NetTradingPosNominal", position * 1E6);
		output.put("SettledPosNominal", position * 1E6);

		return output;
	}

}
