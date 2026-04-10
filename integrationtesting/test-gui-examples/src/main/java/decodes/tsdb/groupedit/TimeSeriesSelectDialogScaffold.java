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
import decodes.gui.TopFrame;
import decodes.launcher.Profile;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.util.DecodesSettings;

final class TimeSeriesSelectDialogScaffold
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

				TimeSeriesSelectDialog groupEvalTsidsDialog = new TimeSeriesSelectDialog(tsDb, true, (TopFrame) null);
				groupEvalTsidsDialog.setAlwaysOnTop(true);
				groupEvalTsidsDialog.setVisible(true);
			}
		});
	}
}
