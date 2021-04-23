set LOG=$INSTALL_PATH\clean.log
set OLDSTUFF=$INSTALL_PATH\old-stuff

cd $INSTALL_PATH

echo Cleaning Release at %DATE% %TIME% >>%LOG%

set OLDJARS=xwork-core-2.1.6.jar poi-3.0.1-FINAL.jar postgresql-8.3-604.jdbc4.jar NWIS.jar RXTXcomm.jar antlr-2.7.6.jar asm-attrs.jar bmp5-java-sdk.jar cobra-no-commons.jar commons-collections-2.1.1.jar commons-logging-1.0.4.jar cwmsdb.jar dbi.jar hibernate3.jar httpcore-4.0.jar jarTeledriftTVD.jar jcommon-1.0.10.jar js.jar json-org.jar jta.jar jtds-1.3.1.jar jython.jar lobo-pub.jar lobo.jar mailapi.jar ojdbc14.jar passay-1.1.0.jar poi-3.0.1-FINAL-20070705.jar slf4j-api-1.7.13.jar slf4j-nop-1.7.13.jar smtp.jar sqljruntime12.jar sshd-core-1.1.0.jar postgresql-9.1-901-1.jdbc4.jar

if not exist %OLDSTUFF% (mkdir %OLDSTUFF%)

cd $INSTALL_PATH\dep
for %%f in (%OLDJARS%) do move "%%f" "%OLDSTUFF%"

exit /B 0
