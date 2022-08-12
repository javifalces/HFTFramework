//+------------------------------------------------------------------+
//|                                            processthreadsapi.mqh |
//|                        Copyright 2018, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <WinAPI\windef.mqh>
#include <WinAPI\winnt.mqh>

//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
//---
enum THREAD_INFORMATION_CLASS
  {
   ThreadMemoryPriority,
   ThreadAbsoluteCpuPriority,
   ThreadDynamicCodePolicy,
   ThreadPowerThrottling,
   ThreadInformationClassMax
  };
//---
enum PROCESS_INFORMATION_CLASS
  {
   ProcessMemoryPriority,
   ProcessMemoryExhaustionInfo,
   ProcessAppMemoryInfo,
   ProcessInPrivateInfo,
   ProcessPowerThrottling,
   ProcessReservedValue1,
   ProcessTelemetryCoverageInfo,
   ProcessProtectionLevelInfo,
   ProcessInformationClassMax
  };
//---
enum PROCESS_MEMORY_EXHAUSTION_TYPE
  {
   PMETypeFailFastOnCommitFailure,
   PMETypeMax
  };
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
//---
struct PROCESS_INFORMATION
  {
   HANDLE            hProcess;
   HANDLE            hThread;
   uint              dwProcessId;
   uint              dwThreadId;
  };
//---
struct STARTUPINFOW
  {
   uint              cb;
   string            lpReserved;
   string            lpDesktop;
   string            lpTitle;
   uint              dwX;
   uint              dwY;
   uint              dwXSize;
   uint              dwYSize;
   uint              dwXCountChars;
   uint              dwYCountChars;
   uint              dwFillAttribute;
   uint              dwFlags;
   ushort            wShowWindow;
   ushort            cbReserved2;
   PVOID             lpReserved2;
   HANDLE            hStdInput;
   HANDLE            hStdOutput;
   HANDLE            hStdError;
  };
//---
struct MEMORY_PRIORITY_INFORMATION
  {
   uint              MemoryPriority;
  };
//---
struct THREAD_POWER_THROTTLING_STATE
  {
   uint              Version;
   uint              ControlMask;
   uint              StateMask;
  };
//---
struct APP_MEMORY_INFORMATION
  {
   ulong             AvailableCommit;
   ulong             PrivateCommitUsage;
   ulong             PeakPrivateCommitUsage;
   ulong             TotalCommitUsage;
  };
//---
struct PROCESS_MEMORY_EXHAUSTION_INFO
  {
   ushort            Version;
   ushort            Reserved;
   PROCESS_MEMORY_EXHAUSTION_TYPE Type;
   ulong             Value;
  };
//---
struct PROCESS_POWER_THROTTLING_STATE
  {
   uint              Version;
   uint              ControlMask;
   uint              StateMask;
  };
//---
struct PROCESS_PROTECTION_LEVEL_INFORMATION
  {
   uint              ProtectionLevel;
  };
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
#import "kernel32.dll"
uint    QueueUserAPC(PVOID apc,HANDLE thread,ulong data);
int     GetProcessTimes(HANDLE process,FILETIME &creation_time,FILETIME &exit_time,FILETIME &kernel_time,FILETIME &user_time);
HANDLE  GetCurrentProcess(void);
uint    GetCurrentProcessId(void);
void    ExitProcess(uint exit_code);
int     TerminateProcess(HANDLE process,uint exit_code);
int     GetExitCodeProcess(HANDLE process,uint &exit_code);
int     SwitchToThread(void);
HANDLE  CreateThread(PVOID thread_attributes,ulong stack_size,PVOID start_address,PVOID parameter,uint creation_flags,uint &thread_id);
HANDLE  CreateRemoteThread(HANDLE process,PVOID thread_attributes,ulong stack_size,PVOID start_address,PVOID parameter,uint creation_flags,uint &thread_id);
HANDLE  GetCurrentThread(void);
uint    GetCurrentThreadId(void);
HANDLE  OpenThread(uint desired_access,int inherit_handle,uint thread_id);
int     SetThreadPriority(HANDLE thread,int priority);
int     SetThreadPriorityBoost(HANDLE thread,int disable_priority_boost);
int     GetThreadPriorityBoost(HANDLE thread,int &disable_priority_boost);
int     GetThreadPriority(HANDLE thread);
void    ExitThread(uint exit_code);
int     TerminateThread(HANDLE thread,uint exit_code);
int     GetExitCodeThread(HANDLE thread,uint &exit_code);
uint    SuspendThread(HANDLE thread);
uint    ResumeThread(HANDLE thread);
uint    TlsAlloc(void);
PVOID   TlsGetValue(uint tls_index);
int     TlsSetValue(uint tls_index,PVOID tls_value);
int     TlsFree(uint tls_index);
int     CreateProcessW(const string application_name,string command_line,PVOID process_attributes,PVOID thread_attributes,int inherit_handles,uint creation_flags,PVOID environment,const string current_directory,STARTUPINFOW &startup_info,PROCESS_INFORMATION &process_information);
int     SetProcessShutdownParameters(uint level,uint flags);
uint    GetProcessVersion(uint process_id);
void    GetStartupInfoW(STARTUPINFOW &startup_info);
int     SetPriorityClass(HANDLE process,uint priority_class);
uint    GetPriorityClass(HANDLE process);
int     SetThreadStackGuarantee(ulong stack_size_in_bytes);
int     ProcessIdToSessionId(uint process_id,uint &session_id);
uint    GetProcessId(HANDLE process);
uint    GetThreadId(HANDLE thread);
void    FlushProcessWriteBuffers(void);
uint    GetProcessIdOfThread(HANDLE thread);
int     InitializeProcThreadAttributeList(PVOID attribute_list,uint attribute_count,uint flags,ulong &size);
void    DeleteProcThreadAttributeList(PVOID  attribute_list);
int     SetProcessAffinityUpdateMode(HANDLE process,uint flags);
int     QueryProcessAffinityUpdateMode(HANDLE process,uint &flags);
int     UpdateProcThreadAttribute(PVOID attribute_list,uint flags,uint attribute,PVOID value,ulong size,PVOID previous_value,ulong &return_size);
HANDLE  CreateRemoteThreadEx(HANDLE process,PVOID thread_attributes,ulong stack_size,PVOID start_address,PVOID parameter,uint creation_flags,PVOID attribute_list,uint &thread_id);
void    GetCurrentThreadStackLimits(ulong &low_limit,ulong &high_limit);
int     GetThreadContext(HANDLE thread,CONTEXT &context);
int     GetProcessMitigationPolicy(HANDLE process,PROCESS_MITIGATION_POLICY mitigation_policy,PVOID buffer,ulong length);
int     SetThreadContext(HANDLE thread,const CONTEXT &context);
int     SetProcessMitigationPolicy(PROCESS_MITIGATION_POLICY mitigation_policy,PVOID buffer,ulong length);
int     FlushInstructionCache(HANDLE process,const PVOID base_address,ulong size);
int     GetThreadTimes(HANDLE thread,FILETIME &creation_time,FILETIME &exit_time,FILETIME &kernel_time,FILETIME &user_time);
HANDLE  OpenProcess(uint desired_access,int inherit_handle,uint process_id);
int     IsProcessorFeaturePresent(uint processor_feature);
int     GetProcessHandleCount(HANDLE process,uint &handle_count);
uint    GetCurrentProcessorNumber(void);
int     SetThreadIdealProcessorEx(HANDLE thread,PROCESSOR_NUMBER &ideal_processor,PROCESSOR_NUMBER &previous_ideal_processor);
int     GetThreadIdealProcessorEx(HANDLE thread,PROCESSOR_NUMBER &ideal_processor);
void    GetCurrentProcessorNumberEx(PROCESSOR_NUMBER &proc_number);
int     GetProcessPriorityBoost(HANDLE process,int &disable_priority_boost);
int     SetProcessPriorityBoost(HANDLE process,int disable_priority_boost);
int     GetThreadIOPendingFlag(HANDLE thread,int &io_is_pending);
int     GetSystemTimes(FILETIME &idle_time,FILETIME &kernel_time,FILETIME &user_time);
int     GetThreadInformation(HANDLE thread,THREAD_INFORMATION_CLASS thread_information_class,PVOID thread_information,uint thread_information_size);
int     SetThreadInformation(HANDLE thread,THREAD_INFORMATION_CLASS thread_information_class,PVOID thread_information,uint thread_information_size);
int     IsProcessCritical(HANDLE process,int &critical);
int     SetProtectedPolicy(const GUID &policy_guid,ulong policy_value,ulong &old_policy_value);
int     QueryProtectedPolicy(const GUID &policy_guid,ulong &policy_value);
uint    SetThreadIdealProcessor(HANDLE thread,uint ideal_processor);
int     SetProcessInformation(HANDLE process,PROCESS_INFORMATION_CLASS process_information_class,PVOID process_information,uint process_information_size);
int     GetProcessInformation(HANDLE process,PROCESS_INFORMATION_CLASS process_information_class,PVOID process_information,uint process_information_size);
int     GetSystemCpuSetInformation(SYSTEM_CPU_SET_INFORMATION &information,uint buffer_length,ulong returned_length,HANDLE process,uint flags);
int     GetProcessDefaultCpuSets(HANDLE process,ulong &cpu_set_ids,uint cpu_set_id_count,ulong required_id_count);
int     SetProcessDefaultCpuSets(HANDLE process,const uint &cpu_set_ids,uint cpu_set_id_count);
int     GetThreadSelectedCpuSets(HANDLE thread,ulong &cpu_set_ids,uint cpu_set_id_count,ulong required_id_count);
int     SetThreadSelectedCpuSets(HANDLE thread,const uint &cpu_set_ids,uint cpu_set_id_count);
int     GetProcessShutdownParameters(uint &level,uint &flags);
int     SetThreadDescription(HANDLE thread,const string thread_description);
int     GetThreadDescription(HANDLE thread,string &thread_description);
#import
#import "advapi32.dll"
int     CreateProcessAsUserW(HANDLE token,const string application_name,string command_line,PVOID process_attributes,PVOID thread_attributes,int inherit_handles,uint creation_flags,PVOID environment,const string current_directory,STARTUPINFOW &startup_info,PROCESS_INFORMATION &process_information);
int     SetThreadToken(HANDLE thread,HANDLE token);
int     OpenProcessToken(HANDLE process_handle,uint desired_access,HANDLE &token_handle);
int     OpenThreadToken(HANDLE thread_handle,uint desired_access,int open_as_self,HANDLE &token_handle);
#import
//+------------------------------------------------------------------+
