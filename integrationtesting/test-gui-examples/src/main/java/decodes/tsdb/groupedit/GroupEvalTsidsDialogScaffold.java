package decodes.tsdb.groupedit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.SwingUtilities;

import org.opendcs.database.DatabaseService;
import org.opendcs.database.api.OpenDcsDatabase;

import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.CwmsTsId;
import decodes.db.DataType;
import decodes.launcher.Profile;
import decodes.sql.DbKey;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.util.DecodesSettings;

final class GroupEvalTsidsDialogScaffold
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

				List<String> timeSeriesIdentifiers = Arrays.asList("Barren.Precip.Inst.1Day.0.lrldlb-raw",
						"Barren.Stage.Inst.1Hour.0.LRGS-rev", "Barren.Stor.Inst.1Hour.0.lrldlb-rev",
						"Bedford.Stage.Inst.15Minutes.0.USGS", "Bedford.Stage.Inst.15Minutes.0.USGS-raw",
						"Bedford.Stage.Inst.15Minutes.0.USGS-rev", "BlueLickSprings.Flow.Ave.15Minutes.15Minutes.USGS",
						"BlueLickSprings.Flow.Ave.15Minutes.15Minutes.USGS-raw");
				ArrayList<TimeSeriesIdentifier> cwmsTsIds = new ArrayList<>();
				for(String tsid : timeSeriesIdentifiers)
				{
					CwmsTsId cwmsTsId = new CwmsTsId();
					cwmsTsId.setUniqueString(tsid);
					cwmsTsIds.add(cwmsTsId);
				}
				GroupEvalTsidsDialog groupEvalTsidsDialog = new GroupEvalTsidsDialog(null, tsDb, cwmsTsIds);
				groupEvalTsidsDialog.setAlwaysOnTop(true);
				groupEvalTsidsDialog.setVisible(true);
			}
		});
	}
}
