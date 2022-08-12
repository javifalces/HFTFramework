//+------------------------------------------------------------------+
//|                                                  TestHashSet.mq5 |
//|                        Copyright 2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <Generic\ArrayList.mqh>
#include <Generic\HashSet.mqh>
//+------------------------------------------------------------------+
//| TestMisc_Constructor.                                            |
//+------------------------------------------------------------------+
bool TestMisc_Constructor(const int count)
  {
//--- create array 
   int array[];
   ArrayResize(array,count);
   for(int i=0; i<count; i++)
      array[i]=MathRand();
//--- create array with duplicates
   int array_duplicates[];
   ArrayResize(array_duplicates,count*2);
   for(int i=0; i<count; i++)
      array_duplicates[i]=array[i];
   for(int i=0; i<count; i++)
      array_duplicates[count+i]=array[i];
//--- create source set
   CDefaultEqualityComparer<int>comparer();
   CHashSet<int>set_test1(array);
   CHashSet<int>set_test2(array_duplicates,GetPointer(comparer));
//--- check
   if(set_test1.Count()!=set_test2.Count())
      return(false);
   if(GetPointer(comparer)==set_test1.Comparer())
      return(false);
   if(GetPointer(comparer)!=set_test2.Comparer())
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestMisc_TrimExpress.                                            |
//+------------------------------------------------------------------+
bool TestMisc_TrimExpress(const int count)
  {
//--- create array 
   int array[];
   ArrayResize(array,count);
   for(int i=0; i<count; i++)
      array[i]=MathRand();
//--- create source set
   CHashSet<int>set_test();
   for(int i=0; i<count; i++)
      set_test.Add(array[i]);
//--- copy set to array
   int expected[];
   set_test.CopyTo(expected);
//--- trim
   set_test.TrimExcess();
   set_test.TrimExcess();
   set_test.TrimExcess();
//--- copy set to array
   int actual[];
   set_test.CopyTo(actual);
//--- check
   if(ArrayCompare(actual,expected)!=0)
      return(false);
//--- get first element
   int elemnet=actual[0];
//--- trim
   set_test.TrimExcess();
//--- remove
   if(!set_test.Remove(elemnet))
      return(false);
//--- trim
   set_test.TrimExcess();
//--- check
   ArrayFree(actual);
   set_test.CopyTo(actual);
   for(int i=0; i<count-1;i++)
      if(expected[i+1]!=actual[i])
         return(false);
//--- trim and clear
   set_test.TrimExcess();
   set_test.Clear();
   set_test.TrimExcess();
//--- check
   if(set_test.Count()!=0)
      return(false);
//--- add values
   for(int i=0; i<count; i++)
      set_test.Add(array[i]);
//--- trim
   set_test.TrimExcess();
//--- check
   if(set_test.Count()!=ArraySize(expected))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestMisc.                                                        |
//+------------------------------------------------------------------+
bool TestMisc(const string test_name)
  {
   PrintFormat("%s started",test_name);
//--- test 1
   PrintFormat("%s: Test 1: Testing Constructor of set based on array with and without duplicates.",test_name);
   if(!TestMisc_Constructor(16))
      return(false);
//--- test 2
   PrintFormat("%s: Test 2: Complex validation of TrimExpress method after adding, removing element and cleaning.",test_name);
   if(!TestMisc_TrimExpress(16))
      return(false);
//--- successful
   PrintFormat("%s passed",test_name);
   return(true);
  }
//+------------------------------------------------------------------+
//| TestHashSet.                                                     |
//+------------------------------------------------------------------+
void TestHashSet(int &tests_performed,int &tests_passed)
  {
   string test_name="";
//--- Misc functions
   tests_performed++;
   test_name="Misc functions test";
   if(TestMisc(test_name))
      tests_passed++;
   else
      PrintFormat("%s failed",test_name);
  }
//+------------------------------------------------------------------+
//| Script program start function                                    |
//+------------------------------------------------------------------+
void OnStart()
  {
   MathSrand(0);
   string package_name="Generic";
   PrintFormat("Unit tests for Package %s\n",package_name);
//--- initial values
   int tests_performed=0;
   int tests_passed=0;
//--- test distributions
   TestHashSet(tests_performed,tests_passed);
//--- print statistics
   PrintFormat("\n%d of %d passed",tests_passed,tests_performed);
  }
//+------------------------------------------------------------------+
