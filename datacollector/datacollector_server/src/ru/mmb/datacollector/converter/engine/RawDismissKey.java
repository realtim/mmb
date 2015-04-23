package ru.mmb.datacollector.converter.engine;

public class RawDismissKey {
	private final int teamId;
	private final int teamUserId;

	public RawDismissKey(int teamId, int teamUserId) {
		this.teamId = teamId;
		this.teamUserId = teamUserId;
	}

	public int getTeamId() {
		return teamId;
	}

	public int getTeamUserId() {
		return teamUserId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + teamId;
		result = prime * result + teamUserId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RawDismissKey other = (RawDismissKey) obj;
		if (teamId != other.teamId)
			return false;
		if (teamUserId != other.teamUserId)
			return false;
		return true;
	}
}
