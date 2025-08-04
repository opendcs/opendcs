package decodes.dbimport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.opendcs.database.SimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.DatabaseObject;
import decodes.db.NetworkList;
import decodes.db.Platform;
import decodes.db.PlatformConfig;
import decodes.db.PlatformList;
import decodes.db.PresentationGroup;
import decodes.db.RoutingSpec;
import decodes.db.ScheduleEntry;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.tsdb.CompAppInfo;
import decodes.xml.DataTypeEquivalenceListParser;
import decodes.xml.ElementFilter;
import decodes.xml.EngineeringUnitParser;
import decodes.xml.EnumParser;
import decodes.xml.TopLevelParser;
import decodes.xml.UnitConverterParser;
import decodes.xml.XmlDatabaseIO;
import decodes.xml.XmlDbTags;

/**
 * Handles reading XML files into a database for the DbImport process.
 */
public class DbXmlReader {
    
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(DbImport.class);

    private final List<String> files;
    private final boolean platformRelatedOnly;
    private final boolean allowHistorical;
    private final Database stageDb;
    private final TopLevelParser topParser;
	
    
    /**
     * Constructs a new XML reader for database import
     * 
     * @param files List of XML file paths to process
     * @param platformRelatedOnly Whether to import only platform-related elements
     * @param allowHistorical Whether to import historical versions
     * @param stageDb The staging database to read into
     * @param topParser The XML parser to use
     */
    public DbXmlReader(List<String> files, boolean platformRelatedOnly,
                     boolean allowHistorical, Database stageDb) throws DatabaseException 
    {
        this.files = files;
        this.platformRelatedOnly = platformRelatedOnly;
        this.allowHistorical = allowHistorical;
        this.stageDb = stageDb;

		javax.sql.DataSource ds = new SimpleDataSource("jdbc:xml:", "", "");
		XmlDatabaseIO stageDbio = new XmlDatabaseIO(ds, null);
		stageDb.setDbIo(stageDbio);
		topParser = stageDbio.getParser();

    }
    
    /**
     * Reads XML files into the database
     * 
     * @throws IOException If there is an error reading the files
     * @throws DatabaseException If there is an error adding objects to the database
     */
    public void readFiles() throws IOException, DatabaseException {
        
        // Reset parsing flags
        resetParsingFlags();

        // Read all the files into the database
        for(String filePath : files) {
            log.info("Processing '{}'", filePath);
            DatabaseObject importedObject = null;

            // Set filter if needed
            if (platformRelatedOnly) {
                topParser.setElementFilter(createPlatformRelatedFilter());
            }

            try {
                importedObject = topParser.parse(new File(filePath));
            } catch(SAXException ex) {
                String msg = "Unable to process " + filePath;
                throw new IOException(msg, ex);
            }

            processImportedObject(importedObject);
        }

        postProcessImportedObjects();
        
    }
    
    /**
     * set XML parsing flags to false
     */
    private void resetParsingFlags() {
        EnumParser.enumParsed = false;
        EngineeringUnitParser.engineeringUnitsParsed = false;
        UnitConverterParser.unitConvertersParsed = false;
        DataTypeEquivalenceListParser.dtEquivalencesParsed = false;
    }
    
    /**
     * Creates a filter to only accept platform-related XML elements
     */
    private ElementFilter createPlatformRelatedFilter() {
        return new ElementFilter() {
            @Override
            public boolean acceptElement(String elementName) {
                return elementName.equalsIgnoreCase(XmlDbTags.Platform_el)
                    || elementName.equalsIgnoreCase(XmlDbTags.NetworkList_el)
                    || elementName.equalsIgnoreCase(XmlDbTags.PlatformConfig_el)
                    || elementName.equalsIgnoreCase(XmlDbTags.EquipmentModel_el)
                    || elementName.equalsIgnoreCase(XmlDbTags.Site_el);
            }
        };
    }
    
    /**
     * Process a single imported object, adding it to the appropriate collection
     * in the database
     * 
     * @param importedObject The object parsed from XML
     * @throws DatabaseException If there is an error adding to the database
     */
    private void processImportedObject(DatabaseObject importedObject) 
            throws DatabaseException {
        
        if (importedObject instanceof Platform) {
            Platform platform = (Platform)importedObject;
            // Ignore historical versions unless allowHistorical is true
            if (platform.expiration == null || allowHistorical) {
                stageDb.platformList.add(platform);
            }
        } else if (importedObject instanceof Site) {
            stageDb.siteList.addSite((Site)importedObject);
        } else if (importedObject instanceof RoutingSpec) {
            stageDb.routingSpecList.add((RoutingSpec)importedObject);
        } else if (importedObject instanceof NetworkList) {
            stageDb.networkListList.add((NetworkList)importedObject);
        } else if (importedObject instanceof PresentationGroup) {
            stageDb.presentationGroupList.add((PresentationGroup)importedObject);
        } else if (importedObject instanceof ScheduleEntry) {
            stageDb.schedEntryList.add((ScheduleEntry)importedObject);
        } else if (importedObject instanceof CompAppInfo) {
            stageDb.loadingAppList.add((CompAppInfo)importedObject);
        } else if (importedObject instanceof PlatformList) {
            String msg = "Cannot import PlatformList files!";
            log.error(msg);
            throw new DatabaseException(msg);
        }
    }
    
    /**
     * Perform post-processing on imported objects, such as extracting
     * platform configs and sites, setting up parent relationships, etc.
     */
    private void postProcessImportedObjects() {
        // Process platforms to extract configs, sites, and equipment models
        extractSubObjectsFromPlatforms();
        
        // Set presentation group parent objects
        setupPresentationGroupParents();
    }
    
    /**
     * Extract config, site, and equipment model objects from platforms
     * and add them to their respective collections
     */
    private void extractSubObjectsFromPlatforms() {
        for(Iterator<Platform> it = stageDb.platformList.iterator(); it.hasNext(); ) {
            Platform platform = it.next();
            
            // The PlatformID needs to be cleared so it won't conflict
            // with an ID in the real editable database
            platform.clearId();
            
            // Extract and add PlatformConfig and EquipmentModel
            PlatformConfig config = platform.getConfig();
            if (config != null) {
                stageDb.platformConfigList.add(config);
                if (config.equipmentModel != null) {
                    stageDb.equipmentModelList.add(config.equipmentModel);
                }
            }
            
            // Extract and add Site
            if (platform.getSite() != null) 
            {
                try {
                    SiteName siteName = platform.getSite().getPreferredName();
                    Site existingSite = stageDb.siteList.getSite(siteName);
                    if (existingSite != null)
                    {
                        log.atWarn().log("Warning: overwriting {} site ",siteName);
                        stageDb.siteList.removeSite(existingSite);
                    }
                    stageDb.siteList.addSite(platform.getSite());
                } catch (Exception ex) 
                {
                    log.atError()
                    .setCause(ex)
                    .log("Platform {} has an invalid site configuration. " +
                                "Platform will be imported without a site", 
                                platform.getDcpAddress());
                    platform.setSite(null);
                }
            }
        }
    }
    
    /**
     * Set up parent-child relationships between presentation groups
     */
    private void setupPresentationGroupParents() 
    {
        for(PresentationGroup pg : stageDb.presentationGroupList.getVector()) 
        {
            if (pg.inheritsFrom != null && pg.inheritsFrom.trim().length() > 0) 
            {
                for (PresentationGroup potentialParent : stageDb.presentationGroupList.getVector()) 
                {
                    if (pg != potentialParent && 
                        pg.inheritsFrom.equalsIgnoreCase(potentialParent.groupName)) 
                        {
                        pg.parent = potentialParent;
                        break;
                        }
                }
            }
        }
    }
}