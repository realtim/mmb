package ru.mmb.sportiduinomanager

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.content.res.ResourcesCompat
import android.util.Log
import android.widget.Toast
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import ru.mmb.sportiduinomanager.model.Station
import java.util.concurrent.TimeUnit

/**
 * Provides foreground service for querying connected station every second
 * for new chips punches.
 */
class StationPoolingService : Service() {

    companion object {
        /**
         * ID of notification messages for ControlPointActivity.
         */
        const val NOTIFICATION_ID: String = "data-from-station-updated"
    }

    private lateinit var application: MainApp

    private var bluetoothPoolingSubscription = Disposables.disposed()

    /**
     * Create foreground service.
     **/
    override fun onCreate() {
        Log.d("SIM StationPooling", "Create")
        super.onCreate()
        application = getApplication() as MainApp
        startForeground()
        bluetoothPoolingSubscription =
                // make following operations on the background thread once a second
                Observable.interval(1, TimeUnit.SECONDS, Schedulers.io())
                        .map {
                            // check if station is available
                            MainApp.mStation?.let {
                                if (MainApp.mStation.isPoolingAllowed) {
                                    // get latest data
                                    MainApp.mStation.setPoolingActive(true)
                                    MainApp.mStation.fetchStatus("StationPooling")
                                    val result = fetchTeamsPunches()
                                    MainApp.mStation.setPoolingActive(false)
                                    result
                                } else {
                                    Log.d("SIM StationPooling", "Skip pooling")
                                }
                            } ?: Int.MIN_VALUE
                        }
                        // if station not available do nothing
                        .filter { it != Int.MIN_VALUE }
                        // if some error occurred - try again
                        .retry()
                        // handle operations result on UI thread
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ fetchTeamsPunchesResult ->
                            // show error message if needed
                            if (fetchTeamsPunchesResult > 0) {
                                Toast.makeText(applicationContext, fetchTeamsPunchesResult, Toast.LENGTH_SHORT).show()
                            } else {
                                // send notification to ControlPointActivity - time to update UI
                                LocalBroadcastManager.getInstance(this)
                                        .sendBroadcast(Intent(NOTIFICATION_ID))
                            }
                        }, { it.printStackTrace() })
    }

    private fun startForeground() {
        val notificationIntent = Intent(getApplication(), ControlPointActivity::class.java)
        val channelId = "ru.mmb.sportiduinomanager"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "SportiduinoManager Station Service"
            val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE)
            chan.lightColor = Color.BLUE
            chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)
        }
        val builder = NotificationCompat.Builder(this, channelId)
                .setContentTitle(getApplication().getString(R.string.app_name))
                .setContentText("Station pooling")
                .setColor(ResourcesCompat.getColor(resources, R.color.bg_primary, null))
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_launcher))
                .setContentIntent(
                        PendingIntent.getActivity(getApplication(), 0, notificationIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT))
        val notification = builder.build()
        val anyForegroundServiceId = 798
        startForeground(anyForegroundServiceId, notification)
    }

    /**
     * Get all punches for all new teams
     * which has been punched at the station after the last check.
     * Returns -1 if some new teams has been punched at the station,
     * 0 if no new teams has been punched,
     * > 0 indicates an error code.
     *
     * @return -1/0/error code
     */
    private fun fetchTeamsPunches(): Int {
        // Do nothing if no teams has been punched yet
        if (MainApp.mStation.chipsRegistered == 0) return 0

        // Number of team punches at local db and at station are the same?
        // Time of last punch in local db and at station is the same?
        // (it can change without changing of number of teams)
        if (MainApp.mPointPunches.size() == MainApp.mStation.chipsRegistered
                && MainApp.mPointPunches.getTeamTime(MainApp.mPointPunches.size() - 1)
                == MainApp.mStation.lastPunchTime) {
            return 0
        }
        // Ok, we have some new punches
        var fullDownload = false
        // Clone previous teams list from station
        val prevLastTeams = MainApp.mStation.lastTeams
        // Ask station for new list
        if (!MainApp.mStation.fetchLastTeams("StationPooling")) {
            fullDownload = true
        }
        val currLastTeams = MainApp.mStation.lastTeams
        // Check if teams from previous list were copied from flash
        for (team in prevLastTeams) {
            if (!MainApp.mPointPunches.contains(team, MainApp.mStation.number)) {
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
            fetchTeams = MainApp.mTeams.teamList
        }
        // Get Sportiduino records for all teams in fetch list
        var flashChanged = false
        var newRecords = false
        var stationError = 0
        for (teamNumber in fetchTeams) {
            // Fetch data for the team punched at the station
            var newError = 0
            if (!MainApp.mStation.fetchTeamRecord(teamNumber, "StationPooling")) {
                newError = MainApp.mStation.getLastError(true)
                // Ignore data absence for teams which are not in last teams list
                // Most probable these teams did not punched at the station at all
                if (newError == R.string.err_station_no_data && !currLastTeams.contains(teamNumber)) {
                    continue
                }
                // Abort scanning in case of serious error
                // Continue scanning in case of problems with copying data from chip to memory
                if (newError != R.string.err_station_flash_empty && newError != R.string.err_station_no_data) {
                    return newError
                }
            }
            // Get team punches as a Sportiduino record list
            val teamPunches = MainApp.mStation.teamPunches
            if (teamPunches.size() == 0) {
                // Team punch was not registered at all due to string.err_station_no_data error
                // Create synthetic team punch with zero chip init time
                val teamMembers = MainApp.mTeams.getMembersNames(teamNumber)
                var originalMask = 0
                for (i in teamMembers.indices) {
                    originalMask = originalMask or (1 shl i)
                }
                teamPunches.addRecord(MainApp.mStation, 0, teamNumber, originalMask,
                        MainApp.mStation.number, MainApp.mStation.lastPunchTime)
            }
            // Prepare to clone init time and mask from this record to punches from the chip
            if (teamNumber != teamPunches.getTeamNumber(0)) return R.string.err_station_team_changed
            val initTime = teamPunches.getInitTime(0)
            val teamMask = teamPunches.getTeamMask(0)
            // Update copy of station flash memory
            if (MainApp.mPointPunches.merge(teamPunches)) {
                flashChanged = true
            }

            // Try to add team punches as new records
            if (MainApp.mAllRecords.join(teamPunches)) {
                newRecords = true
                // Read punches from chip and to record list
                val punchesN = MainApp.mStation.chipRecordsN
                var fromPunch = 0
                do {
                    if (punchesN <= 0) break
                    var toRead = punchesN
                    if (toRead > Station.MAX_PUNCH_COUNT) {
                        toRead = Station.MAX_PUNCH_COUNT
                    }
                    if (!MainApp.mStation.fetchTeamPunches(teamNumber, initTime, teamMask,
                                    fromPunch, toRead, "StationPooling")) {
                        return MainApp.mStation.getLastError(true)
                    }
                    fromPunch += toRead
                    // Add fetched punches to application list of records
                    MainApp.mAllRecords.join(MainApp.mStation.teamPunches)
                } while (fromPunch < punchesN)
            } else {
                // Ignore recurrent problem with copying data from chip to memory
                // as we already created synthetic team punch and warned a user
                newError = 0
            }
            // Save non-fatal station error
            if (newError == R.string.err_station_flash_empty || newError == R.string.err_station_no_data) {
                stationError = newError
            }
        }
        // Save new records (if any) to local db
        if (newRecords) {
            // Save new events in local database
            val result = MainApp.mAllRecords.saveNewRecords(MainApp.mDatabase)
            if ("" != result) return R.string.err_db_sql_error
        }
        // Sort punches by their time
        if (flashChanged) {
            MainApp.mPointPunches.sort()
        }
        // Report non-fatal errors which has been occurred during scanning
        if (stationError != 0) {
            return stationError
        }
        // Report 'data changed' for updating UI
        return if (newRecords || flashChanged) -1 else 0
    }

    /**
     * Stop querying connected station on service destroy.
     */
    override fun onDestroy() {
        bluetoothPoolingSubscription.dispose()
        super.onDestroy()
        Log.d("SIM StationPooling", "Destroy")
    }

    /**
     * Create simple unbound service.
     */
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
