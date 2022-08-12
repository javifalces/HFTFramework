//+------------------------------------------------------------------+
//|                                                   processenv.mqh |
//|                        Copyright 2018, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <WinAPI\windef.mqh>

//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
#import "kernel32.dll"
int    SetEnvironmentStringsW(string new_environment);
HANDLE GetStdHandle(uint std_handle);
int    SetStdHandle(uint std_handle,HANDLE handle);
int    SetStdHandleEx(uint std_handle,HANDLE handle,HANDLE &prev_value);
string GetCommandLineW(void);
string GetEnvironmentStringsW(void);
int    FreeEnvironmentStringsW(string v);
uint   GetEnvironmentVariableW(const string name,ushort &buffer[],uint size);
int    SetEnvironmentVariableW(const string name,const string value);
uint   ExpandEnvironmentStringsW(const string src,string dst,uint size);
int    SetCurrentDirectoryW(const string path_name);
uint   GetCurrentDirectoryW(uint buffer_length,ushort &buffer[]);
uint   GetCurrentDirectoryW(uint buffer_length,string &buffer);
uint   SearchPathW(const string path,const string file_name,const string extension,uint buffer_length,ushort &buffer[],string &file_part);
int    NeedCurrentDirectoryForExePathW(const string exe_name);
#import
