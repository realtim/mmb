package ru.mmb.sportiduinomanager

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.content.res.ResourcesCompat
import android.widget.Toast
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import ru.mmb.sportiduinomanager.model.Chips
import ru.mmb.sportiduinomanager.model.Station
import java.util.concurrent.TimeUnit

/**
 * Provides foreground service for querying connected station every second
 * for new chips visits.
 */
class StationPoolingService : Service() {

    companion object {
        /**
         * ID of notification messages for ActivePointActivity.
         */
        const val NOTIFICATION_ID: String = "data-from-station-updated"
    }

    private lateinit var application: MainApplication

    private var bluetoothPoolingSubscription = Disposables.disposed()

    /**
     * var station: Station
     * Station which was previously paired via Bluetooth.
     */

    /**
     * var chips: Chips
     * Chips events received from all stations.
     */
    /**
     * var flash: Chips
     * Filtered list of events with teams visiting connected station at current point.
     * One event per team only. Should be equal to records in station flash memory.
     */
    /**
     * var teams: Teams
     * List of teams and team members from local database.
     */

    override fun onCreate() {
        super.onCreate()

        application = getApplication() as MainApplication

        startForeground()

        bluetoothPoolingSubscription =
                // make following operations on the background thread once a second
                Observable.interval(1, TimeUnit.SECONDS, Schedulers.io())
                        .map {
                            // check if station is available
                            application.station?.let { station ->
                                // get latest data
                                station.fetchStatus()
                                fetchTeamsVisits(station)
                            } ?: Int.MIN_VALUE
                        }
                        // if station not available do nothing
                        .filter { it != Int.MIN_VALUE }
                        // if some error occurred - try again
                        .retry()
                        // handle operations result on UI thread
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ fetchTeamsVisitsResult ->
                            // show error message if needed
                            if (fetchTeamsVisitsResult > 0) {
                                Toast.makeText(applicationContext, fetchTeamsVisitsResult, Toast.LENGTH_SHORT).show()
                            } else {
                                // send notification to ActivePointActivity - time to update UI
                                LocalBroadcastManager.getInstance(this)
                                        .sendBroadcast(Intent(NOTIFICATION_ID))
                            }
                        }, { it.printStackTrace() })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String) {

        val channelName = "SportiduinoManager Station Service"
        val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(chan)
    }

    private fun startForeground() {

        val notificationIntent = Intent(application.context, ActivePointActivity::class.java)

        val channelId = "ru.mmb.sportiduinomanager"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(channelId)
        }

        val builder =
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    Notification.Builder(this, channelId)
                else Notification.Builder(this))
                        .setContentTitle(application.context.getString(R.string.app_name))
                        .setContentText("Station pooling")
                        .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_launcher))
                        .setContentIntent(
                                PendingIntent.getActivity(
                                        application.context, 0,
                                        notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setSmallIcon(R.drawable.ic_notification)
        } else {
            builder.setSmallIcon(R.drawable.ic_notification)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setColor(ResourcesCompat.getColor(resources, R.color.bg_primary, null))
        }

        val notification = builder.build()

        val anyForegroundServiceId = 798

        startForeground(anyForegroundServiceId, notification)
    }

    private fun getFlash(station: Station): Chips? {
        return application.chips?.getChipsAtPoint(station.number, station.maCasLong)
    }

    /**
     * Get all events for all new teams
     * which has been visited the station after the last check.
     * Returns -1 if some new teams has been visited the station,
     * 0 if no new teams has been fetched,
     * > 0 indicates an error code.
     *
     * @return -1/0/error code
     */
    private fun fetchTeamsVisits(station: Station): Int {
        // Do nothing if no teams visited us yet
        if (station.chipsRegistered == 0) return 0
        val flash = getFlash(station) ?: return R.string.err_internal_error
        val chips = application.chips

        // Number of team visits at local db and at station are the same?
        // Time of last visit in local db and at station is the same?
        // (it can change without changing of number of teams)
        if (flash.size() == station.chipsRegistered && flash.getTeamTime(flash.size() - 1) == station.lastChipTime) {
            return 0
        }
        // Ok, we have some new team visits
        var fullDownload = false
        // Clone previous teams list from station
        val prevLastTeams = station.lastTeams()
        // Ask station for new list
        if (!station.fetchLastTeams()) {
            fullDownload = true
        }
        val currLastTeams = station.lastTeams()
        // Check if teams from previous list were copied from flash
        for (team in prevLastTeams) {
            if (!flash.contains(team, station.number)) {
                // Something strange has been happened, do full download of all teams
                fullDownload = true
            }
        }
        // Start building the final list of teams to fetch
        var fetchTeams: MutableList<Int> = java.util.ArrayList()
        for (team in currLastTeams) {
            if (!prevLastTeams.contains(team)) {
                fetchTeams.add(team)
            }
        }
        // If all members of last teams buffer are new to us,
        // then we need to make a full rescan
        if (fetchTeams.size == Station.LAST_TEAMS_LEN) {
            fullDownload = true
        }
        // If all last teams are the same but last team time has been changed
        // then we need to rescan all teams from the buffer
        if (fetchTeams.isEmpty()) {
            fetchTeams = currLastTeams
        }
        // For full rescan of all teams make a list of all registered teams
        if (fullDownload) {
            fetchTeams = application.teams.teamList
        }
        // Get visit parameters for all teams in fetch list
        var flashChanged = false
        var newEvents = false
        var stationError = 0
        for (teamNumber in fetchTeams) {
            // Fetch data for the team visit to station
            var newError = 0
            if (!station.fetchTeamRecord(teamNumber)) {
                newError = station.getLastError(true)
                // Ignore data absence for teams which are not in last teams list
                // Most probable these teams did not visited the station at all
                if (newError == R.string.err_station_no_data && !currLastTeams.contains(teamNumber)) {
                    continue
                }
                // Abort scanning in case of serious error
                // Continue scanning in case of problems with copying data from chip to memory
                if (newError != R.string.err_station_flash_empty && newError != R.string.err_station_no_data) {
                    return newError
                }
            }
            // Get team visit as an event
            val teamVisit = station.chipEvents
            if (teamVisit.size() == 0) {
                // Team visit was not registered at all due to string.err_station_no_data error
                // Create synthetic team visit with zero chip init time
                val teamMembers = application.teams.getMembersNames(teamNumber)
                var originalMask = 0
                for (i in teamMembers.indices) {
                    originalMask = originalMask or (1 shl i)
                }
                teamVisit.addNewEvent(station, 0, teamNumber, originalMask,
                        station.number, station.lastChipTime)
            }
            // Prepare to clone init time and mask from this event to marks from the chip
            if (teamNumber != teamVisit.getTeamNumber(0)) return R.string.err_station_team_changed
            val initTime = teamVisit.getInitTime(0)
            val teamMask = teamVisit.getTeamMask(0)
            // Update copy of station flash memory
            if (flash.merge(teamVisit)) {
                flashChanged = true
            }

            // Try to add team visit as new event
            if (chips.join(teamVisit)) {
                newEvents = true
                // Read marks from chip and to events list
                val marks = station.chipRecordsN
                var fromMark = 0
                do {
                    if (marks <= 0) break
                    var toRead = marks
                    if (toRead > Station.MAX_MARK_COUNT) {
                        toRead = Station.MAX_MARK_COUNT
                    }
                    if (!station.fetchTeamMarks(teamNumber, initTime, teamMask, fromMark, toRead)) {
                        return station.getLastError(true)
                    }
                    fromMark += toRead
                    // Add fetched chip marks to local list of events
                    chips.join(station.chipEvents)
                } while (fromMark < marks)
            } else {
                // Ignore recurrent problem with copying data from chip to memory
                // as we already created synthetic team visit and warned a user
                newError = 0
            }
            // Save non-fatal station error
            if (newError == R.string.err_station_flash_empty || newError == R.string.err_station_no_data) {
                stationError = newError
            }
        }
        // Save new events (if any) to local db and to main memory
        if (newEvents) {
            // Save new events in local database
            val result = chips.saveNewEvents(application.database)
            if ("" != result) return R.string.err_db_sql_error
            // Copy changed list of chips events to main application
            application.setChips(chips, false)
        }
        // Sort visits by their time
        if (flashChanged) {
            flash.sort()
        }
        // Report non-fatal errors which has been occurred during scanning
        if (stationError != 0) {
            return stationError
        }
        // Report 'data changed' for updating UI
        return if (newEvents || flashChanged) -1 else 0
    }

    /**
     * Stop querying connected station on service destroy.
     */
    override fun onDestroy() {
        super.onDestroy()
        bluetoothPoolingSubscription.dispose()
    }

    /**
     * Create simple unbound service.
     */
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
