# OpenDCS Docker files


# LRGS

To setup a permanent instance of an LRGS do the following, replace `--name lrgs` with a name
appropriate to your installation.

```
docker create volume lrgs_volume
docker run -d name lrgs -p 16003:16003 -v lrgs_volume:/lrgs_home -e LRGS_ADMIN_PASSWORD="the password you want" lrgs:latest

```

16003 is the DDS protocol Port that the gui `rtstat` application can use. At this time there is no API and this is require for later configuration.
There are additional input sources, and the ability to add additional custom input sources that may require you to 
expose additional ports.

# Future

Additional containers will be available later

# Tags

7, 7.0, latest