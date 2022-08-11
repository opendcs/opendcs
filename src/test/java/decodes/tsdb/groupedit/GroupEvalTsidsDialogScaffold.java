package decodes.tsdb.groupedit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.SwingUtilities;

import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.CwmsTsId;
import decodes.db.DataType;
import decodes.sql.DbKey;
import decodes.tsdb.TimeSeriesIdentifier;

final class GroupEvalTsidsDialogScaffold
{
	public static void main(String[] args)
	{
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
				GroupEvalTsidsDialog groupEvalTsidsDialog = new GroupEvalTsidsDialog(null, new CwmsTimeSeriesDb(), cwmsTsIds);
				groupEvalTsidsDialog.setAlwaysOnTop(true);
				groupEvalTsidsDialog.setVisible(true);
			}
		});
	}
}
