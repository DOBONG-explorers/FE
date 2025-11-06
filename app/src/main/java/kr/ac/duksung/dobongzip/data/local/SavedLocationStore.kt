package kr.ac.duksung.dobongzip.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "location_prefs")

class SavedLocationStore(private val context: Context) {

    companion object {
        private val LAST_LATITUDE_KEY = doublePreferencesKey("last_latitude")
        private val LAST_LONGITUDE_KEY = doublePreferencesKey("last_longitude")
    }

    suspend fun saveLatitude(latitude: Double) {
        context.dataStore.edit { preferences ->
            preferences[LAST_LATITUDE_KEY] = latitude
        }
    }

    suspend fun saveLongitude(longitude: Double) {
        context.dataStore.edit { preferences ->
            preferences[LAST_LONGITUDE_KEY] = longitude
        }
    }

    val lastLatitude: Flow<Double?> = context.dataStore.data
        .map { preferences ->
            preferences[LAST_LATITUDE_KEY]
        }

    val lastLongitude: Flow<Double?> = context.dataStore.data
        .map { preferences ->
            preferences[LAST_LONGITUDE_KEY]
        }
}