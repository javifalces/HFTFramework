//+------------------------------------------------------------------+
//|                                                      WinBase.mqh |
//|                        Copyright 2020, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <WinAPI\windef.mqh>
#include <WinAPI\winnt.mqh>
#include <WinAPI\fileapi.mqh>

//---
#define OFS_MAXPATHNAME            128
#define HW_PROFILE_GUIDLEN         39
#define MAX_PROFILE_LEN            80
#define RESTART_MAX_CMD_LINE       1024

//---
enum COPYFILE2_COPY_PHASE
  {
   COPYFILE2_PHASE_NONE=0,
   COPYFILE2_PHASE_PREPARE_SOURCE,
   COPYFILE2_PHASE_PREPARE_DEST,
   COPYFILE2_PHASE_READ_SOURCE,
   COPYFILE2_PHASE_WRITE_DESTINATION,
   COPYFILE2_PHASE_SERVER_COPY,
   COPYFILE2_PHASE_NAMEGRAFT_COPY,
   COPYFILE2_PHASE_MAX
  };
//---
enum COPYFILE2_MESSAGE_ACTION
  {
   COPYFILE2_PROGRESS_CONTINUE=0,
   COPYFILE2_PROGRESS_CANCEL,
   COPYFILE2_PROGRESS_STOP,
   COPYFILE2_PROGRESS_QUIET,
   COPYFILE2_PROGRESS_PAUSE
  };
//---
enum COPYFILE2_MESSAGE_TYPE
  {
   COPYFILE2_CALLBACK_NONE=0,
   COPYFILE2_CALLBACK_CHUNK_STARTED,
   COPYFILE2_CALLBACK_CHUNK_FINISHED,
   COPYFILE2_CALLBACK_STREAM_STARTED,
   COPYFILE2_CALLBACK_STREAM_FINISHED,
   COPYFILE2_CALLBACK_POLL_CONTINUE,
   COPYFILE2_CALLBACK_ERROR,
   COPYFILE2_CALLBACK_MAX
  };
//---
enum DEP_SYSTEM_POLICY_TYPE
  {
   DEPPolicyAlwaysOff=0,
   DEPPolicyAlwaysOn,
   DEPPolicyOptIn,
   DEPPolicyOptOut,
   DEPTotalPolicyCount
  };
//---
enum FILE_ID_TYPE
  {
   FileIdType,
   ObjectIdType,
   ExtendedFileIdType,
   MaximumFileIdType
  };
//---
enum PRIORITY_HINT
  {
   IoPriorityHintVeryLow=0,
   IoPriorityHintLow,
   IoPriorityHintNormal,
   MaximumIoPriorityHintType
  };
//---
enum PROC_THREAD_ATTRIBUTE_NUM
  {
   ProcThreadAttributeParentProcess=0,
   ProcThreadAttributeHandleList=2,
   ProcThreadAttributeGroupAffinity=3,
   ProcThreadAttributePreferredNode=4,
   ProcThreadAttributeIdealProcessor=5,
   ProcThreadAttributeUmsThread=6,
   ProcThreadAttributeMitigationPolicy=7,
   ProcThreadAttributeSecurityCapabilities=9,
   ProcThreadAttributeProtectionLevel=11,
   ProcThreadAttributeJobList=13,
   ProcThreadAttributeChildProcessPolicy=14,
   ProcThreadAttributeAllApplicationPackagesPolicy=15,
   ProcThreadAttributeWin32kFilter=16,
   ProcThreadAttributeSafeOpenPromptOriginClaim=17,
   ProcThreadAttributeDesktopAppPolicy=18
  };
//---
struct ACTCTX_SECTION_KEYED_DATA_ASSEMBLY_METADATA
  {
   PVOID             lpInformation;
   PVOID             lpSectionBase;
   uint              ulSectionLength;
   PVOID             lpSectionGlobalDataBase;
   uint              ulSectionGlobalDataLength;
  };
//---
struct ACTCTX_SECTION_KEYED_DATA
  {
   uint              cbSize;
   uint              ulDataFormatVersion;
   PVOID             lpData;
   uint              ulLength;
   PVOID             lpSectionGlobalData;
   uint              ulSectionGlobalDataLength;
   PVOID             lpSectionBase;
   uint              ulSectionTotalLength;
   HANDLE            hActCtx;
   uint              ulAssemblyRosterIndex;
   uint              ulFlags;
   ACTCTX_SECTION_KEYED_DATA_ASSEMBLY_METADATA AssemblyMetadata;
  };
//---
struct ACTCTX_SECTION_KEYED_DATA_2600
  {
   uint              cbSize;
   uint              ulDataFormatVersion;
   PVOID             lpData;
   uint              ulLength;
   PVOID             lpSectionGlobalData;
   uint              ulSectionGlobalDataLength;
   PVOID             lpSectionBase;
   uint              ulSectionTotalLength;
   HANDLE            hActCtx;
   uint              ulAssemblyRosterIndex;
  };
//---
struct ACTCTXW
  {
   uint              cbSize;
   uint              dwFlags;
   PVOID             lpSource;
   ushort            wProcessorArchitecture;
   ushort            wLangId;
   PVOID             lpAssemblyDirectory;
   PVOID             lpResourceName;
   PVOID             lpApplicationName;
   HANDLE            hModule;
  };
//---
struct ACTIVATION_CONTEXT_BASIC_INFORMATION
  {
   HANDLE            hActCtx;
   uint              dwFlags;
  };
//---
struct DCB
  {
   uint              DCBlength;
   uint              BaudRate;
   uint              Flags;
   ushort            wReserved;
   ushort            XonLim;
   ushort            XoffLim;
   uchar             ByteSize;
   uchar             Parity;
   uchar             StopBits;
   char              XonChar;
   char              XoffChar;
   char              ErrorChar;
   char              EofChar;
   char              EvtChar;
   ushort            wReserved1;
  };
//---
struct COMMCONFIG
  {
   uint              dwSize;
   ushort            wVersion;
   ushort            wReserved;
   DCB               dcb;
   uint              dwProviderSubType;
   uint              dwProviderOffset;
   uint              dwProviderSize;
   short             wcProviderData[2];
  };
//---
struct COMMPROP
  {
   ushort            wPacketLength;
   ushort            wPacketVersion;
   uint              dwServiceMask;
   uint              dwReserved1;
   uint              dwMaxTxQueue;
   uint              dwMaxRxQueue;
   uint              dwMaxBaud;
   uint              dwProvSubType;
   uint              dwProvCapabilities;
   uint              dwSettableParams;
   uint              dwSettableBaud;
   ushort            wSettableData;
   ushort            wSettableStopParity;
   uint              dwCurrentTxQueue;
   uint              dwCurrentRxQueue;
   uint              dwProvSpec1;
   uint              dwProvSpec2;
   short             wcProvChar[1];
  };
//---
struct COMMTIMEOUTS
  {
   uint              ReadIntervalTimeout;
   uint              ReadTotalTimeoutMultiplier;
   uint              ReadTotalTimeoutConstant;
   uint              WriteTotalTimeoutMultiplier;
   uint              WriteTotalTimeoutConstant;
  };
//---
struct COMSTAT
  {
   uint              cbInQue;
   uint              cbOutQue;
  };
//---
struct COPYFILE2_EXTENDED_PARAMETERS
  {
   uint              dwSize;
   uint              dwCopyFlags;
   PVOID             pfCancel;
   PVOID             pProgressRoutine;
   PVOID             pvCallbackContext;
  };
//---
struct EVENTLOG_FULL_INFORMATION
  {
   uint              dwFull;
  };
//---
struct FILE_ALIGNMENT_INFO: public FILE_INFO
  {
   uint              AlignmentRequirement;
  };
//---
struct FILE_ALLOCATION_INFO: public FILE_INFO
  {
   long              AllocationSize;
  };
//---
struct FILE_ATTRIBUTE_TAG_INFO: public FILE_INFO
  {
   uint              FileAttributes;
   uint              ReparseTag;
  };
//---
struct FILE_BASIC_INFO: public FILE_INFO
  {
   long              CreationTime;
   long              LastAccessTime;
   long              LastWriteTime;
   long              ChangeTime;
   uint              FileAttributes;
  };
//---
struct FILE_COMPRESSION_INFO: public FILE_INFO
  {
   long              CompressedFileSize;
   ushort            CompressionFormat;
   uchar             CompressionUnitShift;
   uchar             ChunkShift;
   uchar             ClusterShift;
   uchar             Reserved[3];
  };
//---
struct FILE_DISPOSITION_INFO: public FILE_INFO
  {
   uchar             DeleteFile;
  };
//---
struct FILE_DISPOSITION_INFO_EX: public FILE_INFO
  {
   uint              Flags;
  };
//---
struct FILE_END_OF_FILE_INFO: public FILE_INFO
  {
   long              EndOfFile;
  };
//---
struct FILE_FULL_DIR_INFO: public FILE_INFO
  {
   uint              NextEntryOffset;
   uint              FileIndex;
   long              CreationTime;
   long              LastAccessTime;
   long              LastWriteTime;
   long              ChangeTime;
   long              EndOfFile;
   long              AllocationSize;
   uint              FileAttributes;
   uint              FileNameLength;
   uint              EaSize;
   short             FileName[1];
  };
//---
struct FILE_ID_BOTH_DIR_INFO: public FILE_INFO
  {
   uint              NextEntryOffset;
   uint              FileIndex;
   long              CreationTime;
   long              LastAccessTime;
   long              LastWriteTime;
   long              ChangeTime;
   long              EndOfFile;
   long              AllocationSize;
   uint              FileAttributes;
   uint              FileNameLength;
   uint              EaSize;
   char              ShortNameLength;
   short             ShortName[12];
   long              FileId;
   short             FileName[1];
  };
//---
struct FILE_ID_EXTD_DIR_INFO: public FILE_INFO
  {
   uint              NextEntryOffset;
   uint              FileIndex;
   long              CreationTime;
   long              LastAccessTime;
   long              LastWriteTime;
   long              ChangeTime;
   long              EndOfFile;
   long              AllocationSize;
   uint              FileAttributes;
   uint              FileNameLength;
   uint              EaSize;
   uint              ReparsePointTag;
   FILE_ID_128       FileId;
   short             FileName[1];
  };
//---
struct FILE_ID_INFO: public FILE_INFO
  {
   ulong             VolumeSerialNumber;
   FILE_ID_128       FileId;
  };
//---
struct FILE_IO_PRIORITY_HINT_INFO: public FILE_INFO
  {
   PRIORITY_HINT     PriorityHint;
  };
//---
struct FILE_NAME_INFO
  {
   uint              FileNameLength;
   short             FileName[2];
  };
//---
struct FILE_STANDARD_INFO: public FILE_INFO
  {
   long              AllocationSize;
   long              EndOfFile;
   uint              NumberOfLinks;
   uchar             DeletePending;
   uchar             Directory;
  };
//---
struct FILE_STORAGE_INFO: public FILE_INFO
  {
   uint              LogicalBytesPerSector;
   uint              PhysicalBytesPerSectorForAtomicity;
   uint              PhysicalBytesPerSectorForPerformance;
   uint              FileSystemEffectivePhysicalBytesPerSectorForAtomicity;
   uint              Flags;
   uint              ByteOffsetForSectorAlignment;
   uint              ByteOffsetForPartitionAlignment;
  };
//---
struct FILE_STREAM_INFO: public FILE_INFO
  {
   uint              NextEntryOffset;
   uint              StreamNameLength;
   long              StreamSize;
   long              StreamAllocationSize;
   short             StreamName[1];
  };
//---
struct HW_PROFILE_INFOW
  {
   uint              dwDockInfo;
   short             szHwProfileGuid[HW_PROFILE_GUIDLEN];
   short             szHwProfileName[MAX_PROFILE_LEN];
  };
//---
struct JIT_DEBUG_INFO
  {
   uint              dwSize;
   uint              dwProcessorArchitecture;
   uint              dwThreadID;
   uint              dwReserved0;
   ulong             lpExceptionAddress;
   ulong             lpExceptionRecord;
   ulong             lpContextRecord;
  };
//---
struct MEMORYSTATUS
  {
   uint              dwLength;
   uint              dwMemoryLoad;
   ulong             dwTotalPhys;
   ulong             dwAvailPhys;
   ulong             dwTotalPageFile;
   ulong             dwAvailPageFile;
   ulong             dwTotalVirtual;
   ulong             dwAvailVirtual;
  };
//---
struct OFSTRUCT
  {
   uchar             cBytes;
   uchar             fFixedDisk;
   ushort            nErrCode;
   ushort            Reserved1;
   ushort            Reserved2;
   char              szPathName[OFS_MAXPATHNAME];
  };
//---
struct OPERATION_END_PARAMETERS
  {
   uint              Version;
   uint              OperationId;
   uint              Flags;
  };
//---
struct OPERATION_START_PARAMETERS
  {
   uint              Version;
   uint              OperationId;
   uint              Flags;
  };
//---
struct SYSTEM_POWER_STATUS
  {
   uchar             ACLineStatus;
   uchar             BatteryFlag;
   uchar             BatteryLifePercent;
   uchar             SystemStatusFlag;
   uint              BatteryLifeTime;
   uint              BatteryFullLifeTime;
  };
//---
struct UMS_SCHEDULER_STARTUP_INFO
  {
   uint              UmsVersion;
   PVOID             CompletionList;
   PVOID             SchedulerProc;
   PVOID             SchedulerParam;
  };
//---
struct WIN32_STREAM_ID
  {
   uint              dwStreamId;
   uint              dwStreamAttributes;
   long              Size;
   uint              dwStreamNameSize;
  };
//---
struct UMS_SYSTEM_THREAD_INFORMATION
  {
   uint              UmsVersion;
   uint              ThreadUmsFlags;
  };
//---
struct FILE_ID_DESCRIPTOR
  {
   uint              dwSize;
   FILE_ID_TYPE      Type;
   long              FileId;
  };
//---
struct SYSTEMTIME
  {
   ushort            wYear;
   ushort            wMonth;
   ushort            wDayOfWeek;
   ushort            wDay;
   ushort            wHour;
   ushort            wMinute;
   ushort            wSecond;
   ushort            wMilliseconds;
  };
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
#import "kernel32.dll"
int                    ActivateActCtx(HANDLE act_ctx,PVOID &cookie);
int                    ActivateActCtx(ACTCTXW &act_ctx,PVOID &cookie);
ushort                 AddAtomW(const string str);
int                    AddIntegrityLabelToBoundaryDescriptor(HANDLE &BoundaryDescriptor,SID &IntegrityLabel);
void                   AddRefActCtx(HANDLE act_ctx);
void                   AddRefActCtx(ACTCTXW &act_ctx);
int                    AddSecureMemoryCacheCallback(PVOID call_back);
void                   ApplicationRecoveryFinished(int success);
int                    ApplicationRecoveryInProgress(int &cancelled);
int                    BackupRead(HANDLE file,uchar &buffer[],uint number_of_bytes_to_read,uint &number_of_bytes_read,int abort,int process_security,PVOID &context);
int                    BackupSeek(HANDLE file,uint low_bytes_to_seek,uint high_bytes_to_seek,uint &low_byte_seeked,uint &high_byte_seeked,PVOID &context);
int                    BackupWrite(HANDLE file,uchar &buffer[],uint number_of_bytes_to_write,uint &number_of_bytes_written,int abort,int process_security,PVOID &context);
HANDLE                 BeginUpdateResourceW(const string file_name,int delete_existing_resources);
int                    BindIoCompletionCallback(HANDLE FileHandle,PVOID Function,uint Flags);
int                    BuildCommDCBAndTimeoutsW(const string def,DCB &lpDCB,COMMTIMEOUTS &comm_timeouts);
int                    BuildCommDCBW(const string def,DCB &lpDCB);
int                    CancelDeviceWakeupRequest(HANDLE device);
int                    CancelTimerQueueTimer(HANDLE TimerQueue,HANDLE Timer);
int                    CheckNameLegalDOS8Dot3W(const string name,char &oem_name[],uint oem_name_size,int &name_contains_spaces,int &name_legal);
int                    ClearCommBreak(HANDLE file);
int                    ClearCommError(HANDLE file,uint &errors,COMSTAT &stat);
int                    CommConfigDialogW(const string name,HANDLE wnd,COMMCONFIG &lpCC);
int                    ConvertFiberToThread(void);
PVOID                  ConvertThreadToFiber(PVOID parameter);
PVOID                  ConvertThreadToFiberEx(PVOID parameter,uint flags);
int                    CopyContext(CONTEXT &Destination,uint ContextFlags,CONTEXT &Source);
int                    CopyFile2(const string existing_file_name,const string new_file_name,COPYFILE2_EXTENDED_PARAMETERS &extended_parameters);
int                    CopyFileExW(const string existing_file_name,const string new_file_name,PVOID progress_routine,PVOID data,int &cancel,uint copy_flags);
int                    CopyFileTransactedW(const string existing_file_name,const string new_file_name,PVOID progress_routine,PVOID data,int &cancel,uint copy_flags,HANDLE transaction);
int                    CopyFileW(const string existing_file_name,const string new_file_name,int fail_if_exists);
HANDLE                 CreateActCtxW(const ACTCTXW &act_ctx);
int                    CreateDirectoryExW(const string template_directory,const string new_directory,PVOID security_attributes);
int                    CreateDirectoryTransactedW(const string template_directory,const string new_directory,PVOID security_attributes,HANDLE transaction);
PVOID                  CreateFiber(ulong stack_size,PVOID start_address,PVOID parameter);
PVOID                  CreateFiberEx(ulong stack_commit_size,ulong stack_reserve_size,uint flags,PVOID start_address,PVOID parameter);
HANDLE                 CreateFileTransactedW(const string file_name,uint desired_access,uint share_mode,PVOID security_attributes,uint creation_disposition,uint flags_and_attributes,HANDLE template_file,HANDLE transaction,ushort &mini_version,PVOID extended_parameter);
int                    CreateHardLinkTransactedW(const string file_name,const string existing_file_name,PVOID security_attributes,HANDLE transaction);
int                    CreateHardLinkW(const string file_name,const string existing_file_name,PVOID security_attributes);
int                    CreateJobSet(uint NumJob,JOB_SET_ARRAY &UserJobSet,uint Flags);
HANDLE                 CreateMailslotW(const string name,uint max_message_size,uint read_timeout,PVOID security_attributes);
uchar                  CreateSymbolicLinkTransactedW(const string symlink_file_name,const string target_file_name,uint flags,HANDLE transaction);
uchar                  CreateSymbolicLinkW(const string symlink_file_name,const string target_file_name,uint flags);
uint                   CreateTapePartition(HANDLE device,uint partition_method,uint count,uint size);
int                    CreateUmsCompletionList(PVOID &UmsCompletionList);
int                    CreateUmsThreadContext(PVOID &ums_thread);
int                    DeactivateActCtx(uint flags,ulong cookie);
int                    DebugBreakProcess(HANDLE Process);
int                    DebugSetProcessKillOnExit(int KillOnExit);
ushort                 DeleteAtom(ushort atom);
void                   DeleteFiber(PVOID fiber);
int                    DeleteFileTransactedW(const string file_name,HANDLE transaction);
int                    DeleteTimerQueue(HANDLE TimerQueue);
int                    DeleteUmsCompletionList(PVOID UmsCompletionList);
int                    DeleteUmsThreadContext(PVOID UmsThread);
int                    DequeueUmsCompletionListItems(PVOID UmsCompletionList,uint WaitTimeOut,PVOID &UmsThreadList);
uint                   DisableThreadProfiling(HANDLE PerformanceDataHandle);
int                    DnsHostnameToComputerNameW(const string hostname,ushort &computer_name[],uint &size);
int                    DosDateTimeToFileTime(ushort fat_date,ushort fat_time,FILETIME &file_time);
uint                   EnableThreadProfiling(HANDLE ThreadHandle,uint Flags,ulong HardwareCounters,HANDLE &PerformanceDataHandle);
int                    EndUpdateResourceW(HANDLE update,int discard);
int                    EnterUmsSchedulingMode(UMS_SCHEDULER_STARTUP_INFO &SchedulerStartupInfo);
int                    EnumResourceLanguagesW(HANDLE module,const string type,const string name,PVOID enum_func,long param);
int                    EnumResourceTypesW(HANDLE module,PVOID enum_func,long param);
uint                   EraseTape(HANDLE device,uint erase_type,int immediate);
int                    EscapeCommFunction(HANDLE file,uint func);
int                    ExecuteUmsThread(PVOID UmsThread);
void                   FatalExit(int ExitCode);
int                    FileTimeToDosDateTime(FILETIME &file_time,ushort &fat_date,ushort &fat_time);
int                    FindActCtxSectionGuid(uint flags,const GUID &extension_guid[],uint section_id,const GUID &guid_to_find[],ACTCTX_SECTION_KEYED_DATA &ReturnedData);
int                    FindActCtxSectionStringW(uint flags,const GUID &extension_guid[],uint section_id,const string string_to_find,ACTCTX_SECTION_KEYED_DATA &ReturnedData);
ushort                 FindAtomW(const string str);
HANDLE                 FindFirstFileNameTransactedW(const string file_name,uint flags,uint &StringLength,string LinkName,HANDLE transaction);
HANDLE                 FindFirstFileTransactedW(const string file_name,FINDEX_INFO_LEVELS info_level_id,FIND_DATAW &find_file_data,FINDEX_SEARCH_OPS search_op,PVOID search_filter,uint additional_flags,HANDLE transaction);
HANDLE                 FindFirstStreamTransactedW(const string file_name,STREAM_INFO_LEVELS InfoLevel,FIND_STREAM_DATA &find_stream_data,uint flags,HANDLE transaction);
HANDLE                 FindFirstVolumeMountPointW(const string root_path_name,ushort &volume_mount_point[],uint buffer_length);
int                    FindNextVolumeMountPointW(HANDLE find_volume_mount_point,string volume_mount_point,uint buffer_length);
int                    FindVolumeMountPointClose(HANDLE find_volume_mount_point);
uint                   FormatMessageW(uint flags,const uchar &source[],uint message_id,uint language_id,ushort &buffer[],uint size,PVOID &Arguments[]);
uint                   GetActiveProcessorCount(ushort GroupNumber);
ushort                 GetActiveProcessorGroupCount(void);
int                    GetApplicationRecoveryCallback(HANDLE process,PVOID &recovery_callback,PVOID &parameter,uint &ping_interval,uint &flags);
int                    GetApplicationRestartSettings(HANDLE process,ushort &commandline[],uint &size,uint &flags);
uint                   GetAtomNameW(ushort atom,ushort &buffer[],int size);
int                    GetBinaryTypeW(const string application_name,uint &binary_type);
int                    GetCommConfig(HANDLE comm_dev,COMMCONFIG &lpCC,uint &size);
int                    GetCommMask(HANDLE file,uint &evt_mask);
int                    GetCommModemStatus(HANDLE file,uint &modem_stat);
int                    GetCommProperties(HANDLE file,COMMPROP &comm_prop);
int                    GetCommState(HANDLE file,DCB &lpDCB);
int                    GetCommTimeouts(HANDLE file,COMMTIMEOUTS &comm_timeouts);
uint                   GetCompressedFileSizeTransactedW(const string file_name,uint &file_size_high,HANDLE transaction);
int                    GetComputerNameW(ushort &buffer[],uint &size);
int                    GetCurrentActCtx(HANDLE &act_ctx);
int                    GetCurrentActCtx(ACTCTXW &act_ctx);
PVOID                  GetCurrentUmsThread(void);
int                    GetDefaultCommConfigW(const string name,COMMCONFIG &lpCC,uint &size);
int                    GetDevicePowerState(HANDLE device,int &on);
uint                   GetDllDirectoryW(uint buffer_length,ushort &buffer[]);
ulong                  GetEnabledXStateFeatures(void);
int                    GetFileAttributesTransactedW(const string file_name,GET_FILEEX_INFO_LEVELS info_level_id,PVOID file_information,HANDLE transaction);
int                    GetFileBandwidthReservation(HANDLE file,uint &period_milliseconds,uint &bytes_per_period,int &discardable,uint &transfer_size,uint &num_outstanding_requests);
int                    GetFileInformationByHandleEx(HANDLE file,FILE_INFO_BY_HANDLE_CLASS FileInformationClass,PVOID file_information,uint buffer_size);
int                    GetFileInformationByHandleEx(HANDLE file,FILE_INFO_BY_HANDLE_CLASS FileInformationClass,uchar &file_information[],uint buffer_size);
uint                   GetFirmwareEnvironmentVariableExW(const string name,const string guid,PVOID buffer,uint size,uint &attribubutes);
uint                   GetFirmwareEnvironmentVariableW(const string name,const string guid,PVOID buffer,uint size);
int                    GetFirmwareType(FIRMWARE_TYPE &FirmwareType);
uint                   GetFullPathNameTransactedW(const string file_name,uint buffer_length,string buffer,string &file_part,HANDLE transaction);
uint                   GetLongPathNameTransactedW(const string short_path,string long_path,uint buffer,HANDLE transaction);
int                    GetMailslotInfo(HANDLE mailslot,uint &max_message_size,uint &next_size,uint &message_count,uint &read_timeout);
uint                   GetMaximumProcessorCount(ushort GroupNumber);
ushort                 GetMaximumProcessorGroupCount(void);
int                    GetNamedPipeClientProcessId(HANDLE Pipe,uint &ClientProcessId);
int                    GetNamedPipeClientSessionId(HANDLE Pipe,uint &ClientSessionId);
int                    GetNamedPipeServerProcessId(HANDLE Pipe,uint &ServerProcessId);
int                    GetNamedPipeServerSessionId(HANDLE Pipe,uint &ServerSessionId);
PVOID                  GetNextUmsListItem(PVOID UmsContext);
int                    GetNumaAvailableMemoryNode(uchar Node,ulong &AvailableBytes);
int                    GetNumaAvailableMemoryNodeEx(ushort Node,ulong &AvailableBytes);
int                    GetNumaNodeNumberFromHandle(HANDLE file,ushort &NodeNumber);
int                    GetNumaNodeProcessorMask(uchar Node,ulong &ProcessorMask);
int                    GetNumaProcessorNode(uchar Processor,uchar &NodeNumber);
int                    GetNumaProcessorNodeEx(PROCESSOR_NUMBER &Processor,ushort &NodeNumber);
int                    GetNumaProximityNode(uint ProximityId,uchar &NodeNumber);
uint                   GetPrivateProfileIntW(const string app_name,const string key_name,int default_value,const string file_name);
uint                   GetPrivateProfileSectionNamesW(string return_buffer,uint size,const string file_name);
uint                   GetPrivateProfileSectionW(const string app_name,string returned_string,uint size,const string file_name);
uint                   GetPrivateProfileStringW(const string app_name,const string key_name,const string default_value,string returned_string,uint size,const string file_name);
int                    GetPrivateProfileStructW(const string section,const string key,PVOID struct_obj,uint size_struct,const string file);
int                    GetProcessAffinityMask(HANDLE process,ulong &process_affinity_mask,ulong &system_affinity_mask);
int                    GetProcessDEPPolicy(HANDLE process,uint &flags,int &permanent);
int                    GetProcessIoCounters(HANDLE process,IO_COUNTERS &io_counters);
int                    GetProcessWorkingSetSize(HANDLE process,ulong &minimum_working_set_size,ulong &maximum_working_set_size);
uint                   GetProfileIntW(const string app_name,const string key_name,int default_value);
uint                   GetProfileSectionW(const string app_name,string returned_string,uint size);
uint                   GetProfileStringW(const string app_name,const string key_name,const string default_value,string returned_string,uint size);
DEP_SYSTEM_POLICY_TYPE GetSystemDEPPolicy(void);
int                    GetSystemPowerStatus(SYSTEM_POWER_STATUS &system_power_status);
int                    GetSystemRegistryQuota(uint &quota_allowed,uint &quota_used);
uint                   GetTapeParameters(HANDLE device,uint operation,uint &size,PVOID tape_information);
uint                   GetTapePosition(HANDLE device,uint position_type,uint &partition,uint &offset_low,uint &offset_high);
uint                   GetTapeStatus(HANDLE device);
int                    GetThreadSelectorEntry(HANDLE thread,uint selector,LDT_ENTRY &selector_entry);
int                    GetUmsCompletionListEvent(PVOID UmsCompletionList,HANDLE &UmsCompletionEvent);
int                    GetUmsSystemThreadInformation(HANDLE ThreadHandle,UMS_SYSTEM_THREAD_INFORMATION &SystemThreadInfo);
int                    GetXStateFeaturesMask(CONTEXT &Context,ulong &FeatureMask);
ushort                 GlobalAddAtomExW(const string str,uint Flags);
ushort                 GlobalAddAtomW(const string str);
HANDLE                 GlobalAlloc(uint flags,ulong bytes);
ulong                  GlobalCompact(uint min_free);
ushort                 GlobalDeleteAtom(ushort atom);
ushort                 GlobalFindAtomW(const string str);
void                   GlobalFix(HANDLE mem);
uint                   GlobalFlags(HANDLE mem);
HANDLE                 GlobalFree(HANDLE mem);
uint                   GlobalGetAtomNameW(ushort atom,ushort &buffer[],int size);
HANDLE                 GlobalHandle(const PVOID mem);
PVOID                  GlobalLock(HANDLE mem);
void                   GlobalMemoryStatus(MEMORYSTATUS &buffer);
HANDLE                 GlobalReAlloc(HANDLE mem,ulong bytes,uint flags);
ulong                  GlobalSize(HANDLE mem);
void                   GlobalUnfix(HANDLE mem);
int                    GlobalUnlock(HANDLE mem);
int                    GlobalUnWire(HANDLE mem);
PVOID                  GlobalWire(HANDLE mem);
int                    InitAtomTable(uint size);
int                    InitializeContext(uchar &Buffer[],uint ContextFlags,CONTEXT &Context,uint &ContextLength);
int                    InitializeContext(PVOID Buffer,uint ContextFlags,CONTEXT &Context,uint &ContextLength);
int                    IsBadCodePtr(PVOID lpfn);
int                    IsBadHugeReadPtr(PVOID lp,ulong ucb);
int                    IsBadHugeWritePtr(PVOID lp,ulong ucb);
int                    IsBadReadPtr(PVOID lp,ulong ucb);
int                    IsBadStringPtrW(const string lpsz,ulong max);
int                    IsBadWritePtr(PVOID lp,ulong ucb);
int                    IsNativeVhdBoot(int &NativeVhdBoot);
int                    IsSystemResumeAutomatic(void);
HANDLE                 LoadPackagedLibrary(const string lib_file_name,uint Reserved);
HANDLE                 LocalAlloc(uint flags,ulong bytes);
ulong                  LocalCompact(uint min_free);
uint                   LocalFlags(HANDLE mem);
HANDLE                 LocalFree(HANDLE mem);
HANDLE                 LocalHandle(const PVOID mem);
PVOID                  LocalLock(HANDLE mem);
HANDLE                 LocalReAlloc(HANDLE mem,ulong bytes,uint flags);
ulong                  LocalShrink(HANDLE mem,uint new_size);
ulong                  LocalSize(HANDLE mem);
int                    LocalUnlock(HANDLE mem);
PVOID                  LocateXStateFeature(CONTEXT &Context,uint FeatureId,uint &Length);
string                 lstrcatW(ushort &string1[],const string string2);
int                    lstrcmpiW(const string string1,const string string2);
int                    lstrcmpW(const string string1,const string string2);
string                 lstrcpynW(ushort &string1[],const string string2,int max_length);
string                 lstrcpyW(ushort &string1[],const string string2);
int                    lstrlenW(const string str);
int                    MapUserPhysicalPagesScatter(PVOID &VirtualAddresses[],ulong NumberOfPages,ulong &PageArray[]);
PVOID                  MapViewOfFileExNuma(HANDLE file_mapping_object,uint desired_access,uint file_offset_high,uint file_offset_low,ulong number_of_bytes_to_map,PVOID base_address,uint preferred);
int                    MoveFileExW(const string existing_file_name,const string new_file_name,uint flags);
int                    MoveFileTransactedW(const string existing_file_name,const string new_file_name,PVOID progress_routine,PVOID data,uint flags,HANDLE transaction);
int                    MoveFileW(const string existing_file_name,const string new_file_name);
int                    MoveFileWithProgressW(const string existing_file_name,const string new_file_name,PVOID progress_routine,PVOID data,uint flags);
int                    MulDiv(int number,int numerator,int denominator);
HANDLE                 OpenFileById(HANDLE volume_hint,FILE_ID_DESCRIPTOR &file_id,uint desired_access,uint share_mode,PVOID security_attributes,uint flags_and_attributes);
int                    PowerClearRequest(HANDLE PowerRequest,POWER_REQUEST_TYPE RequestType);
HANDLE                 PowerCreateRequest(REASON_CONTEXT &Context);
int                    PowerSetRequest(HANDLE PowerRequest,POWER_REQUEST_TYPE RequestType);
uint                   PrepareTape(HANDLE device,uint operation,int immediate);
int                    PulseEvent(HANDLE event);
int                    PurgeComm(HANDLE file,uint flags);
int                    QueryActCtxSettingsW(uint flags,HANDLE act_ctx,const string name_space,const string name,string buffer,ulong buffer,ulong &written_or_required);
int                    QueryActCtxSettingsW(uint flags,ACTCTXW &act_ctx,const string name_space,const string name,string buffer,ulong buffer,ulong &written_or_required);
int                    QueryActCtxW(uint flags,HANDLE act_ctx,PVOID sub_instance,uint info_class,PVOID buffer,ulong buffer,ulong &written_or_required);
int                    QueryActCtxW(uint flags,ACTCTXW &act_ctx,PVOID sub_instance,uint info_class,PVOID buffer,ulong buffer,ulong &written_or_required);
int                    QueryFullProcessImageNameW(HANDLE process,uint flags,string exe_name,uint &size);
uint                   QueryThreadProfiling(HANDLE ThreadHandle,uchar &Enabled);
int                    QueryUmsThreadInformation(PVOID UmsThread,RTL_UMS_THREAD_INFO_CLASS UmsThreadInfoClass,PVOID UmsThreadInformation,uint UmsThreadInformationLength,uint &ReturnLength);
int                    ReadDirectoryChangesExW(HANDLE directory,PVOID buffer,uint buffer_length,int watch_subtree,uint notify_filter,uint &bytes_returned,OVERLAPPED &overlapped,PVOID completion_routine,READ_DIRECTORY_NOTIFY_INFORMATION_CLASS ReadDirectoryNotifyInformationClass);
int                    ReadDirectoryChangesW(HANDLE directory,PVOID buffer,uint buffer_length,int watch_subtree,uint notify_filter,uint &bytes_returned,OVERLAPPED &overlapped,PVOID completion_routine);
uint                   ReadThreadProfilingData(HANDLE PerformanceDataHandle,uint Flags,PERFORMANCE_DATA &PerformanceData);
int                    RegisterApplicationRecoveryCallback(PVOID recovey_callback,PVOID parameter,uint ping_interval,uint flags);
int                    RegisterApplicationRestart(const string commandline,uint flags);
int                    RegisterWaitForSingleObject(HANDLE &new_wait_object,HANDLE object,PVOID Callback,PVOID Context,uint milliseconds,uint flags);
void                   ReleaseActCtx(HANDLE act_ctx);
void                   ReleaseActCtx(ACTCTXW &act_ctx);
int                    RemoveDirectoryTransactedW(const string path_name,HANDLE transaction);
int                    RemoveSecureMemoryCacheCallback(PVOID call_back);
HANDLE                 ReOpenFile(HANDLE original_file,uint desired_access,uint share_mode,uint flags_and_attributes);
int                    ReplaceFileW(const string replaced_file_name,const string replacement_file_name,const string backup_file_name,uint replace_flags,PVOID exclude,PVOID reserved);
int                    ReplacePartitionUnit(string TargetPartition,string SparePartition,uint Flags);
int                    RequestDeviceWakeup(HANDLE device);
int                    RequestWakeupLatency(LATENCY_TIME latency);
void                   RestoreLastError(uint err_code);
int                    SetCommBreak(HANDLE file);
int                    SetCommConfig(HANDLE comm_dev,COMMCONFIG &lpCC,uint size);
int                    SetCommMask(HANDLE file,uint evt_mask);
int                    SetCommState(HANDLE file,DCB &lpDCB);
int                    SetCommTimeouts(HANDLE file,COMMTIMEOUTS &comm_timeouts);
int                    SetDefaultCommConfigW(const string name,COMMCONFIG &lpCC,uint size);
int                    SetDllDirectoryW(const string path_name);
int                    SetFileAttributesTransactedW(const string file_name,uint file_attributes,HANDLE transaction);
int                    SetFileBandwidthReservation(HANDLE file,uint period_milliseconds,uint bytes_per_period,int discardable,uint &transfer_size,uint &num_outstanding_requests);
int                    SetFileCompletionNotificationModes(HANDLE FileHandle,uchar Flags);
int                    SetFileShortNameW(HANDLE file,const string short_name);
int                    SetFirmwareEnvironmentVariableExW(const string name,const string guid,PVOID value,uint size,uint attributes);
int                    SetFirmwareEnvironmentVariableW(const string name,const string guid,PVOID value,uint size);
uint                   SetHandleCount(uint number);
int                    SetMailslotInfo(HANDLE mailslot,uint read_timeout);
int                    SetMessageWaitingIndicator(HANDLE msg_indicator,uint msg_count);
int                    SetProcessAffinityMask(HANDLE process,PVOID process_affinity_mask);
int                    SetProcessDEPPolicy(uint flags);
int                    SetProcessWorkingSetSize(HANDLE process,ulong minimum_working_set_size,ulong maximum_working_set_size);
int                    SetSearchPathMode(uint flags);
int                    SetSystemPowerState(int suspend,int force);
uint                   SetTapeParameters(HANDLE device,uint operation,PVOID tape_information);
uint                   SetTapePosition(HANDLE device,uint position_method,uint partition,uint offset_low,uint offset_high,int immediate);
PVOID                  SetThreadAffinityMask(HANDLE thread,PVOID thread_affinity_mask);
uint                   SetThreadExecutionState(uint flags);
HANDLE                 SetTimerQueueTimer(HANDLE TimerQueue,PVOID Callback,PVOID Parameter,uint DueTime,uint Period,int PreferIo);
int                    SetUmsThreadInformation(PVOID UmsThread,RTL_UMS_THREAD_INFO_CLASS UmsThreadInfoClass,PVOID UmsThreadInformation,uint UmsThreadInformationLength);
int                    SetupComm(HANDLE file,uint in_queue,uint out_queue);
int                    SetVolumeLabelW(const string root_path_name,const string volume_name);
int                    SetVolumeMountPointW(const string volume_mount_point,const string volume_name);
int                    SetXStateFeaturesMask(CONTEXT &Context,ulong FeatureMask);
uint                   SignalObjectAndWait(HANDLE object_to_signal,HANDLE object_to_wait_on,uint milliseconds,int alertable);
void                   SwitchToFiber(PVOID fiber);
int                    TransmitCommChar(HANDLE file,char symbol);
int                    UmsThreadYield(PVOID SchedulerParam);
int                    UnregisterApplicationRecoveryCallback(void);
int                    UnregisterApplicationRestart(void);
int                    UnregisterWait(HANDLE WaitHandle);
int                    UpdateResourceW(HANDLE update,const string type,const string name,ushort &language,PVOID data,uint cb);
int                    VerifyVersionInfoW(OSVERSIONINFOEXW &version_information,uint type_mask,ulong condition_mask);
int                    WaitCommEvent(HANDLE file,uint &evt_mask,OVERLAPPED &overlapped);
uchar                  Wow64EnableWow64FsRedirection(uchar Wow64FsEnableRedirection);
int                    Wow64GetThreadContext(HANDLE thread,CONTEXT &context);
int                    Wow64GetThreadSelectorEntry(HANDLE thread,uint selector,LDT_ENTRY &selector_entry);
int                    Wow64SetThreadContext(HANDLE thread,CONTEXT &context);
uint                   Wow64SuspendThread(HANDLE thread);
int                    WritePrivateProfileSectionW(const string app_name,const string str,const string file_name);
int                    WritePrivateProfileStringW(const string app_name,const string key_name,const string str,const string file_name);
int                    WritePrivateProfileStructW(const string section,const string key,PVOID struct_obj,uint size_struct,const string file);
int                    WriteProfileSectionW(const string app_name,const string str);
int                    WriteProfileStringW(const string app_name,const string key_name,const string str);
uint                   WriteTapemark(HANDLE device,uint tapemark_type,uint tapemark_count,int immediate);
uint                   WTSGetActiveConsoleSessionId(void);
int                    ZombifyActCtx(HANDLE act_ctx);
int                    ZombifyActCtx(ACTCTXW &act_ctx);
#import

#import "advapi32.dll"
int                    AddConditionalAce(ACL &acl,uint ace_revision,uint AceFlags,uchar AceType,uint AccessMask,SID &sid,string ConditionStr,uint &ReturnLength);
int                    BackupEventLogW(HANDLE event_log,const string backup_file_name);
int                    ClearEventLogW(HANDLE event_log,const string backup_file_name);
void                   CloseEncryptedFileRaw(PVOID context);
int                    CloseEventLog(HANDLE event_log);
int                    DecryptFileW(const string file_name,uint reserved);
int                    DeregisterEventSource(HANDLE event_log);
int                    EncryptFileW(const string file_name);
int                    FileEncryptionStatusW(const string file_name,uint &status);
int                    GetCurrentHwProfileW(HW_PROFILE_INFOW &hw_profile_info);
int                    GetEventLogInformation(HANDLE event_log,uint info_level,PVOID buffer,uint buf_size,uint &bytes_needed);
int                    GetNumberOfEventLogRecords(HANDLE event_log,uint &NumberOfRecords);
int                    GetOldestEventLogRecord(HANDLE event_log,uint &OldestRecord);
int                    GetUserNameW(string buffer,uint &buffer);
int                    IsTextUnicode(PVOID lpv,int size,int &result);
int                    IsTokenUntrusted(HANDLE TokenHandle);
int                    LogonUserExW(const string username,const string domain,const string password,uint logon_type,uint logon_provider,HANDLE &token,PVOID &logon_sid,PVOID &profile_buffer,uint &profile_length,QUOTA_LIMITS &quota_limits);
int                    LogonUserW(const string username,const string domain,const string password,uint logon_type,uint logon_provider,HANDLE &token);
int                    LookupAccountNameW(const string system_name,const string account_name,SID &Sid,uint &sid,string ReferencedDomainName,uint &referenced_domain_name,SID_NAME_USE &use);
int                    LookupAccountSidW(const string system_name,SID &Sid,string Name,uint &name,string ReferencedDomainName,uint &referenced_domain_name,SID_NAME_USE &use);
int                    LookupPrivilegeDisplayNameW(const string system_name,const string name,string display_name,uint &display_name,uint &language_id);
int                    LookupPrivilegeNameW(const string system_name,LUID &luid,string name,uint &name);
int                    LookupPrivilegeValueW(const string system_name,const string name,LUID &luid);
int                    NotifyChangeEventLog(HANDLE event_log,HANDLE event);
HANDLE                 OpenBackupEventLogW(const string lpUNCServerName,const string file_name);
uint                   OpenEncryptedFileRawW(const string file_name,uint flags,PVOID &context);
HANDLE                 OpenEventLogW(const string lpUNCServerName,const string source_name);
int                    OperationEnd(OPERATION_END_PARAMETERS &OperationEndParams);
int                    OperationStart(OPERATION_START_PARAMETERS &OperationStartParams);
uint                   ReadEncryptedFileRaw(PVOID export_callback,PVOID callback_context,PVOID context);
int                    ReadEventLogW(HANDLE event_log,uint read_flags,uint record_offset,PVOID buffer,uint number_of_bytes_to_read,uint &bytes_read,uint &min_number_of_bytes_needed);
HANDLE                 RegisterEventSourceW(const string lpUNCServerName,const string source_name);
int                    ReportEventW(HANDLE event_log,ushort &type,ushort &category,uint dwEventID,SID &user_sid,ushort &num_strings,uint data_size,const string &strings[],PVOID raw_data);
uint                   WriteEncryptedFileRaw(PVOID import_callback,PVOID callback_context,PVOID context);
#import
//+------------------------------------------------------------------+
