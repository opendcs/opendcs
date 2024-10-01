package decodes.tsdb.groupedit;

import decodes.sql.DbKey;
import decodes.tsdb.TsGroup;

public class SubGroupReference
{
	DbKey groupId = DbKey.NullKey;
	String groupName = null;
	String groupType = null;
	String groupDesc = null;
	String combine = null;

	public SubGroupReference(TsGroup group, String combine)
	{
		groupId = group.getGroupId();
		groupName = group.getGroupName();
		groupType = group.getGroupType();
		groupDesc = group.getDescription();
		this.combine = combine;
	}
}
