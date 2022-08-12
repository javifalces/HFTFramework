//+------------------------------------------------------------------+
//|                                                    TestStack.mq5 |
//|                        Copyright 2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <Generic\Stack.mqh>
#include <Generic\ArrayList.mqh>
//+------------------------------------------------------------------+
//| TestConstructor_Valid.                                           |
//+------------------------------------------------------------------+
bool TestConstructor_Valid(const int count)
  {
//--- create random array
   int array[];
   ArrayResize(array,count);
   for(int i=0; i<count; i++)
      array[i]=MathRand();
//--- create list
   CArrayList<int>list(array);
//--- create source stack
   CStack<int>stack_test1(array);
   CStack<int>stack_test2(GetPointer(list));
   CStack<int>stack_test3(count);
   for(int i=0; i<count; i++)
      stack_test3.Add(array[i]);
//--- check count
   if(stack_test1.Count()!=count)
      return(false);
   if(stack_test2.Count()!=count)
      return(false);
   if(stack_test3.Count()!=count)
      return(false);
//--- check elements
   for(int i=0; i<count; i++)
     {
      if(array[count-1-i]!=stack_test1.Pop())
         return(false);
      if(array[count-1-i]!=stack_test2.Pop())
         return(false);
      if(array[count-1-i]!=stack_test3.Pop())
         return(false);
     }
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestConstructor_Invalid.                                         |
//+------------------------------------------------------------------+
bool TestConstructor_Invalid(const int count)
  {
//--- create source stack
   CStack<int>stack_test1(-1);
   CStack<int>stack_test2(INT_MIN);
//--- check
   if(stack_test1.Count()!=0)
      return(false);
   if(stack_test2.Count()!=0)
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestConstructor.                                                 |
//+------------------------------------------------------------------+
bool TestConstructor(const string test_name)
  {
   PrintFormat("%s started",test_name);
//--- test 1
   PrintFormat("%s: Test 1: Testing Constructor of stack with array, ICollection object and correct capacity.",test_name);
   if(!TestConstructor_Valid(10))
      return(false);
//--- test 2
   PrintFormat("%s: Test 2: Testing Constructor of stack with incorrect capacity.",test_name);
   if(!TestConstructor_Invalid(10))
      return(false);
//--- successful
   PrintFormat("%s passed",test_name);
   return(true);
  }
//+------------------------------------------------------------------+
//| TestMisc_Pop.                                                    |
//+------------------------------------------------------------------+
bool TestMisc_Pop(const int count)
  {
//--- create random array
   int array[];
   ArrayResize(array,count);
   for(int i=0; i<count; i++)
      array[i]=MathRand();
//--- create source stack
   CStack<int>stack_test(array);
//--- check elements
   for(int i=0; i<count; i++)
      if(stack_test.Pop()!=array[count-1-i])
         return(false);
//--- check count
   if(stack_test.Count()!=0)
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestMisc_Peek.                                                   |
//+------------------------------------------------------------------+
bool TestMisc_Peek(const int count)
  {
//--- create random array
   int array[];
   ArrayResize(array,count);
   for(int i=0; i<count; i++)
      array[i]=MathRand();
//--- create source stack
   CStack<int>stack_test(array);
//--- check element
   if(stack_test.Peek()!=array[count-1])
      return(false);
//--- check count
   if(stack_test.Count()!=count)
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestMisc_Complex.                                                |
//+------------------------------------------------------------------+
bool TestMisc_Complex(const int count)
  {
//--- create random array
   int array[];
   ArrayResize(array,count);
   for(int i=0; i<count; i++)
      array[i]=MathRand();
//--- create source stack
   CStack<int>stack_test(count);
//--- push values
   for(int i=0; i<count; i++)
      if(!stack_test.Push(array[i]))
         return(false);
//--- check count
   if(stack_test.Count()!=count)
      return(false);
//--- check elements
   for(int i=0; i<count; i++)
     {
      if(array[count-1-i]!=stack_test.Pop())
         return(false);
      if(count-i-1!=stack_test.Count())
         return(false);
     }
//--- recheck
   stack_test.Push(0);
   if(stack_test.Pop()!=0)
      return(false);
//--- successful
   return(true);
  }
//+------------------------------------------------------------------+
//| TestMisc_TrimExcess.                                             |
//+------------------------------------------------------------------+
bool TestMisc_TrimExcess(const int count)
  {
//--- create random array
   int array[];
   ArrayResize(array,count);
   for(int i=0; i<count; i++)
      array[i]=MathRand();
//--- create source stack
   CStack<int>stack_test(array);
//--- trim
   stack_test.TrimExcess();
   int removed=stack_test.Pop();
   stack_test.TrimExcess();
//--- check element
   if(stack_test.Peek()!=array[count-2])
      return(false);
//--- check count
   if(stack_test.Count()!=count-1)
      return(false);
//--- trim and clear
   stack_test.TrimExcess();
   stack_test.Clear();
   stack_test.TrimExcess();
//--- check count
   if(stack_test.Count()!=0)
      return(false);
//--- add elemnt and trim
   stack_test.Add(0);
   stack_test.TrimExcess();
//--- check count
   if(stack_test.Count()!=1)
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
   PrintFormat("%s: Test 1: Validation of Pop method for all element of the stack and check count after.",test_name);
   if(!TestMisc_Pop(10))
      return(false);
//--- test 2
   PrintFormat("%s: Test 2: Validation of Peek method for all element of the stack and check count after.",test_name);
   if(!TestMisc_Peek(10))
      return(false);
//--- test 3
   PrintFormat("%s: Test 3: Complex validation of Push and Pop methods.",test_name);
   if(!TestMisc_Complex(10))
      return(false);
//--- test 4
   PrintFormat("%s: Test 4: Testing TrimExcess method on the stack after filling, clearing and adding elements.",test_name);
   if(!TestMisc_TrimExcess(10))
      return(false);
//--- successful
   PrintFormat("%s passed",test_name);
   return(true);
  }
//+------------------------------------------------------------------+
//| TestStack.                                                       |
//+------------------------------------------------------------------+
void TestStack(int &tests_performed,int &tests_passed)
  {
   string test_name="";
//--- Constructor functions
   tests_performed++;
   test_name="TestConstructor functions test";
   if(TestConstructor(test_name))
      tests_passed++;
   else
      PrintFormat("%s failed",test_name);

//--- AddRange functions
   tests_performed++;
   test_name="TestMisc functions test";
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
   TestStack(tests_performed,tests_passed);
//--- print statistics
   PrintFormat("\n%d of %d passed",tests_passed,tests_performed);
  }
//+------------------------------------------------------------------+
