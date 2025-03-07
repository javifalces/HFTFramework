package com.lambda.investing.interactive_brokers;

import com.ib.client.Contract;
import com.lambda.investing.model.asset.Instrument;

public class ContractFactory {

    public static Contract createContract(Instrument instrument) {
        if (instrument.isFX()) {
            return createForexContract(instrument.getSymbol().toUpperCase(), instrument.getCurrency().name().toUpperCase());
        }
        if (instrument.isCrypto()) {
            return createCryptoContract(instrument.getSymbol().toUpperCase(), instrument.getMarket().toUpperCase(), instrument.getCurrency().name().toUpperCase());
        }
        return createStockContract(instrument.getSymbol().toUpperCase(), instrument.getMarket().toUpperCase(), instrument.getCurrency().name().toUpperCase());
    }


    public static Contract createContract(String symbol, String secType, String exchange, String currency) {
        Contract contract = new Contract();
        contract.symbol(symbol);
        contract.secType(secType);
        contract.exchange(exchange);
        contract.currency(currency);
        return contract;
    }

    public static Contract createStockContract(String symbol, String exchange, String currency) {
        return createContract(symbol, "STK", exchange, currency);
    }

    public static Contract createCryptoContract(String symbol, String exchange, String currency) {
        return createContract(symbol, "CRYPTO", exchange, currency);
    }

    public static Contract createOptionContract(String symbol, String exchange, String currency, String right, String expiry, double strike) {
        Contract contract = createContract(symbol, "OPT", exchange, currency);
        contract.right(right);
        contract.lastTradeDateOrContractMonth(expiry);
        contract.strike(strike);
        return contract;
    }

    public static Contract createFutureContract(String symbol, String exchange, String currency, String expiry) {
        Contract contract = createContract(symbol, "FUT", exchange, currency);
        contract.lastTradeDateOrContractMonth(expiry);
        return contract;
    }

    public static Contract createFutureContract(String symbol, String exchange, String currency, String expiry, double strike) {
        Contract contract = createContract(symbol, "FOP", exchange, currency);
        contract.lastTradeDateOrContractMonth(expiry);
        contract.strike(strike);
        return contract;
    }

    public static Contract createForexContract(String symbol, String currency) {
        int baseCurrencyLength = currency.length();
        int totalLength = symbol.length();
        String symbolPrefix = symbol.substring(0, totalLength - baseCurrencyLength);
        return createContract(symbolPrefix, "CASH", "IDEALPRO", currency);
    }

    public static Contract createIndexContract(String symbol, String exchange) {
        return createContract(symbol, "IND", exchange, "USD");
    }

    public static Contract createFutureOptionContract(String symbol, String exchange, String currency, String right, String expiry, double strike) {
        Contract contract = createContract(symbol, "FOP", exchange, currency);
        contract.right(right);
        contract.lastTradeDateOrContractMonth(expiry);
        contract.strike(strike);
        return contract;
    }

    public static Contract createFutureOptionContract(String symbol, String exchange, String currency, String right, String expiry, double strike, String multiplier) {
        Contract contract = createContract(symbol, "FOP", exchange, currency);
        contract.right(right);
        contract.lastTradeDateOrContractMonth(expiry);
        contract.strike(strike);
        contract.multiplier(multiplier);
        return contract;
    }
}
