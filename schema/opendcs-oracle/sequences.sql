-- 
-- Sequences for OPEN TSDB Tables
--


-- Used to assign IDs for new sites:
CREATE SEQUENCE SiteIdSeq  nocache;

-- Used to assign IDs to new EquipmentModel records:
CREATE SEQUENCE EquipmentIdSeq  nocache;

-- Used to assign IDs to new Enum records:
CREATE SEQUENCE EnumIdSeq  nocache;

-- Used to assign IDs to new DataType records:
CREATE SEQUENCE DataTypeIdSeq  nocache;

-- Used to assign IDs to new Platform records:
CREATE SEQUENCE PlatformIdSeq  nocache;

-- Used to assign IDs to new PlatformConfig records:
CREATE SEQUENCE PlatformConfigIdSeq  nocache;

-- Used to assign IDs to new DecodesScript records:
CREATE SEQUENCE DecodesScriptIdSeq  nocache;

-- Used to assign IDs to new RoutingSpec records:
CREATE SEQUENCE RoutingSpecIdSeq  nocache;

-- Used to assign IDs to new DataSource records:
CREATE SEQUENCE DataSourceIdSeq  nocache;

-- Used to assign IDs to new Network List records:
CREATE SEQUENCE NetworkListIdSeq  nocache;

-- Used to assign IDs to new PresentationGroup records:
CREATE SEQUENCE PresentationGroupIdSeq  nocache;

-- Used to assign IDs to new DataPresentation records:
CREATE SEQUENCE DataPresentationIdSeq  nocache;

-- Used to assign IDs to new UnitConverter records:
CREATE SEQUENCE UnitConverterIdSeq  nocache;

CREATE SEQUENCE CP_COMP_TASKLISTIdSeq  nocache;
CREATE SEQUENCE CP_ALGORITHMIdSeq nocache;
CREATE SEQUENCE CP_COMPUTATIONIdSeq nocache;
CREATE SEQUENCE HDB_LOADING_APPLICATIONIdSeq  nocache;
CREATE SEQUENCE tsdb_data_sourceIdSeq  nocache;
CREATE SEQUENCE tsdb_groupIdSeq nocache;
CREATE SEQUENCE interval_codeIdSeq nocache;
CREATE SEQUENCE SCHEDULE_ENTRYIdSeq nocache;
CREATE SEQUENCE SCHEDULE_ENTRY_STATUSIdSeq nocache;
CREATE SEQUENCE DACQ_EVENTIdSeq nocache;

CREATE SEQUENCE ALARM_GROUPIdSeq nocache;
CREATE SEQUENCE ALARM_EVENTIdSeq nocache;
CREATE SEQUENCE ALARM_SCREENINGIdSeq nocache;
CREATE SEQUENCE ALARM_LIMIT_SETIdSeq nocache;

CREATE SEQUENCE TS_SPECIdSeq nocache;
CREATE SEQUENCE TSDB_DATA_SOURCEIdSeq;

CREATE SEQUENCE CP_DEPENDS_NOTIFYIdSeq;
