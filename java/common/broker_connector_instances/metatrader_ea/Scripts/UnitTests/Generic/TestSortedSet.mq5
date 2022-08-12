//+------------------------------------------------------------------+
//|                                                TestSortedSet.mq5 |
//|                        Copyright 2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <Generic\ArrayList.mqh>
#include <Generic\SortedSet.mqh>
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
   CDefaultComparer<int>comparer();
   CSortedSet<int>set_test1(array);
   CSortedSet<int>set_test2(array_duplicates,GetPointer(comparer));
//--- check
   if(set_test1.Count()!=set_test2.Count())
      return(false);
   if(GetPointer(comparer)==set_test1.Comparer())
      return(false);
   if(GetPointer(comparer)!=set_test2.Comparer())
      return(false);
//--- check ordering of the first set
   int check_array[];
   set_test1.CopyTo(check_array);
   for(int i=0; i<ArraySize(check_array)-1; i++)
      if(comparer.Compare(check_array[i],check_array[i+1])>0 && check_array[i]>check_array[i+1])
         return(false);
//--- check ordering of the second set
   ArrayFree(check_array);
   set_test2.CopyTo(check_array);
   for(int i=0; i<ArraySize(check_array)-1; i++)
      if(comparer.Compare(check_array[i],check_array[i+1])>0 && check_array[i]>check_array[i+1])
         return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestMisc_GetViewBetween.                                         |
//+------------------------------------------------------------------+
bool TestMisc_GetViewBetween(const int count)
  {
   int view[];
   int check_array[];
//--- create array 
   int array[];
   ArrayResize(array,count);
   for(int i=0; i<count; i++)
      array[i]=MathRand();
//--- create source set
   CSortedSet<int>set_test(array);
//--- calculate max and min
   int min = array[ArrayMinimum(array)];
   int max = array[ArrayMaximum(array)];
   if(!set_test.GetViewBetween(view,min,max))
      return(false);
//--- get view
   set_test.GetViewBetween(view,min,max);
//--- check 
   set_test.CopyTo(check_array);
   if(ArrayCompare(check_array,view)!=0)
      return(false);
//--- lower value greater than upper value
   min=array[ArrayMaximum(array)];
   max=array[ArrayMinimum(array)];
//--- get view
   ArrayFree(view);
   if(set_test.GetViewBetween(view,min,max))
      return(false);
//--- check
   if(ArraySize(view)!=0)
      return(false);
//--- minimum lower value and maximum upper value    
   min=INT_MIN;
   max=INT_MAX;
//--- get view
   ArrayFree(view);
   if(!set_test.GetViewBetween(view,min,max))
      return(false);
//--- check
   if(ArrayCompare(check_array,view)!=0)
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
   PrintFormat("%s: Test 1: Testing Constructor of set based of array with and without duplicates.",test_name);
   if(!TestMisc_Constructor(16))
      return(false);
//--- test 2
   PrintFormat("%s: Test 2: Complex validation of GetViewBetween method with correct and incorrect input parameters.",test_name);
   if(!TestMisc_GetViewBetween(16))
      return(false);
//--- successful
   PrintFormat("%s passed",test_name);
   return(true);
  }
//+------------------------------------------------------------------+
//| TestSortedSet.                                                   |
//+------------------------------------------------------------------+
void TestSortedSet(int &tests_performed,int &tests_passed)
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
   TestSortedSet(tests_performed,tests_passed);
//--- print statistics
   PrintFormat("\n%d of %d passed",tests_passed,tests_performed);
  }
//+------------------------------------------------------------------+
