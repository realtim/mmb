package ru.mmb.sportiduinomanager.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Support of team list and team members.
 */
public final class Teams {
    /**
     * Sparse array of teams, array index == team number.
     */
    private final SingleTeam[] mTeams;

    /**
     * Allocate team array with maxNumber as max array index.
     *
     * @param maxNumber Max index in team array
     */
    public Teams(final int maxNumber) {
        mTeams = new SingleTeam[maxNumber + 1];
    }

    /**
     * Get count of team members from the team mask.
     *
     * @param teamMask Some team members mask
     * @return Number of team members
     */
    public static int getMembersCount(final int teamMask) {
        int teamMembersCount = 0;
        for (int i = 0; i < 16; i++) {
            if ((teamMask & (1 << i)) != 0) {
                teamMembersCount++;
            }
        }
        return teamMembersCount;
    }

    /**
     * Construct team and save it to appropriate position in team array.
     *
     * @param number       Team number
     * @param membersCount Number of team members
     * @param mapsCount    Number of maps
     * @param name         Team name
     * @return True in case of valid team number value
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean addTeam(final int number, final int membersCount, final int mapsCount, final String name) {
        // Check if team number is valid
        if (number < 0 || number >= mTeams.length) return false;
        // Check if the point was already set
        if (mTeams[number] == null) {
            // set the point
            mTeams[number] = new SingleTeam(membersCount, mapsCount, name);
            return true;
        }
        return false;
    }

    /**
     * Add new member to the list of team members.
     *
     * @param memberId    Member id
     * @param teamN       Team number
     * @param memberName  Member first and last name and year of birth
     * @param memberPhone Member mobile phone (can be empty)
     * @return True in case of valid team and number of its members
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean addTeamMember(final long memberId, final int teamN, final String memberName, final String memberPhone) {
        // Check if team number is valid
        if (teamN < 0 || teamN >= mTeams.length) return false;
        final int numberOfMembers = mTeams[teamN].mMembers.length;
        for (int i = 0; i <= numberOfMembers; i++) {
            if (i == numberOfMembers) return false;
            if (mTeams[teamN].mMembers[i] == null) {
                mTeams[teamN].mMembers[i] = new Member(memberId, memberName, memberPhone);
                break;
            }
        }
        return true;
    }

    /**
     * Get max team number in teams array.
     *
     * @return Max team number
     */
    int getMaxTeam() {
        return mTeams.length - 1;
    }

    /**
     * Get the team name.
     *
     * @param number Team number
     * @return String with team name or null if the team does not exist
     */
    public String getTeamName(final int number) {
        if (number <= 0 || number >= mTeams.length) return null;
        if (mTeams[number] == null) return null;
        return mTeams[number].mName;
    }

    /**
     * Get the team maps count.
     *
     * @param number Team number
     * @return Number of maps for the team
     */
    public int getTeamMaps(final int number) {
        if (number <= 0 || number >= mTeams.length) return 0;
        if (mTeams[number] == null) return 0;
        return mTeams[number].mMaps;
    }

    /**
     * Get the list of team members ids.
     *
     * @param number Team number
     * @return List of team members ids (or empty list if the team does not exist)
     */
    List<Long> getMembersIds(final int number) {
        if (number <= 0 || number >= mTeams.length) return new ArrayList<>();
        if (mTeams[number] == null) return new ArrayList<>();
        final List<Long> list = new ArrayList<>();
        for (final Member member : mTeams[number].mMembers) {
            list.add(member.mId);
        }
        return list;
    }

    /**
     * Get the list of team members names.
     *
     * @param number Team number
     * @return List of team members names (or empty list if the team does not exist)
     */
    public List<String> getMembersNames(final int number) {
        if (number <= 0 || number >= mTeams.length) return new ArrayList<>();
        if (mTeams[number] == null) return new ArrayList<>();
        final List<String> list = new ArrayList<>();
        for (final Member member : mTeams[number].mMembers) {
            list.add(member.mName);
        }
        return list;
    }

    /**
     * Get the list of team members phones.
     *
     * @param number Team number
     * @return List of team members phones (or empty list if the team does not exist)
     */
    List<String> getMembersPhones(final int number) {
        if (number <= 0 || number >= mTeams.length) return new ArrayList<>();
        if (mTeams[number] == null) return new ArrayList<>();
        final List<String> list = new ArrayList<>();
        for (final Member member : mTeams[number].mMembers) {
            list.add(member.mPhone);
        }
        return list;
    }

    /**
     * Get the list of all teams numbers.
     *
     * @return List of registered teams numbers
     */
    public List<Integer> getTeamList() {
        final List<Integer> teams = new ArrayList<>();
        for (int number = 1; number < mTeams.length; number++) {
            if (mTeams[number] != null) {
                teams.add(number);
            }
        }
        return teams;
    }

    /**
     * Check the team list (loaded from site or from local db) for various errors.
     *
     * @return True if some errors were found
     */
    public boolean hasErrors() {
        // Check if some teams were loaded
        if (mTeams.length == 0) return true;
        // Check if all teams were loaded
        if (mTeams[mTeams.length - 1] == null) return true;
        // Check teams data
        for (final SingleTeam team : mTeams) {
            if (team != null) {
                // Check if all team members were loaded
                for (final Member member : team.mMembers) {
                    if (member == null) return true;
                    // Check for bad member data
                    if (member.mId <= 0) return true;
                    if (member.mName.isEmpty()) return true;
                }
                // Check number of maps
                if (team.mMaps <= 0) return true;
                // Check for empty team name
                if (team.mName.isEmpty()) return true;
            }
        }
        // No errors were detected
        return false;
    }

    /**
     * A team parameters.
     */
    private static final class SingleTeam {
        /**
         * Number of maps printed for the team.
         */
        private final int mMaps;
        /**
         * Team name.
         */
        private final String mName;
        /**
         * List of team members.
         */
        private final Member[] mMembers;

        /**
         * Constructor for Team class.
         *
         * @param membersCount Number of members in this team
         * @param mapsCount    Number of ordered maps
         * @param name         Team name
         */
        private SingleTeam(final int membersCount, final int mapsCount, final String name) {
            mMaps = mapsCount;
            mName = name;
            mMembers = new Member[membersCount];
        }
    }

    /**
     * A member of a team.
     */
    private static final class Member {
        /**
         * Member id.
         */
        private final long mId;
        /**
         * Member first name, last name and year of birth.
         */
        private final String mName;
        /**
         * Member phone (can be empty).
         */
        private final String mPhone;

        /**
         * Constructor for Member class.
         *
         * @param memberId ID of the member
         * @param name     Member name and birth date
         * @param phone    Member phone
         */
        private Member(final long memberId, final String name, final String phone) {
            mId = memberId;
            mName = name;
            mPhone = phone;
        }
    }

}
