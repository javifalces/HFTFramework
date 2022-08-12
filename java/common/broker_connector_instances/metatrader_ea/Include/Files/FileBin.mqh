//+------------------------------------------------------------------+
//|                                                      FileBin.mqh |
//|                   Copyright 2009-2017, MetaQuotes Software Corp. |
//|                                              http://www.mql5.com |
//+------------------------------------------------------------------+
#include "File.mqh"
//+------------------------------------------------------------------+
//| Class CFileBin                                                   |
//| Purpose: Class of operations with binary files                   |
//|          Derives from class CFile                                |
//+------------------------------------------------------------------+
class CFileBin : public CFile
  {
public:
                     CFileBin(void);
                    ~CFileBin(void);
   //--- methods for working with files
   int               Open(const string file_name,const int open_flags);
   //--- methods for writing data
   uint              WriteChar(const char value);
   uint              WriteShort(const short value);
   uint              WriteInteger(const int value);
   uint              WriteLong(const long value);
   uint              WriteFloat(const float value);
   uint              WriteDouble(const double value);
   uint              WriteString(const string value);
   uint              WriteString(const string value,const int size);
   uint              WriteCharArray(const char &array[],const int start_item=0,const int items_count=WHOLE_ARRAY);
   uint              WriteShortArray(const short& array[],const int start_item=0,const int items_count=WHOLE_ARRAY);
   uint              WriteIntegerArray(const int& array[],const int start_item=0,const int items_count=WHOLE_ARRAY);
   uint              WriteLongArray(const long &array[],const int start_item=0,const int items_count=WHOLE_ARRAY);
   uint              WriteFloatArray(const float &array[],const int start_item=0,const int items_count=WHOLE_ARRAY);
   uint              WriteDoubleArray(const double &array[],const int start_item=0,const int items_count=WHOLE_ARRAY);
   template<typename T>
   uint              WriteArray(T &array[],const int start_item=0,const int items_count=WHOLE_ARRAY);
   template<typename T>
   uint              WriteStruct(T &data);
   bool              WriteObject(CObject *object);
   template<typename T>
   uint              WriteEnum(const T value) { return(WriteInteger((int)value)); }
   //--- methods for reading data
   bool              ReadChar(char &value);
   bool              ReadShort(short &value);
   bool              ReadInteger(int &value);
   bool              ReadLong(long &value);
   bool              ReadFloat(float &value);
   bool              ReadDouble(double &value);
   bool              ReadString(string &value);
   bool              ReadString(string &value,const int size);
   uint              ReadCharArray(char &array[],const int start_item=0,const int items_count=WHOLE_ARRAY);
   uint              ReadShortArray(short& array[],const int start_item=0,const int items_count=WHOLE_ARRAY);
   uint              ReadIntegerArray(int& array[],const int start_item=0,const int items_count=WHOLE_ARRAY);
   uint              ReadLongArray(long &array[],const int start_item=0,const int items_count=WHOLE_ARRAY);
   uint              ReadFloatArray(float &array[],const int start_item=0,const int items_count=WHOLE_ARRAY);
   uint              ReadDoubleArray(double &array[],const int start_item=0,const int items_count=WHOLE_ARRAY);
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
CFileBin::CFileBin(void)
  {
  }
//+------------------------------------------------------------------+
//| Destructor                                                       |
//+------------------------------------------------------------------+
CFileBin::~CFileBin(void)
  {
  }
//+------------------------------------------------------------------+
//| Opening a binary file                                            |
//+------------------------------------------------------------------+
int CFileBin::Open(const string file_name,const int open_flags)
  {
   return(CFile::Open(file_name,open_flags|FILE_BIN));
  }
//+------------------------------------------------------------------+
//| Write a variable of char or uchar type                           |
//+------------------------------------------------------------------+
uint CFileBin::WriteChar(const char value)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileWriteInteger(m_handle,value,sizeof(char)));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Write a variable of short or ushort type                         |
//+------------------------------------------------------------------+
uint CFileBin::WriteShort(const short value)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileWriteInteger(m_handle,value,sizeof(short)));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Write a variable of int or uint type                             |
//+------------------------------------------------------------------+
uint CFileBin::WriteInteger(const int value)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileWriteInteger(m_handle,value,sizeof(int)));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Write a variable of long or ulong type                           |
//+------------------------------------------------------------------+
uint CFileBin::WriteLong(const long value)
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
uint CFileBin::WriteFloat(const float value)
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
uint CFileBin::WriteDouble(const double value)
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
uint CFileBin::WriteString(const string value)
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
uint CFileBin::WriteString(const string value,const int size)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileWriteString(m_handle,value,size));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Write array variables of type char or uchar                      |
//+------------------------------------------------------------------+
uint CFileBin::WriteCharArray(const char &array[],const int start_item,const int items_count)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileWriteArray(m_handle,array,start_item,items_count));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Write an array of variables of short or ushort type              |
//+------------------------------------------------------------------+
uint CFileBin::WriteShortArray(const short &array[],const int start_item,const int items_count)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileWriteArray(m_handle,array,start_item,items_count));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Write an array of variables of int or uint type                  |
//+------------------------------------------------------------------+
uint CFileBin::WriteIntegerArray(const int &array[],const int start_item,const int items_count)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileWriteArray(m_handle,array,start_item,items_count));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Write an array of variables of long or ulong type                |
//+------------------------------------------------------------------+
uint CFileBin::WriteLongArray(const long &array[],const int start_item,const int items_count)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileWriteArray(m_handle,array,start_item,items_count));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Write an array of variables of float type                        |
//+------------------------------------------------------------------+
uint CFileBin::WriteFloatArray(const float &array[],const int start_item,const int items_count)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileWriteArray(m_handle,array,start_item,items_count));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Write an array of variables of double type                       |
//+------------------------------------------------------------------+
uint CFileBin::WriteDoubleArray(const double &array[],const int start_item,const int items_count)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileWriteArray(m_handle,array,start_item,items_count));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Write an array of variables of any type                          |
//+------------------------------------------------------------------+
template<typename T>
uint CFileBin::WriteArray(T &array[],const int start_item=0,const int items_count=WHOLE_ARRAY)
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
uint CFileBin::WriteStruct(T &data)
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
bool CFileBin::WriteObject(CObject *object)
  {
//--- check handle & object
   if(m_handle!=INVALID_HANDLE)
      if(CheckPointer(object))
         return(object.Save(m_handle));
//--- failure
   return(false);
  }
//+------------------------------------------------------------------+
//| Read a variable of char or uchar type                            |
//+------------------------------------------------------------------+
bool CFileBin::ReadChar(char &value)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
     {
      ResetLastError();
      value=(char)FileReadInteger(m_handle,sizeof(char));
      return(GetLastError()==0);
     }
//--- failure
   return(false);
  }
//+------------------------------------------------------------------+
//| Read a variable of short or ushort type                          |
//+------------------------------------------------------------------+
bool CFileBin::ReadShort(short &value)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
     {
      ResetLastError();
      value=(short)FileReadInteger(m_handle,sizeof(short));
      return(GetLastError()==0);
     }
//--- failure
   return(false);
  }
//+------------------------------------------------------------------+
//| Read a variable of int or uint type                              |
//+------------------------------------------------------------------+
bool CFileBin::ReadInteger(int &value)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
     {
      ResetLastError();
      value=FileReadInteger(m_handle,sizeof(int));
      return(GetLastError()==0);
     }
//--- failure
   return(false);
  }
//+------------------------------------------------------------------+
//| Read a variable of long or ulong type                            |
//+------------------------------------------------------------------+
bool CFileBin::ReadLong(long &value)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
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
bool CFileBin::ReadFloat(float &value)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
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
bool CFileBin::ReadDouble(double &value)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
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
bool CFileBin::ReadString(string &value)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
     {
      ResetLastError();
      int size=FileReadInteger(m_handle);
      if(GetLastError()==0)
        {
         value=FileReadString(m_handle,size);
         return(size==StringLen(value));
        }
     }
//--- failure
   return(false);
  }
//+------------------------------------------------------------------+
//| Read a part of string                                            |
//+------------------------------------------------------------------+
bool CFileBin::ReadString(string &value,const int size)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
     {
      value=FileReadString(m_handle,size);
      return(size==StringLen(value));
     }
//--- failure
   return(false);
  }
//+------------------------------------------------------------------+
//| Read an array of variables of char or uchar type                 |
//+------------------------------------------------------------------+
uint CFileBin::ReadCharArray(char &array[],const int start_item,const int items_count)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileReadArray(m_handle,array,start_item,items_count));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Read an array of variables of short or ushort type               |
//+------------------------------------------------------------------+
uint CFileBin::ReadShortArray(short &array[],const int start_item,const int items_count)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileReadArray(m_handle,array,start_item,items_count));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Read an array of variables of int or uint type                   |
//+------------------------------------------------------------------+
uint CFileBin::ReadIntegerArray(int &array[],const int start_item,const int items_count)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileReadArray(m_handle,array,start_item,items_count));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Read an array of variables of long or ulong type                 |
//+------------------------------------------------------------------+
uint CFileBin::ReadLongArray(long &array[],const int start_item,const int items_count)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileReadArray(m_handle,array,start_item,items_count));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Read an array of variables of float type                         |
//+------------------------------------------------------------------+
uint CFileBin::ReadFloatArray(float &array[],const int start_item,const int items_count)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileReadArray(m_handle,array,start_item,items_count));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Read an array of variables of double type                        |
//+------------------------------------------------------------------+
uint CFileBin::ReadDoubleArray(double &array[],const int start_item,const int items_count)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileReadArray(m_handle,array,start_item,items_count));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Read an array of variables of any type                           |
//+------------------------------------------------------------------+
template<typename T>
uint CFileBin::ReadArray(T &array[],const int start_item=0,const int items_count=WHOLE_ARRAY)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileReadArray(m_handle,array,start_item,items_count));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Read an structure                                                |
//+------------------------------------------------------------------+
template<typename T>
uint CFileBin::ReadStruct(T &data)
  {
//--- check handle
   if(m_handle!=INVALID_HANDLE)
      return(FileReadStruct(m_handle,data));
//--- failure
   return(0);
  }
//+------------------------------------------------------------------+
//| Read data of an instance of the CObject class                    |
//+------------------------------------------------------------------+
bool CFileBin::ReadObject(CObject *object)
  {
//--- check handle & object
   if(m_handle!=INVALID_HANDLE)
      if(CheckPointer(object))
         return(object.Load(m_handle));
//--- failure
   return(false);
  }
//+------------------------------------------------------------------+
//| Read a variable of an enumeration type                           |
//+------------------------------------------------------------------+
template<typename T>
bool CFileBin::ReadEnum(T &value)
  {
   int val;
   if(!ReadInteger(val))
      return(false);
//---
   value=(T)val;
   return(true);
  }
//+------------------------------------------------------------------+
