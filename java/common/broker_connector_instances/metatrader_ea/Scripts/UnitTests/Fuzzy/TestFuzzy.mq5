//+------------------------------------------------------------------+
//|                                                    TestFuzzy.mq5 |
//|                   Copyright 2015-2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
//| Implementation of Fuzzy library in MetaQuotes Language 5         |
//|                                                                  |
//| The features of the library include:                             |
//| - Create Mamdani fuzzy model                                     |
//| - Create Sugeno fuzzy model                                      |
//| - Normal membership function                                     |
//| - Triangular membership function                                 |
//| - Trapezoidal membership function                                |
//| - Constant membership function                                   |
//| - Defuzzification method of center of gravity (COG)              |
//| - Defuzzification method of bisector of area (BOA)               |
//| - Defuzzification method of mean of maxima (MeOM)                |
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
#include <Math\Fuzzy\MamdaniFuzzySystem.mqh>
#include <Math\Fuzzy\SugenoFuzzySystem.mqh>
//+------------------------------------------------------------------+
//| Test_NormalCombinationMembershipFunction()                       |
//+------------------------------------------------------------------+
bool Test_NormalCombinationMembershipFunction()
  {
   bool result=true;
   double delta=1e-20;
   double x[]={-4.0,5.1,0.1};
   double b1=1.2;
   double sigma1=0.45;
   double b2=3.1;
   double sigma2=0.9;
   CNormalCombinationMembershipFunction function(b1,sigma1,b2,sigma2);
   for(int i=0; i<ArraySize(x); i++)
     {
      double actual=function.GetValue(x[i]);
      double expected=0.0;
      if(x[i]<1.2)
        {
         expected=MathExp(MathPow(x[i]-b1,2)/(-2.0*MathPow(0.45,2)));
        }
      else if(x[i]>3.1)
        {
         expected=MathExp(MathPow(x[i]-b2,2)/(-2.0*MathPow(0.9,2)));
        }
      if(MathAbs(actual-expected)>1e-20)
        {
         Print("Expected: ",expected," +/- ",delta," ; But was: ",actual);
         result=false;
        }
     }
   return (result);
  }
//+------------------------------------------------------------------+
//| Test_GeneralizedBellShapedMembershipFunction()                   |
//+------------------------------------------------------------------+
bool Test_GeneralizedBellShapedMembershipFunction()
  {
   bool result=true;
   double delta=1e-20;
   double x[]={-4,5.1,0.1};
   double a = 2.4;
   double b = 0.9;
   double c = 1.33;
   CGeneralizedBellShapedMembershipFunction function(a,b,c);
   for(int i=0; i<ArraySize(x); i++)
     {
      double actual=function.GetValue(x[i]);
      double expected=1/(1+MathPow(MathAbs((x[i]-a)/c),2*b));
      if(actual!=expected)
        {
         Print("Expected: ",expected," +/- ",delta," ; But was: ",actual);
         result=false;
        }
     }
   return (result);
  }
//+------------------------------------------------------------------+
//| Test_SigmoidalMembershipFunction()                               |
//+------------------------------------------------------------------+
bool Test_SigmoidalMembershipFunction()
  {
   bool result=true;
   double delta=1e-20;
   double x[]={-4,4.1,0.1};
   double a = 1.75;
   double c = -M_PI/2;
   CSigmoidalMembershipFunction function(a,c);
   for(int i=0; i<ArraySize(x); i++)
     {
      double actual=function.GetValue(x[i]);
      double expected=1/(1.0+MathExp(-a *(x[i]-c)));
      if(actual!=expected)
        {
         Print("Expected: ",expected," +/- ",delta," ; But was: ",actual);
         result=false;
        }
     }
   return (result);
  }
//+------------------------------------------------------------------+
//| Test_ProductTwoSigmoidalMembershipFunctions()                    |
//+------------------------------------------------------------------+
bool Test_ProductTwoSigmoidalMembershipFunctions()
  {
   bool result=true;
   double delta=1e-20;
   double x[]={-4.0,4.1,0.1};
   double a1 = -1.75;
   double c1 = -M_PI/2.0;
   double a2 = 0.972;
   double c2 = 0.43;
   CProductTwoSigmoidalMembershipFunctions function(a1,c1,a2,c2);
   for(int i=0; i<ArraySize(x); i++)
     {
      double actual=function.GetValue(x[i]);
      double expected=((1.0/(1.0+MathExp(-a1 *(x[i]-c1)))) *
                       (1.0/(1.0+MathExp(-a2 *(x[i]-c2)))));
      if(actual!=expected)
        {
         Print("Expected: ",expected," +/- ",delta," ; But was: ",actual);
         result=false;
        }
     }
   return (result);
  }
//+------------------------------------------------------------------+
//| Test_TrapezoidMembershipFunction()                               |
//+------------------------------------------------------------------+
bool Test_TrapezoidMembershipFunction()
  {
   bool result=true;
   double delta=1e-20;
   double x[]={-4,3.1,0.1};
   double x1 = -4;
   double x2 = -4;
   double x3 = 2;
   double x4 = M_PI;
   CTrapezoidMembershipFunction function(x1,x2,x3,x4);
   for(int i=0; i<ArraySize(x); i++)
     {
      double actual=function.GetValue(x[i]);
      double expected=1.0;
      if(x[i]>2 && x[i]<M_PI)
        {
         expected=(-x[i]/(x4-x3))+(x4/(x4-x3));
        }
      else if(x[i]>M_PI)
        {
         expected=0;
        }
      if(actual!=expected)
        {
         Print("Expected: ",expected," +/- ",delta," ; But was: ",actual);
         result=false;
        }
     }
   return (result);
  }
//+------------------------------------------------------------------+
//| Test_NormalMembershipFunction()                                  |
//+------------------------------------------------------------------+
bool Test_NormalMembershipFunction()
  {
   bool result=true;
   double delta=1e-20;
   double x[]={-4,5.1,0.1};
   double b=1.33;
   double sigma=0.45;
   CNormalMembershipFunction function(b,sigma);
   for(int i=0; i<ArraySize(x); i++)
     {
      double actual=function.GetValue(x[i]);
      double expected=MathExp(MathPow(x[i]-1.33,2.0)/(-2.0*MathPow(0.45,2.0)));
      if(actual!=expected)
        {
         Print("Expected: ",expected," +/- ",delta," ; But was: ",actual);
         result=false;
        }
     }
   return (result);
  }
//+------------------------------------------------------------------+
//| Test_TriangularMembershipFunction()                              |
//+------------------------------------------------------------------+
bool Test_TriangularMembershipFunction()
  {
   bool result=true;
   double delta=1e-20;
   double x[]={-4,4.1,0.1};
   double x1 = -M_PI;
   double x2 = -M_E/2.0;
   double x3 = M_PI/5.0;
   CTriangularMembershipFunction function(x1,x2,x3);
   for(int i=0; i<ArraySize(x); i++)
     {
      double actual=function.GetValue(x[i]);
      double expected=0;
      if(x[i]==x2)
        {
         expected=1;
        }
      else if(x[i]>x1 && x[i]<x2)
        {
         expected=(x[i]/(x2-x1)) -(x1/(x2-x1));
        }
      else if(x[i]>x2 && x[i]<x3)
        {
         expected=(-x[i]/(x3-x2))+(x3/(x3-x2));
        }
      if(actual!=expected)
        {
         Print("Expected: ",expected," +/- ",delta," ; But was: ",actual);
         result=false;
        }
     }
   return (result);
  }
//+------------------------------------------------------------------+
//| Test_ConstantMembershipFunction()                                |
//+------------------------------------------------------------------+
bool Test_ConstantMembershipFunction()
  {
   bool result=true;
   double delta=1e-20;
   double x[]={-4,4.1,0.1};
   double value=1.0;
   CConstantMembershipFunction function(1.0);
   for(int i=0; i<ArraySize(x); i++)
     {
      double actual=function.GetValue(x[i]);
      double expected=value;
      if(actual!=expected)
        {
         Print("Expected: ",expected," +/- ",delta," ; But was: ",actual);
         result=false;
        }
     }
   return (result);
  }
//+------------------------------------------------------------------+
//| Test_P_S_Z_ShapedMembershipFunction()                            |
//+------------------------------------------------------------------+
bool Test_P_S_Z_ShapedMembershipFunction()
  {
   bool result=true;
   double delta=1e-20;
   double x[]={-4.0,4.1,0.1};
   CS_ShapedMembershipFunction sfunction(-1.0/137.0,M_PI/2.0);
   CZ_ShapedMembershipFunction zfunction(MathExp(1.0),M_PI);
   CP_ShapedMembershipFunction pfunction(-1.0/137.0,M_PI/2.0,MathExp(1.0),M_PI);
   for(int i=0; i<ArraySize(x); i++)
     {
      double actual=pfunction.GetValue(x[i]);
      double expected=sfunction.GetValue(x[i])*zfunction.GetValue(x[i]);
      if(actual!=expected)
        {
         Print("Expected: ",expected," +/- ",delta," ; But was: ",actual);
         result=false;
        }
     }
   return (result);
  }
//+------------------------------------------------------------------+
//| Test_Bisector()                                                  |
//+------------------------------------------------------------------+
bool Test_Bisector()
  {
   double delta=1e-10;
   CMamdaniFuzzySystem system();
   system.DefuzzificationMethod(BisectorDef);
   CTriangularMembershipFunction *function=new CTriangularMembershipFunction(0,5,5);
   double actual=system.Defuzzify(function,0,5);
   double expected=3.5;
   if(MathAbs(actual-expected)>=delta)
     {
      Print("Expected: ",expected," +/- ",delta," ; But was: ",actual);
      return (false);
     }
   return (true);
  }
//+------------------------------------------------------------------+
//| Test_Centroid()                                                  |
//+------------------------------------------------------------------+
bool Test_Centroid()
  {
   bool result=true;
   double delta=1e-1;
   CMamdaniFuzzySystem system();
   system.DefuzzificationMethod(CentroidDef);
   double mean[]={-5,-3,-1,1,3,5};
   double sigma[]={1,2};
   for(int i=0; i<ArraySize(mean); i++)
     {
      for(int j=0; j<ArraySize(sigma); j++)
        {
         CNormalMembershipFunction *function=new CNormalMembershipFunction(mean[i],sigma[j]);
         double actual=system.Defuzzify(function,-10,10);
         double expected=mean[i];
         if(MathAbs(actual-expected)>=delta)
           {
            Print("Expected: ",expected," +/- ",delta," ; But was: ",actual);
            result=false;
           }
        }
     }
   return (result);
  }
//+------------------------------------------------------------------+
//| Test_Defuzzification()                                           |
//+------------------------------------------------------------------+
bool Test_Defuzzification()
  {
   bool result=true;
   double delta=1e-12;
   CMamdaniFuzzySystem system();
   double actual=0.0;
   double expected=0.0;
//---  
   system.DefuzzificationMethod(CentroidDef);
   actual=system.Defuzzify(new CNormalMembershipFunction(0,2),-10,10);
   if(MathAbs(actual-expected)>=delta)
     {
      Print("Expected: ",expected," +/- ",delta," ; But was: ",actual);
      result=false;
     }
//---
   system.DefuzzificationMethod(BisectorDef);
   actual=system.Defuzzify(new CNormalMembershipFunction(0,2),-10,10);
   if(MathAbs(actual-expected)>=delta)
     {
      Print("Expected: ",expected," +/- ",delta," ; But was: ",actual);
      result=false;
     }
//---
   system.DefuzzificationMethod(AverageMaximumDef);
   actual=system.Defuzzify(new CNormalMembershipFunction(0,2),-10,10);
   if(MathAbs(actual-expected)>=delta)
     {
      Print("Expected: ",expected," +/- ",delta," ; But was: ",actual);
      result=false;
     }
//---
   system.DefuzzificationMethod(SmallestMaximumDef);
   actual=system.Defuzzify(new CNormalMembershipFunction(0,2),-10,10);
   if(MathAbs(actual-expected)>=delta)
     {
      Print("Expected: ",expected," +/- ",delta," ; But was: ",actual);
      result=false;
     }
//---
   system.DefuzzificationMethod(LargestMaximumDef);
   actual=system.Defuzzify(new CNormalMembershipFunction(0,2),-10,10);
   if(MathAbs(actual-expected)>=delta)
     {
      Print("Expected: ",expected," +/- ",delta," ; But was: ",actual);
      result=false;
     }
   return (result);
  }
//+------------------------------------------------------------------+
//| Test_TipingProblem()                                             |
//+------------------------------------------------------------------+
bool Test_TipingProblem()
  {
//--- Mamdani Fuzzy System  
   CMamdaniFuzzySystem *fsTips=new CMamdaniFuzzySystem();
//--- Create first input variables for the system
   CFuzzyVariable *fvService=new CFuzzyVariable("service",0.0,10.0);
   fvService.Terms().Add(new CFuzzyTerm("poor", new CTriangularMembershipFunction(-5.0, 0.0, 5.0)));
   fvService.Terms().Add(new CFuzzyTerm("good", new CTriangularMembershipFunction(0.0, 5.0, 10.0)));
   fvService.Terms().Add(new CFuzzyTerm("excellent", new CTriangularMembershipFunction(5.0, 10.0, 15.0)));
   fsTips.Input().Add(fvService);
//--- Create second input variables for the system
   CFuzzyVariable *fvFood=new CFuzzyVariable("food",0.0,10.0);
   fvFood.Terms().Add(new CFuzzyTerm("rancid", new CTrapezoidMembershipFunction(0.0, 0.0, 1.0, 3.0)));
   fvFood.Terms().Add(new CFuzzyTerm("delicious", new CTrapezoidMembershipFunction(7.0, 9.0, 10.0, 10.0)));
   fsTips.Input().Add(fvFood);
//--- Create Output
   CFuzzyVariable *fvTips=new CFuzzyVariable("tips",0.0,30.0);
   fvTips.Terms().Add(new CFuzzyTerm("cheap", new CTriangularMembershipFunction(0.0, 5.0, 10.0)));
   fvTips.Terms().Add(new CFuzzyTerm("average", new CTriangularMembershipFunction(10.0, 15.0, 20.0)));
   fvTips.Terms().Add(new CFuzzyTerm("generous", new CTriangularMembershipFunction(20.0, 25.0, 30.0)));
   fsTips.Output().Add(fvTips);
//--- Create three Mamdani fuzzy rule
   CMamdaniFuzzyRule *rule1 = fsTips.ParseRule("if (service is poor )  or (food is rancid) then tips is cheap");
   CMamdaniFuzzyRule *rule2 = fsTips.ParseRule("if ((service is good)) then tips is average");
   CMamdaniFuzzyRule *rule3 = fsTips.ParseRule("if (service is excellent) or (food is delicious) then (tips is generous)");
//--- Add three Mamdani fuzzy rule in system
   fsTips.Rules().Add(rule1);
   fsTips.Rules().Add(rule2);
   fsTips.Rules().Add(rule3);
//--- Set input value
   CList *in=new CList;
   CDictionary_Obj_Double *p_od_Service=new CDictionary_Obj_Double;
   CDictionary_Obj_Double *p_od_Food=new CDictionary_Obj_Double;
//--- Testing values
   double Food=6.5;
   double Service=9.8;
   double expected=24.3;
   p_od_Service.SetAll(fvService,Service);
   p_od_Food.SetAll(fvFood,Food);
   in.Add(p_od_Service);
   in.Add(p_od_Food);
//--- Get result
   CList *result;
   CDictionary_Obj_Double *p_od_Tips;
   result=fsTips.Calculate(in);
   p_od_Tips=result.GetNodeAtIndex(0);
   double actual=NormalizeDouble(p_od_Tips.Value(),1);
   delete in;
   delete result;
   delete fsTips;
   if(expected!=actual)
     {
      Print("Expected: ",expected," ; But was: ",actual);
      //--- failed
      return (false);
     }
   else
     {
      //--- success
      return (true);
     }
  }
//+------------------------------------------------------------------+
//| Test_TypicalFuzzyControlSystem()                                 |
//+------------------------------------------------------------------+
bool Test_TypicalFuzzyControlSystem()
  {
//--- Sugeno Fuzzy System  
   CSugenoFuzzySystem *fsCruiseControl=new CSugenoFuzzySystem();
//--- Create first input variables for the system
   CFuzzyVariable *fvSpeedError=new CFuzzyVariable("SpeedError",-20.0,20.0);
   fvSpeedError.Terms().Add(new CFuzzyTerm("slower",new CTriangularMembershipFunction(-35.0,-20.0,-5.0)));
   fvSpeedError.Terms().Add(new CFuzzyTerm("zero", new CTriangularMembershipFunction(-15.0, -0.0, 15.0)));
   fvSpeedError.Terms().Add(new CFuzzyTerm("faster", new CTriangularMembershipFunction(5.0, 20.0, 35.0)));
   fsCruiseControl.Input().Add(fvSpeedError);
//--- Create second input variables for the system
   CFuzzyVariable *fvSpeedErrorDot=new CFuzzyVariable("SpeedErrorDot",-5.0,5.0);
   fvSpeedErrorDot.Terms().Add(new CFuzzyTerm("slower", new CTriangularMembershipFunction(-9.0, -5.0, -1.0)));
   fvSpeedErrorDot.Terms().Add(new CFuzzyTerm("zero", new CTriangularMembershipFunction(-4.0, -0.0, 4.0)));
   fvSpeedErrorDot.Terms().Add(new CFuzzyTerm("faster", new CTriangularMembershipFunction(1.0, 5.0, 9.0)));
   fsCruiseControl.Input().Add(fvSpeedErrorDot);
//--- Create Output
   CSugenoVariable *svAccelerate=new CSugenoVariable("Accelerate");
   double coeff1[3]={0.0,0.0,0.0};
   svAccelerate.Functions().Add(fsCruiseControl.CreateSugenoFunction("zero",coeff1));
   double coeff2[3]={0.0,0.0,1.0};
   svAccelerate.Functions().Add(fsCruiseControl.CreateSugenoFunction("faster",coeff2));
   double coeff3[3]={0.0,0.0,-1.0};
   svAccelerate.Functions().Add(fsCruiseControl.CreateSugenoFunction("slower",coeff3));
   double coeff4[3]={-0.04,-0.1,0.0};
   svAccelerate.Functions().Add(fsCruiseControl.CreateSugenoFunction("func",coeff4));
   fsCruiseControl.Output().Add(svAccelerate);
//--- Craete Sugeno fuzzy rules
   AddSugenoFuzzyRule(fsCruiseControl,fvSpeedError,fvSpeedErrorDot,svAccelerate,"slower","slower","faster");
   AddSugenoFuzzyRule(fsCruiseControl,fvSpeedError,fvSpeedErrorDot,svAccelerate,"slower","zero","faster");
   AddSugenoFuzzyRule(fsCruiseControl,fvSpeedError,fvSpeedErrorDot,svAccelerate,"slower","faster","zero");
   AddSugenoFuzzyRule(fsCruiseControl,fvSpeedError,fvSpeedErrorDot,svAccelerate,"zero","slower","faster");
   AddSugenoFuzzyRule(fsCruiseControl,fvSpeedError,fvSpeedErrorDot,svAccelerate,"zero","zero","func");
   AddSugenoFuzzyRule(fsCruiseControl,fvSpeedError,fvSpeedErrorDot,svAccelerate,"zero","faster","slower");
   AddSugenoFuzzyRule(fsCruiseControl,fvSpeedError,fvSpeedErrorDot,svAccelerate,"faster","slower","zero");
   AddSugenoFuzzyRule(fsCruiseControl,fvSpeedError,fvSpeedErrorDot,svAccelerate,"faster","zero","slower");
   AddSugenoFuzzyRule(fsCruiseControl,fvSpeedError,fvSpeedErrorDot,svAccelerate,"faster","faster","slower");
//--- Set input value and get result
   CList *in=new CList;
   CDictionary_Obj_Double *p_od_Error=new CDictionary_Obj_Double;
   CDictionary_Obj_Double *p_od_ErrorDot=new CDictionary_Obj_Double;
   double Speed_Error=18.3;
   double Speed_ErrorDot=-3.5;
   double expected=-16.7;
   p_od_Error.SetAll(fvSpeedError,Speed_Error);
   p_od_ErrorDot.SetAll(fvSpeedErrorDot,Speed_ErrorDot);
   in.Add(p_od_Error);
   in.Add(p_od_ErrorDot);
//--- Get result
   CList *result;
   CDictionary_Obj_Double *p_od_Accelerate;
   result=fsCruiseControl.Calculate(in);
   p_od_Accelerate=result.GetNodeAtIndex(0);
   double actual=NormalizeDouble(p_od_Accelerate.Value()*100,1);
   delete in;
   delete result;
   delete fsCruiseControl;
   if(expected!=actual)
     {
      Print("Expected: ",expected," ; But was: ",actual);
      //--- failed
      return (false);
     }
   else
     {
      //--- success
      return (true);
     }
  }
//+------------------------------------------------------------------+
//| AddSugenoFuzzyRule()                                             |
//+------------------------------------------------------------------+
void AddSugenoFuzzyRule(CSugenoFuzzySystem *fs,CFuzzyVariable *fv1,CFuzzyVariable *fv2,CSugenoVariable *sv,
                        const string value1,const string value2,const string result)
  {
   CSugenoFuzzyRule *rule=fs.EmptyRule();
   rule.Conclusion(new CSingleCondition());
   rule.Condition().Op(OperatorType::And);
   rule.Condition().ConditionsList().Add(rule.CreateCondition(fv1, fv1.GetTermByName(value1)));
   rule.Condition().ConditionsList().Add(rule.CreateCondition(fv2, fv2.GetTermByName(value2)));
   rule.Conclusion().Var(sv);
   INamedValue *sf=sv.GetFuncByName(result);
   rule.Conclusion().Term(sf);
   fs.Rules().Add(rule);
  }
//+------------------------------------------------------------------+
//| TestMembersipFunctions                                           |
//+------------------------------------------------------------------+
bool TestMembersipFunctions(const string test_name)
  {
   PrintFormat("%s started",test_name);
//--- test 1
   PrintFormat("%s: Test 1: calculation of the values for combination two normal membership function",test_name);
   if(!Test_NormalCombinationMembershipFunction())
      return (false);
//--- test 2
   PrintFormat("%s: Test 2: calculation of the values for generalized bell-shaped membership function",test_name);
   if(!Test_GeneralizedBellShapedMembershipFunction())
      return (false);
//--- test 3
   PrintFormat("%s: Test 3: calculation of the values for sigmoidal membership function",test_name);
   if(!Test_SigmoidalMembershipFunction())
      return (false);
//--- test 4
   PrintFormat("%s: Test 4: calculation of the values for product two sigmoidal membership function",test_name);
   if(!Test_ProductTwoSigmoidalMembershipFunctions())
      return (false);
//--- test 5
   PrintFormat("%s: Test 5: calculation of the values for trapezoid membership function",test_name);
   if(!Test_TrapezoidMembershipFunction())
      return (false);
//--- test 6
   PrintFormat("%s: Test 6: calculation of the values for normal membership function",test_name);
   if(!Test_NormalMembershipFunction())
      return (false);
//--- test 7
   PrintFormat("%s: Test 7: calculation of the values for triangular membership function",test_name);
   if(!Test_TriangularMembershipFunction())
      return (false);
//--- test 8
   PrintFormat("%s: Test 8: calculation of the values for constant membership function",test_name);
   if(!Test_ConstantMembershipFunction())
      return (false);
//--- test 9
   PrintFormat("%s: Test 9: comparing the results of calculations 'P' shaped function and product 'S' and 'Z' shaped function",test_name);
   if(!Test_P_S_Z_ShapedMembershipFunction())
      return (false);
//--- successful
   PrintFormat("%s passed",test_name);
   return (true);
  }
//+------------------------------------------------------------------+
//| TestDefuzzificationMethods                                       |
//+------------------------------------------------------------------+
bool TestDefuzzificationMethods(const string test_name)
  {
   PrintFormat("%s started",test_name);
//--- test 1
   PrintFormat("%s: Test 1: application of the Bisector defuzzification for a triangular membership function",test_name);
   if(!Test_Bisector())
      return (false);
//--- test 2
   PrintFormat("%s: Test 2: application of the Centroid defuzzification for a normal membership function",test_name);
   if(!Test_Centroid())
      return (false);
//--- test 3
   PrintFormat("%s: Test 3: application of the Bisector, Centroid, Middle, Smallest, and Largest of Maximum  defuzzification for a normal membership function",test_name);
   if(!Test_Defuzzification())
      return (false);
//--- successful
   PrintFormat("%s passed",test_name);
   return (true);
  }
//+------------------------------------------------------------------+
//| TestFuzzySystems                                                 |
//+------------------------------------------------------------------+
bool TestFuzzySystems(const string test_name)
  {
   PrintFormat("%s started",test_name);
//--- test 1
   PrintFormat("%s: Test 1: calculate result for Mamdani fuzzy system",test_name);
   if(!Test_TipingProblem())
      return (false);
//--- test 2
   PrintFormat("%s: Test 2: calculate result for Sugeno fuzzy system",test_name);
   if(!Test_TypicalFuzzyControlSystem())
      return (false);
//--- successful
   PrintFormat("%s passed",test_name);
   return (true);
  }
//+------------------------------------------------------------------+
//| TestFuzzy                                                        |
//+------------------------------------------------------------------+
void TestFuzzy(int &tests_performed,int &tests_passed)
  {
//--- Membersip functions
   tests_performed++;
   string test_name="Membersip functions test";
   if(TestMembersipFunctions(test_name))
      tests_passed++;
   else
      PrintFormat("%s failed",test_name);

//--- Defuzzification methods
   tests_performed++;
   test_name="Defuzzification methods test";
   if(TestDefuzzificationMethods(test_name))
      tests_passed++;
   else
      PrintFormat("%s failed",test_name);

//--- Fuzzy systems
   tests_performed++;
   test_name="Fuzzy systems test";
   if(TestFuzzySystems(test_name))
      tests_passed++;
   else
      PrintFormat("%s failed",test_name);
   return;
  }
//+------------------------------------------------------------------+
//| UnitTests()                                                      |
//+------------------------------------------------------------------+
void UnitTests(const string package_name)
  {
   PrintFormat("Unit tests for Package %s\n",package_name);
//--- initial values
   int tests_performed=0;
   int tests_passed=0;
//--- test distributions
   TestFuzzy(tests_performed,tests_passed);
//--- print statistics
   PrintFormat("\n%d of %d passed",tests_passed,tests_performed);
  }
//+------------------------------------------------------------------+
//| Script program start function                                    |
//+------------------------------------------------------------------+
void OnStart()
  {
   UnitTests("Fuzzy");
  }
//+------------------------------------------------------------------+
