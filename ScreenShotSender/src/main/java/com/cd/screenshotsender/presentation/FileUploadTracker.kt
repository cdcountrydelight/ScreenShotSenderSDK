package com.cd.screenshotsender.presentation

import android.content.Context
import android.graphics.Bitmap
import com.cd.screenshotsender.domain.domain_utils.AppErrorCodes
import com.cd.screenshotsender.domain.domain_utils.DataResponseStatus
import com.cd.screenshotsender.domain.use_cases.SendPackageNameUseCase
import com.cd.screenshotsender.domain.use_cases.SendScreenshotUseCase
import com.cd.screenshotsender.presentation.utils.DataUiResponseStatus
import com.cd.screenshotsender.presentation.utils.FunctionHelper.mapToDataUiResponseStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

internal class FileUploadTracker {

    private val _sendScreenShotStateFlow: MutableStateFlow<DataUiResponseStatus<Unit>> =
        MutableStateFlow(DataUiResponseStatus.Companion.none())

    val sendScreenShotStateFlow = _sendScreenShotStateFlow.asStateFlow()

    fun sendScreenShot(context: Context, imageBitmap: Bitmap, packageName: String) {
        if (_sendScreenShotStateFlow.value is DataUiResponseStatus.Loading) {
            return
        }
        _sendScreenShotStateFlow.value = DataUiResponseStatus.Companion.loading()
        CoroutineScope(Dispatchers.IO).launch {
            _sendScreenShotStateFlow.value = try {
                val sendPackageNameUseCase = SendPackageNameUseCase()
                val response = sendPackageNameUseCase.invoke(packageName, context)
                when (response) {
                    is DataResponseStatus.Failure -> {
                        DataUiResponseStatus.Companion.failure(
                            response.errorMessage,
                            response.errorCode
                        )
                    }

                    is DataResponseStatus.Success -> {
                        val flowId = response.data.flowId
                        if (flowId == null) {
                            DataUiResponseStatus.Companion.failure(
                                "Flow id can't be null",
                                AppErrorCodes.UNKNOWN_ERROR
                            )
                        } else {
                            val screenshotFile = createFileFromBitmap(imageBitmap, context)
                            val screenshotPart = MultipartBody.Part.createFormData(
                                "screenshot",
                                screenshotFile.name,
                                screenshotFile.asRequestBody("image/png".toMediaTypeOrNull())
                            )
                            val sendScreenshotUseCase = SendScreenshotUseCase()
                            val response =
                                sendScreenshotUseCase.invoke(
                                    flowId = flowId,
                                    screenshotPart,
                                    context
                                ).mapToDataUiResponseStatus()

                            if (response is DataUiResponseStatus.Success) {
                                screenshotFile.delete()
                            }
                            response
                        }

                    }
                }
            } catch (exception: Exception) {
                DataUiResponseStatus.Companion.failure(
                    exception.localizedMessage ?: exception.message ?: "",
                    AppErrorCodes.UNKNOWN_ERROR
                )
            }
        }
    }
    private fun createFileFromBitmap(bitmap: Bitmap, context: Context): File {
        val timestamp = System.currentTimeMillis()
        val file = File(context.cacheDir, "screenshot_$timestamp.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }
}