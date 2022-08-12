//+------------------------------------------------------------------+
//|                                                TestSortedMap.mq5 |
//|                        Copyright 2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <Generic\ArrayList.mqh>
#include <Generic\SortedMap.mqh>
//+------------------------------------------------------------------+
//| TestMisc_Constructor.                                            |
//+------------------------------------------------------------------+
bool TestMisc_Constructor(const int count)
  {
//--- create arrays
   int keys[];
   string values[];
   ArrayResize(keys,count);
   ArrayResize(values,count);
   for(int i=0; i<count; i++)
     {
      keys[i]=i+1;
      values[i]=(string)(i+1);
     }
//--- create source map
   CDefaultComparer<int>comparer();
   CSortedMap<int,string>map_test(GetPointer(comparer));
   for(int i=0; i<count; i++)
      map_test.Add(keys[i],values[i]);
//--- create map on map
   CSortedMap<int,string>map_copy(GetPointer(map_test));
//--- check
   if(map_copy.Count()!=map_test.Count())
      return(false);
   if(map_test.Comparer()!=GetPointer(comparer))
      return(false);
   if(map_copy.Comparer()==GetPointer(comparer))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestMisc_Contains.                                               |
//+------------------------------------------------------------------+
bool TestMisc_Contains(const int count)
  {
//--- create arrays
   int keys[];
   string values[];
   ArrayResize(keys,count);
   ArrayResize(values,count);
   for(int i=0; i<count; i++)
     {
      keys[i]=i+1;
      values[i]=(string)(i+1);
     }
//--- create source map
   CSortedMap<int,string>map_test();
   for(int i=0; i<count; i++)
      map_test.Add(keys[i],values[i]);
//--- create elemet 
   int element=keys[0];
   while(map_test.Contains(element,(string)element) && map_test.ContainsKey(element))
      element++;
//--- check
   if(map_test.Contains(element,(string)element) || map_test.ContainsKey(element))
      return(false);
//--- add element to map
   map_test.Add(element,"new");
//--- check
   if(!map_test.Contains(element,"new") ||
      !map_test.ContainsKey(element) ||
      !map_test.ContainsValue("new"))
      return(false);
//--- clear
   map_test.Clear();
//--- check
   if(map_test.Contains(keys[0],values[0]) ||
      map_test.ContainsKey(keys[0]) ||
      map_test.ContainsValue(values[0]))
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestMisc_Ordering.                                               |
//+------------------------------------------------------------------+
bool TestMisc_Ordering(const int count)
  {
//--- create arrays
   int keys[];
   string values[];
   ArrayResize(keys,count);
   ArrayResize(values,count);
   for(int i=0; i<count; i++)
     {
      keys[i]=i+1;
      values[i]=(string)(i+1);
     }
//--- create source map
   CSortedMap<int,string>map_test();
   for(int i=0; i<count; i++)
      map_test.Add(keys[i],values[i]);
//--- copy map to array
   CKeyValuePair<int,string>*pairs[];
   int size=map_test.CopyTo(pairs);
//--- check
   CDefaultComparer<CKeyValuePair<int,string>*>comparer();
   CArrayList<CKeyValuePair<int,string>*>expected(pairs);
   expected.Sort(GetPointer(comparer));
   int actual_keys[];
   string actual_values[];
   map_test.CopyTo(actual_keys,actual_values);
   for(int i=0; i<size; i++)
     {
      CKeyValuePair<int,string>*pair;
      expected.TryGetValue(i,pair);
      if(pair.Key()!=actual_keys[i] || pair.Value()!=actual_values[i])
         return(false);
      //--- check TryGetValue         
      string value;
      if(!map_test.TryGetValue(pair.Key(),value))
         return(false);
      if(pair.Value()!=value)
         return(false);
     }
//--- delete pairs
   for(int i=0; i<ArraySize(pairs); i++)
     {
      CKeyValuePair<int,string>*pair;
      expected.TryGetValue(i,pair);
      delete pair;
     }
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
   PrintFormat("%s: Test 1: Testing Constructor of map based on another map.",test_name);
   if(!TestMisc_Constructor(16))
      return(false);
//--- test 2
   PrintFormat("%s: Test 2: Complex validation of Contains, ContainsKey and ContainsValues methods.",test_name);
   if(!TestMisc_Contains(16))
      return(false);
//--- test 3
   PrintFormat("%s: Test 3: Testing the ordering of elements in the map.",test_name);
   if(!TestMisc_Ordering(16))
      return(false);
//--- successful
   PrintFormat("%s passed",test_name);
   return(true);
  }
//+------------------------------------------------------------------+
//| TestSortedMap.                                                   |
//+------------------------------------------------------------------+
void TestSortedMap(int &tests_performed,int &tests_passed)
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
   TestSortedMap(tests_performed,tests_passed);
//--- print statistics
   PrintFormat("\n%d of %d passed",tests_passed,tests_performed);
  }
//+------------------------------------------------------------------+
