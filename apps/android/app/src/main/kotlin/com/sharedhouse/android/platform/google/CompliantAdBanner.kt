package com.sharedhouse.android.platform.google

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.sharedhouse.android.BuildConfig

@Composable
fun CompliantAdBanner(
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val inspectionMode = LocalInspectionMode.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val width = maxWidth.value.toInt().coerceAtLeast(320)
        val adView = remember(enabled, activity, width, inspectionMode) {
            if (enabled && activity != null && !inspectionMode) AdView(activity) else null
        }
        LaunchedEffect(adView, activity, width) {
            if (adView == null || activity == null) return@LaunchedEffect
            val size = AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, width)
            val request = BannerAdRequest.Builder(BuildConfig.ADMOB_BANNER_ID, size).build()
            adView.loadAd(
                request,
                object : AdLoadCallback<BannerAd> {},
            )
        }

        adView?.let { view ->
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AndroidView(
                    modifier = Modifier.wrapContentSize(),
                    factory = { view },
                )
            }
        }

        DisposableEffect(adView) {
            onDispose {
                adView?.destroy()
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
