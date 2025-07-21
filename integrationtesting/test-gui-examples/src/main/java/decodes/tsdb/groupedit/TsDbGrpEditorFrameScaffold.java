package decodes.tsdb.groupedit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.swing.SwingUtilities;

import org.opendcs.database.DatabaseService;
import org.opendcs.database.api.OpenDcsDatabase;

import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.CwmsTsId;
import decodes.db.Database;
import decodes.db.DatabaseIO;
import decodes.launcher.Profile;
import decodes.tsdb.BadConnectException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.DecodesSettings;

final class TsDbGrpEditorFrameScaffold
{
	public static void main(String[] args) throws Exception
	{
		Profile p = Profile.getDefaultProfile();
		OpenDcsDatabase db = DatabaseService.getDatabaseFor("utility", DecodesSettings.fromProfile(p));
		TimeSeriesDb tsDb = db.getLegacyDatabase(TimeSeriesDb.class).get();
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				TsdbAppTemplate.theDb = tsDb;
				TsDbGrpEditorFrame tsDbGrpEditorFrame = new TsDbGrpEditorFrame(TsdbAppTemplate.theDb);
				tsDbGrpEditorFrame.setVisible(true);
			}
		});
	}
}
