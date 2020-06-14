package org.allatra.calendar.db

import android.content.Context
import io.realm.Realm
import io.realm.RealmConfiguration
import timber.log.Timber

object RealmHandlerObject {
    private lateinit var context: Context
    
    private const val APP_REAL_NAME = "realm.allatra.soul.calendar"
    private const val DB_FIELD_ID = "id"
    const val DEFAULT_ID = 1L

    // Initialize Realm
    fun initWithContext(context: Context){
        this.context = context
    }

    private fun getInstance(): Realm{
        Realm.init(context)
        val config = RealmConfiguration.Builder()
            .name(APP_REAL_NAME)
            .schemaVersion(1)
            .deleteRealmIfMigrationNeeded()
            .build()
        return Realm.getInstance(config)
    }

    private fun dropRealm(realmConfiguration: RealmConfiguration){
        Realm.deleteRealm(realmConfiguration)
    }

    private fun close(realm: Realm){
        if(!realm.isClosed){
            realm.close()
        }
    }

    /**
     * Creates default settings.
     */
    fun createDefaultSettings(settings: Settings){
        val realm = getInstance()

        val settingsDAO = SettingsDAO()
        settingsDAO.setId(DEFAULT_ID)
        settingsDAO.setLanguage(settings.language)
        settingsDAO.setAllowNotifications(settings.allowNotifications)
        settingsDAO.setNotificationTime(settings.notificationTime.toString())

        realm.executeTransaction {
            realm.copyToRealm(settingsDAO)
        }

        close(realm)
        Timber.tag("createDefaultSettings").i("New DB object with settings has been created. Object = ${settingsDAO.toString()}")
    }

    fun updateDefaultSettings(settings: Settings){
        val realm = getInstance()
        val settingsDAO = realm.where(SettingsDAO::class.java).equalTo(DB_FIELD_ID, DEFAULT_ID).findFirst()

        settingsDAO?.let {
            realm.executeTransaction {
                settingsDAO.setLanguage(settings.language)
                settingsDAO.setAllowNotifications(settings.allowNotifications)
                settingsDAO.setNotificationTime(settings.notificationTime.toString())

                realm.copyToRealm(settingsDAO)
                Timber.tag("updateDefaultSettings").i("Db object SettingsDAO was updated. Object = ${settingsDAO.toString()}")
            }

            close(realm)
        }?:run{
            Timber.tag("updateDefaultSettings").e("Db object SettingsDAO was not updated as search returned null.")
            close(realm)
        }
    }

    /**
     * Return default settings.
     */
    fun getDefaultSettings(): Settings? {
        val realm = getInstance()
        val settingsDAO = realm.where(SettingsDAO::class.java).equalTo("id", DEFAULT_ID).findFirst()

        settingsDAO?.let {
            val objectSettings = mapToKotlinObject(it)
            close(realm)
            return objectSettings
        }?:run{
            close(realm)
            return null
        }
    }

    private fun mapToKotlinObject(settingsDAO: SettingsDAO): Settings {
        return Settings(settingsDAO.getId(), settingsDAO.getLanguage(), settingsDAO.getAllowNotifications(), settingsDAO.getNotificationTime())
    }
}