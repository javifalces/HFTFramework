package com.lambda.investing.backtest_engine;

import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.model.asset.Instrument;
import lombok.Getter;
import lombok.Setter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter @Setter public class BacktestConfiguration {

	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
	private static SimpleDateFormat dateFormatTime = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
	private Algorithm algorithm;//if backtest type is zero , is not a must

	//instrument data file
	private List<Instrument> instruments = new ArrayList<>();
	private Date startTime;//start time included
	private Date endTime;//not included
	private int speed = -1;
	private long initialSleepSeconds = 5;

	//backtest type
	private BacktestSource backtestSource;
	private BacktestExternalConnection backtestExternalConnection;

	/**
	 * @param startTime included
	 * @throws ParseException
	 */
	public void setStartTime(String startTime) throws ParseException {
		if (startTime.trim().contains(" ")) {
			this.startTime = dateFormatTime.parse(startTime);
		} else {
			this.startTime = dateFormat.parse(startTime);
		}
	}

	/**
	 * @param endTime not included
	 * @throws ParseException
	 */
	public void setEndTime(String endTime) throws ParseException {
		if (endTime.trim().contains(" ")) {
			this.endTime = dateFormatTime.parse(endTime);
		} else {
			this.endTime = dateFormat.parse(endTime);
		}
	}

	public void setBacktestSource(String backtestSource) {
		this.backtestSource = BacktestSource.valueOf(backtestSource);
	}

	public void setBacktestExternalConnection(String backtestExternalConnection) {
		this.backtestExternalConnection = BacktestExternalConnection.valueOf(backtestExternalConnection);
	}
}
