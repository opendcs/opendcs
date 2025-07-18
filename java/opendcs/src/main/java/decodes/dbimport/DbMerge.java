/**
 * Copyright 2025 The OpenDCS Consortium and contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package decodes.dbimport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.slf4j.LoggerFactory;

import decodes.db.DataSource;
import decodes.db.DataType;
import decodes.db.Database;
import decodes.db.DbEnum;
import decodes.db.EngineeringUnit;
import decodes.db.EnumValue;
import decodes.db.EquipmentModel;
import decodes.db.IdDatabaseObject;
import decodes.db.NetworkList;
import decodes.db.Platform;
import decodes.db.PlatformConfig;
import decodes.db.PlatformSensor;
import decodes.db.PresentationGroup;
import decodes.db.RoutingSpec;
import decodes.db.RoutingSpecList;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.db.TransportMedium;
import decodes.db.UnitConverterDb;
import decodes.sql.DbKey;
import decodes.xml.EnumParser;

public class DbMerge
{

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(DbMerge.class);

	private final Database destination;
	private final Database source;
	private final String newDesignator;
	private final boolean overwriteDb;
	private final boolean validateOnly;
	private final boolean keepOld;

	// tracks if new platforms were written
	private boolean writePlatformList;
	private List<IdDatabaseObject> newObjects = new ArrayList<>();

	private DbMerge(Builder b)
	{
		this.destination = b.destination;
		this.source = b.source;
		this.newDesignator = b.newDesignator;
		this.overwriteDb = b.overwriteDb;
		this.validateOnly = b.validateOnly;
		this.keepOld = b.keepOld;
	}

	public boolean getWritePlatformList()
	{
		return writePlatformList;
	}

	public List<IdDatabaseObject> getImmutableNewObjects()
	{
		return Collections.unmodifiableList(newObjects);
	}

	public void merge()
	{
		Database theDb = destination;
		Database stageDb = source;
		Database.setDb(theDb);

		if (overwriteDb)
		{
			theDb.engineeringUnitList.clear();
			theDb.unitConverterSet.clear();
			theDb.dataTypeSet.clear();
			if (EnumParser.enumParsed)
			{
				theDb.enumList.clear();
			}
		}

		// An EU is just a bean, not a DatabaseObject. So I can just copy the
		// objects
		// from stage into db-to-write.
		for (Iterator<EngineeringUnit> euit = stageDb.engineeringUnitList.iterator(); euit.hasNext();)
		{
			theDb.engineeringUnitList.add(euit.next());
		}
		// A UnitConverterDb is an IdDatabaseObject, so it knows which database
		// it
		// belongs to. Therefore, I have to make copies in the db to write.
		for (Iterator<UnitConverterDb> ucdbit = stageDb.unitConverterSet.iteratorDb(); ucdbit.hasNext();)
		{
			UnitConverterDb stageUC = ucdbit.next();
			UnitConverterDb dbUC = stageUC.copy();
			dbUC.clearId();
			theDb.unitConverterSet.addDbConverter(dbUC);
		}
		// Likewise, a DataType is an IdDatabaseObject, so have to make copies.
		for (Iterator<DataType> dtit = stageDb.dataTypeSet.iterator(); dtit.hasNext();)
		{
			DataType stageDT = dtit.next();
			// getDataType will create it in the current database ('theDb')
			DataType newDT = DataType.getDataType(stageDT.getStandard(), stageDT.getCode());
			newDT.setDisplayName(stageDT.getDisplayName());
		}

		log.trace("mergeStageToTheDb 1: #EUs={}", theDb.engineeringUnitList.size());
		log.trace("mergeStageToTheDb 3: #stageEUs={}", stageDb.engineeringUnitList.size());

		if (validateOnly)
		{
			log.info("The following messages indicate what WOULD BE modified in the"
					+ " database. No changes will actually been made.");
		}

		if (EnumParser.enumParsed)
		{
			for (Iterator<DbEnum> it = stageDb.enumList.iterator(); it.hasNext();)
			{
				DbEnum stageOb = it.next();
				DbEnum oldOb = theDb.getDbEnum(stageOb.enumName);
				if (oldOb == null)
				{
					log.info("Adding new Enum '{}'", stageOb.enumName);
					theDb.enumList.addEnum(stageOb);
					for (Iterator<EnumValue> evit = stageOb.iterator(); evit.hasNext();)
					{
						EnumValue ev = evit.next();
						log.info("  Value  {} - {}", ev.getValue(), ev.getDescription());
					}
				} else
				{
					if (!keepOld)
					{
						log.info("Overwriting Enum '{}'", stageOb.enumName);
						for (Iterator<EnumValue> evit = stageOb.iterator(); evit.hasNext();)
						{
							EnumValue ev = evit.next();
							log.info("  DbEnum:AddingOrReplacing  {} - {}", ev.getValue(), ev.getDescription());
							oldOb.replaceValue(ev.getValue(), ev.getDescription(), ev.getExecClassName(),
									ev.getEditClassName());
						}
					} else
					{
						log.info("Keeping old version of Enum '{}'", stageOb.enumName);
					}
				}
			}
		}

		for (Iterator<NetworkList> it = stageDb.networkListList.iterator(); it.hasNext();)
		{
			NetworkList ob = it.next();
			NetworkList oldOb = theDb.networkListList.find(ob.name);

			if (oldOb == null)
			{
				log.info("Adding new {} '{}'", ob.getObjectType(), ob.name);
				theDb.networkListList.add(ob);
				newObjects.add(ob);
			} else if (!oldOb.equals(ob))
			{
				if (!keepOld)
				{
					log.info("Overwriting {} '{}'", oldOb.getObjectType(), ob.name);
					theDb.networkListList.add(ob);
					newObjects.add(ob);
				} else
				{
					log.info("Keeping old version of {} '{}'", oldOb.getObjectType(), ob.name);
				}
			}
		}

		// If a designator was specified with -G arg, add it to all platforms in
		// the
		// stage
		// db that have no designator.
		if (newDesignator != null)
		{
			for (Platform p : stageDb.platformList.getPlatformVector())
			{
				if ((p.getPlatformDesignator() == null || p.getPlatformDesignator().trim().length() == 0))
				{
					p.setPlatformDesignator(newDesignator);
				}
			}
		}

		// Platform matching is tricky because there are two unique keys.
		// The main key is by matching Site and Platform Designator.
		// The secondary key is by matching a transport medium (TM).
		//
		// There are 5 possible use cases:
		// 1. (site,desig) matches existing platform AND TM matches same
		// platform
		// ==> New platform replaces existing
		// 2. (site,desig) matches existing platform. TM does not match any
		// platform
		// ==> New platform replaces existing
		// 3. (site,desig) matches existing platform, but TM matches a different
		// platform.
		// ==> New platform replaces existing
		// ==> Remove TM from the different existing platform, and if that
		// platform now
		// has
		// no remaining TMs, remove it entirely.
		// 4. No match for (site,desig). No match for TM.
		// ==> Import new platform
		// 5. No match for (site,desig). There is a match for TM.
		// ==> Import New Platform
		// ==> Remove TM from the different existing platform, and if that
		// platform now
		// has
		// no remaining TMs, remove it entirely.
		writePlatformList = false;
		for (Iterator<Platform> it = stageDb.platformList.iterator(); it.hasNext();)
		{
			Platform newPlat = it.next();
			if (log.isTraceEnabled())
			{
				log.trace("merging platform '{}'", newPlat.getDisplayName());
				for (PlatformSensor ps : newPlat.platformSensors)
				{
					log.trace("   Sensor {}" + ps.sensorNumber + ": actualSite={}", ps.sensorNumber,
							(ps.site == null ? "null" : ps.site.getPreferredName()));
				}
			}

			if (newPlat.getSite() == null)
			{
				log.warn("Skipping platform with ID={} in the XML input file because " + "it has no associated site.",
						newPlat.getId());
				continue;
			}

			Platform oldPlatformMatch = null;

			// See if a matching old site exists
			Site oldSite = null;
			for (SiteName newPlatSiteName : newPlat.getSite().getNameArray())
			{
				if ((oldSite = theDb.siteList.getSite(newPlatSiteName)) != null)
				{
					break;
				}
			}
			if (oldSite == null)
			{
				for (SiteName sn : newPlat.getSite().getNameArray())
				{
					if ((oldSite = theDb.siteList.getSite(sn)) != null)
					{
						break;
					}
				}
			}
			log.trace("    site does {} exist in old database.", (oldSite == null ? "not " : ""));

			// Then find an old platform with matching (site,designator)
			if (oldSite != null)
			{
				oldPlatformMatch = theDb.platformList.findPlatform(oldSite, newPlat.getPlatformDesignator());
				log.trace(
						"    Old platform does {} exist with matching site/desig siteId={}"
								+ ", sitename={}, desig='{}'",
						(oldPlatformMatch == null ? "not" : ""), oldSite.getId(), oldSite.getPreferredName(),
						newPlat.getPlatformDesignator());
			}

			// Try to find existing platform with a matching transport id.
			Platform oldTmMatch = null;
			for (Iterator<TransportMedium> tmit = newPlat.transportMedia.iterator(); oldTmMatch == null
					&& tmit.hasNext();)
			{
				TransportMedium tm = tmit.next();
				Date d = newPlat.expiration;
				log.trace("    Looking for match to TM {} with expiration {}", tm.toString(), d);
				oldTmMatch = theDb.platformList.findPlatform(tm.getMediumType(), tm.getMediumId(), d);
				final Platform logOldTmMatch = oldTmMatch;
				log.atTrace().log(() -> "        - Match was "
						+ (logOldTmMatch == null ? "not found." : ("found with id=" + logOldTmMatch.getId())));
				oldPlatformMatch = oldTmMatch;
			}

			if (oldPlatformMatch == null)
			{
				// use cases 4 & 5: This is a NEW platform.
				log.info("Adding New Platform '{}'", newPlat.makeFileName());
				theDb.platformList.add(newPlat);
				newObjects.add(newPlat);
				if (log.isTraceEnabled())
				{
					log.trace("Added new platform '{}' to newObjects List", newPlat.makeFileName());
					for (PlatformSensor ps : newPlat.platformSensors)
					{
						log.trace("   Sensor {}: actualSite={}", ps.sensorNumber,
								(ps.site == null ? "null" : ps.site.getPreferredName()));
					}
				}
				writePlatformList = true;
			} else if (!oldPlatformMatch.equals(newPlat))
			{
				// use cases 1, 2, and 3: There was a match for (site,desig)
				if (!keepOld)
				{
					log.info("Overwriting Platform '{}'", newPlat.makeFileName());

					DbKey oldId = oldPlatformMatch.getId();
					theDb.platformList.removePlatform(oldPlatformMatch);
					newPlat.clearId();
					try
					{
						newPlat.setId(oldId);
					} catch (Exception ex)
					{
						log.atError().setCause(ex)
								.log("An exception was thrown in a call that should never throw an exception.");
					}
					log.info("set platform ID to match existing ID={}", oldId);
					theDb.platformList.add(newPlat);

					newObjects.add(newPlat);

					log.debug("Added platform '{}' with id={} and siteid={} to newObjects list.",
							newPlat.makeFileName(), newPlat.getId(),
							(newPlat.getSite() == null ? "<nullsite!>" : newPlat.getSite().getId()));
					if (log.isTraceEnabled())
					{
						for (PlatformSensor ps : newPlat.platformSensors)
						{
							log.trace("   sensor {}" + ps.sensorNumber + " actualSite={}", ps.sensorNumber,
									(ps.site == null ? "null" : ps.site.getPreferredName()));
						}
					}
					writePlatformList = true;
				} else
				{
					log.info("Keeping old version of {} '{}'", oldPlatformMatch.getObjectType(),
							newPlat.makeFileName());
				}
			}
		}

		for (Iterator<PresentationGroup> it = stageDb.presentationGroupList.iterator(); it.hasNext();)
		{
			PresentationGroup ob = it.next();
			PresentationGroup oldOb = theDb.presentationGroupList.find(ob.groupName);

			if (oldOb == null)
			{
				log.info("Adding new {} '{}'", ob.getObjectType(), ob.groupName);
				theDb.presentationGroupList.add(ob);
				newObjects.add(ob);
			} else if (!oldOb.equals(ob))
			{
				if (!keepOld)
				{
					log.info("Overwriting {} '{}'", oldOb.getObjectType(), ob.groupName);
					theDb.presentationGroupList.add(ob);
					newObjects.add(ob);
				} else
				{
					log.info("Keeping old version of {} '{}'", oldOb.getObjectType(), ob.groupName);
				}
			}
		}

		// MJM 6/3/04 - Must do DataSources before Routing Specs so that
		// new DS records will have an ID for the RS to reference.
		for (Iterator<DataSource> it = stageDb.dataSourceList.iterator(); it.hasNext();)
		{
			DataSource ob = it.next();
			DataSource oldOb = theDb.dataSourceList.get(ob.getName());

			if (oldOb == null)
			{
				log.info("Adding new {} '{}'", ob.getObjectType(), ob.getName());
				theDb.dataSourceList.add(ob);
				newObjects.add(ob);
			} else if (!oldOb.equals(ob))
			{
				if (!keepOld)
				{
					log.info("Overwriting {} '{}'", oldOb.getObjectType(), ob.getName());
					theDb.dataSourceList.add(ob);
					newObjects.add(ob);
				} else
				{
					log.info("Keeping old version of {} '{}'", oldOb.getObjectType(), ob.getName());
				}
			} else
			{
				log.info("Imported data source '{}' is unchanged from DB version.", ob.getName());
				ob.forceSetId(oldOb.getId());
			}
		}

		RoutingSpecList.silentFind = true;
		for (Iterator<RoutingSpec> it = stageDb.routingSpecList.iterator(); it.hasNext();)
		{
			RoutingSpec ob = it.next();
			RoutingSpec oldOb = theDb.routingSpecList.find(ob.getName());

			if (oldOb == null)
			{
				log.info("Adding new {} '{}'", ob.getObjectType(), ob.getName());
				theDb.routingSpecList.add(ob);
				newObjects.add(ob);
			} else if (!oldOb.equals(ob))
			{
				if (!keepOld)
				{
					log.info("Overwriting {} '{}'", oldOb.getObjectType(), ob.getName());
					theDb.routingSpecList.add(ob);
					newObjects.add(ob);
				} else
				{
					log.info("Keeping old version of {} '{}'", oldOb.getObjectType(), ob.getName());
				}
			}
		}

		for (Iterator<Site> it = stageDb.siteList.iterator(); it.hasNext();)
		{
			Site ob = it.next();
			if (ob.getPreferredName() == null)
			{
				log.warn("Import file contained a site with no name. Ignoring.");
				continue;
			}
			Site oldOb = theDb.siteList.getSite(ob.getPreferredName());

			if (oldOb == null)
			{
				log.info("Adding new {} '{}'", ob.getObjectType(), ob.getPreferredName());
				theDb.siteList.addSite(ob);
				newObjects.add(ob);
			} else if (!oldOb.equals(ob))
			{
				if (!keepOld)
				{
					log.info("Overwriting {} '{}'", oldOb.getObjectType(), ob.getPreferredName());
					theDb.siteList.addSite(ob);
					newObjects.add(ob);
				} else
				{
					log.info("Keeping old version of {} '{}'", oldOb.getObjectType(), ob.getPreferredName());
				}
			}
		}

		// Then make sure that all new equivalences are asserted in the new DB.
		// Note: data-type equivalences can never be unasserted by dbimport.
		// The only way to do that is to flush the database and re-import.
		for (Iterator<DataType> it = stageDb.dataTypeSet.iterator(); it.hasNext();)
		{
			DataType stageOb = it.next();
			DataType theOb = theDb.dataTypeSet.get(stageOb.getStandard(), stageOb.getCode());
			if (theOb == null)
			{
				continue; // shouldn't happen.
			}

			// loop through this dt's equivalences in the stage db.
			for (DataType sdt = stageOb.equivRing; sdt != null && sdt != stageOb; sdt = sdt.equivRing)
			{
				// Fetch the copy of this data type that's in the new DB.
				DataType tdt = DataType.getDataType(sdt.getStandard(), sdt.getCode());
				if (!theOb.isEquivalent(tdt))
				{
					log.debug("Asserting equivalence between data types '{}' and '{}'", theOb.toString(),
							tdt.toString());
					theOb.assertEquivalence(tdt);
				}
			}
		}

		for (PlatformConfig ob : stageDb.platformConfigList.values())
		{
			PlatformConfig oldOb = theDb.platformConfigList.get(ob.configName);

			if (oldOb == null)
			{
				log.info("Adding new {} '{}'", ob.getObjectType(), ob.configName);
				theDb.platformConfigList.add(ob);
				newObjects.add(ob);
			} else if (!oldOb.equals(ob))
			{
				if (!keepOld)
				{
					log.info("Overwriting {} '{}'", oldOb.getObjectType(), ob.configName);
					theDb.platformConfigList.add(ob);
					newObjects.add(ob);
				} else
				{
					log.info("Keeping old version of {} '{}'", oldOb.getObjectType(), ob.configName);
				}
			}
		}

		for (Iterator<EquipmentModel> it = stageDb.equipmentModelList.iterator(); it.hasNext();)
		{
			EquipmentModel ob = it.next();
			EquipmentModel oldOb = theDb.equipmentModelList.get(ob.name);

			if (oldOb == null)
			{
				log.info("Adding new {} '{}'", ob.getObjectType(), ob.name);
				theDb.equipmentModelList.add(ob);
				newObjects.add(ob);
			} else if (!oldOb.equals(ob))
			{
				if (!keepOld)
				{
					log.info("Overwriting {} '{}'", oldOb.getObjectType(), ob.name);
					theDb.equipmentModelList.add(ob);
					newObjects.add(ob);
				} else
				{
					log.info("Keeping old version of {} '{}'", oldOb.getObjectType(), ob.name);
				}
			}

		}

	}

	public static class Builder
	{
		// required
		private final Database destination;
		private final Database source;

		// optional – defaults
		private String newDesignator = null;
		private boolean overwriteDb = false;
		private boolean validateOnly = false;
		private boolean keepOld = false;

		/**
		 * Builder constructor enforces the two required parameters.
		 */
		public Builder(Database destination, Database source)
		{
			if (destination == null || source == null)
			{
				throw new IllegalArgumentException("Destination and source must not be null");
			}
			this.destination = destination;
			this.source = source;
		}

		public Builder overwriteDb(boolean flag)
		{
			this.overwriteDb = flag;
			return this;
		}

		public Builder validateOnly(boolean flag)
		{
			this.validateOnly = flag;
			return this;
		}

		public Builder keepOld(boolean flag)
		{
			this.keepOld = flag;
			return this;
		}

		public Builder newDesignator(String designator)
		{
			this.newDesignator = designator;
			return this;
		}

		/**
		 * Builds the DbMerge instance.
		 */
		public DbMerge build()
		{
			return new DbMerge(this);
		}
	}
}
