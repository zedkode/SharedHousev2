package com.sharedhouse.android.ui.chat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64

@Composable
fun rememberChatMediaActions(
    context: Context,
    onPhoto: (String,Int,Int,String)->Unit,
    onLocation: (Double,Double)->Unit,
): ChatMediaActions {
    val photoPicker=rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(8)) { uris -> uris.forEach { uri -> compressPhoto(context,uri)?.let { onPhoto("image/jpeg",it.width,it.height,it.base64) } } }
    val captureFile=remember { File(context.cacheDir,"chat-capture.jpg") }
    val captureUri=remember { FileProvider.getUriForFile(context,"${context.packageName}.files",captureFile) }
    val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if(ok) compressPhoto(context,captureUri)?.let { onPhoto("image/jpeg",it.width,it.height,it.base64) } }
    val cameraPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if(granted) camera.launch(captureUri) }
    val locationPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants -> if(grants.values.any { it }) readLastLocation(context)?.let { onLocation(it.first,it.second) } }
    return ChatMediaActions(
        pickPhotos={photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},
        takePhoto={if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) camera.launch(captureUri) else cameraPermission.launch(Manifest.permission.CAMERA)},
        shareLocation={if(ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED) readLastLocation(context)?.let { onLocation(it.first,it.second) } else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION))},
    )
}

data class ChatMediaActions(val pickPhotos:()->Unit,val takePhoto:()->Unit,val shareLocation:()->Unit)
private data class CompressedPhoto(val width:Int,val height:Int,val base64:String)

private fun compressPhoto(context:Context,uri:Uri):CompressedPhoto?=runCatching {
    val bytes=context.contentResolver.openInputStream(uri)?.use { it.readBytes() }?:return null
    val original=BitmapFactory.decodeByteArray(bytes,0,bytes.size)?:return null
    val scale=minOf(1f,1600f/maxOf(original.width,original.height).toFloat())
    val bitmap=if(scale<1f) Bitmap.createScaledBitmap(original,(original.width*scale).toInt(),(original.height*scale).toInt(),true) else original
    val output=ByteArrayOutputStream(); bitmap.compress(Bitmap.CompressFormat.JPEG,82,output)
    val width=bitmap.width; val height=bitmap.height
    if(bitmap!==original) bitmap.recycle(); original.recycle()
    CompressedPhoto(width,height,Base64.getEncoder().encodeToString(output.toByteArray()))
}.getOrNull()

@Suppress("MissingPermission")
private fun readLastLocation(context:Context):Pair<Double,Double>? {
    val manager=context.getSystemService(LocationManager::class.java)
    return manager.getProviders(true).mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }?.let { it.latitude to it.longitude }
}
