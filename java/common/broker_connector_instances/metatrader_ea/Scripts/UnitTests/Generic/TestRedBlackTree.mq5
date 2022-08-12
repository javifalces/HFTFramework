//+------------------------------------------------------------------+
//|                                             TestRedBlackTree.mq5 |
//|                        Copyright 2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <Generic\RedBlackTree.mqh>
#include <Generic\ArrayList.mqh>
#include <Generic\HashSet.mqh>
//+------------------------------------------------------------------+
//| TestMisc_Constructor.                                            |
//+------------------------------------------------------------------+
bool TestMisc_Constructor(const int count)
  {
//--- create list
   CArrayList<int>list(count);
   for(int i=0; i<count; i++)
      list.Add(MathRand());
//--- create source tree
   CDefaultComparer<int>comparer();
   CRedBlackTree<int>tree_test1(GetPointer(list),GetPointer(comparer));
   CRedBlackTree<int>tree_test2(GetPointer(tree_test1));
//--- check 
   if(tree_test1.Count()!=tree_test2.Count())
      return(false);
   if(tree_test1.Comparer()!=GetPointer(comparer))
      return(false);
   if(tree_test2.Comparer()==GetPointer(comparer))
      return(false);
//--- get unique sorted values
   CHashSet<int>set(GetPointer(list));
   int expected[];
   set.CopyTo(expected);
   ArraySort(expected);
//--- check
   int actual1[];
   int actual2[];
   tree_test1.CopyTo(actual1);
   tree_test1.CopyTo(actual2);
   for(int i=0; i<ArraySize(expected); i++)
      if(actual1[i]!=expected[i] || actual2[i]!=expected[i])
         return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestMisc_Contains.                                               |
//+------------------------------------------------------------------+
bool TestMisc_Contains(const int count)
  {
//--- create list
   CArrayList<int>list(count);
   for(int i=0; i<count; i++)
      list.Add(MathRand());
//--- create source tree
   CRedBlackTree<int>tree_test(GetPointer(list));
//--- check
   for(int i=0; i<count; i++)
     {
      int value;
      list.TryGetValue(i,value);
      if(!tree_test.Contains(value))
         return(false);
     }
//--- clear tree
   tree_test.Clear();
//--- check
   for(int i=0; i<count; i++)
     {
      int value;
      list.TryGetValue(i,value);
      if(tree_test.Contains(value))
         return(false);
     }
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestMisc_Add.                                                    |
//+------------------------------------------------------------------+
bool TestMisc_Add(const int count)
  {
//--- create list
   CArrayList<int>list(count);
   for(int i=0; i<count; i++)
      list.Add(MathRand());
//--- create source tree
   CRedBlackTree<int>tree_test(GetPointer(list));
//--- check
   if(tree_test.Count()>count)
      return(false);
//--- create new unique element
   int element=MathRand();
   while(tree_test.Contains(element))
      element=MathRand();
//--- add new unique element
   if(!tree_test.Add(element))
      return(false);
   if(tree_test.Add(element))
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
   PrintFormat("%s: Test 1: Testing Constructor of map based on several different ICollection objects with and without comparer.",test_name);
   if(!TestMisc_Constructor(16))
      return(false);
//--- test 2
   PrintFormat("%s: Test 2: Validation of Contains method before and after cleaning the tree.",test_name);
   if(!TestMisc_Contains(16))
      return(false);
//--- test 3
   PrintFormat("%s: Test 3: Simple validation of Add method.",test_name);
   if(!TestMisc_Add(16))
      return(false);
//--- successful
   PrintFormat("%s passed",test_name);
   return(true);
  }
//+------------------------------------------------------------------+
//| TestFind_Max.                                                    |
//+------------------------------------------------------------------+
bool TestFind_Max(const int count)
  {
//--- create list
   CArrayList<int>list(count);
   for(int i=0; i<count; i++)
      list.Add(MathRand());
//--- create source tree
   CRedBlackTree<int>tree_test(GetPointer(list));
//--- get max
   list.Sort();
   CRedBlackTreeNode<int>*max_node=tree_test.FindMax();
   int expected;
   list.TryGetValue(count-1,expected);
   int actual=max_node.Value();
//--- check
   if(expected!=actual)
      return(false);
   if(tree_test.Find(expected)!=max_node)
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestFind_Min.                                                    |
//+------------------------------------------------------------------+
bool TestFind_Min(const int count)
  {
//--- create list
   CArrayList<int>list(count);
   for(int i=0; i<count; i++)
      list.Add(MathRand());
//--- create source tree
   CRedBlackTree<int>tree_test(GetPointer(list));
//--- get max
   list.Sort();
   CRedBlackTreeNode<int>*min_node=tree_test.FindMin();
   int expected;
   list.TryGetValue(0, expected);
   int actual=min_node.Value();
//--- check
   if(expected!=actual)
      return(false);
   if(tree_test.Find(expected)!=min_node)
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestFind.                                                        |
//+------------------------------------------------------------------+
bool TestFind(const string test_name)
  {
   PrintFormat("%s started",test_name);
//--- test 1
   PrintFormat("%s: Test 1: Simple validation of FindMax method for tree.",test_name);
   if(!TestFind_Max(16))
      return(false);
//--- test 2
   PrintFormat("%s: Test 2: Simple validation of FindMin method for tree.",test_name);
   if(!TestFind_Min(16))
      return(false);
//--- successful
   PrintFormat("%s passed",test_name);
   return(true);
  }
//+------------------------------------------------------------------+
//| TestRemove_Node.                                                 |
//+------------------------------------------------------------------+
bool TestRemove_Node(const int count)
  {
//--- create list
   CArrayList<int>list(count);
   for(int i=0; i<count; i++)
      list.Add(MathRand());
//--- create source tree
   CRedBlackTree<int>tree_test(GetPointer(list));
//--- get max and min
   list.Sort();
   int min;
   int max;
   list.TryGetValue(0, min);
   list.TryGetValue(count-1, max);
//--- check
   if(tree_test.Count()>0)
     {
      //--- remove min value
      if(!tree_test.Remove(min))
         return(false);
     }
   if(tree_test.Count()>0)
     {
      //--- remove max node
      CRedBlackTreeNode<int>*node=tree_test.FindMax();
      if(!tree_test.Remove(node))
         return(false);
     }
//--- get unique sorted values
   CHashSet<int>set(GetPointer(list));
   set.Remove(min);
   set.Remove(max);
   int expected[];
   set.CopyTo(expected);
   ArraySort(expected);
//--- check
   int actual[];
   tree_test.CopyTo(actual);
   if(ArrayCompare(expected,actual)!=0)
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestRemove_Max.                                                  |
//+------------------------------------------------------------------+
bool TestRemove_Max(const int count)
  {
//--- create list
   CArrayList<int>list(count);
   for(int i=0; i<count; i++)
      list.Add(MathRand());
//--- create source tree
   CRedBlackTree<int>tree_test(GetPointer(list));
//--- get unique sorted values
   CHashSet<int>set(GetPointer(list));
   int expected[];
   set.CopyTo(expected);
   ArraySort(expected);
//--- check
   for(int i=0; i<ArraySize(expected); i++)
     {
      int actual[];
      tree_test.CopyTo(actual);
      if(ArrayCompare(expected,actual,0,0,count-i)!=0)
         return(false);
      tree_test.RemoveMax();
     }
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestRemove_Min.                                                  |
//+------------------------------------------------------------------+
bool TestRemove_Min(const int count)
  {
//--- create list
   CArrayList<int>list(count);
   for(int i=0; i<count; i++)
      list.Add(MathRand());
//--- create source tree
   CRedBlackTree<int>tree_test(GetPointer(list));
//--- get unique sorted values
   CHashSet<int>set(GetPointer(list));
   int expected[];
   set.CopyTo(expected);
   ArraySort(expected);
//--- check
   for(int i=0; i<ArraySize(expected); i++)
     {
      int actual[];
      tree_test.CopyTo(actual);
      if(ArrayCompare(expected,actual,i,0,count-i)!=0)
         return(false);
      tree_test.RemoveMin();
     }
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestRemove.                                                      |
//+------------------------------------------------------------------+
bool TestRemove(const string test_name)
  {
   PrintFormat("%s started",test_name);
//--- test 1
   PrintFormat("%s: Test 2: Validation of Remove method used independent element and specified node of tree.",test_name);
   if(!TestRemove_Node(16))
      return(false);
//--- test 2
   PrintFormat("%s: Test 2: Validation of RemoveMax method for cleaning the tree.",test_name);
   if(!TestRemove_Max(16))
      return(false);
//--- test 3
   PrintFormat("%s: Test 3: Validation of RemoveMin method for cleaning the tree.",test_name);
   if(!TestRemove_Min(16))
      return(false);
//--- successful
   PrintFormat("%s passed",test_name);
   return(true);
  }
//+------------------------------------------------------------------+
//| TestRedBlackTree.                                                |
//+------------------------------------------------------------------+
void TestRedBlackTree(int &tests_performed,int &tests_passed)
  {
   string test_name="";
//--- Misc functions
   tests_performed++;
   test_name="Misc functions test";
   if(TestMisc(test_name))
      tests_passed++;
   else
      PrintFormat("%s failed",test_name);
//--- Find functions
   tests_performed++;
   test_name="Find functions test";
   if(TestFind(test_name))
      tests_passed++;
   else
      PrintFormat("%s failed",test_name);
//--- Remove functions
   tests_performed++;
   test_name="Remove functions test";
   if(TestRemove(test_name))
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
   TestRedBlackTree(tests_performed,tests_passed);
//--- print statistics
   PrintFormat("\n%d of %d passed",tests_passed,tests_performed);
  }
//+------------------------------------------------------------------+
