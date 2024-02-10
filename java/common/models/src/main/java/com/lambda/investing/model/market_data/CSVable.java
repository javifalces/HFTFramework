package com.lambda.investing.model.market_data;

import com.lambda.investing.model.asset.Instrument;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;

public abstract class CSVable implements Serializable {

	static Logger logger = LogManager.getLogger(CSVable.class);

	public abstract String toCSV(boolean withHeader);

	public abstract Object getParquetObject();

	public static CSVable getCSVAble(Object parquetObject, Instrument instrument) {
		if (parquetObject instanceof DepthParquet) {
			return new Depth((DepthParquet) parquetObject, instrument);
		}
		if (parquetObject instanceof TradeParquet) {
			return new Trade((TradeParquet) parquetObject, instrument);
		}

		logger.error("getCSVAble parquetObject not recognized! return null");
		//TODO something better
		return null;
	}

}
