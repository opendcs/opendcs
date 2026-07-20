package org.opendcs.database.impl.cwms.dao;

import org.jdbi.v3.stringtemplate4.StringTemplateSqlLocator;
import org.opendcs.database.dai.PlatformDao;
import org.opendcs.database.impl.cwms.jdbi.mapper.CwmsSiteMapper;
import org.opendcs.database.impl.opendcs.dao.PlatformDaoImpl;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.configs.DecodesConfigMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms.PlatformMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms.PlatformSensorMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms.PlatformSensorPropertyMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms.TransportMediumMapper;
import org.opendcs.database.model.mappers.properties.PropertiesMapper;
import org.opendcs.database.model.mappers.sites.OpenDcsSiteNameMapper;
import org.opendcs.database.model.mappers.sites.OpenDcsSiteReducer;
import org.openide.util.lookup.ServiceProvider;

/**
 * Overides the SiteMapper so handle the CWMS Location table variation.
 * CwmsPlatformDaoImpl
 */
@ServiceProvider(service = PlatformDao.class, path = "dao/CWMS-Oracle")
public final class CwmsPlatformDaoImpl extends PlatformDaoImpl
{
    public CwmsPlatformDaoImpl()
    {
        super(new Mappers(
                PlatformMapper.withPrefix("p"),
                DecodesConfigMapper.withPrefix("config"),
                TransportMediumMapper.withPrefix("tm"),
                CwmsSiteMapper.withPrefix("s"),
                OpenDcsSiteNameMapper.withPrefix("sn"),
                PropertiesMapper.withPrefix("sp", true),
                new OpenDcsSiteReducer("s"),
                PropertiesMapper.withPrefix("pp", true),
                PlatformSensorMapper.withPrefix("ps"),
                PlatformSensorPropertyMapper.withPrefix("psp")
                ),
            new Mappers(
                PlatformMapper.withPrefix("p"),
                null,
                TransportMediumMapper.withPrefix("tm"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null),
            StringTemplateSqlLocator.findStringTemplateGroup(CwmsPlatformDaoImpl.class)
        );
    }
}
