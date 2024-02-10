package com.lambda.investing.trading_engine_connector.metatrader.models;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class MtPosition {
    @Getter
    @Setter
    public class MtPositionInstrument {
        private String _magic;
        private String _symbol;
        private String _lots;
        private String _type;
        private String _open_price;
        private String _open_time;
        private String _SL;
        private String _pnl;
        private String _comment;
    }


    //{'_action': 'OPEN_POSITIONS', '_positions': {
    //	2015962186: {'_magic': 123456, '_symbol': 'EURCHF', '_lots': 0.01000000, '_type': 0,
    //	'_open_price': 0.98049000, '_open_time': '2023.03.10 23:54:21', '_SL': 0.00000000, '_TP': 0.00000000, '_pnl': -0.38000000, '_comment': 'MarketFactorInvestingAlgorithm_'}, 2015962185: {'_magic': 123456, '_symbol': 'EURUSD', '_lots': 0.01000000, '_type': 1, '_open_price': 1.06385000, '_open_time': '2023.03.10 23:54:21', '_SL': 0.00000000, '_TP': 0.00000000, '_pnl': -0.11000000, '_comment': 'MarketFactorInvestingAlgorithm_'}, 2015962153: {'_magic': 123456, '_symbol': 'EURAUD', '_lots': 0.01000000, '_type': 0, '_open_price': 1.61656000, '_open_time': '2023.03.10 23:51:18', '_SL': 0.00000000, '_TP': 0.00000000, '_pnl': -0.24000000, '_comment': 'MarketFactorInvestingAlgorithm_'}, 2015962152: {'_magic': 123456, '_symbol': 'EURCHF', '_lots': 0.01000000, '_type': 0, '_open_price': 0.98046000, '_open_time': '2023.03.10 23:51:18', '_SL': 0.00000000, '_TP': 0.00000000, '_pnl': -0.35000000, '_comment': 'MarketFactorInvestingAlgorithm_'}, 2015962150: {'_magic': 123456, '_symbol': 'EURUSD', '_lots': 0.01000000, '_type': 1, '_open_price': 1.06392000, '_open_time': '2023.03.10 23:51:18', '_SL': 0.00000000, '_TP': 0.00000000, '_pnl': -0.05000000, '_comment': 'MarketFactorInvestingAlgorithm_'}, 2015962078: {'_magic': 123456, '_symbol': 'EURCHF', '_lots': 0.01000000, '_type': 0, '_open_price': 0.98054000, '_open_time': '2023.03.10 23:43:10', '_SL': 0.00000000, '_TP': 0.00000000, '_pnl': -0.43000000, '_comment': 'MarketFactorInvestingAlgorithm_'}, 2015961537: {'_magic': 123456, '_symbol': 'EURAUD', '_lots': 0.01000000, '_type': 0, '_open_price': 1.61614000, '_open_time': '2023.03.10 23:08:36', '_SL': 0.00000000, '_TP': 0.00000000, '_pnl': 0.02000000, '_comment': 'MarketFactorInvestingAlgorithm_'}, 2015959313: {'_magic': 123456, '_symbol': 'EURNZD', '_lots': 0.01000000, '_type': 1, '_open_price': 1.73640000, '_open_time': '2023.03.10 21:25:56', '_SL': 0.00000000, '_TP': 0.00000000, '_pnl': 0.92000000, '_comment': 'MarketFactorInvestingAlgorithm_'}, 2015955887: {'_magic': 123456, '_symbol': 'EURNZD', '_lots': 0.02000000, '_type': 1, '_open_price': 1.73364000, '_open_time': '2023.03.10 19:15:48', '_SL': 0.00000000, '_TP': 0.00000000, '_pnl': -1.35000000, '_comment': 'MarketFactorInvestingAlgorithm_'}}}
    private String _action;
    private Map<String, MtPositionInstrument> _positions;

    public int getLength() {
        return _positions.size();
    }
}
