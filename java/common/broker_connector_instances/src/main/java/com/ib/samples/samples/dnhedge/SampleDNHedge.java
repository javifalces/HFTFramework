/* Copyright (C) 2019 Interactive Brokers LLC. All rights reserved. This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package com.ib.samples.samples.dnhedge;


import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.contracts.OptContract;
import com.ib.contracts.StkContract;

import com.ib.samples.samples.rfq.SimpleWrapper;


public class SampleDNHedge extends SimpleWrapper {
    private enum Status {None, SecDef, Order, Done, Error}

    private static final int ParentAcked = 1;
    private static final int ChildAcked = 2;

    private static final int AllAcked = ParentAcked | ChildAcked;

    private final Object m_mutex = new Object();
    private Status m_status = Status.None;

    private int m_clientId;
    private int m_orderId;

    private String m_account;
    private String m_settlingFirm;
    private String m_designatedLocation;

    private Contract m_contract = null;
    private int m_underConId = 0;

    private int m_receivedAcks = 0;

    public SampleDNHedge(int clientId, int orderId, String account,
                         String settlingFirm, String designatedLocation) {

        m_clientId = clientId;
        m_orderId = orderId;

        m_account = account;
        m_settlingFirm = settlingFirm;
        m_designatedLocation = designatedLocation;
    }

    public void testOrder() throws Exception {
        connect(m_clientId);

        if (client() != null && client().isConnected()) {
            try {
                synchronized (m_mutex) {

                    if (client().serverVersion() < 66) {
                        error("Sample will not work with TWS older that 932");
                    }

                    while (m_status != Status.Done &&
                            m_status != Status.Error) {

                        if (m_status == Status.None) {
                            obtainContract();
                            if (m_status != Status.Error &&
                                    m_status != Status.SecDef) {
                                submitOrder();
                            }
                        }
                        m_mutex.wait();
                    }
                }
            } finally {
                disconnect();
            }

            if (m_status == Status.Done) {

                consoleMsg("Done");
            }
        }
    }

    private void obtainContract() {
        m_contract = new OptContract("IBM", "20121019", 200, "CALL");
        m_contract.currency("USD");
        m_contract.multiplier("100");

        Contract underlying = new StkContract("IBM");
        submitSecDef(1, underlying);

        m_status = Status.SecDef;
    }

    private void submitSecDef(int reqId, Contract contract) {
        consoleMsg("REQ: secDef " + reqId);

        client().reqContractDetails(reqId, contract);
    }

    private void submitOrder() {
        consoleMsg("REQ: order " + m_orderId);

        m_status = Status.Order;

        client().placeOrder(m_orderId, m_contract,
                new DNHedgeOrder(m_clientId, m_orderId, 1, m_account,
                        m_settlingFirm, m_underConId, m_designatedLocation));
    }

    private void checkReceivedAllAcks() {
        if ((m_receivedAcks & AllAcked) == AllAcked) {
            m_status = Status.Done;
            m_mutex.notify();
        }
    }

    public void contractDetails(int reqId, ContractDetails contractDetails) {
        consoleMsg("contractDetails: " + reqId);

        try {
            synchronized (m_mutex) {
                if (m_status == Status.SecDef) {
                    /*
                     * Store underConId if needed
                     */
                    if (m_underConId == 0) {
                        m_underConId = contractDetails.contract().conid();
                    }

                    consoleMsg("using " + m_underConId + " for hedging");

                    /*
                     * And finally submit RFQ
                     */
                    submitOrder();
                }
            }
        } catch (Exception e) {
            // will update status and notify main thread
            error(e.toString());
        }
    }

    public void contractDetailsEnd(int reqId) {
        consoleMsg("contractDetailsEnd: " + reqId);

        try {
            synchronized (m_mutex) {
                if (m_status == Status.SecDef) {
                    error("Could not find hedge contract id");
                }
            }
        } catch (Exception e) {
            // will update status and notify main thread
            error(e.toString());
        }
    }

    public void orderStatus(int orderId, String status, int filled,
                            int remaining, double avgFillPrice, int permId, int parentId,
                            double lastFillPrice, int clientId, String whyHeld) {
        consoleMsg("orderStatus:" + orderId + " status=" + status);

        synchronized (m_mutex) {
            if (status.equals("Cancelled")) {
                m_status = Status.Error;
                m_mutex.notify();
            } else if (status.equals("Submitted") ||
                    status.equals("PreSubmitted")) {

                if (orderId == m_orderId) {
                    m_receivedAcks |= ParentAcked;
                } else if (orderId < 0) {
                    m_receivedAcks |= ChildAcked;
                }

                checkReceivedAllAcks();
            }
        }
    }

    public void error(String str) {
        consoleMsg("Error=" + str);

        synchronized (m_mutex) {
            m_status = Status.Error;
            m_mutex.notify();
        }
    }

    public void error(int id, int errorCode, String errorMsg) {
        consoleMsg("Error id=" + id + " code=" + errorCode + " msg=" + errorMsg);

        if (errorCode >= 2100 && errorCode < 2200) {
            return;
        }

        synchronized (m_mutex) {
            m_status = Status.Error;
            m_mutex.notify();
        }
    }

    /* ***************************************************************
     * Main Method
     *****************************************************************/
    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                System.err.println("Account and settlingFirm parameters " +
                        "are required");
                return;
            }

            int orderId = (int) (System.currentTimeMillis() / 1000);
            String account = args[0];
            String settlingFirm = args[1];
            String designatedLocation = (args.length >= 3) ? args[2] : "";
            SampleDNHedge ut = new SampleDNHedge(/* clientId */ 2, orderId,
                    account, settlingFirm, designatedLocation);
            ut.testOrder();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
