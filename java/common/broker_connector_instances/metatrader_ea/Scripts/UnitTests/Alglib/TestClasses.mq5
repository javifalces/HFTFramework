//+------------------------------------------------------------------+
//|                                                  TestClasses.mq5 |
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
//| This file is free software; you can redistribute it and/or       |
//| modify it under the terms of the GNU General Public License as   |
//| published by the Free Software Foundation (www.fsf.org); either  |
//| version 2 of the License, or (at your option) any later version. |
//|                                                                  |
//| This program is distributed in the hope that it will be useful,  |
//| but WITHOUT ANY WARRANTY; without even the implied warranty of   |
//| MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the     |
//| GNU General Public License for more details.                     |
//+------------------------------------------------------------------+
#include "TestClasses.mqh"
//+------------------------------------------------------------------+
//| Testing script                                                   |
//+------------------------------------------------------------------+
void OnStart()
  {
   uint seed;
   int  result;
   bool silent;
//--- setting this option to generate random numbers
   _RandomSeed=GetTickCount();
//--- initialization
   seed=_RandomSeed;
   result=0;
   silent=true;
//--- start time
   Print(TimeLocal());
//--- seed
   PrintFormat("RandomSeed = %d",seed);
//--- check class
   if(CTestHQRndUnit::TestHQRnd(silent))
      PrintFormat("CHighQualityRand: OK");
   else
      PrintFormat("CHighQualityRand: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestTSortUnit::TestTSort(silent))
      PrintFormat("CTSort: OK");
   else
      PrintFormat("CTSort: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestNearestNeighborUnit::TestNearestNeighbor(silent))
      PrintFormat("CNearestNeighbor: OK");
   else
      PrintFormat("CNearestNeighbor: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestAblasUnit::TestAblas(silent))
      PrintFormat("CAblas: OK");
   else
      PrintFormat("CAblas: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestBaseStatUnit::TestBaseStat(silent))
      PrintFormat("CBaseStat: OK");
   else
      PrintFormat("CBaseStat: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestBdSSUnit::TestBdSS(silent))
      PrintFormat("CBdSS: OK");
   else
      PrintFormat("CBdSS: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestDForestUnit::TestDForest(silent))
      PrintFormat("CDForest: OK");
   else
      PrintFormat("CDForest: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestBlasUnit::TestBlas(silent))
      PrintFormat("CBlas: OK");
   else
      PrintFormat("CBlas: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestKMeansUnit::TestKMeans(silent))
      PrintFormat("CKMeans: OK");
   else
      PrintFormat("CKMeans: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestHblasUnit::TestHblas(silent))
      PrintFormat("CHblas: OK");
   else
      PrintFormat("CHblas: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestReflectionsUnit::TestReflections(silent))
      PrintFormat("CReflections: OK");
   else
      PrintFormat("CReflections: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestCReflectionsUnit::TestCReflections(silent))
      PrintFormat("CComplexReflections: OK");
   else
      PrintFormat("CComplexReflections: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestSblasUnit::TestSblas(silent))
      PrintFormat("CSblas: OK");
   else
      PrintFormat("CSblas: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestOrtFacUnit::TestOrtFac(silent))
      PrintFormat("COrtFac: OK");
   else
      PrintFormat("COrtFac: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestEVDUnit::TestEVD(silent))
      PrintFormat("CEigenVDetect: OK");
   else
      PrintFormat("CEigenVDetect: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestMatGenUnit::TestMatGen(silent))
      PrintFormat("CMatGen: OK");
   else
      PrintFormat("CMatGen: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestTrFacUnit::TestTrFac(silent))
      PrintFormat("CTrFac: OK");
   else
      PrintFormat("CTrFac: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestTrLinSolveUnit::TestTrLinSolve(silent))
      PrintFormat("CTrLinSolve: OK");
   else
      PrintFormat("CTrLinSolve: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestSafeSolveUnit::TestSafeSolve(silent))
      PrintFormat("CSafeSolve: OK");
   else
      PrintFormat("CSafeSolve: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestRCondUnit::TestRCond(silent))
      PrintFormat("CRCond: OK");
   else
      PrintFormat("CRCond: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestMatInvUnit::TestMatInv(silent))
      PrintFormat("CMatInv: OK");
   else
      PrintFormat("CMatInv: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestLDAUnit::TestLDA(silent))
      PrintFormat("CLDA: OK");
   else
      PrintFormat("CLDA: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestGammaFuncUnit::TestGammaFunc(silent))
      PrintFormat("CGammaFunc: OK");
   else
      PrintFormat("CGammaFunc: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestBdSVDUnit::TestBdSVD(silent))
      PrintFormat("CBdSingValueDecompose: OK");
   else
      PrintFormat("CBdSingValueDecompose: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestSVDUnit::TestSVD(silent))
      PrintFormat("CSingValueDecompose: OK");
   else
      PrintFormat("CSingValueDecompose: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestLinRegUnit::TestLinReg(silent))
      PrintFormat("CLinReg: OK");
   else
      PrintFormat("CLinReg: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestXBlasUnit::TestXBlas(silent))
      PrintFormat("CXblas: OK");
   else
      PrintFormat("CXblas: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestDenseSolverUnit::TestDenseSolver(silent))
      PrintFormat("CDenseSolver: OK");
   else
      PrintFormat("CDenseSolver: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestLinMinUnit::TestLinMin(silent))
      PrintFormat("CLinMin: OK");
   else
      PrintFormat("CLinMin: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestMinCGUnit::TestMinCG(silent))
      PrintFormat("CMinCG: OK");
   else
      PrintFormat("CMinCG: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestMinBLEICUnit::TestMinBLEIC(silent))
      PrintFormat("CMinBLEIC: OK");
   else
      PrintFormat("CMinBLEIC: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestMCPDUnit::TestMCPD(silent))
      PrintFormat("CMarkovCPD: OK");
   else
      PrintFormat("CMarkovCPD: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestFblsUnit::TestFbls(silent))
      PrintFormat("CFbls: OK");
   else
      PrintFormat("CFbls: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestMinLBFGSUnit::TestMinLBFGS(silent))
      PrintFormat("CMinLBFGS: OK");
   else
      PrintFormat("CMinLBFGS: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestMLPTrainUnit::TestMLPTrain(silent))
      PrintFormat("CMLPTrain: OK");
   else
      PrintFormat("CMLPTrain: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestMLPEUnit::TestMLPE(silent))
      PrintFormat("CMLPE: OK");
   else
      PrintFormat("CMLPE: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestPCAUnit::TestPCA(silent))
      PrintFormat("CPCAnalysis: OK");
   else
      PrintFormat("CPCAnalysis: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestODESolverUnit::TestODESolver(silent))
      PrintFormat("CODESolver: OK");
   else
      PrintFormat("CODESolver: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestFFTUnit::TestFFT(silent))
      PrintFormat("CFastFourierTransform: OK");
   else
      PrintFormat("CFastFourierTransform: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestConvUnit::TestConv(silent))
      PrintFormat("CConv: OK");
   else
      PrintFormat("CConv: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestCorrUnit::TestCorr(silent))
      PrintFormat("CCorr: OK");
   else
      PrintFormat("CCorr: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestFHTUnit::TestFHT(silent))
      PrintFormat("CFastHartleyTransform: OK");
   else
      PrintFormat("CFastHartleyTransform: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestGQUnit::TestGQ(silent))
      PrintFormat("CGaussQ: OK");
   else
      PrintFormat("CGaussQ: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestGKQUnit::TestGKQ(silent))
      PrintFormat("CGaussKronrodQ: OK");
   else
      PrintFormat("CGaussKronrodQ: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestAutoGKUnit::TestAutoGK(silent))
      PrintFormat("CAutoGK: OK");
   else
      PrintFormat("CAutoGK: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestIDWIntUnit::TestIDWInt(silent))
      PrintFormat("CIDWInt: OK");
   else
      PrintFormat("CIDWInt: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestRatIntUnit::TestRatInt(silent))
      PrintFormat("CRatInt: OK");
   else
      PrintFormat("CRatInt: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestPolIntUnit::TestPolInt(silent))
      PrintFormat("CPolInt: OK");
   else
      PrintFormat("CPolInt: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestSpline1DUnit::TestSpline1D(silent))
      PrintFormat("CSpline1D: OK");
   else
      PrintFormat("CSpline1D: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestMinLMUnit::TestMinLM(silent))
      PrintFormat("CMinLM: OK");
   else
      PrintFormat("CMinLM: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestLSFitUnit::TestLSFit(silent))
      PrintFormat("CLSFit: OK");
   else
      PrintFormat("CLSFit: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestPSplineUnit::TestPSpline(silent))
      PrintFormat("CPSpline: OK");
   else
      PrintFormat("CPSpline: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestSpline2DUnit::TestSpline2D(silent))
      PrintFormat("CSpline2D: OK");
   else
      PrintFormat("CSpline2D: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestSpdGEVDUnit::TestSpdGEVD(silent))
      PrintFormat("CSpdGEVD: OK");
   else
      PrintFormat("CSpdGEVD: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestInverseUpdateUnit::TestInverseUpdate(silent))
      PrintFormat("CInverseUpdate: OK");
   else
      PrintFormat("CInverseUpdate: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestSchurUnit::TestSchur(silent))
      PrintFormat("CSchur: OK");
   else
      PrintFormat("CSchur: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestNlEqUnit::TestNlEq(silent))
      PrintFormat("CNlEq: OK");
   else
      PrintFormat("CNlEq: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestChebyshevUnit::TestChebyshev(silent))
      PrintFormat("CChebyshev: OK");
   else
      PrintFormat("CChebyshev: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestHermiteUnit::TestHermite(silent))
      PrintFormat("CHermite: OK");
   else
      PrintFormat("CHermite: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestLaguerreUnit::TestLaguerre(silent))
      PrintFormat("CLaguerre: OK");
   else
      PrintFormat("CLaguerre: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestLegendreUnit::TestLegendre(silent))
      PrintFormat("CLegendre: OK");
   else
      PrintFormat("CLegendre: FAILED(seed=%d)",seed);
//--- check class
   _RandomSeed=seed;
   if(CTestAlglibBasicsUnit::TestAlglibBasics(silent))
      PrintFormat("AlglibBasics: OK");
   else
      PrintFormat("AlglibBasics: FAILED(seed=%d)",seed);
//--- finish time
   Print(TimeLocal());
  }
//+------------------------------------------------------------------+
