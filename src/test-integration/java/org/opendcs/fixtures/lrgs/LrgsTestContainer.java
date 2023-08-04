package org.opendcs.fixtures.lrgs;

import java.io.File;
import java.net.URL;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;


public class LrgsTestContainer extends GenericContainer<LrgsTestContainer>
{
    public LrgsTestContainer(String image)
    {
        super(DockerImageName.parse(image));
    }

    public LrgsTestContainer(DockerImageName image)
    {
        super(image);
    }

    @Override
    public void start()
    {
        super.start();
    }

    public LrgsTestContainer withLrgsAdminPassword(String password)
    {
        return withEnv("LRGS_ADMIN_PASSWORD",password);
    }

    public LrgsTestContainer withLrgsConf(File conf)
    {
        return withFileSystemBind(conf.getAbsolutePath(),"/lrgshome/lrgs.conf",BindMode.READ_ONLY);
    }

    public LrgsTestContainer withLrgsConf(URL conf)
    {
        return withClasspathResourceMapping(conf.toString(), "/lrgshome/lrgs.conf", BindMode.READ_ONLY);
    }
}
