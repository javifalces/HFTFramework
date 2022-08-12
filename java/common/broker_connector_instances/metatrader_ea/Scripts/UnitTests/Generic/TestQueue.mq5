//+------------------------------------------------------------------+
//|                                                    TestQueue.mq5 |
//|                        Copyright 2017, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <Generic\Queue.mqh>
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
//--- create source queue
   CQueue<int>queue_test1(array);
   CQueue<int>queue_test2(GetPointer(list));
   CQueue<int>queue_test3(count);
   for(int i=0; i<count; i++)
      queue_test3.Add(array[i]);
//--- check count
   if(queue_test1.Count()!=count)
      return(false);
   if(queue_test2.Count()!=count)
      return(false);
   if(queue_test3.Count()!=count)
      return(false);
//--- check elements
   for(int i=0; i<count; i++)
     {
      if(array[i]!=queue_test1.Dequeue())
         return(false);
      if(array[i]!=queue_test2.Dequeue())
         return(false);
      if(array[i]!=queue_test3.Dequeue())
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
//--- create source queue
   CQueue<int>queue_test1(-1);
   CQueue<int>queue_test2(INT_MIN);
//--- check
   if(queue_test1.Count()!=0)
      return(false);
   if(queue_test2.Count()!=0)
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
   PrintFormat("%s: Test 1: Testing Constructor of queue with array, ICollection object and correct capacity.",test_name);
   if(!TestConstructor_Valid(10))
      return(false);
//--- test 2
   PrintFormat("%s: Test 2: Testing Constructor of queue with incorrect capacity.",test_name);
   if(!TestConstructor_Invalid(10))
      return(false);
//--- successful
   PrintFormat("%s passed",test_name);
   return(true);
  }
//+------------------------------------------------------------------+
//| TestMisc_Dequeue.                                                |
//+------------------------------------------------------------------+
bool TestMisc_Dequeue(const int count)
  {
//--- create random array
   int array[];
   ArrayResize(array,count);
   for(int i=0; i<count; i++)
      array[i]=MathRand();
//--- create source queue
   CQueue<int>queue_test(array);
//--- check elements
   for(int i=0; i<count; i++)
      if(queue_test.Dequeue()!=array[i])
         return(false);
//--- check count
   if(queue_test.Count()!=0)
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
//--- create source queue
   CQueue<int>queue_test(array);
//--- check element
   if(queue_test.Peek()!=array[0])
      return(false);
//--- check count
   if(queue_test.Count()!=count)
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
//--- create source queue
   CQueue<int>queue_test(count);
//--- enqueue values
   for(int i=0; i<count; i++)
      if(!queue_test.Enqueue(array[i]))
         return(false);
//--- check count
   if(queue_test.Count()!=count)
      return(false);
//--- check elements
   for(int i=0; i<count; i++)
     {
      if(array[i]!=queue_test.Dequeue())
         return(false);
      if(count-i-1!=queue_test.Count())
         return(false);
     }
//--- recheck
   queue_test.Enqueue(0);
   if(queue_test.Dequeue()!=0)
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
//--- create source queue
   CQueue<int>queue_test(array);
//--- trim
   queue_test.TrimExcess();
   int removed=queue_test.Dequeue();
   queue_test.TrimExcess();
//--- check element
   if(queue_test.Peek()!=array[1])
      return(false);
//--- check count
   if(queue_test.Count()!=count-1)
      return(false);
//--- trim and clear
   queue_test.TrimExcess();
   queue_test.Clear();
   queue_test.TrimExcess();
//--- check count
   if(queue_test.Count()!=0)
      return(false);
//--- add elemnt and trim
   queue_test.Add(0);
   queue_test.TrimExcess();
//--- check count
   if(queue_test.Count()!=1)
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
   PrintFormat("%s: Test 1: Validation of Dequeue method for all element of the queue and check count after.",test_name);
   if(!TestMisc_Dequeue(10))
      return(false);
//--- test 2
   PrintFormat("%s: Test 2: Validation of Peek method for all element of the queue and check count after.",test_name);
   if(!TestMisc_Peek(10))
      return(false);
//--- test 3
   PrintFormat("%s: Test 3: Complex validation of Enqueue and Dequeue methods.",test_name);
   if(!TestMisc_Complex(10))
      return(false);
//--- test 4
   PrintFormat("%s: Test 4: Testing TrimExcess method on the queue after filling, clearing and adding elements.",test_name);
   if(!TestMisc_TrimExcess(10))
      return(false);
//--- successful
   PrintFormat("%s passed",test_name);
   return(true);
  }
//+------------------------------------------------------------------+
//| TestQueue.                                                       |
//+------------------------------------------------------------------+
void TestQueue(int &tests_performed,int &tests_passed)
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
   TestQueue(tests_performed,tests_passed);
//--- print statistics
   PrintFormat("\n%d of %d passed",tests_passed,tests_performed);
  }
//+------------------------------------------------------------------+
