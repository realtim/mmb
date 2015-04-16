package ru.mmb.datacollector.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.mmb.datacollector.conf.ConfigurationAdapter;
import ru.mmb.datacollector.model.Participant;
import ru.mmb.datacollector.model.Team;

public class TeamsDB {
	private static final Logger logger = LogManager.getLogger(TeamsDB.class);

	private static final String TABLE_DISTANCES = "Distances";
	private static final String TABLE_TEAMS = "Teams";
	private static final String TABLE_USERS = "Users";
	private static final String TABLE_TEAMUSERS = "TeamUsers";

	private static final String RAID_ID = "raid_id";
	private static final String DISTANCE_ID = "distance_id";
	private static final String TEAM_ID = "team_id";
	private static final String TEAM_NAME = "team_name";
	private static final String TEAM_NUM = "team_num";
	private static final String USER_ID = "user_id";
	private static final String USER_NAME = "user_name";
	private static final String USER_BIRTHYEAR = "user_birthyear";
	private static final String TEAMUSER_ID = "teamuser_id";
	private static final String TEAMUSER_HIDE = "teamuser_hide";

	public static synchronized List<Team> loadTeams() {
		List<Team> result = new ArrayList<Team>();
		try {
			Connection conn = ConnectionPool.getInstance().getConnection();
			Statement stmt = null;
			ResultSet rs = null;
			try {
				String sql = "select t.`" + TEAM_ID + "`, t.`" + DISTANCE_ID + "`, t.`" + TEAM_NAME + "`, t.`"
						+ TEAM_NUM + "` from `" + TABLE_TEAMS + "` as t where t.`" + DISTANCE_ID + "` in (select d.`"
						+ DISTANCE_ID + "` from `" + TABLE_DISTANCES + "` as d where d.`" + RAID_ID + "` = "
						+ getCurrentRaidId() + ")";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while (rs.next()) {
					int teamId = rs.getInt(1);
					int distanceId = rs.getInt(2);
					String teamName = rs.getString(3);
					int teamNum = rs.getInt(4);
					result.add(new Team(teamId, distanceId, teamNum, teamName));
				}
			} finally {
				try {
					if (rs != null) {
						rs.close();
						rs = null;
					}
					if (stmt != null) {
						stmt.close();
						stmt = null;
					}
					if (conn != null) {
						conn.close();
						conn = null;
					}
				} catch (SQLException e) {
					// resource release failed
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			logger.debug("error trace", e);
		}

		loadTeamUsers(result);

		return result;
	}

	private static int getCurrentRaidId() {
		return ConfigurationAdapter.getInstance().getCurrentRaidId();
	}

	private static void loadTeamUsers(List<Team> teams) {
		List<Participant> participants = new ArrayList<Participant>();
		try {
			Connection conn = ConnectionPool.getInstance().getConnection();
			Statement stmt = null;
			ResultSet rs = null;
			try {
				String sql = "select t.`" + TEAM_ID + "`, u.`" + USER_ID + "`, tu.`" + TEAMUSER_ID + "`, u.`"
						+ USER_NAME + "`, u.`" + USER_BIRTHYEAR + "` from `" + TABLE_USERS + "` as u join `"
						+ TABLE_TEAMUSERS + "` as tu on (u.`" + USER_ID + "` = tu.`" + USER_ID + "`) join `"
						+ TABLE_TEAMS + "` as t on (tu.`" + TEAM_ID + "` = t.`" + TEAM_ID + "`) where t.`"
						+ DISTANCE_ID + "` in (select d.`" + DISTANCE_ID + "` from `" + TABLE_DISTANCES
						+ "` as d where d.`" + RAID_ID + "` = " + getCurrentRaidId() + ") and (tu.`" + TEAMUSER_HIDE
						+ "` is NULL or tu.`" + TEAMUSER_HIDE + "` = 0)";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while (rs.next()) {
					int teamId = rs.getInt(1);
					int userId = rs.getInt(2);
					int teamUserId = rs.getInt(3);
					String userName = rs.getString(4);
					Integer userBirthYear = rs.getInt(5);
					if (rs.wasNull()) {
						userBirthYear = null;
					}
					participants.add(new Participant(userId, teamId, teamUserId, userName, userBirthYear));
				}
			} finally {
				try {
					if (rs != null) {
						rs.close();
						rs = null;
					}
					if (stmt != null) {
						stmt.close();
						stmt = null;
					}
					if (conn != null) {
						conn.close();
						conn = null;
					}
				} catch (SQLException e) {
					// resource release failed
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			logger.debug("error trace", e);
		}

		putParticipantsToTeams(teams, participants);
	}

	private static void putParticipantsToTeams(List<Team> teams, List<Participant> participants) {
		Map<Integer, Team> teamsMap = createTeamsMap(teams);
		for (Participant participant : participants) {
			Team team = teamsMap.get(participant.getTeamId());
			team.addMember(participant);
			participant.setTeam(team);
		}
	}

	private static Map<Integer, Team> createTeamsMap(List<Team> teams) {
		Map<Integer, Team> result = new HashMap<Integer, Team>();
		for (Team team : teams) {
			result.put(team.getTeamId(), team);
		}
		return result;
	}
}
