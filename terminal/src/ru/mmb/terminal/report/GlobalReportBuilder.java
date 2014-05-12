package ru.mmb.terminal.report;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ru.mmb.terminal.activity.report.global.GlobalReportMode;
import ru.mmb.terminal.model.Distance;
import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.model.registry.DistancesRegistry;
import ru.mmb.terminal.model.registry.Settings;
import ru.mmb.terminal.model.registry.TeamsRegistry;
import ru.mmb.terminal.model.report.TeamReport;

public class GlobalReportBuilder
{
	private final GlobalReportMode reportMode;
	private Set<Integer> selectedTeams = null;

	private BufferedWriter writer;

	public GlobalReportBuilder(GlobalReportMode reportMode, String selectedTeams)
	{
		this.reportMode = reportMode;
		if (reportMode == GlobalReportMode.SELECTED_TEAMS)
		{
			this.selectedTeams = parseSelectedTeams(selectedTeams);
		}
	}

	private Set<Integer> parseSelectedTeams(String selectedTeams)
	{
		Set<Integer> result = new HashSet<Integer>();
		String[] parsed = selectedTeams.split(";");
		for (String numberString : parsed)
		{
			Integer number = Integer.parseInt(numberString);
			result.add(number);
		}
		return result.isEmpty() ? null : result;
	}

	public String buildReport() throws IOException
	{
		String fileName = generateFileName();
		writer =
		    new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName, false), "UTF8"));
		try
		{
			innerBuildReport();
		}
		finally
		{
			writer.close();
		}
		return fileName;
	}

	private String generateFileName()
	{
		return Settings.getInstance().getExportDir() + "/report_global_"
		        + reportMode.getShortName() + "." + "html";
	}

	private void innerBuildReport() throws IOException
	{
		List<Distance> distances = DistancesRegistry.getInstance().getDistances();
		writer.write("<html><head><meta charset=\"UTF-8\"></head><body>");
		for (Distance distance : distances)
		{
			String distanceReport = buildDistanceTeamsReport(distance);
			if (distanceReport != null && distanceReport.length() > 0)
			{
				// row_number; team number; team name; team members; total time; table with levels
				writer.write("<table cellspacing=\"5\">");
				writer.write("<col width=\"50\"><col width=\"50\"><col width=\"175\"><col width=\"150\"><col width=\"50\"><col width=\"500\">");
				int columnCount = 6;
				writer.write("<tr><td colspan=\"" + columnCount + "\" /></tr>");
				writer.write("<tr><td colspan=\"" + columnCount + "\">");
				writer.write(distance.getDistanceName());
				writer.write("</td></tr>");
				writer.write("<tr><td colspan=\"" + columnCount + "\" /></tr>");
				writer.write(distanceReport);
				writer.write("</table>");
				writer.flush();
			}
		}
		writer.write("</body></html>");
	}

	private String buildDistanceTeamsReport(Distance distance)
	{
		StringBuilder sb = new StringBuilder();
		List<TeamReport> calculatedReports = new ArrayList<TeamReport>();
		List<Team> teams = TeamsRegistry.getInstance().getTeams(distance.getDistanceId());
		for (Team team : teams)
		{
			if (reportMode != GlobalReportMode.ALL_TEAMS)
			{
				if (!selectedTeams.contains(team.getTeamNum())) continue;
			}
			TeamReportBuilder builder = new TeamReportBuilder(team);
			calculatedReports.add(builder.buildReport());
		}
		Collections.sort(calculatedReports);
		int rowNum = 0;
		for (TeamReport teamReport : calculatedReports)
		{
			sb.append(teamReport.toCompactHtml(rowNum));
			rowNum++;
		}
		return sb.toString();
	}
}
