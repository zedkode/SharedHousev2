package com.sharedhouse.android

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sharedhouse.android.ui.app.ApiSharedHouseGateway
import com.sharedhouse.android.ui.app.HouseholdFormState
import com.sharedhouse.android.ui.app.SharedHouseApp
import com.sharedhouse.android.ui.app.SharedHouseViewModel
import com.sharedhouse.android.ui.theme.SharedHouseTheme
import com.sharedhouse.network.SharedHouseApiClient
import com.sharedhouse.network.createSharedHouseHttpClient
import io.ktor.client.engine.okhttp.OkHttp
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Currency
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val gateway by lazy {
        ApiSharedHouseGateway(
            SharedHouseApiClient(
                client = createSharedHouseHttpClient(OkHttp.create()),
                baseUrl = BuildConfig.API_BASE_URL,
            ),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SharedHouseTheme {
                val appViewModel: SharedHouseViewModel = viewModel(
                    factory = sharedHouseViewModelFactory(),
                )
                SharedHouseApp(viewModel = appViewModel)
            }
        }
    }

    private fun sharedHouseViewModelFactory(): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(SharedHouseViewModel::class.java))
                val locale = resources.configuration.locales[0] ?: Locale.getDefault()
                return SharedHouseViewModel(
                    gateway = gateway,
                    deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(80),
                    preferredLocale = if (locale.language == "ro") "ro" else "en",
                    initialHousehold = defaultHouseholdForm(locale),
                ) as T
            }
        }

    private fun defaultHouseholdForm(locale: Locale): HouseholdFormState {
        val countryCode = locale.country.takeIf { it.length == 2 }?.uppercase() ?: "GB"
        val currency = runCatching { Currency.getInstance(locale).currencyCode }
            .getOrDefault(if (countryCode == "RO") "RON" else "GBP")
        val firstDay = WeekFields.of(locale).firstDayOfWeek.value.takeIf { it in setOf(1, 6, 7) } ?: 1
        return HouseholdFormState(
            countryCode = countryCode,
            timezone = ZoneId.systemDefault().id,
            currency = currency,
            firstDayOfWeek = firstDay,
            cycleAnchor = LocalDate.now().toString(),
        )
    }
}
