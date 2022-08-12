//+------------------------------------------------------------------+
//|                                                     FilePipe.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include "File.mqh"
//+------------------------------------------------------------------+
//| Class CFilePipe                                                  |
//| Purpose: Class of operations with binary files                   |
//|          Derives from class CFile                                |
//+------------------------------------------------------------------+
class CFilePipe : public CFile
  {
public:
                     CFilePipe(void);
                    ~CFilePipe(void);
   //--- methods for working with files
   int               Open(const string file_name,const int open_flags);
   //--- wait for incoming data
   bool              WaitForRead(const ulong size);
   //--- methods for writing data
   template<typename T>
   uint              WriteInteger(const T value);
   uint              WriteLong(const long value);
   uint              WriteFloat(const float value);
   uint              WriteDouble(const double value);
   uint              WriteString(const string value);
   uint              WriteString(const string value,const int size);
   template<typename T>
   uint              WriteArray(T &array[],const int start_item=0,const int items_count=WHOLE_ARRAY);
   template<typename T>
   uint              WriteStruct(T &data);
   bool              WriteObject(CObject *object);
   template<typename T>
   uint              WriteEnum(const T value) { return(WriteInteger((int)value)); }
   //--- methods for reading data
   template<typename T>
   bool              ReadInteger(T &value);
   bool              ReadLong(long &value);
   bool              ReadFloat(float &value);
   bool              ReadDouble(double &value);
   bool              ReadString(string &value);
   bool              ReadString(string &value,const int size);
   template<typename T>
   uint              ReadArray(T &array[],const int start_item=0,const int items_count=WHOLE_ARRAY);
   template<typename T>
   uint              ReadStruct(T &data);
   bool              ReadObject(CObject *object);
   template<typename T>
   bool              ReadEnum(T &value);
  };
//+------------------------------------------------------------------+
//| Constructor                                                      |
//+------------------------------------------------------------------+
CFilePipe::CFilePipe(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CFilePipe::~CFilePipe(void)
  {
  }
//+------------------------------------------------------------------+
//| Opening a binary file                                            |
//+------------------------------------------------------------------+
int CFilePipe::Open(const string file_name,const int open_flags)
  {
   return(CFile::Open(file_name,open_flags|FILE_BIN));
  }
//+------------------------------------------------------------------+
//| Wait for incoming data                                           |
//+------------------------------------------------------------------+
bool CFilePipe::WaitForRead(const ulong size)
  {
//--- check handle and stop flag
   while(m_handle!=INVALID_HANDLE && !IsStopped())
     {
      //--- enought data?
      if(FileSize(m_handle)>=size)
         return(true);
      //--- wait a little
      Sleep(1);
     }
//--- failure
   return(false);
  }
//+------------------------------------------------------------------+
//| Write a variable of integer types                                |
//+------------------------------------------------------------------+
template<typename T>
uint CFilePipe::WriteInteger(const T value)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileWriteInteger(m_handle,value,sizeof(T)));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Write a variable of long or ulong type                           |
//+------------------------------------------------------------------+
uint CFilePipe::WriteLong(const long value)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileWriteLong(m_handle,value));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Write a variable of float type                                   |
//+------------------------------------------------------------------+
uint CFilePipe::WriteFloat(const float value)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileWriteFloat(m_handle,value));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Write a variable of double type                                  |
//+------------------------------------------------------------------+
uint CFilePipe::WriteDouble(const double value)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileWriteDouble(m_handle,value));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Write a variable of string type                                  |
//+------------------------------------------------------------------+
uint CFilePipe::WriteString(const string value)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
     {
      //--- size of string
      int size=StringLen(value);
      //--- write
      if(FileWriteInteger(m_handle,size)==sizeof(int))
         return(FileWriteString(m_handle,value,size));
     }
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Write a part of string                                           |
//+------------------------------------------------------------------+
uint CFilePipe::WriteString(const string value,const int size)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileWriteString(m_handle,value,size));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Write an array of variables of any type                          |
//+------------------------------------------------------------------+
template<typename T>
uint CFilePipe::WriteArray(T &array[],const int start_item=0,const int items_count=WHOLE_ARRAY)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileWriteArray(m_handle,array,start_item,items_count));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Write an structure                                               |
//+------------------------------------------------------------------+
template<typename T>
uint CFilePipe::WriteStruct(T &data)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileWriteStruct(m_handle,data));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Write data of an instance of the CObject class                   |
//+------------------------------------------------------------------+
bool CFilePipe::WriteObject(CObject *object)
  {
//--- check handle & object
   if(m_handle!=INVALID_HANDLE)
      if(CheckPointer(object))
         return(object.Save(m_handle));
//--- failure
   return(false);
  }
//+------------------------------------------------------------------+
//| Read a variable of integer types                                 |
//+------------------------------------------------------------------+
template<typename T>
bool CFilePipe::ReadInteger(T &value)
  {
//--- check for data
   if(WaitForRead(sizeof(T)))
     {
      ResetLastError();
      value=FileReadInteger(m_handle,sizeof(T));
      return(GetLastError()==0);
     }
//--- failure
   return(false);
  }
//+------------------------------------------------------------------+
//| Read a variable of long or ulong type                            |
//+------------------------------------------------------------------+
bool CFilePipe::ReadLong(long &value)
  {
//--- check handle
   if(WaitForRead(sizeof(long)))
     {
      ResetLastError();
      value=FileReadLong(m_handle);
      return(GetLastError()==0);
     }
//--- failure
   return(false);
  }
//+------------------------------------------------------------------+
//| Read a variable of float type                                    |
//+------------------------------------------------------------------+
bool CFilePipe::ReadFloat(float &value)
  {
//--- check for data
   if(WaitForRead(sizeof(float)))
     {
      ResetLastError();
      value=FileReadFloat(m_handle);
      return(GetLastError()==0);
     }
//--- failure
   return(false);
  }
//+------------------------------------------------------------------+
//| Read a variable of double type                                   |
//+------------------------------------------------------------------+
bool CFilePipe::ReadDouble(double &value)
  {
//--- check for data
   if(WaitForRead(sizeof(double)))
     {
      ResetLastError();
      value=FileReadDouble(m_handle);
      return(GetLastError()==0);
     }
//--- failure
   return(false);
  }
//+------------------------------------------------------------------+
//| Read a variable of string type                                   |
//+------------------------------------------------------------------+
bool CFilePipe::ReadString(string &value)
  {
//--- check for data
   if(WaitForRead(sizeof(int)))
     {
      ResetLastError();
      int size=FileReadInteger(m_handle);
      if(GetLastError()==0)
        {
         //--- check for data
         if(WaitForRead(size))
           {
            value=FileReadString(m_handle,size);
            return(size==StringLen(value));
           }
        }
     }
//--- failure
   return(false);
  }
//+------------------------------------------------------------------+
//| Read a part of string                                            |
//+------------------------------------------------------------------+
bool CFilePipe::ReadString(string &value,const int size)
  {
//--- check for data
   if(WaitForRead(size))
     {
      value=FileReadString(m_handle,size);
      return(size==StringLen(value));
     }
//--- failure
   return(false);
  }
//+------------------------------------------------------------------+
//| Read an array of variables of any type                           |
//+------------------------------------------------------------------+
template<typename T>
uint CFilePipe::ReadArray(T &array[],const int start_item=0,const int items_count=WHOLE_ARRAY)
  {
//--- calculate size
   uint size=ArraySize(array);
   if(items_count!=WHOLE_ARRAY) size=items_count;
//--- check for data
   if(WaitForRead(size*sizeof(T)))
      return(FileReadArray(m_handle,array,start_item,items_count));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Read an structure                                                |
//+------------------------------------------------------------------+
template<typename T>
uint CFilePipe::ReadStruct(T &data)
  {
//--- check for data
   if(WaitForRead(sizeof(T)))
      return(FileReadStruct(m_handle,data));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Read data of an instance of the CObject class                    |
//+------------------------------------------------------------------+
bool CFilePipe::ReadObject(CObject *object)
  {
//--- check for object & data
   if(CheckPointer(object))
      if(WaitForRead(sizeof(int))) // only 4 bytes!
         return(object.Load(m_handle));
//--- failure
   return(false);
  }
//+------------------------------------------------------------------+
//| Read a variable of an enumeration type                           |
//+------------------------------------------------------------------+
template<typename T>
bool CFilePipe::ReadEnum(T &value)
  {
   int val;
   if(!ReadInteger(val))
      return(false);
//---
   value=(T)val;
   return(true);
  }
//+------------------------------------------------------------------+
