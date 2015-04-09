package ru.mmb.datacollector.activity.input.data.history.list;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import android.widget.Filter;

public class HistoryFilter extends Filter
{
	private final HistoryAdapter owner;
	private final List<HistoryListRecord> sourceItems = new ArrayList<HistoryListRecord>();

	public HistoryFilter(HistoryAdapter owner)
	{
		this.owner = owner;
	}

	public void reset(List<HistoryListRecord> sourceItems)
	{
		this.sourceItems.clear();
		this.sourceItems.addAll(sourceItems);
	}

	@Override
	protected FilterResults performFiltering(CharSequence constraint)
	{
		FilterResults result = new FilterResults();
		result.values = sourceItems;
		return result;
	}

	@Override
	protected void publishResults(CharSequence constraint, FilterResults results)
	{
		@SuppressWarnings("unchecked")
		List<HistoryListRecord> filterResult = (List<HistoryListRecord>) results.values;
		owner.setNotifyOnChange(false);
		owner.clear();
		if (filterResult != null)
		{
			for (HistoryListRecord item : filterResult)
			{
				owner.add(item);
			}
			owner.sort(new Comparator<HistoryListRecord>()
			{
				@Override
				public int compare(HistoryListRecord record1, HistoryListRecord record2)
				{
					// reverse sort
					return -1 * record1.compareTo(record2);
				}
			});
		}
		owner.notifyDataSetChanged();
	}
}
