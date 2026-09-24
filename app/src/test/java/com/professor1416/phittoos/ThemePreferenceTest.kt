package com.professor1416.phittoos

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.professor1416.phittoos.data.dao.ActivityDao
import com.professor1416.phittoos.data.dao.FriendDao
import com.professor1416.phittoos.data.dao.TransactionDao
import com.professor1416.phittoos.data.db.AppDatabase
import com.professor1416.phittoos.data.preferences.AppThemeMode
import com.professor1416.phittoos.data.preferences.UserPreferences
import com.professor1416.phittoos.data.repository.PhittoosRepository
import com.professor1416.phittoos.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ThemePreferenceTest {

    private lateinit var db: AppDatabase
    private lateinit var friendDao: FriendDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var activityDao: ActivityDao
    private lateinit var repository: PhittoosRepository
    private lateinit var userPreferences: UserPreferences
    private lateinit var context: Context
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        friendDao = db.friendDao()
        transactionDao = db.transactionDao()
        activityDao = db.activityDao()
        repository = PhittoosRepository(friendDao, transactionDao, activityDao)
        userPreferences = UserPreferences(context)
        userPreferences.clearAll()
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun defaultThemeMode_isSystemDefault() {
        assertEquals(AppThemeMode.SYSTEM, userPreferences.themeMode)
        assertEquals(AppThemeMode.SYSTEM, userPreferences.themeModeFlow.value)
    }

    @Test
    fun updateThemeMode_persistsAndUpdatesFlow() {
        userPreferences.themeMode = AppThemeMode.DARK
        assertEquals(AppThemeMode.DARK, userPreferences.themeMode)
        assertEquals(AppThemeMode.DARK, userPreferences.themeModeFlow.value)

        userPreferences.themeMode = AppThemeMode.LIGHT
        assertEquals(AppThemeMode.LIGHT, userPreferences.themeMode)
        assertEquals(AppThemeMode.LIGHT, userPreferences.themeModeFlow.value)

        userPreferences.themeMode = AppThemeMode.SYSTEM
        assertEquals(AppThemeMode.SYSTEM, userPreferences.themeMode)
        assertEquals(AppThemeMode.SYSTEM, userPreferences.themeModeFlow.value)
    }

    @Test
    fun settingsViewModel_initializesAndUpdatesThemeMode() = runTest {
        val viewModel = SettingsViewModel(repository, userPreferences)

        assertEquals(AppThemeMode.SYSTEM, viewModel.uiState.value.themeMode)

        viewModel.updateThemeMode(AppThemeMode.DARK)
        assertEquals(AppThemeMode.DARK, viewModel.uiState.value.themeMode)
        assertEquals(AppThemeMode.DARK, userPreferences.themeMode)

        viewModel.updateThemeMode(AppThemeMode.LIGHT)
        assertEquals(AppThemeMode.LIGHT, viewModel.uiState.value.themeMode)
        assertEquals(AppThemeMode.LIGHT, userPreferences.themeMode)

        viewModel.updateThemeMode(AppThemeMode.SYSTEM)
        assertEquals(AppThemeMode.SYSTEM, viewModel.uiState.value.themeMode)
        assertEquals(AppThemeMode.SYSTEM, userPreferences.themeMode)
    }

    @Test
    fun appThemeModeEnum_fromKeyResolution() {
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromKey("system"))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.fromKey("light"))
        assertEquals(AppThemeMode.DARK, AppThemeMode.fromKey("dark"))
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromKey("unknown"))
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromKey(null))
    }
}
