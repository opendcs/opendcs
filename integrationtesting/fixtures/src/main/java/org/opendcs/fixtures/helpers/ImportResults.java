package org.opendcs.fixtures.helpers;

import java.util.ArrayList;
import java.util.List;

import decodes.tsdb.CTimeSeries;

public final class ImportResults
{
	private final List<CTimeSeries> importedTsList;
	private final List<Long> tsCompIds;
	private final List<CTimeSeries> outputTsList;
	private final List<CTimeSeries> inputTsList;

	private ImportResults(Builder builder)
	{
		importedTsList = builder.importedTsList;
		tsCompIds = builder.tsCompIds;
		inputTsList = builder.inputTsList;
		outputTsList = builder.outputTsList;
	}

	public List<CTimeSeries> getImportedTsList()
	{
		return importedTsList;
	}

	public List<CTimeSeries> getOutputTsList()
	{
		return outputTsList;
	}

	public List<CTimeSeries> getInputTsList()
	{
		return inputTsList;
	}

	public List<Long> getTsCompIds()
	{
		return tsCompIds;
	}

	public static class Builder
	{
		private List<CTimeSeries> importedTsList;
		private List<Long> tsCompIds = new ArrayList<>();
		private List<CTimeSeries> inputTsList;
		private List<CTimeSeries> outputTsList;

		public Builder withImportedTsList(List<CTimeSeries> expectedTsList)
		{
			this.importedTsList = expectedTsList;
			return this;
		}

		public Builder withInputTsList(List<CTimeSeries> inputTsList)
		{
			this.inputTsList = inputTsList;
			return this;
		}

		public Builder withOutputTsList(List<CTimeSeries> outputTsList)
		{
			this.outputTsList = outputTsList;
			return this;
		}

		public Builder withTsCompIds(long tsCompId)
		{
			this.tsCompIds.add(tsCompId);
			return this;
		}

		public ImportResults build()
		{
			return new ImportResults(this);
		}
	}
}
