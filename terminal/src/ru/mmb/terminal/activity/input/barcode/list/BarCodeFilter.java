package ru.mmb.terminal.activity.input.barcode.list;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import android.widget.Filter;

public class BarCodeFilter extends Filter
{
	private final BarCodeAdapter owner;
	private final List<BarCodeListRecord> sourceItems = new ArrayList<BarCodeListRecord>();

	public BarCodeFilter(BarCodeAdapter owner)
	{
		this.owner = owner;
	}

	public void reset(List<BarCodeListRecord> sourceItems)
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
		List<BarCodeListRecord> filterResult = (List<BarCodeListRecord>) results.values;
		owner.setNotifyOnChange(false);
		owner.clear();
		if (filterResult != null)
		{
			for (BarCodeListRecord item : filterResult)
			{
				owner.add(item);
			}
			owner.sort(new Comparator<BarCodeListRecord>()
			{
				@Override
				public int compare(BarCodeListRecord record1, BarCodeListRecord record2)
				{
					// reverse sort
					return -1 * record1.compareTo(record2);
				}
			});
		}
		owner.notifyDataSetChanged();
	}
}
