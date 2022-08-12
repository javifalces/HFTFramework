//+------------------------------------------------------------------+
//|                                                        winnt.mqh |
//|                        Copyright 2018, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#include <WinAPI\windef.mqh>

//---
#define UNWIND_HISTORY_TABLE_SIZE            12
#define SIZE_OF_80387_REGISTERS              80
#define MAXIMUM_SUPPORTED_EXTENSION          512
#define WOW64_SIZE_OF_80387_REGISTERS        80
#define WOW64_MAXIMUM_SUPPORTED_EXTENSION    512
#define SID_HASH_SIZE                        32
#define POLICY_AUDIT_SUBCATEGORY_COUNT       59
#define TOKEN_SOURCE_LENGTH                  8
#define MAXIMUM_XSTATE_FEATURES              64
#define POWER_SYSTEM_MAXIMUM                 7
#define NUM_DISCHARGE_POLICIES               4
#define HIBERFILE_TYPE_MAX                   0x03
#define IMAGE_NUMBEROF_DIRECTORY_ENTRIES     16
#define IMAGE_SIZEOF_SHORT_NAME              8
#define IMAGE_ENCLAVE_LONG_ID_LENGTH         32
#define IMAGE_ENCLAVE_SHORT_ID_LENGTH        16
#define RTL_CORRELATION_VECTOR_STRING_LENGTH 129

//---
enum SID_NAME_USE
  {
   SidTypeUser=1,
   SidTypeGroup,
   SidTypeDomain,
   SidTypeAlias,
   SidTypeWellKnownGroup,
   SidTypeDeletedAccount,
   SidTypeInvalid,
   SidTypeUnknown,
   SidTypeComputer,
   SidTypeLabel,
   SidTypeLogonSession
  };
//---
enum ACL_INFORMATION_CLASS
  {
   AclRevisionInformation=1,
   AclSizeInformation
  };
//---
enum AUDIT_EVENT_TYPE
  {
   AuditEventObjectAccess,
   AuditEventDirectoryServiceAccess
  };
//---
enum ACCESS_REASON_TYPE
  {
   AccessReasonNone=0x00000000,
   AccessReasonAllowedAce=0x00010000,
   AccessReasonDeniedAce=0x00020000,
   AccessReasonAllowedParentAce=0x00030000,
   AccessReasonDeniedParentAce=0x00040000,
   AccessReasonNotGrantedByCape=0x00050000,
   AccessReasonNotGrantedByParentCape=0x00060000,
   AccessReasonNotGrantedToAppContainer=0x00070000,
   AccessReasonMissingPrivilege=0x00100000,
   AccessReasonFromPrivilege=0x00200000,
   AccessReasonIntegrityLevel=0x00300000,
   AccessReasonOwnership=0x00400000,
   AccessReasonNullDacl=0x00500000,
   AccessReasonEmptyDacl=0x00600000,
   AccessReasonNoSD=0x00700000,
   AccessReasonNoGrant=0x00800000,
   AccessReasonTrustLabel=0x00900000,
   AccessReasonFilterAce=0x00a00000
  };
//---
enum SECURITY_IMPERSONATION_LEVEL
  {
   SecurityAnonymous,
   SecurityIdentification,
   SecurityImpersonation,
   SecurityDelegation
  };
//---
enum TOKEN_TYPE
  {
   TokenPrimary=1,
   TokenImpersonation
  };
//---
enum TOKEN_ELEVATION_TYPE
  {
   TokenElevationTypeDefault=1,
   TokenElevationTypeFull,
   TokenElevationTypeLimited
  };
//---
enum TOKEN_INFORMATION_CLASS
  {
   TokenUser=1,
   TokenGroups,
   TokenPrivileges,
   TokenOwner,
   TokenPrimaryGroup,
   TokenDefaultDacl,
   TokenSource,
   TokenType,
   TokenImpersonationLevel,
   TokenStatistics,
   TokenRestrictedSids,
   TokenSessionId,
   TokenGroupsAndPrivileges,
   TokenSessionReference,
   TokenSandBoxInert,
   TokenAuditPolicy,
   TokenOrigin,
   TokenElevationType,
   TokenLinkedToken,
   TokenElevation,
   TokenHasRestrictions,
   TokenAccessInformation,
   TokenVirtualizationAllowed,
   TokenVirtualizationEnabled,
   TokenIntegrityLevel,
   TokenUIAccess,
   TokenMandatoryPolicy,
   TokenLogonSid,
   TokenIsAppContainer,
   TokenCapabilities,
   TokenAppContainerSid,
   TokenAppContainerNumber,
   TokenUserClaimAttributes,
   TokenDeviceClaimAttributes,
   TokenRestrictedUserClaimAttributes,
   TokenRestrictedDeviceClaimAttributes,
   TokenDeviceGroups,
   TokenRestrictedDeviceGroups,
   TokenSecurityAttributes,
   TokenIsRestricted,
   TokenProcessTrustLevel,
   TokenPrivateNameSpace,
   TokenSingletonAttributes,
   TokenBnoIsolation,
   TokenChildProcessFlags,
   MaxTokenInfoClass
  };
//---
enum MANDATORY_LEVEL
  {
   MandatoryLevelUntrusted=0,
   MandatoryLevelLow,
   MandatoryLevelMedium,
   MandatoryLevelHigh,
   MandatoryLevelSystem,
   MandatoryLevelSecureProcess,
   MandatoryLevelCount
  };
//---
enum SE_IMAGE_SIGNATURE_TYPE
  {
   SeImageSignatureNone=0,
   SeImageSignatureEmbedded,
   SeImageSignatureCache,
   SeImageSignatureCatalogCached,
   SeImageSignatureCatalogNotCached,
   SeImageSignatureCatalogHint,
   SeImageSignaturePackageCatalog
  };
//---
enum SE_LEARNING_MODE_DATA_TYPE
  {
   SeLearningModeInvalidType=0,
   SeLearningModeSettings,
   SeLearningModeMax
  };
//---
enum HARDWARE_COUNTER_TYPE
  {
   PMCCounter,
   MaxHardwareCounterType
  };
//---
enum PROCESS_MITIGATION_POLICY
  {
   ProcessDEPPolicy,
   ProcessASLRPolicy,
   ProcessDynamicCodePolicy,
   ProcessStrictHandleCheckPolicy,
   ProcessSystemCallDisablePolicy,
   ProcessMitigationOptionsMask,
   ProcessExtensionPointDisablePolicy,
   ProcessControlFlowGuardPolicy,
   ProcessSignaturePolicy,
   ProcessFontDisablePolicy,
   ProcessImageLoadPolicy,
   ProcessSystemCallFilterPolicy,
   ProcessPayloadRestrictionPolicy,
   ProcessChildProcessPolicy,
   MaxProcessMitigationPolicy
  };
//---
enum JOBOBJECT_RATE_CONTROL_TOLERANCE
  {
   ToleranceLow=1,
   ToleranceMedium,
   ToleranceHigh
  };
//---
enum JOBOBJECT_RATE_CONTROL_TOLERANCE_INTERVAL
  {
   ToleranceIntervalShort=1,
   ToleranceIntervalMedium,
   ToleranceIntervalLong
  };
//---
enum JOB_OBJECT_NET_RATE_CONTROL_FLAGS
  {
   JOB_OBJECT_NET_RATE_CONTROL_ENABLE=0x1,
   JOB_OBJECT_NET_RATE_CONTROL_MAX_BANDWIDTH=0x2,
   JOB_OBJECT_NET_RATE_CONTROL_DSCP_TAG=0x4,
   JOB_OBJECT_NET_RATE_CONTROL_VALID_FLAGS=0x7
  };
//---
enum JOB_OBJECT_IO_RATE_CONTROL_FLAGS
  {
   JOB_OBJECT_IO_RATE_CONTROL_ENABLE=0x1,
   JOB_OBJECT_IO_RATE_CONTROL_STANDALONE_VOLUME=0x2,
   JOB_OBJECT_IO_RATE_CONTROL_FORCE_UNIT_ACCESS_ALL=0x4,
   JOB_OBJECT_IO_RATE_CONTROL_FORCE_UNIT_ACCESS_ON_SOFT_CAP=0x8,
   JOB_OBJECT_IO_RATE_CONTROL_VALID_FLAGS=JOB_OBJECT_IO_RATE_CONTROL_ENABLE|
   JOB_OBJECT_IO_RATE_CONTROL_STANDALONE_VOLUME|
   JOB_OBJECT_IO_RATE_CONTROL_FORCE_UNIT_ACCESS_ALL|
   JOB_OBJECT_IO_RATE_CONTROL_FORCE_UNIT_ACCESS_ON_SOFT_CAP
  };
//---
enum JOBOBJECT_IO_ATTRIBUTION_CONTROL_FLAGS
  {
   JOBOBJECT_IO_ATTRIBUTION_CONTROL_ENABLE=0x1,
   JOBOBJECT_IO_ATTRIBUTION_CONTROL_DISABLE=0x2,
   JOBOBJECT_IO_ATTRIBUTION_CONTROL_VALID_FLAGS=0x3
  };
//---
enum JOBOBJECTINFOCLASS
  {
   JobObjectBasicAccountingInformation=1,
   JobObjectBasicLimitInformation,
   JobObjectBasicProcessIdList,
   JobObjectBasicUIRestrictions,
   JobObjectSecurityLimitInformation,
   JobObjectEndOfJobTimeInformation,
   JobObjectAssociateCompletionPortInformation,
   JobObjectBasicAndIoAccountingInformation,
   JobObjectExtendedLimitInformation,
   JobObjectJobSetInformation,
   JobObjectGroupInformation,
   JobObjectNotificationLimitInformation,
   JobObjectLimitViolationInformation,
   JobObjectGroupInformationEx,
   JobObjectCpuRateControlInformation,
   JobObjectCompletionFilter,
   JobObjectCompletionCounter,
   JobObjectReserved1Information=18,
   JobObjectReserved2Information,
   JobObjectReserved3Information,
   JobObjectReserved4Information,
   JobObjectReserved5Information,
   JobObjectReserved6Information,
   JobObjectReserved7Information,
   JobObjectReserved8Information,
   JobObjectReserved9Information,
   JobObjectReserved10Information,
   JobObjectReserved11Information,
   JobObjectReserved12Information,
   JobObjectReserved13Information,
   JobObjectReserved14Information=31,
   JobObjectNetRateControlInformation,
   JobObjectNotificationLimitInformation2,
   JobObjectLimitViolationInformation2,
   JobObjectCreateSilo,
   JobObjectSiloBasicInformation,
   JobObjectReserved15Information=37,
   JobObjectReserved16Information=38,
   JobObjectReserved17Information=39,
   JobObjectReserved18Information=40,
   JobObjectReserved19Information=41,
   JobObjectReserved20Information=42,
   JobObjectReserved21Information=43,
   JobObjectReserved22Information=44,
   JobObjectReserved23Information=45,
   JobObjectReserved24Information=46,
   JobObjectReserved25Information=47,
   MaxJobObjectInfoClass
  };
//---
enum SERVERSILO_STATE
  {
   SERVERSILO_INITING=0,
   SERVERSILO_STARTED,
   SERVERSILO_SHUTTING_DOWN,
   SERVERSILO_TERMINATING,
   SERVERSILO_TERMINATED
  };
//---
enum FIRMWARE_TYPE
  {
   FirmwareTypeUnknown,
   FirmwareTypeBios,
   FirmwareTypeUefi,
   FirmwareTypeMax
  };
//---
enum LOGICAL_PROCESSOR_RELATIONSHIP
  {
   RelationProcessorCore,
   RelationNumaNode,
   RelationCache,
   RelationProcessorPackage,
   RelationGroup,
   RelationAll=0xffff
  };
//---
enum PROCESSOR_CACHE_TYPE
  {
   CacheUnified,
   CacheInstruction,
   CacheData,
   CacheTrace
  };
//---
enum CPU_SET_INFORMATION_TYPE
  {
   CpuSetInformation
  };
//---
enum MEM_EXTENDED_PARAMETER_TYPE
  {
   MemExtendedParameterInvalidType=0,
   MemExtendedParameterAddressRequirements,
   MemExtendedParameterNumaNode,
   MemExtendedParameterPartitionHandle,
   MemExtendedParameterMax
  };
//---
enum SharedVirtualDiskSupportType
  {
   SharedVirtualDisksUnsupported=0,
   SharedVirtualDisksSupported=1,
   SharedVirtualDiskSnapshotsSupported=3,
   SharedVirtualDiskCDPSnapshotsSupported=7
  };
//---
enum SharedVirtualDiskHandleState
  {
   SharedVirtualDiskHandleStateNone=0,
   SharedVirtualDiskHandleStateFileShared=1,
   SharedVirtualDiskHandleStateHandleShared=3
  };
//---
enum SYSTEM_POWER_STATE
  {
   PowerSystemUnspecified=0,
   PowerSystemWorking=1,
   PowerSystemSleeping1=2,
   PowerSystemSleeping2=3,
   PowerSystemSleeping3=4,
   PowerSystemHibernate=5,
   PowerSystemShutdown=6,
   PowerSystemMaximum=7
  };
//---
enum DEVICE_POWER_STATE
  {
   PowerDeviceUnspecified=0,
   PowerDeviceD0,
   PowerDeviceD1,
   PowerDeviceD2,
   PowerDeviceD3,
   PowerDeviceMaximum
  };
//---
enum MONITOR_DISPLAY_STATE
  {
   PowerMonitorOff=0,
   PowerMonitorOn,
   PowerMonitorDim
  };
//---
enum USER_ACTIVITY_PRESENCE
  {
   PowerUserPresent=0,
   PowerUserNotPresent,
   PowerUserInactive,
   PowerUserMaximum,
   PowerUserInvalid=PowerUserMaximum
  };
//---
enum POWER_REQUEST_TYPE
  {
   PowerRequestDisplayRequired,
   PowerRequestSystemRequired,
   PowerRequestAwayModeRequired,
   PowerRequestExecutionRequired
  };
//---
enum POWER_MONITOR_REQUEST_TYPE
  {
   MonitorRequestTypeOff,
   MonitorRequestTypeOnAndPresent,
   MonitorRequestTypeToggleOn
  };
//---
enum POWER_PLATFORM_ROLE
  {
   PlatformRoleUnspecified=0,
   PlatformRoleDesktop,
   PlatformRoleMobile,
   PlatformRoleWorkstation,
   PlatformRoleEnterpriseServer,
   PlatformRoleSOHOServer,
   PlatformRoleAppliancePC,
   PlatformRolePerformanceServer,
   PlatformRoleSlate,
   PlatformRoleMaximum
  };
//---
enum HIBERFILE_BUCKET_SIZE
  {
   HiberFileBucket1GB=0,
   HiberFileBucket2GB,
   HiberFileBucket4GB,
   HiberFileBucket8GB,
   HiberFileBucket16GB,
   HiberFileBucket32GB,
   HiberFileBucketUnlimited,
   HiberFileBucketMax
  };
//---
enum IMAGE_AUX_SYMBOL_TYPE
  {
   IMAGE_AUX_SYMBOL_TYPE_TOKEN_DEF=1
  };
//---
enum IMPORT_OBJECT_TYPE
  {
   IMPORT_OBJECT_CODE=0,
   IMPORT_OBJECT_DATA=1,
   IMPORT_OBJECT_CONST=2
  };
//---
enum IMPORT_OBJECT_NAME_TYPE
  {
   IMPORT_OBJECT_ORDINAL=0,
   IMPORT_OBJECT_NAME=1,
   IMPORT_OBJECT_NAME_NO_PREFIX=2,
   IMPORT_OBJECT_NAME_UNDECORATE=3,
   IMPORT_OBJECT_NAME_EXPORTAS=4
  };
//---
enum ReplacesCorHdrNumericDefines
  {
   COMIMAGE_FLAGS_ILONLY=0x00000001,
   COMIMAGE_FLAGS_32BITREQUIRED=0x00000002,
   COMIMAGE_FLAGS_IL_LIBRARY=0x00000004,
   COMIMAGE_FLAGS_STRONGNAMESIGNED=0x00000008,
   COMIMAGE_FLAGS_NATIVE_ENTRYPOINT=0x00000010,
   COMIMAGE_FLAGS_TRACKDEBUGDATA=0x00010000,
   COMIMAGE_FLAGS_32BITPREFERRED=0x00020000,
   COR_VERSION_MAJOR_V2=2,
   COR_VERSION_MAJOR=COR_VERSION_MAJOR_V2,
   COR_VERSION_MINOR=5,
   COR_DELETED_NAME_LENGTH=8,
   COR_VTABLEGAP_NAME_LENGTH=8,
   NATIVE_TYPE_MAX_CB=1,
   COR_ILMETHOD_SECT_SMALL_MAX_DATASIZE=0xFF,
   IMAGE_COR_MIH_METHODRVA=0x01,
   IMAGE_COR_MIH_EHRVA=0x02,
   IMAGE_COR_MIH_BASICBLOCK=0x08,
   COR_VTABLE_32BIT=0x01,
   COR_VTABLE_64BIT=0x02,
   COR_VTABLE_FROM_UNMANAGED=0x04,
   COR_VTABLE_FROM_UNMANAGED_RETAIN_APPDOMAIN=0x08,
   COR_VTABLE_CALL_MOST_DERIVED=0x10,
   IMAGE_COR_EATJ_THUNK_SIZE=32,
   MAX_CLASS_NAME=1024,
   MAX_PACKAGE_NAME=1024
  };
//---
enum RTL_UMS_THREAD_INFO_CLASS
  {
   UmsThreadInvalidInfoClass=0,
   UmsThreadUserContext,
   UmsThreadPriority,
   UmsThreadAffinity,
   UmsThreadTeb,
   UmsThreadIsSuspended,
   UmsThreadIsTerminated,
   UmsThreadMaxInfoClass
  };
//---
enum RTL_UMS_SCHEDULER_REASON
  {
   UmsSchedulerStartup=0,
   UmsSchedulerThreadBlocked,
   UmsSchedulerThreadYield
  };
//---
enum OS_DEPLOYEMENT_STATE_VALUES
  {
   OS_DEPLOYMENT_STANDARD=1,
   OS_DEPLOYMENT_COMPACT
  };
//---
enum IMAGE_POLICY_ENTRY_TYPE
  {
   ImagePolicyEntryTypeNone=0,
   ImagePolicyEntryTypeBool,
   ImagePolicyEntryTypeInt8,
   ImagePolicyEntryTypeUInt8,
   ImagePolicyEntryTypeInt16,
   ImagePolicyEntryTypeUInt16,
   ImagePolicyEntryTypeInt32,
   ImagePolicyEntryTypeUInt32,
   ImagePolicyEntryTypeInt64,
   ImagePolicyEntryTypeUInt64,
   ImagePolicyEntryTypeAnsiString,
   ImagePolicyEntryTypeUnicodeString,
   ImagePolicyEntryTypeOverride,
   ImagePolicyEntryTypeMaximum
  };
//---
enum IMAGE_POLICY_ID
  {
   ImagePolicyIdNone=0,
   ImagePolicyIdEtw,
   ImagePolicyIdDebug,
   ImagePolicyIdCrashDump,
   ImagePolicyIdCrashDumpKey,
   ImagePolicyIdCrashDumpKeyGuid,
   ImagePolicyIdParentSd,
   ImagePolicyIdParentSdRev,
   ImagePolicyIdSvn,
   ImagePolicyIdDeviceId,
   ImagePolicyIdCapability,
   ImagePolicyIdScenarioId,
   ImagePolicyIdMaximum
  };
//---
enum HEAP_INFORMATION_CLASS
  {
   HeapCompatibilityInformation=0,
   HeapEnableTerminationOnCorruption=1,
   HeapOptimizeResources=3
  };
//---
enum ACTIVATION_CONTEXT_INFO_CLASS
  {
   ActivationContextBasicInformation=1,
   ActivationContextDetailedInformation=2,
   AssemblyDetailedInformationInActivationContext=3,
   FileInformationInAssemblyOfAssemblyInActivationContext=4,
   RunlevelInformationInActivationContext=5,
   CompatibilityInformationInActivationContext=6,
   ActivationContextManifestResourceName=7,
   MaxActivationContextInfoClass,
   AssemblyDetailedInformationInActivationContxt=3,
   FileInformationInAssemblyOfAssemblyInActivationContxt=4
  };
//---
enum SERVICE_NODE_TYPE
  {
   DriverType=0x00000001,
   FileSystemType=0x00000002,
   Win32ServiceOwnProcess=0x00000010,
   Win32ServiceShareProcess=0x00000020,
   AdapterType=0x00000004,
   RecognizerType=0x00000008
  };
//---
enum SERVICE_LOAD_TYPE
  {
   BootLoad=0x00000000,
   SystemLoad=0x00000001,
   AutoLoad=0x00000002,
   DemandLoad=0x00000003,
   DisableLoad=0x00000004
  };
//---
enum SERVICE_ERROR_TYPE
  {
   IgnoreError=0x00000000,
   NormalError=0x00000001,
   SevereError=0x00000002,
   CriticalError=0x00000003
  };
//---
enum TAPE_DRIVE_PROBLEM_TYPE
  {
   TapeDriveProblemNone,
   TapeDriveReadWriteWarning,
   TapeDriveReadWriteError,
   TapeDriveReadWarning,
   TapeDriveWriteWarning,
   TapeDriveReadError,
   TapeDriveWriteError,
   TapeDriveHardwareError,
   TapeDriveUnsupportedMedia,
   TapeDriveScsiConnectionError,
   TapeDriveTimetoClean,
   TapeDriveCleanDriveNow,
   TapeDriveMediaLifeExpired,
   TapeDriveSnappedTape
  };
//---
enum TRANSACTION_OUTCOME
  {
   TransactionOutcomeUndetermined=1,
   TransactionOutcomeCommitted,
   TransactionOutcomeAborted
  };
//---
enum TRANSACTION_STATE
  {
   TransactionStateNormal=1,
   TransactionStateIndoubt,
   TransactionStateCommittedNotify
  };
//---
enum TRANSACTION_INFORMATION_CLASS
  {
   TransactionBasicInformation,
   TransactionPropertiesInformation,
   TransactionEnlistmentInformation,
   TransactionSuperiorEnlistmentInformation,
   TransactionBindInformation,
   TransactionDTCPrivateInformation
  };
//---
enum TRANSACTIONMANAGER_INFORMATION_CLASS
  {
   TransactionManagerBasicInformation,
   TransactionManagerLogInformation,
   TransactionManagerLogPathInformation,
   TransactionManagerRecoveryInformation=4,
   TransactionManagerOnlineProbeInformation=3,
   TransactionManagerOldestTransactionInformation=5
  };
//---
enum RESOURCEMANAGER_INFORMATION_CLASS
  {
   ResourceManagerBasicInformation,
   ResourceManagerCompletionInformation
  };
//---
enum ENLISTMENT_INFORMATION_CLASS
  {
   EnlistmentBasicInformation,
   EnlistmentRecoveryInformation,
   EnlistmentCrmInformation
  };
//---
enum KTMOBJECT_TYPE
  {
   KTMOBJECT_TRANSACTION,
   KTMOBJECT_TRANSACTION_MANAGER,
   KTMOBJECT_RESOURCE_MANAGER,
   KTMOBJECT_ENLISTMENT,
   KTMOBJECT_INVALID
  };
//---
enum TP_CALLBACK_PRIORITY
  {
   TP_CALLBACK_PRIORITY_HIGH,
   TP_CALLBACK_PRIORITY_NORMAL,
   TP_CALLBACK_PRIORITY_LOW,
   TP_CALLBACK_PRIORITY_INVALID,
   TP_CALLBACK_PRIORITY_COUNT=TP_CALLBACK_PRIORITY_INVALID
  };
//---
enum POWER_USER_PRESENCE_TYPE
  {
   UserNotPresent=0,
   UserPresent=1,
   UserUnknown=0xff
  };
//---
enum POWER_MONITOR_REQUEST_REASON
  {
   MonitorRequestReasonUnknown,
   MonitorRequestReasonPowerButton,
   MonitorRequestReasonRemoteConnection,
   MonitorRequestReasonScMonitorpower,
   MonitorRequestReasonUserInput,
   MonitorRequestReasonAcDcDisplayBurst,
   MonitorRequestReasonUserDisplayBurst,
   MonitorRequestReasonPoSetSystemState,
   MonitorRequestReasonSetThreadExecutionState,
   MonitorRequestReasonFullWake,
   MonitorRequestReasonSessionUnlock,
   MonitorRequestReasonScreenOffRequest,
   MonitorRequestReasonIdleTimeout,
   MonitorRequestReasonPolicyChange,
   MonitorRequestReasonSleepButton,
   MonitorRequestReasonLid,
   MonitorRequestReasonBatteryCountChange,
   MonitorRequestReasonGracePeriod,
   MonitorRequestReasonPnP,
   MonitorRequestReasonDP,
   MonitorRequestReasonSxTransition,
   MonitorRequestReasonSystemIdle,
   MonitorRequestReasonNearProximity,
   MonitorRequestReasonThermalStandby,
   MonitorRequestReasonResumePdc,
   MonitorRequestReasonResumeS4,
   MonitorRequestReasonTerminal,
   MonitorRequestReasonPdcSignal,
   MonitorRequestReasonAcDcDisplayBurstSuppressed,
   MonitorRequestReasonSystemStateEntered,
   MonitorRequestReasonWinrt,
   MonitorRequestReasonUserInputKeyboard,
   MonitorRequestReasonUserInputMouse,
   MonitorRequestReasonUserInputTouch,
   MonitorRequestReasonUserInputPen,
   MonitorRequestReasonUserInputAccelerometer,
   MonitorRequestReasonUserInputHid,
   MonitorRequestReasonUserInputPoUserPresent,
   MonitorRequestReasonUserInputSessionSwitch,
   MonitorRequestReasonUserInputInitialization,
   MonitorRequestReasonPdcSignalWindowsMobilePwrNotif,
   MonitorRequestReasonPdcSignalWindowsMobileShell,
   MonitorRequestReasonPdcSignalHeyCortana,
   MonitorRequestReasonPdcSignalHolographicShell,
   MonitorRequestReasonPdcSignalFingerprint,
   MonitorRequestReasonMax
  };
//---
enum POWER_ACTION
  {
   PowerActionNone=0,
   PowerActionReserved,
   PowerActionSleep,
   PowerActionHibernate,
   PowerActionShutdown,
   PowerActionShutdownReset,
   PowerActionShutdownOff,
   PowerActionWarmEject,
   PowerActionDisplayOff
  };
//---
enum ACTCTX_REQUESTED_RUN_LEVEL
  {
   ACTCTX_RUN_LEVEL_UNSPECIFIED=0,
   ACTCTX_RUN_LEVEL_AS_INVOKER,
   ACTCTX_RUN_LEVEL_HIGHEST_AVAILABLE,
   ACTCTX_RUN_LEVEL_REQUIRE_ADMIN,
   ACTCTX_RUN_LEVEL_NUMBERS
  };
//---
enum ACTCTX_COMPATIBILITY_ELEMENT_TYPE
  {
   ACTCTX_COMPATIBILITY_ELEMENT_TYPE_UNKNOWN=0,
   ACTCTX_COMPATIBILITY_ELEMENT_TYPE_OS,
   ACTCTX_COMPATIBILITY_ELEMENT_TYPE_MITIGATION
  };
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
//---
struct PROCESSOR_NUMBER
  {
   ushort            Group;
   uchar             Number;
   uchar             Reserved;
  };
//---
struct GROUP_AFFINITY
  {
   ulong             Mask;
   ushort            Group;
   ushort            Reserved[3];
  };
//---
struct FLOAT128
  {
   long              LowPart;
   long              HighPart;
  };
//---
struct LARGE_INTEGER
  {
   long              QuadPart;
  };
//---
struct ULARGE_INTEGER
  {
   ulong             QuadPart;
  };
//---
struct LUID
  {
   uint              LowPart;
   int               HighPart;
  };
//---
struct LIST_ENTRY
  {
   PVOID             Flink;
   PVOID             Blink;
  };
//---
struct SINGLE_LIST_ENTRY
  {
   PVOID             Next;
  };
//---
struct LIST_ENTRY32
  {
   uint              Flink;
   uint              Blink;
  };
//---
struct LIST_ENTRY64
  {
   ulong             Flink;
   ulong             Blink;
  };
//---
struct OBJECTID
  {
   GUID              Lineage;
   uint              Uniquifier;
  };
//---
struct M128A
  {
   ulong             Low;
   long              High;
  };
//---
struct XSAVE_FORMAT
  {
   ushort            Controlushort;
   ushort            Statusushort;
   uchar             Tagushort;
   uchar             Reserved1;
   ushort            ErrorOpcode;
   uint              ErrorOffset;
   ushort            ErrorSelector;
   ushort            Reserved2;
   uint              DataOffset;
   ushort            DataSelector;
   ushort            Reserved3;
   uint              MxCsr;
   uint              MxCsr_Mask;
   M128A             FloatRegisters[8];
   M128A             XmmRegisters[16];
   uchar             Reserved4[96];
  };
//---
struct XSAVE_AREA_HEADER
  {
   ulong             Mask;
   ulong             CompactionMask;
   ulong             Reserved2[6];
  };
//---
struct XSAVE_AREA
  {
   XSAVE_FORMAT      LegacyState;
   XSAVE_AREA_HEADER Header;
  };
//---
struct XSTATE_CONTEXT
  {
   ulong             Mask;
   uint              Length;
   uint              Reserved1;
   PVOID             Area;
   uint              Reserved2;
   PVOID             Buffer;
   uint              Reserved3;
  };
//---
struct SCOPE_TABLE_AMD64
  {
   uint              Count;
   uint              BeginAddress;
   uint              EndAddress;
   uint              HandlerAddress;
   uint              JumpTarget;
  };
//---
struct UNWIND_HISTORY_TABLE_ENTRY
  {
   ulong             ImageBase;
   PVOID             FunctionEntry;
  };
//---
struct UNWIND_HISTORY_TABLE
  {
   uint              Count;
   uchar             LocalHint;
   uchar             GlobalHint;
   uchar             Search;
   uchar             Once;
   ulong             LowAddress;
   ulong             HighAddress;
   UNWIND_HISTORY_TABLE_ENTRY Entry[UNWIND_HISTORY_TABLE_SIZE];
  };
//---
struct SCOPE_TABLE_ARM64
  {
   uint              Count;
   uint              BeginAddress;
   uint              EndAddress;
   uint              HandlerAddress;
   uint              JumpTarget;
  };
//---
struct NEON128
  {
   ulong             Low;
   long              High;
  };
//---
struct DISPATCHER_CONTEXT
  {
   uint              ControlPc;
   uint              ImageBase;
   PVOID             FunctionEntry;
   uint              EstablisherFrame;
   uint              TargetPc;
   PVOID             ContextRecord;
   PVOID             LanguageHandler;
   PVOID             HandlerData;
   PVOID             HistoryTable;
   uint              ScopeIndex;
   uchar             ControlPcIsUnwound;
   PVOID             NonVolatileRegisters;
   uint              Reserved;
  };
//---
struct KNONVOLATILE_CONTEXT_POINTERS
  {
   PVOID             FloatingContext[16];
   PVOID             IntegerContext[16];
  };
//---
struct SCOPE_TABLE_ARM
  {
   uint              Count;
   uint              BeginAddress;
   uint              EndAddress;
   uint              HandlerAddress;
   uint              JumpTarget;
  };
//---
struct DISPATCHER_CONTEXT_ARM64
  {
   ulong             ControlPc;
   ulong             ImageBase;
   PVOID             FunctionEntry;
   ulong             EstablisherFrame;
   ulong             TargetPc;
   PVOID             ContextRecord;
   PVOID             LanguageHandler;
   PVOID             HandlerData;
   PVOID             HistoryTable;
   uint              ScopeIndex;
   uchar             ControlPcIsUnwound;
   PVOID             NonVolatileRegisters;
  };
//---
struct KNONVOLATILE_CONTEXT_POINTERS_ARM64
  {
   PVOID             X19;
   PVOID             X20;
   PVOID             X21;
   PVOID             X22;
   PVOID             X23;
   PVOID             X24;
   PVOID             X25;
   PVOID             X26;
   PVOID             X27;
   PVOID             X28;
   PVOID             Fp;
   PVOID             Lr;
   PVOID             D8;
   PVOID             D9;
   PVOID             D10;
   PVOID             D11;
   PVOID             D12;
   PVOID             D13;
   PVOID             D14;
   PVOID             D15;
  };
//---
struct FLOATING_SAVE_AREA
  {
   uint              Controlushort;
   uint              Statusushort;
   uint              Tagushort;
   uint              ErrorOffset;
   uint              ErrorSelector;
   uint              DataOffset;
   uint              DataSelector;
   uchar             RegisterArea[SIZE_OF_80387_REGISTERS];
   uint              Spare0;
  };
//---
struct CONTEXT
  {
   ulong             P1Home;
   ulong             P2Home;
   ulong             P3Home;
   ulong             P4Home;
   ulong             P5Home;
   ulong             P6Home;
   uint              ContextFlags;
   uint              MxCsr;
   ushort            SegCs;
   ushort            SegDs;
   ushort            SegEs;
   ushort            SegFs;
   ushort            SegGs;
   ushort            SegSs;
   uint              EFlags;
   ulong             Dr0;
   ulong             Dr1;
   ulong             Dr2;
   ulong             Dr3;
   ulong             Dr6;
   ulong             Dr7;
   ulong             Rax;
   ulong             Rcx;
   ulong             Rdx;
   ulong             Rbx;
   ulong             Rsp;
   ulong             Rbp;
   ulong             Rsi;
   ulong             Rdi;
   ulong             R8;
   ulong             R9;
   ulong             R10;
   ulong             R11;
   ulong             R12;
   ulong             R13;
   ulong             R14;
   ulong             R15;
   ulong             Rip;
   M128A             Header[2];
   M128A             Legacy[8];
   M128A             Xmm0;
   M128A             Xmm1;
   M128A             Xmm2;
   M128A             Xmm3;
   M128A             Xmm4;
   M128A             Xmm5;
   M128A             Xmm6;
   M128A             Xmm7;
   M128A             Xmm8;
   M128A             Xmm9;
   M128A             Xmm10;
   M128A             Xmm11;
   M128A             Xmm12;
   M128A             Xmm13;
   M128A             Xmm14;
   M128A             Xmm15;
   M128A             VectorRegister[26];
   ulong             VectorControl;
   ulong             DebugControl;
   ulong             LastBranchToRip;
   ulong             LastBranchFromRip;
   ulong             LastExceptionToRip;
   ulong             LastExceptionFromRip;
  };
//---
struct WOW64_FLOATING_SAVE_AREA
  {
   uint              Controlushort;
   uint              Statusushort;
   uint              Tagushort;
   uint              ErrorOffset;
   uint              ErrorSelector;
   uint              DataOffset;
   uint              DataSelector;
   uchar             RegisterArea[WOW64_SIZE_OF_80387_REGISTERS];
   uint              Cr0NpxState;
  };
//---
struct WOW64_CONTEXT
  {
   uint              ContextFlags;
   uint              Dr0;
   uint              Dr1;
   uint              Dr2;
   uint              Dr3;
   uint              Dr6;
   uint              Dr7;
   WOW64_FLOATING_SAVE_AREA FloatSave;
   uint              SegGs;
   uint              SegFs;
   uint              SegEs;
   uint              SegDs;
   uint              Edi;
   uint              Esi;
   uint              Ebx;
   uint              Edx;
   uint              Ecx;
   uint              Eax;
   uint              Ebp;
   uint              Eip;
   uint              SegCs;
   uint              EFlags;
   uint              Esp;
   uint              SegSs;
   uchar             ExtendedRegisters[WOW64_MAXIMUM_SUPPORTED_EXTENSION];
  };
//---
struct WOW64_LDT_ENTRY
  {
   uint              LimitLow;
   uint              BaseLow;
   uchar             BaseMid;
   uchar             Flags1;
   uchar             Flags2;
   uchar             BaseHi;
  };
//---
struct WOW64_DESCRIPTOR_TABLE_ENTRY
  {
   uint              Selector;
   WOW64_LDT_ENTRY   Descriptor;
  };
//---
struct EXCEPTION_RECORD
  {
   uint              ExceptionCode;
   uint              ExceptionFlags;
   PVOID             ExceptionRecord;
   PVOID             ExceptionAddress;
   uint              NumberParameters;
   PVOID             ExceptionInformation[EXCEPTION_MAXIMUM_PARAMETERS];
  };
//---
struct EXCEPTION_RECORD32
  {
   uint              ExceptionCode;
   uint              ExceptionFlags;
   uint              ExceptionRecord;
   uint              ExceptionAddress;
   uint              NumberParameters;
   uint              ExceptionInformation[EXCEPTION_MAXIMUM_PARAMETERS];
  };
//---
struct EXCEPTION_RECORD64
  {
   uint              ExceptionCode;
   uint              ExceptionFlags;
   ulong             ExceptionRecord;
   ulong             ExceptionAddress;
   uint              NumberParameters;
   uint              __unusedAlignment;
   ulong             ExceptionInformation[EXCEPTION_MAXIMUM_PARAMETERS];
  };
//---
struct EXCEPTION_POINTERS
  {
   PVOID             ExceptionRecord;
   PVOID             ContextRecord;
  };
//---
struct GENERIC_MAPPING
  {
   uint              GenericRead;
   uint              GenericWrite;
   uint              GenericExecute;
   uint              GenericAll;
  };
//---
struct LUID_AND_ATTRIBUTES
  {
   LUID              Luid;
   uint              Attributes;
  };
//---
struct SID_IDENTIFIER_AUTHORITY
  {
   uchar             Value[6];
  };
//---
//---
struct SID
  {
   uchar             Revision;
   uchar             SubAuthorityCount;
   SID_IDENTIFIER_AUTHORITY IdentifierAuthority;
   uint              SubAuthority[ANYSIZE_ARRAY];
  };
//---
struct SID_AND_ATTRIBUTES
  {
   SID               Sid;
   uint              Attributes;
  };
//---
struct SID_AND_ATTRIBUTES_HASH
  {
   uint              SidCount;
   PVOID             SidAttr;
   ulong             Hash[SID_HASH_SIZE];
  };
//---
struct ACL
  {
   uchar             AclRevision;
   uchar             Sbz1;
   ushort            AclSize;
   ushort            AceCount;
   ushort            Sbz2;
  };
//---
struct ACE_HEADER
  {
   uchar             AceType;
   uchar             AceFlags;
   ushort            AceSize;
  };
//---
struct ACCESS_ALLOWED_ACE
  {
   ACE_HEADER        Header;
   uint              Mask;
   uint              SidStart;
  };
//---
struct ACCESS_DENIED_ACE
  {
   ACE_HEADER        Header;
   uint              Mask;
   uint              SidStart;
  };
//---
struct SYSTEM_AUDIT_ACE
  {
   ACE_HEADER        Header;
   uint              Mask;
   uint              SidStart;
  };
//---
struct SYSTEM_ALARM_ACE
  {
   ACE_HEADER        Header;
   uint              Mask;
   uint              SidStart;
  };
//---
struct SYSTEM_RESOURCE_ATTRIBUTE_ACE
  {
   ACE_HEADER        Header;
   uint              Mask;
   uint              SidStart;
  };
//---
struct SYSTEM_SCOPED_POLICY_ID_ACE
  {
   ACE_HEADER        Header;
   uint              Mask;
   uint              SidStart;
  };
//---
struct SYSTEM_MANDATORY_LABEL_ACE
  {
   ACE_HEADER        Header;
   uint              Mask;
   uint              SidStart;
  };
//---
struct SYSTEM_PROCESS_TRUST_LABEL_ACE
  {
   ACE_HEADER        Header;
   uint              Mask;
   uint              SidStart;
  };
//---
struct SYSTEM_ACCESS_FILTER_ACE
  {
   ACE_HEADER        Header;
   uint              Mask;
   uint              SidStart;
  };
//---
struct ACCESS_ALLOWED_OBJECT_ACE
  {
   ACE_HEADER        Header;
   uint              Mask;
   uint              Flags;
   GUID              ObjectType;
   GUID              InheritedObjectType;
   uint              SidStart;
  };
//---
struct ACCESS_DENIED_OBJECT_ACE
  {
   ACE_HEADER        Header;
   uint              Mask;
   uint              Flags;
   GUID              ObjectType;
   GUID              InheritedObjectType;
   uint              SidStart;
  };
//---
struct SYSTEM_AUDIT_OBJECT_ACE
  {
   ACE_HEADER        Header;
   uint              Mask;
   uint              Flags;
   GUID              ObjectType;
   GUID              InheritedObjectType;
   uint              SidStart;
  };
//---
struct SYSTEM_ALARM_OBJECT_ACE
  {
   ACE_HEADER        Header;
   uint              Mask;
   uint              Flags;
   GUID              ObjectType;
   GUID              InheritedObjectType;
   uint              SidStart;
  };
//---
struct ACCESS_ALLOWED_CALLBACK_ACE
  {
   ACE_HEADER        Header;
   uint              Mask;
   uint              SidStart;
  };
//---
struct ACCESS_DENIED_CALLBACK_ACE
  {
   ACE_HEADER        Header;
   uint              Mask;
   uint              SidStart;
  };
//---
struct SYSTEM_AUDIT_CALLBACK_ACE
  {
   ACE_HEADER        Header;
   uint              Mask;
   uint              SidStart;
  };
//---
struct SYSTEM_ALARM_CALLBACK_ACE
  {
   ACE_HEADER        Header;
   uint              Mask;
   uint              SidStart;
  };
//---
struct ACCESS_ALLOWED_CALLBACK_OBJECT_ACE
  {
   ACE_HEADER        Header;
   uint              Mask;
   uint              Flags;
   GUID              ObjectType;
   GUID              InheritedObjectType;
   uint              SidStart;
  };
//---
struct ACCESS_DENIED_CALLBACK_OBJECT_ACE
  {
   ACE_HEADER        Header;
   uint              Mask;
   uint              Flags;
   GUID              ObjectType;
   GUID              InheritedObjectType;
   uint              SidStart;
  };
//---
struct SYSTEM_AUDIT_CALLBACK_OBJECT_ACE
  {
   ACE_HEADER        Header;
   uint              Mask;
   uint              Flags;
   GUID              ObjectType;
   GUID              InheritedObjectType;
   uint              SidStart;
  };
//---
struct SYSTEM_ALARM_CALLBACK_OBJECT_ACE
  {
   ACE_HEADER        Header;
   uint              Mask;
   uint              Flags;
   GUID              ObjectType;
   GUID              InheritedObjectType;
   uint              SidStart;
  };
//---
struct ACL_REVISION_INFORMATION
  {
   uint              AclRevision;
  };
//---
struct ACL_SIZE_INFORMATION
  {
   uint              AceCount;
   uint              AclBytesInUse;
   uint              AclBytesFree;
  };
//---
struct SECURITY_DESCRIPTOR_RELATIVE
  {
   uchar             Revision;
   uchar             Sbz1;
   ushort            Control;
   uint              Owner;
   uint              Group;
   uint              Sacl;
   uint              Dacl;
  };
//---
struct SECURITY_DESCRIPTOR
  {
   uchar             Revision;
   uchar             Sbz1;
   ushort            Control;
   uchar             offset[4];
   PVOID             Owner;
   PVOID             Group;
   PVOID             Sacl;
   PVOID             Dacl;
  };
//---
struct SECURITY_OBJECT_AI_PARAMS
  {
   uint              Size;
   uint              ConstraintMask;
  };
//---
struct OBJECT_TYPE_LIST
  {
   ushort            Level;
   ushort            Sbz;
   GUID              ObjectType;
  };
//---
struct PRIVILEGE_SET
  {
   uint              PrivilegeCount;
   uint              Control;
   LUID_AND_ATTRIBUTES Privilege[ANYSIZE_ARRAY];
  };
//---
struct ACCESS_REASONS
  {
   uint              Data[32];
  };
//---
struct SE_SECURITY_DESCRIPTOR
  {
   uint              Size;
   uint              Flags;
   PVOID             SecurityDescriptor;
  };
//---
struct SE_ACCESS_REQUEST
  {
   uint              Size;
   PVOID             SeSecurityDescriptor;
   uint              DesiredAccess;
   uint              PreviouslyGrantedAccess;
   PVOID             PrincipalSelfSid;
   PVOID             GenericMapping;
   uint              ObjectTypeListCount;
   PVOID             ObjectTypeList;
  };
//---
struct SE_ACCESS_REPLY
  {
   uint              Size;
   uint              ResultListCount;
   PVOID             GrantedAccess;
   uint              AccessStatus;
   PVOID             AccessReason;
   PVOID             Privileges;
  };
//---
struct TOKEN_USER
  {
   SID_AND_ATTRIBUTES User;
  };
//---
struct SE_TOKEN_USER
  {
   TOKEN_USER        TokenUser;
   SID               Sid;
  };
//---
struct TOKEN_GROUPS
  {
   uint              GroupCount;
   SID_AND_ATTRIBUTES Groups[ANYSIZE_ARRAY];
  };
//---
struct TOKEN_PRIVILEGES
  {
   uint              PrivilegeCount;
   LUID_AND_ATTRIBUTES Privileges[ANYSIZE_ARRAY];
  };
//---
struct TOKEN_OWNER
  {
   PVOID             Owner;
  };
//---
struct TOKEN_PRIMARY_GROUP
  {
   PVOID             PrimaryGroup;
  };
//---
struct TOKEN_DEFAULT_DACL
  {
   PVOID             DefaultDacl;
  };
//---
struct TOKEN_USER_CLAIMS
  {
   PVOID             UserClaims;
  };
//---
struct TOKEN_DEVICE_CLAIMS
  {
   PVOID             DeviceClaims;
  };
//---
struct TOKEN_GROUPS_AND_PRIVILEGES
  {
   uint              SidCount;
   uint              SidLength;
   PVOID             Sids;
   uint              RestrictedSidCount;
   uint              RestrictedSidLength;
   PVOID             RestrictedSids;
   uint              PrivilegeCount;
   uint              PrivilegeLength;
   PVOID             Privileges;
   LUID              AuthenticationId;
  };
//---
struct TOKEN_LINKED_TOKEN
  {
   HANDLE            LinkedToken;
  };
//---
struct TOKEN_ELEVATION
  {
   uint              TokenIsElevated;
  };
//---
struct TOKEN_MANDATORY_LABEL
  {
   SID_AND_ATTRIBUTES Label;
  };
//---
struct TOKEN_MANDATORY_POLICY
  {
   uint              Policy;
  };
//---
struct TOKEN_ACCESS_INFORMATION
  {
   PVOID             SidHash;
   PVOID             RestrictedSidHash;
   PVOID             Privileges;
   LUID              AuthenticationId;
   TOKEN_TYPE        TokenType;
   SECURITY_IMPERSONATION_LEVEL ImpersonationLevel;
   TOKEN_MANDATORY_POLICY MandatoryPolicy;
   uint              Flags;
   uint              AppContainerNumber;
   PVOID             PackageSid;
   PVOID             CapabilitiesHash;
   PVOID             TrustLevelSid;
   PVOID             SecurityAttributes;
  };
//---
struct TOKEN_AUDIT_POLICY
  {
   uchar             PerUserPolicy[((POLICY_AUDIT_SUBCATEGORY_COUNT)>>1)+1];
  };
//---
struct TOKEN_SOURCE
  {
   char              SourceName[TOKEN_SOURCE_LENGTH];
   LUID              SourceIdentifier;
  };
//---
struct TOKEN_STATISTICS
  {
   LUID              TokenId;
   LUID              AuthenticationId;
   long              ExpirationTime;
   TOKEN_TYPE        TokenType;
   SECURITY_IMPERSONATION_LEVEL ImpersonationLevel;
   uint              DynamicCharged;
   uint              DynamicAvailable;
   uint              GroupCount;
   uint              PrivilegeCount;
   LUID              ModifiedId;
  };
//---
struct TOKEN_CONTROL
  {
   LUID              TokenId;
   LUID              AuthenticationId;
   LUID              ModifiedId;
   TOKEN_SOURCE      TokenSource;
  };
//---
struct TOKEN_ORIGIN
  {
   LUID              OriginatingLogonSession;
  };
//---
struct TOKEN_APPCONTAINER_INFORMATION
  {
   PVOID             TokenAppContainer;
  };
//---
struct TOKEN_SID_INFORMATION
  {
   PVOID             Sid;
  };
//---
struct TOKEN_BNO_ISOLATION_INFORMATION
  {
   string            IsolationPrefix;
   uchar             IsolationEnabled;
  };
//---
struct CLAIM_SECURITY_ATTRIBUTE_FQBN_VALUE
  {
   ulong             Version;
   string            Name;
  };
//---
struct CLAIM_SECURITY_ATTRIBUTE_OCTET_STRING_VALUE
  {
   PVOID             pValue;
   uint              ValueLength;
  };
//---
struct CLAIM_SECURITY_ATTRIBUTE_RELATIVE_V1
  {
   uint              Name;
   ushort            ValueType;
   ushort            Reserved;
   uint              Flags;
   uint              ValueCount;
   uint              pInt64[ANYSIZE_ARRAY];
  };
//---
struct Attribute
  {
   ushort            Version;
   ushort            Reserved;
   uint              AttributeCount;
   PVOID             pAttributeV1;
  };
//---
struct SECURITY_QUALITY_OF_SERVICE
  {
   uint              Length;
   SECURITY_IMPERSONATION_LEVEL ImpersonationLevel;
   uchar             ContextTrackingMode;
   uchar             EffectiveOnly;
  };
//---
struct SE_IMPERSONATION_STATE
  {
   PVOID             Token;
   uchar             CopyOnOpen;
   uchar             EffectiveOnly;
   SECURITY_IMPERSONATION_LEVEL Level;
  };
//---
struct SECURITY_CAPABILITIES
  {
   PVOID             AppContainerSid;
   PVOID             Capabilities;
   uint              CapabilityCount;
   uint              Reserved;
  };
//---
struct JOB_SET_ARRAY
  {
   HANDLE            JobHandle;
   uint              MemberLevel;
   uint              Flags;
  };
//---
struct EXCEPTION_REGISTRATION_RECORD
  {
   PVOID             Next;
   PVOID             Handler;
  };
//---
struct NT_TIB
  {
   PVOID             ExceptionList;
   PVOID             StackBase;
   PVOID             StackLimit;
   PVOID             SubSystemTib;
   PVOID             FiberData;
   PVOID             ArbitraryUserPointer;
   PVOID             Self;
  };
//---
struct UMS_CREATE_THREAD_ATTRIBUTES
  {
   uint              UmsVersion;
   PVOID             UmsContext;
   PVOID             UmsCompletionList;
  };
//---
struct WOW64_ARCHITECTURE_INFORMATION
  {
   uint              Info;
  };
//---
struct QUOTA_LIMITS
  {
   ulong             PagedPoolLimit;
   ulong             NonPagedPoolLimit;
   ulong             MinimumWorkingSetSize;
   ulong             MaximumWorkingSetSize;
   ulong             PagefileLimit;
   long              TimeLimit;
  };
//---
struct QUOTA_LIMITS_EX
  {
   ulong             PagedPoolLimit;
   ulong             NonPagedPoolLimit;
   ulong             MinimumWorkingSetSize;
   ulong             MaximumWorkingSetSize;
   ulong             PagefileLimit;
   long              TimeLimit;
   ulong             WorkingSetLimit;
   ulong             Reserved2;
   ulong             Reserved3;
   ulong             Reserved4;
   uint              Flags;
   uint              CpuRateLimit;
  };
//---
struct IO_COUNTERS
  {
   ulong             ReadOperationCount;
   ulong             WriteOperationCount;
   ulong             OtherOperationCount;
   ulong             ReadTransferCount;
   ulong             WriteTransferCount;
   ulong             OtherTransferCount;
  };
//---
struct JOBOBJECT_BASIC_ACCOUNTING_INFORMATION
  {
   long              TotalUserTime;
   long              TotalKernelTime;
   long              ThisPeriodTotalUserTime;
   long              ThisPeriodTotalKernelTime;
   uint              TotalPageFaultCount;
   uint              TotalProcesses;
   uint              ActiveProcesses;
   uint              TotalTerminatedProcesses;
  };
//---
struct JOBOBJECT_BASIC_LIMIT_INFORMATION
  {
   long              PerProcessUserTimeLimit;
   long              PerJobUserTimeLimit;
   uint              LimitFlags;
   ulong             MinimumWorkingSetSize;
   ulong             MaximumWorkingSetSize;
   uint              ActiveProcessLimit;
   ulong             Affinity;
   uint              PriorityClass;
   uint              SchedulingClass;
  };
//---
struct JOBOBJECT_EXTENDED_LIMIT_INFORMATION
  {
   JOBOBJECT_BASIC_LIMIT_INFORMATION BasicLimitInformation;
   IO_COUNTERS       IoInfo;
   ulong             ProcessMemoryLimit;
   ulong             JobMemoryLimit;
   ulong             PeakProcessMemoryUsed;
   ulong             PeakJobMemoryUsed;
  };
//---
struct JOBOBJECT_BASIC_PROCESS_ID_LIST
  {
   uint              NumberOfAssignedProcesses;
   uint              NumberOfProcessIdsInList;
   ulong             ProcessIdList[1];
  };
//---
struct JOBOBJECT_BASIC_UI_RESTRICTIONS
  {
   uint              UIRestrictionsClass;
  };
//---
struct JOBOBJECT_SECURITY_LIMIT_INFORMATION
  {
   uint              SecurityLimitFlags;
   HANDLE            JobToken;
   PVOID             SidsToDisable;
   PVOID             PrivilegesToDelete;
   PVOID             RestrictedSids;
  };
//---
struct JOBOBJECT_END_OF_JOB_TIME_INFORMATION
  {
   uint              EndOfJobTimeAction;
  };
//---
struct JOBOBJECT_ASSOCIATE_COMPLETION_PORT
  {
   PVOID             CompletionKey;
   HANDLE            CompletionPort;
  };
//---
struct JOBOBJECT_BASIC_AND_IO_ACCOUNTING_INFORMATION
  {
   JOBOBJECT_BASIC_ACCOUNTING_INFORMATION BasicInfo;
   IO_COUNTERS       IoInfo;
  };
//---
struct JOBOBJECT_JOBSET_INFORMATION
  {
   uint              MemberLevel;
  };
//---
struct JOBOBJECT_NOTIFICATION_LIMIT_INFORMATION
  {
   ulong             IoReadBytesLimit;
   ulong             IoWriteBytesLimit;
   long              PerJobUserTimeLimit;
   ulong             JobMemoryLimit;
   JOBOBJECT_RATE_CONTROL_TOLERANCE RateControlTolerance;
   JOBOBJECT_RATE_CONTROL_TOLERANCE_INTERVAL RateControlToleranceInterval;
   uint              LimitFlags;
  };
//---
struct JOBOBJECT_LIMIT_VIOLATION_INFORMATION
  {
   uint              LimitFlags;
   uint              ViolationLimitFlags;
   ulong             IoReadBytes;
   ulong             IoReadBytesLimit;
   ulong             IoWriteBytes;
   ulong             IoWriteBytesLimit;
   long              PerJobUserTime;
   long              PerJobUserTimeLimit;
   ulong             JobMemory;
   ulong             JobMemoryLimit;
   JOBOBJECT_RATE_CONTROL_TOLERANCE RateControlTolerance;
   JOBOBJECT_RATE_CONTROL_TOLERANCE RateControlToleranceLimit;
  };
//---
struct JOBOBJECT_NET_RATE_CONTROL_INFORMATION
  {
   ulong             MaxBandwidth;
   JOB_OBJECT_NET_RATE_CONTROL_FLAGS ControlFlags;
   uchar             DscpTag;
  };
//---
struct JOBOBJECT_IO_RATE_CONTROL_INFORMATION_NATIVE
  {
   long              MaxIops;
   long              MaxBandwidth;
   long              ReservationIops;
   string            VolumeName;
   uint              BaseIoSize;
   JOB_OBJECT_IO_RATE_CONTROL_FLAGS ControlFlags;
   ushort            VolumeNameLength;
  };
//---
struct JOBOBJECT_IO_RATE_CONTROL_INFORMATION_NATIVE_V2
  {
   long              MaxIops;
   long              MaxBandwidth;
   long              ReservationIops;
   string            VolumeName;
   uint              BaseIoSize;
   JOB_OBJECT_IO_RATE_CONTROL_FLAGS ControlFlags;
   ushort            VolumeNameLength;
   long              CriticalReservationIops;
   long              ReservationBandwidth;
   long              CriticalReservationBandwidth;
   long              MaxTimePercent;
   long              ReservationTimePercent;
   long              CriticalReservationTimePercent;
  };
//---
struct JOBOBJECT_IO_RATE_CONTROL_INFORMATION_NATIVE_V3
  {
   long              MaxIops;
   long              MaxBandwidth;
   long              ReservationIops;
   string            VolumeName;
   uint              BaseIoSize;
   JOB_OBJECT_IO_RATE_CONTROL_FLAGS ControlFlags;
   ushort            VolumeNameLength;
   long              CriticalReservationIops;
   long              ReservationBandwidth;
   long              CriticalReservationBandwidth;
   long              MaxTimePercent;
   long              ReservationTimePercent;
   long              CriticalReservationTimePercent;
   long              SoftMaxIops;
   long              SoftMaxBandwidth;
   long              SoftMaxTimePercent;
   long              LimitExcessNotifyIops;
   long              LimitExcessNotifyBandwidth;
   long              LimitExcessNotifyTimePercent;
  };
//---
struct JOBOBJECT_IO_ATTRIBUTION_STATS
  {
   ulong             IoCount;
   ulong             TotalNonOverlappedQueueTime;
   ulong             TotalNonOverlappedServiceTime;
   ulong             TotalSize;
  };
//---
struct JOBOBJECT_IO_ATTRIBUTION_INFORMATION
  {
   uint              ControlFlags;
   JOBOBJECT_IO_ATTRIBUTION_STATS ReadStats;
   JOBOBJECT_IO_ATTRIBUTION_STATS WriteStats;
  };
//---
struct SILOOBJECT_BASIC_INFORMATION
  {
   uint              SiloId;
   uint              SiloParentId;
   uint              NumberOfProcesses;
   uchar             IsInServerSilo;
   uchar             Reserved[3];
  };
//---
struct SERVERSILO_BASIC_INFORMATION
  {
   uint              ServiceSessionId;
   SERVERSILO_STATE  State;
   uint              ExitStatus;
  };
//---
struct CACHE_DESCRIPTOR
  {
   uchar             Level;
   uchar             Associativity;
   ushort            LineSize;
   uint              Size;
   PROCESSOR_CACHE_TYPE Type;
  };
//---
struct ProcessorCore
  {
   ulong             ProcessorMask;
   LOGICAL_PROCESSOR_RELATIONSHIP Relationship;
   uchar             Flags;
  };
//---
struct PROCESSOR_RELATIONSHIP
  {
   uchar             Flags;
   uchar             EfficiencyClass;
   uchar             Reserved[20];
   ushort            GroupCount;
   GROUP_AFFINITY    GroupMask[ANYSIZE_ARRAY];
  };
//---
struct NUMA_NODE_RELATIONSHIP
  {
   uint              NodeNumber;
   uchar             Reserved[20];
   GROUP_AFFINITY    GroupMask;
  };
//---
struct CACHE_RELATIONSHIP
  {
   uchar             Level;
   uchar             Associativity;
   ushort            LineSize;
   uint              CacheSize;
   PROCESSOR_CACHE_TYPE Type;
   uchar             Reserved[20];
   GROUP_AFFINITY    GroupMask;
  };
//---
struct PROCESSOR_GROUP_INFO
  {
   uchar             MaximumProcessorCount;
   uchar             ActiveProcessorCount;
   uchar             Reserved[38];
   ulong             ActiveProcessorMask;
  };
//---
struct GROUP_RELATIONSHIP
  {
   ushort            MaximumGroupCount;
   ushort            ActiveGroupCount;
   uchar             Reserved[20];
   PROCESSOR_GROUP_INFO GroupInfo[ANYSIZE_ARRAY];
  };
//---
struct SYSTEM_PROCESSOR_CYCLE_TIME_INFORMATION
  {
   ulong             CycleTime;
  };
//---
struct XSTATE_FEATURE
  {
   uint              Offset;
   uint              Size;
  };
//---
struct XSTATE_CONFIGURATION
  {
   ulong             EnabledFeatures;
   ulong             EnabledVolatileFeatures;
   uint              Size;
   uint              ControlFlags;
   XSTATE_FEATURE    Features[MAXIMUM_XSTATE_FEATURES];
   ulong             EnabledSupervisorFeatures;
   ulong             AlignedFeatures;
   uint              AllFeatureSize;
   uint              AllFeatures[MAXIMUM_XSTATE_FEATURES];
  };
//---
struct MEMORY_BASIC_INFORMATION
  {
   PVOID             BaseAddress;
   PVOID             AllocationBase;
   uint              AllocationProtect;
   ulong             RegionSize;
   uint              State;
   uint              Protect;
   uint              Type;
  };
//---
struct MEMORY_BASIC_INFORMATION32
  {
   uint              BaseAddress;
   uint              AllocationBase;
   uint              AllocationProtect;
   uint              RegionSize;
   uint              State;
   uint              Protect;
   uint              Type;
  };
//---
struct MEMORY_BASIC_INFORMATION64
  {
   ulong             BaseAddress;
   ulong             AllocationBase;
   uint              AllocationProtect;
   uint              __alignment1;
   ulong             RegionSize;
   uint              State;
   uint              Protect;
   uint              Type;
   uint              __alignment2;
  };
//---
struct CFG_CALL_TARGET_INFO
  {
   ulong             Offset;
   ulong             Flags;
  };
//---
struct MEM_ADDRESS_REQUIREMENTS
  {
   PVOID             LowestStartingAddress;
   PVOID             HighestEndingAddress;
   ulong             Alignment;
  };
//---
struct ENCLAVE_CREATE_INFO_SGX
  {
   uchar             Secs[4096];
  };
//---
struct ENCLAVE_INIT_INFO_SGX
  {
   uchar             SigStruct[1808];
   uchar             Reserved1[240];
   uchar             EInitToken[304];
   uchar             Reserved2[1744];
  };
//---
struct ENCLAVE_CREATE_INFO_VBS
  {
   uint              Flags;
   uchar             OwnerID[32];
  };
//---
struct ENCLAVE_INIT_INFO_VBS
  {
   uint              Length;
   uint              ThreadCount;
  };
//---
struct FILE_ID_128
  {
   uchar             Identifier[16];
  };
//---
struct FILE_NOTIFY_INFORMATION
  {
   uint              NextEntryOffset;
   uint              Action;
   uint              FileNameLength;
   short             FileName[1];
  };
//---
struct FILE_NOTIFY_EXTENDED_INFORMATION
  {
   uint              NextEntryOffset;
   uint              Action;
   long              CreationTime;
   long              LastModificationTime;
   long              LastChangeTime;
   long              LastAccessTime;
   long              AllocatedLength;
   long              FileSize;
   uint              FileAttributes;
   uint              ReparsePointTag;
   long              FileId;
   long              ParentFileId;
   uint              FileNameLength;
   short             FileName[1];
  };
//---
struct GenericReparseBuffer
  {
   uint              ReparseTag;
   ushort            ReparseDataLength;
   ushort            Reserved;
   GUID              ReparseGuid;
   uchar             DataBuffer[1];
  };
//---
struct SCRUB_DATA_INPUT
  {
   uint              Size;
   uint              Flags;
   uint              MaximumIos;
   uint              ObjectId[4];
   uint              Reserved[13];
   uchar             ResumeContext[816];
  };
//---
struct SCRUB_PARITY_EXTENT
  {
   long              Offset;
   ulong             Length;
  };
//---
struct SCRUB_PARITY_EXTENT_DATA
  {
   ushort            Size;
   ushort            Flags;
   ushort            NumberOfParityExtents;
   ushort            MaximumNumberOfParityExtents;
   SCRUB_PARITY_EXTENT ParityExtents[ANYSIZE_ARRAY];
  };
//---
struct SCRUB_DATA_OUTPUT
  {
   uint              Size;
   uint              Flags;
   uint              Status;
   ulong             ErrorFileOffset;
   ulong             ErrorLength;
   ulong             NumberOfBytesRepaired;
   ulong             NumberOfBytesFailed;
   ulong             InternalFileReference;
   ushort            ResumeContextLength;
   ushort            ParityExtentDataOffset;
   uint              Reserved[5];
   uchar             ResumeContext[816];
  };
//---
struct SHARED_VIRTUAL_DISK_SUPPORT
  {
   SharedVirtualDiskSupportType SharedVirtualDiskSupport;
   SharedVirtualDiskHandleState HandleState;
  };
//---
struct NETWORK_APP_INSTANCE_EA
  {
   GUID              AppInstanceID;
   uint              CsvFlags;
  };
//---
struct CM_POWER_DATA
  {
   uint              PD_Size;
   DEVICE_POWER_STATE PD_MostRecentPowerState;
   uint              PD_Capabilities;
   uint              PD_D1Latency;
   uint              PD_D2Latency;
   uint              PD_D3Latency;
   DEVICE_POWER_STATE PD_PowerStateMapping[POWER_SYSTEM_MAXIMUM];
   SYSTEM_POWER_STATE PD_DeepestSystemWake;
  };
//---
struct POWER_USER_PRESENCE
  {
   POWER_USER_PRESENCE_TYPE UserPresence;
  };
//---
struct POWER_SESSION_CONNECT
  {
   uchar             Connected;
   uchar             Console;
  };
//---
struct POWER_SESSION_TIMEOUTS
  {
   uint              InputTimeout;
   uint              DisplayTimeout;
  };
//---
struct POWER_SESSION_RIT_STATE
  {
   uchar             Active;
   uint              LastInputTime;
  };
//---
struct POWER_SESSION_WINLOGON
  {
   uint              SessionId;
   uchar             Console;
   uchar             Locked;
  };
//---
struct POWER_IDLE_RESILIENCY
  {
   uint              CoalescingTimeout;
   uint              IdleResiliencyPeriod;
  };
//---
struct POWER_MONITOR_INVOCATION
  {
   uchar             Console;
   POWER_MONITOR_REQUEST_REASON RequestReason;
  };
//---
struct RESUME_PERFORMANCE
  {
   uint              PostTimeMs;
   ulong             TotalResumeTimeMs;
   ulong             ResumeCompleteTimestamp;
  };
//---
struct APPLICATIONLAUNCH_SETTING_VALUE
  {
   long              ActivationTime;
   uint              Flags;
   uint              ButtonInstanceID;
  };
//---
struct POWER_PLATFORM_INFORMATION
  {
   uchar             AoAc;
  };
//---
struct POWER_ACTION_POLICY
  {
   POWER_ACTION      Action;
   uint              Flags;
   uint              EventCode;
  };
//---
struct SYSTEM_POWER_LEVEL
  {
   uchar             Enable;
   uchar             Spare[3];
   uint              BatteryLevel;
   POWER_ACTION_POLICY PowerPolicy;
   SYSTEM_POWER_STATE MinSystemState;
  };
//---
struct SYSTEM_POWER_POLICY
  {
   uint              Revision;
   POWER_ACTION_POLICY PowerButton;
   POWER_ACTION_POLICY SleepButton;
   POWER_ACTION_POLICY LidClose;
   SYSTEM_POWER_STATE LidOpenWake;
   uint              Reserved;
   POWER_ACTION_POLICY Idle;
   uint              IdleTimeout;
   uchar             IdleSensitivity;
   uchar             DynamicThrottle;
   uchar             Spare2[2];
   SYSTEM_POWER_STATE MinSleep;
   SYSTEM_POWER_STATE MaxSleep;
   SYSTEM_POWER_STATE ReducedLatencySleep;
   uint              WinLogonFlags;
   uint              Spare3;
   uint              DozeS4Timeout;
   uint              BroadcastCapacityResolution;
   SYSTEM_POWER_LEVEL DischargePolicy[NUM_DISCHARGE_POLICIES];
   uint              VideoTimeout;
   uchar             VideoDimDisplay;
   uint              VideoReserved[3];
   uint              SpindownTimeout;
   uchar             OptimizeForPower;
   uchar             FanThrottleTolerance;
   uchar             ForcedThrottle;
   uchar             MinThrottle;
   POWER_ACTION_POLICY OverThrottled;
  };
//---
struct PROCESSOR_POWER_POLICY_INFO
  {
   uint              TimeCheck;
   uint              DemoteLimit;
   uint              PromoteLimit;
   uchar             DemotePercent;
   uchar             PromotePercent;
   uchar             Spare[2];
   uint              Flags;
  };
//---
struct PROCESSOR_POWER_POLICY
  {
   uint              Revision;
   uchar             DynamicThrottle;
   uchar             Spare[3];
   uint              Flags;
   uint              PolicyCount;
   PROCESSOR_POWER_POLICY_INFO Policy[3];
  };
//---
struct ADMINISTRATOR_POWER_POLICY
  {
   SYSTEM_POWER_STATE MinSleep;
   SYSTEM_POWER_STATE MaxSleep;
   uint              MinVideoTimeout;
   uint              MaxVideoTimeout;
   uint              MinSpindownTimeout;
   uint              MaxSpindownTimeout;
  };
//---
struct HIBERFILE_BUCKET
  {
   ulong             MaxPhysicalMemory;
   uint              PhysicalMemoryPercent[HIBERFILE_TYPE_MAX];
  };
//---
struct IMAGE_DOS_HEADER
  {
   ushort            e_magic;
   ushort            e_cblp;
   ushort            e_cp;
   ushort            e_crlc;
   ushort            e_cparhdr;
   ushort            e_minalloc;
   ushort            e_maxalloc;
   ushort            e_ss;
   ushort            e_sp;
   ushort            e_csum;
   ushort            e_ip;
   ushort            e_cs;
   ushort            e_lfarlc;
   ushort            e_ovno;
   ushort            e_res[4];
   ushort            e_oemid;
   ushort            e_oeminfo;
   ushort            e_res2[10];
   int               e_lfanew;
  };
//---
struct IMAGE_OS2_HEADER
  {
   ushort            ne_magic;
   char              ne_ver;
   char              ne_rev;
   ushort            ne_enttab;
   ushort            ne_cbenttab;
   int               ne_crc;
   ushort            ne_flags;
   ushort            ne_autodata;
   ushort            ne_heap;
   ushort            ne_stack;
   int               ne_csip;
   int               ne_sssp;
   ushort            ne_cseg;
   ushort            ne_cmod;
   ushort            ne_cbnrestab;
   ushort            ne_segtab;
   ushort            ne_rsrctab;
   ushort            ne_restab;
   ushort            ne_modtab;
   ushort            ne_imptab;
   int               ne_nrestab;
   ushort            ne_cmovent;
   ushort            ne_align;
   ushort            ne_cres;
   uchar             ne_exetyp;
   uchar             ne_flagsothers;
   ushort            ne_pretthunks;
   ushort            ne_psegrefbytes;
   ushort            ne_swaparea;
   ushort            ne_expver;
  };
//---
struct IMAGE_VXD_HEADER
  {
   ushort            e32_magic;
   uchar             e32_border;
   uchar             e32_ushorter;
   uint              e32_level;
   ushort            e32_cpu;
   ushort            e32_os;
   uint              e32_ver;
   uint              e32_mflags;
   uint              e32_mpages;
   uint              e32_startobj;
   uint              e32_eip;
   uint              e32_stackobj;
   uint              e32_esp;
   uint              e32_pagesize;
   uint              e32_lastpagesize;
   uint              e32_fixupsize;
   uint              e32_fixupsum;
   uint              e32_ldrsize;
   uint              e32_ldrsum;
   uint              e32_objtab;
   uint              e32_objcnt;
   uint              e32_objmap;
   uint              e32_itermap;
   uint              e32_rsrctab;
   uint              e32_rsrccnt;
   uint              e32_restab;
   uint              e32_enttab;
   uint              e32_dirtab;
   uint              e32_dircnt;
   uint              e32_fpagetab;
   uint              e32_frectab;
   uint              e32_impmod;
   uint              e32_impmodcnt;
   uint              e32_impproc;
   uint              e32_pagesum;
   uint              e32_datapage;
   uint              e32_preload;
   uint              e32_nrestab;
   uint              e32_cbnrestab;
   uint              e32_nressum;
   uint              e32_autodata;
   uint              e32_debuginfo;
   uint              e32_debuglen;
   uint              e32_instpreload;
   uint              e32_instdemand;
   uint              e32_heapsize;
   uchar             e32_res3[12];
   uint              e32_winresoff;
   uint              e32_winreslen;
   ushort            e32_devid;
   ushort            e32_ddkver;
  };
//---
struct IMAGE_FILE_HEADER
  {
   ushort            Machine;
   ushort            NumberOfSections;
   uint              TimeDateStamp;
   uint              PointerToSymbolTable;
   uint              NumberOfSymbols;
   ushort            SizeOfOptionalHeader;
   ushort            Characteristics;
  };
//---
struct IMAGE_DATA_DIRECTORY
  {
   uint              VirtualAddress;
   uint              Size;
  };
//---
struct IMAGE_OPTIONAL_HEADER32
  {
   ushort            Magic;
   uchar             MajorLinkerVersion;
   uchar             MinorLinkerVersion;
   uint              SizeOfCode;
   uint              SizeOfInitializedData;
   uint              SizeOfUninitializedData;
   uint              AddressOfEntryPoint;
   uint              BaseOfCode;
   uint              BaseOfData;
   uint              ImageBase;
   uint              SectionAlignment;
   uint              FileAlignment;
   ushort            MajorOperatingSystemVersion;
   ushort            MinorOperatingSystemVersion;
   ushort            MajorImageVersion;
   ushort            MinorImageVersion;
   ushort            MajorSubsystemVersion;
   ushort            MinorSubsystemVersion;
   uint              Win32VersionValue;
   uint              SizeOfImage;
   uint              SizeOfHeaders;
   uint              CheckSum;
   ushort            Subsystem;
   ushort            DllCharacteristics;
   uint              SizeOfStackReserve;
   uint              SizeOfStackCommit;
   uint              SizeOfHeapReserve;
   uint              SizeOfHeapCommit;
   uint              LoaderFlags;
   uint              NumberOfRvaAndSizes;
   IMAGE_DATA_DIRECTORY DataDirectory[IMAGE_NUMBEROF_DIRECTORY_ENTRIES];
  };
//---
struct IMAGE_ROM_OPTIONAL_HEADER
  {
   ushort            Magic;
   uchar             MajorLinkerVersion;
   uchar             MinorLinkerVersion;
   uint              SizeOfCode;
   uint              SizeOfInitializedData;
   uint              SizeOfUninitializedData;
   uint              AddressOfEntryPoint;
   uint              BaseOfCode;
   uint              BaseOfData;
   uint              BaseOfBss;
   uint              GprMask;
   uint              CprMask[4];
   uint              GpValue;
  };
//---
struct IMAGE_OPTIONAL_HEADER64
  {
   ushort            Magic;
   uchar             MajorLinkerVersion;
   uchar             MinorLinkerVersion;
   uint              SizeOfCode;
   uint              SizeOfInitializedData;
   uint              SizeOfUninitializedData;
   uint              AddressOfEntryPoint;
   uint              BaseOfCode;
   ulong             ImageBase;
   uint              SectionAlignment;
   uint              FileAlignment;
   ushort            MajorOperatingSystemVersion;
   ushort            MinorOperatingSystemVersion;
   ushort            MajorImageVersion;
   ushort            MinorImageVersion;
   ushort            MajorSubsystemVersion;
   ushort            MinorSubsystemVersion;
   uint              Win32VersionValue;
   uint              SizeOfImage;
   uint              SizeOfHeaders;
   uint              CheckSum;
   ushort            Subsystem;
   ushort            DllCharacteristics;
   ulong             SizeOfStackReserve;
   ulong             SizeOfStackCommit;
   ulong             SizeOfHeapReserve;
   ulong             SizeOfHeapCommit;
   uint              LoaderFlags;
   uint              NumberOfRvaAndSizes;
   IMAGE_DATA_DIRECTORY DataDirectory[IMAGE_NUMBEROF_DIRECTORY_ENTRIES];
  };
//---
struct IMAGE_NT_HEADERS64
  {
   uint              Signature;
   IMAGE_FILE_HEADER FileHeader;
   IMAGE_OPTIONAL_HEADER64 OptionalHeader;
  };
//---
struct IMAGE_NT_HEADERS32
  {
   uint              Signature;
   IMAGE_FILE_HEADER FileHeader;
   IMAGE_OPTIONAL_HEADER32 OptionalHeader;
  };
//---
struct IMAGE_ROM_HEADERS
  {
   IMAGE_FILE_HEADER FileHeader;
   IMAGE_ROM_OPTIONAL_HEADER OptionalHeader;
  };
//---
struct ANON_OBJECT_HEADER
  {
   ushort            Sig1;
   ushort            Sig2;
   ushort            Version;
   ushort            Machine;
   uint              TimeDateStamp;
   GUID              ClassID;
   uint              SizeOfData;
  };
//---
struct ANON_OBJECT_HEADER_V2
  {
   ushort            Sig1;
   ushort            Sig2;
   ushort            Version;
   ushort            Machine;
   uint              TimeDateStamp;
   GUID              ClassID;
   uint              SizeOfData;
   uint              Flags;
   uint              MetaDataSize;
   uint              MetaDataOffset;
  };
//---
struct ANON_OBJECT_HEADER_BIGOBJ
  {
   ushort            Sig1;
   ushort            Sig2;
   ushort            Version;
   ushort            Machine;
   uint              TimeDateStamp;
   GUID              ClassID;
   uint              SizeOfData;
   uint              Flags;
   uint              MetaDataSize;
   uint              MetaDataOffset;
   uint              NumberOfSections;
   uint              PointerToSymbolTable;
   uint              NumberOfSymbols;
  };
//---
struct IMAGE_SECTION_HEADER
  {
   uchar             Name[IMAGE_SIZEOF_SHORT_NAME];
   uint              PhysicalAddress;
   uint              VirtualAddress;
   uint              SizeOfRawData;
   uint              PointerToRawData;
   uint              PointerToRelocations;
   uint              PointerToLinenumbers;
   ushort            NumberOfRelocations;
   ushort            NumberOfLinenumbers;
   uint              Characteristics;
  };
//---
struct IMAGE_SYMBOL
  {
   uchar             ShortName[8];
   uint              Value;
   short             SectionNumber;
   ushort            Type;
   uchar             StorageClass;
   uchar             NumberOfAuxSymbols;
  };
//---
struct IMAGE_SYMBOL_EX
  {
   uchar             ShortName[8];
   uint              Value;
   int               SectionNumber;
   ushort            Type;
   uchar             StorageClass;
   uchar             NumberOfAuxSymbols;
  };
//---
struct IMAGE_AUX_SYMBOL_TOKEN_DEF
  {
   uchar             bAuxType;
   uchar             bReserved;
   uint              SymbolTableIndex;
   uchar             rgbReserved[12];
  };
//---
struct IMAGE_LINENUMBER
  {
   uint              VirtualAddress;
   ushort            Linenumber;
  };
//---
struct IMAGE_BASE_RELOCATION
  {
   uint              VirtualAddress;
   uint              SizeOfBlock;
  };
//---
struct IMAGE_ARCHIVE_MEMBER_HEADER
  {
   uchar             Name[16];
   uchar             Date[12];
   uchar             UserID[6];
   uchar             GroupID[6];
   uchar             Mode[8];
   uchar             Size[10];
   uchar             EndHeader[2];
  };
//---
struct IMAGE_EXPORT_DIRECTORY
  {
   uint              Characteristics;
   uint              TimeDateStamp;
   ushort            MajorVersion;
   ushort            MinorVersion;
   uint              Name;
   uint              Base;
   uint              NumberOfFunctions;
   uint              NumberOfNames;
   uint              AddressOfFunctions;
   uint              AddressOfNames;
   uint              AddressOfNameOrdinals;
  };
//---
struct IMAGE_IMPORT_BY_NAME
  {
   ushort            Hint;
   char              Name[1];
  };
//---
struct IMAGE_THUNK_DATA64
  {
   ulong             Data;
  };
//---
struct IMAGE_THUNK_DATA32
  {
   uint              Data;
  };
//---
struct IMAGE_BOUND_IMPORT_DESCRIPTOR
  {
   uint              TimeDateStamp;
   ushort            OffsetModuleName;
   ushort            NumberOfModuleForwarderRefs;
  };
//---
struct IMAGE_BOUND_FORWARDER_REF
  {
   uint              TimeDateStamp;
   ushort            OffsetModuleName;
   ushort            Reserved;
  };
//---
struct IMAGE_RESOURCE_DIRECTORY
  {
   uint              Characteristics;
   uint              TimeDateStamp;
   ushort            MajorVersion;
   ushort            MinorVersion;
   ushort            NumberOfNamedEntries;
   ushort            NumberOfIdEntries;
  };
//---
struct IMAGE_RESOURCE_DIRECTORY_STRING
  {
   ushort            Length;
   char              NameString[1];
  };
//---
struct IMAGE_RESOURCE_DIR_STRING_U
  {
   ushort            Length;
   short             NameString[1];
  };
//---
struct IMAGE_RESOURCE_DATA_ENTRY
  {
   uint              OffsetToData;
   uint              Size;
   uint              CodePage;
   uint              Reserved;
  };
//---
struct IMAGE_LOAD_CONFIG_CODE_INTEGRITY
  {
   ushort            Flags;
   ushort            Catalog;
   uint              CatalogOffset;
   uint              Reserved;
  };
//---
struct IMAGE_DYNAMIC_RELOCATION_TABLE
  {
   uint              Version;
   uint              Size;
  };
//---
struct IMAGE_DYNAMIC_RELOCATION32
  {
   uint              Symbol;
   uint              BaseRelocSize;
  };
//---
struct IMAGE_DYNAMIC_RELOCATION64
  {
   ulong             Symbol;
   uint              BaseRelocSize;
  };
//---
struct IMAGE_DYNAMIC_RELOCATION32_V2
  {
   uint              HeaderSize;
   uint              FixupInfoSize;
   uint              Symbol;
   uint              SymbolGroup;
   uint              Flags;
  };
//---
struct IMAGE_DYNAMIC_RELOCATION64_V2
  {
   uint              HeaderSize;
   uint              FixupInfoSize;
   ulong             Symbol;
   uint              SymbolGroup;
   uint              Flags;
  };
//---
struct IMAGE_PROLOGUE_DYNAMIC_RELOCATION_HEADER
  {
   uchar             PrologueByteCount;
  };
//---
struct IMAGE_EPILOGUE_DYNAMIC_RELOCATION_HEADER
  {
   uint              EpilogueCount;
   uchar             EpilogueByteCount;
   uchar             BranchDescriptorElementSize;
   ushort            BranchDescriptorCount;
  };
//---
struct IMAGE_LOAD_CONFIG_DIRECTORY32
  {
   uint              Size;
   uint              TimeDateStamp;
   ushort            MajorVersion;
   ushort            MinorVersion;
   uint              GlobalFlagsClear;
   uint              GlobalFlagsSet;
   uint              CriticalSectionDefaultTimeout;
   uint              DeCommitFreeBlockThreshold;
   uint              DeCommitTotalFreeThreshold;
   uint              LockPrefixTable;
   uint              MaximumAllocationSize;
   uint              VirtualMemoryThreshold;
   uint              ProcessHeapFlags;
   uint              ProcessAffinityMask;
   ushort            CSDVersion;
   ushort            DependentLoadFlags;
   uint              EditList;
   uint              SecurityCookie;
   uint              SEHandlerTable;
   uint              SEHandlerCount;
   uint              GuardCFCheckFunctionPointer;
   uint              GuardCFDispatchFunctionPointer;
   uint              GuardCFFunctionTable;
   uint              GuardCFFunctionCount;
   uint              GuardFlags;
   IMAGE_LOAD_CONFIG_CODE_INTEGRITY CodeIntegrity;
   uint              GuardAddressTakenIatEntryTable;
   uint              GuardAddressTakenIatEntryCount;
   uint              GuardLongJumpTargetTable;
   uint              GuardLongJumpTargetCount;
   uint              DynamicValueRelocTable;
   uint              CHPEMetadataPointer;
   uint              GuardRFFailureRoutine;
   uint              GuardRFFailureRoutineFunctionPointer;
   uint              DynamicValueRelocTableOffset;
   ushort            DynamicValueRelocTableSection;
   ushort            Reserved2;
   uint              GuardRFVerifyStackPointerFunctionPointer;
   uint              HotPatchTableOffset;
   uint              Reserved3;
   uint              EnclaveConfigurationPointer;
  };
//---
struct IMAGE_LOAD_CONFIG_DIRECTORY64
  {
   uint              Size;
   uint              TimeDateStamp;
   ushort            MajorVersion;
   ushort            MinorVersion;
   uint              GlobalFlagsClear;
   uint              GlobalFlagsSet;
   uint              CriticalSectionDefaultTimeout;
   ulong             DeCommitFreeBlockThreshold;
   ulong             DeCommitTotalFreeThreshold;
   ulong             LockPrefixTable;
   ulong             MaximumAllocationSize;
   ulong             VirtualMemoryThreshold;
   ulong             ProcessAffinityMask;
   uint              ProcessHeapFlags;
   ushort            CSDVersion;
   ushort            DependentLoadFlags;
   ulong             EditList;
   ulong             SecurityCookie;
   ulong             SEHandlerTable;
   ulong             SEHandlerCount;
   ulong             GuardCFCheckFunctionPointer;
   ulong             GuardCFDispatchFunctionPointer;
   ulong             GuardCFFunctionTable;
   ulong             GuardCFFunctionCount;
   uint              GuardFlags;
   IMAGE_LOAD_CONFIG_CODE_INTEGRITY CodeIntegrity;
   ulong             GuardAddressTakenIatEntryTable;
   ulong             GuardAddressTakenIatEntryCount;
   ulong             GuardLongJumpTargetTable;
   ulong             GuardLongJumpTargetCount;
   ulong             DynamicValueRelocTable;
   ulong             CHPEMetadataPointer;
   ulong             GuardRFFailureRoutine;
   ulong             GuardRFFailureRoutineFunctionPointer;
   uint              DynamicValueRelocTableOffset;
   ushort            DynamicValueRelocTableSection;
   ushort            Reserved2;
   ulong             GuardRFVerifyStackPointerFunctionPointer;
   uint              HotPatchTableOffset;
   uint              Reserved3;
   ulong             EnclaveConfigurationPointer;
  };
//---
struct IMAGE_HOT_PATCH_INFO
  {
   uint              Version;
   uint              Size;
   uint              SequenceNumber;
   uint              BaseImageList;
   uint              BaseImageCount;
   uint              BufferOffset;
  };
//---
struct IMAGE_HOT_PATCH_BASE
  {
   uint              SequenceNumber;
   uint              Flags;
   uint              OriginalTimeDateStamp;
   uint              OriginalCheckSum;
   uint              CodeIntegrityInfo;
   uint              CodeIntegritySize;
   uint              PatchTable;
   uint              BufferOffset;
  };
//---
struct IMAGE_HOT_PATCH_HASHES
  {
   uchar             SHA256[32];
   uchar             SHA1[20];
  };
//---
struct IMAGE_CE_RUNTIME_FUNCTION_ENTRY
  {
   uint              FuncStart;
   uint              Flags;
  };
//---
struct IMAGE_ALPHA64_RUNTIME_FUNCTION_ENTRY
  {
   ulong             BeginAddress;
   ulong             EndAddress;
   ulong             ExceptionHandler;
   ulong             HandlerData;
   ulong             PrologEndAddress;
  };
//---
struct IMAGE_ALPHA_RUNTIME_FUNCTION_ENTRY
  {
   uint              BeginAddress;
   uint              EndAddress;
   uint              ExceptionHandler;
   uint              HandlerData;
   uint              PrologEndAddress;
  };
//---
struct IMAGE_ENCLAVE_CONFIG32
  {
   uint              Size;
   uint              MinimumRequiredConfigSize;
   uint              PolicyFlags;
   uint              NumberOfImports;
   uint              ImportList;
   uint              ImportEntrySize;
   uchar             FamilyID[IMAGE_ENCLAVE_SHORT_ID_LENGTH];
   uchar             ImageID[IMAGE_ENCLAVE_SHORT_ID_LENGTH];
   uint              ImageVersion;
   uint              SecurityVersion;
   uint              EnclaveSize;
   uint              NumberOfThreads;
   uint              EnclaveFlags;
  };
//---
struct IMAGE_ENCLAVE_CONFIG64
  {
   uint              Size;
   uint              MinimumRequiredConfigSize;
   uint              PolicyFlags;
   uint              NumberOfImports;
   uint              ImportList;
   uint              ImportEntrySize;
   uchar             FamilyID[IMAGE_ENCLAVE_SHORT_ID_LENGTH];
   uchar             ImageID[IMAGE_ENCLAVE_SHORT_ID_LENGTH];
   uint              ImageVersion;
   uint              SecurityVersion;
   ulong             EnclaveSize;
   uint              NumberOfThreads;
   uint              EnclaveFlags;
  };
//---
struct IMAGE_ENCLAVE_IMPORT
  {
   uint              MatchType;
   uint              MinimumSecurityVersion;
   uchar             UniqueOrAuthorID[IMAGE_ENCLAVE_LONG_ID_LENGTH];
   uchar             FamilyID[IMAGE_ENCLAVE_SHORT_ID_LENGTH];
   uchar             ImageID[IMAGE_ENCLAVE_SHORT_ID_LENGTH];
   uint              ImportName;
   uint              Reserved;
  };
//---
struct IMAGE_DEBUG_DIRECTORY
  {
   uint              Characteristics;
   uint              TimeDateStamp;
   ushort            MajorVersion;
   ushort            MinorVersion;
   uint              Type;
   uint              SizeOfData;
   uint              AddressOfRawData;
   uint              PointerToRawData;
  };
//---
struct IMAGE_COFF_SYMBOLS_HEADER
  {
   uint              NumberOfSymbols;
   uint              LvaToFirstSymbol;
   uint              NumberOfLinenumbers;
   uint              LvaToFirstLinenumber;
   uint              RvaToFirstByteOfCode;
   uint              RvaToLastByteOfCode;
   uint              RvaToFirstByteOfData;
   uint              RvaToLastByteOfData;
  };
//---
struct FPO_DATA
  {
   uint              ulOffStart;
   uint              cbProcSize;
   uint              cdwLocals;
   ushort            cdwParams;
   ushort            data;
  };
//---
struct IMAGE_DEBUG_MISC
  {
   uint              DataType;
   uint              Length;
   uchar             Unicode;
   uchar             Reserved[3];
   uchar             Data[1];
  };
//---
struct IMAGE_FUNCTION_ENTRY
  {
   uint              StartingAddress;
   uint              EndingAddress;
   uint              EndOfPrologue;
  };
//---
struct IMAGE_SEPARATE_DEBUG_HEADER
  {
   ushort            Signature;
   ushort            Flags;
   ushort            Machine;
   ushort            Characteristics;
   uint              TimeDateStamp;
   uint              CheckSum;
   uint              ImageBase;
   uint              SizeOfImage;
   uint              NumberOfSections;
   uint              ExportedNamesSize;
   uint              DebugDirectorySize;
   uint              SectionAlignment;
   uint              Reserved[2];
  };
//---
struct NON_PAGED_DEBUG_INFO
  {
   ushort            Signature;
   ushort            Flags;
   uint              Size;
   ushort            Machine;
   ushort            Characteristics;
   uint              TimeDateStamp;
   uint              CheckSum;
   uint              SizeOfImage;
   ulong             ImageBase;
  };
//---
struct IMAGE_ARCHITECTURE_HEADER
  {
   int               mask;
   uint              FirstEntryRVA;
  };
//---
struct IMAGE_ARCHITECTURE_ENTRY
  {
   uint              FixupInstRVA;
   uint              NewInst;
  };
//---
struct SLIST_ENTRY
  {
   PVOID             Next;
  };
//---
struct RTL_BARRIER
  {
   uint              Reserved1;
   uint              Reserved2;
   ulong             Reserved3[2];
   uint              Reserved4;
   uint              Reserved5;
  };
//---
struct MESSAGE_RESOURCE_ENTRY
  {
   ushort            Length;
   ushort            Flags;
   uchar             Text[1];
  };
//---
struct MESSAGE_RESOURCE_BLOCK
  {
   uint              LowId;
   uint              HighId;
   uint              OffsetToEntries;
  };
//---
struct MESSAGE_RESOURCE_DATA
  {
   uint              NumberOfBlocks;
   MESSAGE_RESOURCE_BLOCK Blocks[1];
  };
//---
struct OSVERSIONINFOW
  {
   uint              dwOSVersionInfoSize;
   uint              dwMajorVersion;
   uint              dwMinorVersion;
   uint              dwBuildNumber;
   uint              dwPlatformId;
   ushort            szCSDVersion[128];
  };
//---
struct OSVERSIONINFOEXW
  {
   uint              dwOSVersionInfoSize;
   uint              dwMajorVersion;
   uint              dwMinorVersion;
   uint              dwBuildNumber;
   uint              dwPlatformId;
   short             szCSDVersion[128];
   ushort            wServicePackMajor;
   ushort            wServicePackMinor;
   ushort            wSuiteMask;
   uchar             wProductType;
   uchar             wReserved;
  };
//---
struct NV_MEMORY_RANGE
  {
   PVOID             BaseAddress;
   ulong             Length;
  };
//---
struct CORRELATION_VECTOR
  {
   char              Version;
   char              Vector[RTL_CORRELATION_VECTOR_STRING_LENGTH];
  };
//---
struct CUSTOM_SYSTEM_EVENT_TRIGGER_CONFIG
  {
   uint              Size;
   const string      TriggerId;
  };
//---
struct IMAGE_POLICY_ENTRY
  {
   IMAGE_POLICY_ENTRY_TYPE Type;
   IMAGE_POLICY_ID   PolicyId;
   PVOID             Value;
  };
//---
struct IMAGE_POLICY_METADATA
  {
   uchar             Version;
   uchar             Reserved0[7];
   ulong             ApplicationId;
   IMAGE_POLICY_ENTRY Policies[];
  };
//---
struct RTL_CRITICAL_SECTION_DEBUG
  {
   ushort            Type;
   ushort            CreatorBackTraceIndex;
   PVOID             CriticalSection;
   LIST_ENTRY        ProcessLocksList;
   uint              EntryCount;
   uint              ContentionCount;
   uint              Flags;
   ushort            CreatorBackTraceIndexHigh;
   ushort            Spareushort;
  };
//---
struct RTL_CRITICAL_SECTION
  {
   PVOID             DebugInfo;
   int               LockCount;
   int               RecursionCount;
   HANDLE            OwningThread;
   HANDLE            LockSemaphore;
   ulong             SpinCount;
  };
//---
struct RTL_SRWLOCK
  {
   PVOID             Ptr;
  };
//---
struct RTL_CONDITION_VARIABLE
  {
   PVOID             Ptr;
  };
//---
struct HEAP_OPTIMIZE_RESOURCES_INFORMATION
  {
   uint              Version;
   uint              Flags;
  };
//---
struct ACTIVATION_CONTEXT_QUERY_INDEX
  {
   uint              ulAssemblyIndex;
   uint              ulFileIndexInAssembly;
  };
//---
struct ASSEMBLY_FILE_DETAILED_INFORMATION
  {
   uint              ulFlags;
   uint              ulFilenameLength;
   uint              ulPathLength;
   const string      lpFileName;
   const string      lpFilePath;
  };
//---
struct ACTIVATION_CONTEXT_ASSEMBLY_DETAILED_INFORMATION
  {
   uint              ulFlags;
   uint              ulEncodedAssemblyIdentityLength;
   uint              ulManifestPathType;
   uint              ulManifestPathLength;
   long              liManifestLastWriteTime;
   uint              ulPolicyPathType;
   uint              ulPolicyPathLength;
   long              liPolicyLastWriteTime;
   uint              ulMetadataSatelliteRosterIndex;
   uint              ulManifestVersionMajor;
   uint              ulManifestVersionMinor;
   uint              ulPolicyVersionMajor;
   uint              ulPolicyVersionMinor;
   uint              ulAssemblyDirectoryNameLength;
   const string      lpAssemblyEncodedAssemblyIdentity;
   const string      lpAssemblyManifestPath;
   const string      lpAssemblyPolicyPath;
   const string      lpAssemblyDirectoryName;
   uint              ulFileCount;
  };
//---
struct ACTIVATION_CONTEXT_RUN_LEVEL_INFORMATION
  {
   uint              ulFlags;
   ACTCTX_REQUESTED_RUN_LEVEL RunLevel;
   uint              UiAccess;
  };
//---
struct COMPATIBILITY_CONTEXT_ELEMENT
  {
   GUID              Id;
   ACTCTX_COMPATIBILITY_ELEMENT_TYPE Type;
  };
//---
struct ACTIVATION_CONTEXT_COMPATIBILITY_INFORMATION
  {
   uint              ElementCount;
   COMPATIBILITY_CONTEXT_ELEMENT Elements[];
  };
//---
struct SUPPORTED_OS_INFO
  {
   ushort            MajorVersion;
   ushort            MinorVersion;
  };
//---
struct ACTIVATION_CONTEXT_DETAILED_INFORMATION
  {
   uint              dwFlags;
   uint              ulFormatVersion;
   uint              ulAssemblyCount;
   uint              ulRootManifestPathType;
   uint              ulRootManifestPathChars;
   uint              ulRootConfigurationPathType;
   uint              ulRootConfigurationPathChars;
   uint              ulAppDirPathType;
   uint              ulAppDirPathChars;
   const string      lpRootManifestPath;
   const string      lpRootConfigurationPath;
   const string      lpAppDirPath;
  };
//---
struct HARDWARE_COUNTER_DATA
  {
   HARDWARE_COUNTER_TYPE Type;
   uint              Reserved;
   ulong             Value;
  };
//---
struct PERFORMANCE_DATA
  {
   ushort            Size;
   uchar             Version;
   uchar             HwCountersCount;
   uint              ContextSwitchCount;
   ulong             WaitReasonBitMap;
   ulong             CycleTime;
   uint              RetryCount;
   uint              Reserved;
   HARDWARE_COUNTER_DATA HwCounters[MAX_HW_COUNTERS];
  };
//---
struct EVENTLOGRECORD
  {
   uint              Length;
   uint              Reserved;
   uint              RecordNumber;
   uint              TimeGenerated;
   uint              TimeWritten;
   uint              EventID;
   ushort            EventType;
   ushort            NumStrings;
   ushort            EventCategory;
   ushort            ReservedFlags;
   uint              ClosingRecordNumber;
   uint              StringOffset;
   uint              UserSidLength;
   uint              UserSidOffset;
   uint              DataLength;
   uint              DataOffset;
  };
//---
struct TAPE_ERASE
  {
   uint              Type;
   uchar             Immediate;
  };
//---
struct TAPE_PREPARE
  {
   uint              Operation;
   uchar             Immediate;
  };
//---
struct TAPE_WRITE_MARKS
  {
   uint              Type;
   uint              Count;
   uchar             Immediate;
  };
//---
struct TAPE_GET_POSITION
  {
   uint              Type;
   uint              Partition;
   long              Offset;
  };
//---
struct TAPE_SET_POSITION
  {
   uint              Method;
   uint              Partition;
   long              Offset;
   uchar             Immediate;
  };
//---
struct TAPE_GET_DRIVE_PARAMETERS
  {
   uchar             ECC;
   uchar             Compression;
   uchar             DataPadding;
   uchar             ReportSetmarks;
   uint              DefaultBlockSize;
   uint              MaximumBlockSize;
   uint              MinimumBlockSize;
   uint              MaximumPartitionCount;
   uint              FeaturesLow;
   uint              FeaturesHigh;
   uint              EOTWarningZoneSize;
  };
//---
struct TAPE_SET_DRIVE_PARAMETERS
  {
   uchar             ECC;
   uchar             Compression;
   uchar             DataPadding;
   uchar             ReportSetmarks;
   uint              EOTWarningZoneSize;
  };
//---
struct TAPE_GET_MEDIA_PARAMETERS
  {
   long              Capacity;
   long              Remaining;
   uint              BlockSize;
   uint              PartitionCount;
   uchar             WriteProtected;
  };
//---
struct TAPE_SET_MEDIA_PARAMETERS
  {
   uint              BlockSize;
  };
//---
struct TAPE_CREATE_PARTITION
  {
   uint              Method;
   uint              Count;
   uint              Size;
  };
//---
struct TAPE_WMI_OPERATIONS
  {
   uint              Method;
   uint              DataBufferSize;
   PVOID             DataBuffer;
  };
//---
struct TRANSACTION_BASIC_INFORMATION
  {
   GUID              TransactionId;
   uint              State;
   uint              Outcome;
  };
//---
struct TRANSACTIONMANAGER_BASIC_INFORMATION
  {
   GUID              TmIdentity;
   long              VirtualClock;
  };
//---
struct TRANSACTIONMANAGER_LOG_INFORMATION
  {
   GUID              LogIdentity;
  };
//---
struct TRANSACTIONMANAGER_LOGPATH_INFORMATION
  {
   uint              LogPathLength;
   short             LogPath[1];
  };
//---
struct TRANSACTIONMANAGER_RECOVERY_INFORMATION
  {
   ulong             LastRecoveredLsn;
  };
//---
struct TRANSACTIONMANAGER_OLDEST_INFORMATION
  {
   GUID              OldestTransactionGuid;
  };
//---
struct TRANSACTION_PROPERTIES_INFORMATION
  {
   uint              IsolationLevel;
   uint              IsolationFlags;
   long              Timeout;
   uint              Outcome;
   uint              DescriptionLength;
   short             Description[1];
  };
//---
struct TRANSACTION_BIND_INFORMATION
  {
   HANDLE            TmHandle;
  };
//---
struct TRANSACTION_ENLISTMENT_PAIR
  {
   GUID              EnlistmentId;
   GUID              ResourceManagerId;
  };
//---
struct TRANSACTION_ENLISTMENTS_INFORMATION
  {
   uint              NumberOfEnlistments;
   TRANSACTION_ENLISTMENT_PAIR EnlistmentPair[1];
  };
//---
struct TRANSACTION_SUPERIOR_ENLISTMENT_INFORMATION
  {
   TRANSACTION_ENLISTMENT_PAIR SuperiorEnlistmentPair;
  };
//---
struct RESOURCEMANAGER_BASIC_INFORMATION
  {
   GUID              ResourceManagerId;
   uint              DescriptionLength;
   short             Description[1];
  };
//---
struct RESOURCEMANAGER_COMPLETION_INFORMATION
  {
   HANDLE            IoCompletionPortHandle;
   ulong             CompletionKey;
  };
//---
struct ENLISTMENT_BASIC_INFORMATION
  {
   GUID              EnlistmentId;
   GUID              TransactionId;
   GUID              ResourceManagerId;
  };
//---
struct ENLISTMENT_CRM_INFORMATION
  {
   GUID              CrmTransactionManagerId;
   GUID              CrmResourceManagerId;
   GUID              CrmEnlistmentId;
  };
//---
struct TRANSACTION_LIST_ENTRY
  {
   GUID              UOW;
  };
//---
struct TRANSACTION_LIST_INFORMATION
  {
   uint              NumberOfTransactions;
   TRANSACTION_LIST_ENTRY TransactionInformation[1];
  };
//---
struct KTMOBJECT_CURSOR
  {
   GUID              LastQuery;
   uint              ObjectIdCount;
   GUID              ObjectIds[1];
  };
//---
struct TP_POOL_STACK_INFORMATION
  {
   ulong             StackReserve;
   ulong             StackCommit;
  };
//---
struct TP_CALLBACK_ENVIRON_V3
  {
   uint              Version;
   PVOID             Pool;
   PVOID             CleanupGroup;
   PVOID             CleanupGroupCancelCallback;
   PVOID             RaceDll;
   PVOID             ActivationContext;
   PVOID             FinalizationCallback;
   uint              Flags;
   TP_CALLBACK_PRIORITY CallbackPriority;
   uint              Size;
  };
//---
struct SYSTEM_LOGICAL_PROCESSOR_INFORMATION
  {
   ulong             ProcessorMask;
   LOGICAL_PROCESSOR_RELATIONSHIP Relationship;
   uchar             offset[4];
   ulong             Reserved[2];
  };
//---
struct SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX
  {
   LOGICAL_PROCESSOR_RELATIONSHIP Relationship;
   uint              Size;
   uchar             info[72];
  };
//---
struct SYSTEM_CPU_SET_INFORMATION
  {
   uint              Size;
   CPU_SET_INFORMATION_TYPE Type;
   uint              Id;
   ushort            Group;
   uchar             LogicalProcessorIndex;
   uchar             CoreIndex;
   uchar             LastLevelCacheIndex;
   uchar             NumaNodeIndex;
   uchar             EfficiencyClass;
   uchar             AllFlags;
   uint              Reserved;
   ulong             AllocationTag;
  };
//+------------------------------------------------------------------+
//|                                                                  |
//+------------------------------------------------------------------+
#import "kernel32.dll"
ushort   RtlCaptureStackBackTrace(uint frames_to_skip,uint frames_to_capture,PVOID &back_trace[],uint &back_trace_hash);
ulong    RtlCompareMemory(const uchar &source1[],const uchar &source2[],ulong length);
ulong    VerSetConditionMask(ulong condition_mask,uint type_mask,uchar condition);
#import

#import "Win32k.sys"
void     RtlCaptureContext(PVOID context_record);
void     RtlUnwind(PVOID target_frame,PVOID target_ip,EXCEPTION_RECORD &exception_record,PVOID return_value);
PVOID    RtlLookupFunctionEntry(ulong control_pc,PVOID image_base,UNWIND_HISTORY_TABLE &history_table);
void     RtlUnwindEx(PVOID target_frame,PVOID target_ip,EXCEPTION_RECORD &exception_record,PVOID return_value,PVOID context_record,UNWIND_HISTORY_TABLE &history_table);
PVOID    RtlVirtualUnwind(uint handler_type,ulong image_base,ulong control_pc,PVOID function_entry,PVOID context_record,PVOID &handler_data,PVOID establisher_frame,KNONVOLATILE_CONTEXT_POINTERS &context_pointers);
PVOID    RtlLookupFunctionEntry(ulong control_pc,uint &image_base,UNWIND_HISTORY_TABLE &history_table);
void     RtlUnwindEx(PVOID target_frame,PVOID target_ip,EXCEPTION_RECORD &exception_record,PVOID return_value,PVOID context_record,UNWIND_HISTORY_TABLE &history_table);
PVOID    RtlVirtualUnwind(uint handler_type,uint image_base,uint control_pc,PVOID function_entry,PVOID context_record,PVOID &handler_data,uint &establisher_frame,KNONVOLATILE_CONTEXT_POINTERS &context_pointers);
PVOID    RtlLookupFunctionEntry(ulong control_pc,PVOID image_base,UNWIND_HISTORY_TABLE &history_table);
void     RtlUnwindEx(PVOID target_frame,PVOID target_ip,EXCEPTION_RECORD &exception_record,PVOID return_value,PVOID context_record,UNWIND_HISTORY_TABLE &history_table);
PVOID    RtlVirtualUnwind(uint handler_type,ulong image_base,ulong control_pc,PVOID function_entry,PVOID context_record,PVOID &handler_data,PVOID establisher_frame,KNONVOLATILE_CONTEXT_POINTERS &context_pointers);
void     RtlUnwindEx(PVOID target_frame,PVOID target_ip,EXCEPTION_RECORD &exception_record,PVOID return_value,PVOID context_record,PVOID history_table);
PVOID    RtlPcToFileHeader(PVOID pc_value,PVOID &base_of_image);
#import
//+------------------------------------------------------------------+
