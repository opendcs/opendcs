-- 
-- Sequences for OPEN TSDB Tables
--


-- Used to assign IDs for new sites:
CREATE SEQUENCE SiteIdSeq ;

-- Used to assign IDs to new EquipmentModel records:
CREATE SEQUENCE EquipmentIdSeq ;

-- Used to assign IDs to new Enum records:
CREATE SEQUENCE EnumIdSeq ;

-- Used to assign IDs to new DataType records:
CREATE SEQUENCE DataTypeIdSeq ;

-- Used to assign IDs to new Platform records:
CREATE SEQUENCE PlatformIdSeq ;

-- Used to assign IDs to new PlatformConfig records:
CREATE SEQUENCE PlatformConfigIdSeq ;

-- Used to assign IDs to new DecodesScript records:
CREATE SEQUENCE DecodesScriptIdSeq ;

-- Used to assign IDs to new RoutingSpec records:
CREATE SEQUENCE RoutingSpecIdSeq ;

-- Used to assign IDs to new DataSource records:
CREATE SEQUENCE DataSourceIdSeq ;

-- Used to assign IDs to new Network List records:
CREATE SEQUENCE NetworkListIdSeq ;

-- Used to assign IDs to new PresentationGroup records:
CREATE SEQUENCE PresentationGroupIdSeq ;

-- Used to assign IDs to new DataPresentation records:
CREATE SEQUENCE DataPresentationIdSeq ;

-- Used to assign IDs to new UnitConverter records:
CREATE SEQUENCE UnitConverterIdSeq ;

CREATE SEQUENCE CP_COMP_TASKLISTIdSeq ;
CREATE SEQUENCE CP_ALGORITHMIdSeq;
CREATE SEQUENCE CP_COMPUTATIONIdSeq;
CREATE SEQUENCE HDB_LOADING_APPLICATIONIdSeq ;
CREATE SEQUENCE tsdb_data_sourceIdSeq ;
CREATE SEQUENCE tsdb_groupIdSeq;
CREATE SEQUENCE interval_codeIdSeq;
CREATE SEQUENCE SCHEDULE_ENTRYIdSeq;
CREATE SEQUENCE SCHEDULE_ENTRY_STATUSIdSeq;
CREATE SEQUENCE DACQ_EVENTIdSeq;

CREATE SEQUENCE ALARM_GROUPIdSeq;
CREATE SEQUENCE ALARM_EVENTIdSeq;
CREATE SEQUENCE ALARM_SCREENINGIdSeq;
CREATE SEQUENCE ALARM_LIMIT_SETIdSeq;

CREATE SEQUENCE TS_SPECIdSeq;
CREATE SEQUENCE TSDB_DATA_SOURCEIdSeq;

CREATE SEQUENCE CP_DEPENDS_NOTIFYIdSeq;
