/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.apidemo.util;


/**
 * Delegate for connection parameters
 */
public interface IConnectionConfiguration {

    String getDefaultHost();

    String getDefaultPort();

    String getDefaultConnectOptions();

    /**
     * Standard ApiDemo configuration for pre-v100 connection
     */
    class DefaultConnectionConfiguration implements IConnectionConfiguration {
        @Override
        public String getDefaultHost() {
            return "";
        }

        @Override
        public String getDefaultPort() {
            return "7496";
        }

        @Override
        public String getDefaultConnectOptions() {
            return null;
        }
    }
}
