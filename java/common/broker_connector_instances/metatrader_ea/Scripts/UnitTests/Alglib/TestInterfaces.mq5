//+------------------------------------------------------------------+
//|                                               TestInterfaces.mq5 |
//|            Copyright 2003-2012 Sergey Bochkanov (ALGLIB project) |
//|                   Copyright 2012-2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
//| Implementation of ALGLIB library in MetaQuotes Language 5        |
//|                                                                  |
//| The features of the library include:                             |
//| - Linear algebra (direct algorithms, EVD, SVD)                   |
//| - Solving systems of linear and non-linear equations             |
//| - Interpolation                                                  |
//| - Optimization                                                   |
//| - FFT (Fast Fourier Transform)                                   |
//| - Numerical integration                                          |
//| - Linear and nonlinear least-squares fitting                     |
//| - Ordinary differential equations                                |
//| - Computation of special functions                               |
//| - Descriptive statistics and hypothesis testing                  |
//| - Data analysis - classification, regression                     |
//| - Implementing linear algebra algorithms, interpolation, etc.    |
//|   in high-precision arithmetic (using MPFR)                      |
//|                                                                  |
//| This program is free software; you can redistribute it and/or    |
//| modify it under the terms of the GNU General Public License as   |
//| published by the Free Software Foundation (www.fsf.org); either  |
//| version 2 of the License, or (at your option) any later version. |
//|                                                                  |
//| This program is distributed in the hope that it will be useful,  |
//| but WITHOUT ANY WARRANTY; without even the implied warranty of   |
//| MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the     |
//| GNU General Public License for more details.                     |
//+------------------------------------------------------------------+
#include "TestInterfaces.mqh"

//+------------------------------------------------------------------+
//| Testing script                                                   |
//+------------------------------------------------------------------+
void OnStart()
  {
//--- total result
   bool _TotalResult=true;
//--- temp result
   bool _TestResult;
//--- spoil scenario
   int  _spoil_scenario;
   Print("MQL5 interface tests. Please wait...");
   Print("0/91");
//--- testing
   TEST_NNeighbor_D_1(_spoil_scenario,_TestResult,_TotalResult);
   TEST_NNeighbor_T_2(_spoil_scenario,_TestResult,_TotalResult);
   TEST_NNeighbor_D_2(_spoil_scenario,_TestResult,_TotalResult);
   TEST_BaseStat_D_Base(_spoil_scenario,_TestResult,_TotalResult);
   TEST_BaseStat_D_C2(_spoil_scenario,_TestResult,_TotalResult);
   TEST_BaseStat_D_CM(_spoil_scenario,_TestResult,_TotalResult);
   TEST_BaseStat_D_CM2(_spoil_scenario,_TestResult,_TotalResult);
   TEST_BaseStat_T_Base(_spoil_scenario,_TestResult,_TotalResult);
   TEST_BaseStat_T_CovCorr(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MatInv_D_R1(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MatInv_D_C1(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MatInv_D_SPD1(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MatInv_D_HPD1(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MatInv_T_R1(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MatInv_T_C1(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MatInv_E_SPD1(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MatInv_E_HPD1(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MinCG_D_1(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MinCG_D_2(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MinCG_NumDiff(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MinCG_FTRIM(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MinBLEIC_D_1(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MinBLEIC_D_2(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MinBLEIC_NumDiff(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MinBLEIC_FTRIM(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MCPD_Simple1(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MCPD_Simple2(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MinLBFGS_D_1(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MinLBFGS_D_2(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MinLBFGS_NumDiff(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MinLBFGS_FTRIM(_spoil_scenario,_TestResult,_TotalResult);
   TEST_ODESolver_D1(_spoil_scenario,_TestResult,_TotalResult);
   TEST_FFT_Complex_D1(_spoil_scenario,_TestResult,_TotalResult);
   TEST_FFT_Complex_D2(_spoil_scenario,_TestResult,_TotalResult);
   TEST_FFT_Real_D1(_spoil_scenario,_TestResult,_TotalResult);
   TEST_FFT_Real_D2(_spoil_scenario,_TestResult,_TotalResult);
   TEST_FFT_Complex_E1(_spoil_scenario,_TestResult,_TotalResult);
   TEST_AutoGK_D1(_spoil_scenario,_TestResult,_TotalResult);
   TEST_PolInt_D_CalcDiff(_spoil_scenario,_TestResult,_TotalResult);
   TEST_PolInt_D_Conv(_spoil_scenario,_TestResult,_TotalResult);
   TEST_PolInt_D_Spec(_spoil_scenario,_TestResult,_TotalResult);
   TEST_PolInt_T_1(_spoil_scenario,_TestResult,_TotalResult);
   TEST_PolInt_T_2(_spoil_scenario,_TestResult,_TotalResult);
   TEST_PolInt_T_3(_spoil_scenario,_TestResult,_TotalResult);
   TEST_PolInt_T_4(_spoil_scenario,_TestResult,_TotalResult);
   TEST_PolInt_T_5(_spoil_scenario,_TestResult,_TotalResult);
   TEST_PolInt_T_6(_spoil_scenario,_TestResult,_TotalResult);
   TEST_PolInt_T_7(_spoil_scenario,_TestResult,_TotalResult);
   TEST_PolInt_T_8(_spoil_scenario,_TestResult,_TotalResult);
   TEST_PolInt_T_9(_spoil_scenario,_TestResult,_TotalResult);
   //--- 50 blocks were successful
   Print("50/91");
   TEST_PolInt_T_10(_spoil_scenario,_TestResult,_TotalResult);
   TEST_PolInt_T_11(_spoil_scenario,_TestResult,_TotalResult);
   TEST_PolInt_T_12(_spoil_scenario,_TestResult,_TotalResult);
   TEST_PolInt_T_13(_spoil_scenario,_TestResult,_TotalResult);
   TEST_Spline1D_D_Linear(_spoil_scenario,_TestResult,_TotalResult);
   TEST_Spline1D_D_Cubic(_spoil_scenario,_TestResult,_TotalResult);
   TEST_Spline1D_D_GridDiff(_spoil_scenario,_TestResult,_TotalResult);
   TEST_Spline1D_D_ConvDiff(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MinQP_D_U1(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MinQP_D_BC1(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MinLM_D_V(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MinLM_D_VJ(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MinLM_D_FGH(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MinLM_D_VB(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MinLM_D_Restarts(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MinLM_T_1(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MinLM_T_2(_spoil_scenario,_TestResult,_TotalResult);
   TEST_LSFit_D_NLF(_spoil_scenario,_TestResult,_TotalResult);
   TEST_LSFit_D_NLFG(_spoil_scenario,_TestResult,_TotalResult);
   TEST_LSFit_D_NLFGH(_spoil_scenario,_TestResult,_TotalResult);
   TEST_LSFit_D_NLFB(_spoil_scenario,_TestResult,_TotalResult);
   TEST_LSFit_D_NLScale(_spoil_scenario,_TestResult,_TotalResult);
   TEST_LSFit_D_Lin(_spoil_scenario,_TestResult,_TotalResult);
   TEST_LSFit_D_Linc(_spoil_scenario,_TestResult,_TotalResult);
   TEST_LSFit_D_Pol(_spoil_scenario,_TestResult,_TotalResult);
   TEST_LSFit_D_Polc(_spoil_scenario,_TestResult,_TotalResult);
   TEST_LSFit_D_Spline(_spoil_scenario,_TestResult,_TotalResult);
   TEST_LSFit_T_PolFit_1(_spoil_scenario,_TestResult,_TotalResult);
   TEST_LSFit_T_PolFit_2(_spoil_scenario,_TestResult,_TotalResult);
   TEST_LSFit_T_PolFit_3(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MatDet_D_1(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MatDet_D_2(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MatDet_D_3(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MatDet_D_4(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MatDet_D_5(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MatDet_T_0(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MatDet_T_1(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MatDet_T_2(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MatDet_T_3(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MatDet_T_4(_spoil_scenario,_TestResult,_TotalResult);
   TEST_MatDet_T_5(_spoil_scenario,_TestResult,_TotalResult);
   //--- all blocks were successful
   Print("91/91");
   //--- print total result
   Print("Result = ",_TotalResult);
  }
//+------------------------------------------------------------------+
