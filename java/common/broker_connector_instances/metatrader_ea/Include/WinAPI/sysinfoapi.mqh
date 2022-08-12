//+------------------------------------------------------------------+
//|                                                   sysinfoapi.mqh |
//|                        Copyright 2018, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <WinAPI\windef.mqh>
#include <WinAPI\winbase.mqh>

//---
enum COMPUTER_NAME_FORMAT
  {
   ComputerNameNetBIOS,
   ComputerNameDnsHostname,
   ComputerNameDnsDomain,
   ComputerNameDnsFullyQualified,
   ComputerNamePhysicalNetBIOS,
   ComputerNamePhysicalDnsHostname,
   ComputerNamePhysicalDnsDomain,
   ComputerNamePhysicalDnsFullyQualified,
   ComputerNameMax
  };
//---
struct DUMMYSTRUCTNAME
  {
   uint              dwOemId;
   ushort            wProcessorArchitecture;
   ushort            wReserved;
  };
//---
struct MEMORYSTATUSEX
  {
   uint              dwLength;
   uint              dwMemoryLoad;
   ulong             ullTotalPhys;
   ulong             ullAvailPhys;
   ulong             ullTotalPageFile;
   ulong             ullAvailPageFile;
   ulong             ullTotalVirtual;
   ulong             ullAvailVirtual;
   ulong             ullAvailExtendedVirtual;
  };
//---
struct SYSTEM_INFO
  {
   uint              dwOemId;
   uint              dwPageSize;
   PVOID             lpMinimumApplicationAddress;
   PVOID             lpMaximumApplicationAddress;
   ulong             dwActiveProcessorMask;
   uint              dwNumberOfProcessors;
   uint              dwProcessorType;
   uint              dwAllocationGranularity;
   ushort            wProcessorLevel;
   ushort            wProcessorRevision;
  };
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
#import "kernel32.dll"
int     GlobalMemoryStatusEx(MEMORYSTATUSEX &buffer);
void    GetSystemInfo(SYSTEM_INFO &system_info);
void    GetSystemTime(SYSTEMTIME &system_time);
void    GetSystemTimeAsFileTime(FILETIME &system_time_as_file_time);
void    GetLocalTime(SYSTEMTIME &system_time);
uint    GetVersion(void);
int     SetLocalTime(const SYSTEMTIME &system_time);
uint    GetTickCount(void);
ulong   GetTickCount64(void);
int     GetSystemTimeAdjustment(uint &time_adjustment,uint &time_increment,int &time_adjustment_disabled);
uint    GetSystemDirectoryW(ushort &buffer[],uint size);
uint    GetWindowsDirectoryW(ushort &buffer[],uint size);
uint    GetSystemWindowsDirectoryW(ushort &buffer[],uint size);
int     GetComputerNameExW(COMPUTER_NAME_FORMAT name_type,ushort &buffer[],uint &size);
int     SetComputerNameExW(COMPUTER_NAME_FORMAT name_type,const string buffer);
int     SetSystemTime(const SYSTEMTIME &system_time);
int     GetVersionExW(OSVERSIONINFOW &version_information);
int     GetLogicalProcessorInformation(SYSTEM_LOGICAL_PROCESSOR_INFORMATION &buffer[],uint &returned_length);
int     GetLogicalProcessorInformationEx(LOGICAL_PROCESSOR_RELATIONSHIP relationship_type,SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX &buffer[],uint &returned_length);
void    GetNativeSystemInfo(SYSTEM_INFO &system_info);
void    GetSystemTimePreciseAsFileTime(FILETIME &system_time_as_file_time);
int     GetProductInfo(uint os_major_version,uint os_minor_version,uint sp_major_version,uint sp_minor_version,uint &returned_product_type);
uint    EnumSystemFirmwareTables(uint firmware_table_provider_signature,PVOID &firmware_table_enum_buffer,uint buffer_size);
uint    EnumSystemFirmwareTables(uint firmware_table_provider_signature,uchar &firmware_table_enum_buffer[],uint buffer_size);
uint    GetSystemFirmwareTable(uint firmware_table_provider_signature,uint firmware_table_id,PVOID firmware_table_buffer,uint buffer_size);
uint    GetSystemFirmwareTable(uint firmware_table_provider_signature,uint firmware_table_id,uchar &firmware_table_buffer[],uint buffer_size);
int     DnsHostnameToComputerNameExW(const string hostname,ushort &computer_name[],uint &size);
int     GetPhysicallyInstalledSystemMemory(ulong &total_memory_in_kilobytes);
int     SetComputerNameEx2W(COMPUTER_NAME_FORMAT name_type,uint flags,const string buffer);
int     SetSystemTimeAdjustment(uint time_adjustment,int time_adjustment_disabled);
int     InstallELAMCertificateInfo(HANDLE elam_file);
int     GetProcessorSystemCycleTime(ushort group,PVOID &buffer,uint &returned_length);
int     GetProcessorSystemCycleTime(ushort group,SYSTEM_PROCESSOR_CYCLE_TIME_INFORMATION &buffer[],uint &returned_length);
int     SetComputerNameW(const string computer_name);
#import
//+------------------------------------------------------------------+
