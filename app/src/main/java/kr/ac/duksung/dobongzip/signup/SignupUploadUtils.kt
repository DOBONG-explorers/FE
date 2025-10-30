// SignupUploadUtils.kt
package kr.ac.duksung.dobongzip.signup

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import kr.ac.duksung.dobongzip.data.api.ApiClient
import kr.ac.duksung.dobongzip.data.api.ImageObjectKey
import kr.ac.duksung.dobongzip.data.auth.TokenHolder
import java.io.File
import java.io.FileOutputStream

/** 🔔 회원가입 성공 시 호출: accessToken 저장 + (이미지 있으면) 2단계 업로드 */
fun AppCompatActivity.onSignUpSuccess(accessToken: String, selectedImageUri: Uri?) {
    TokenHolder.accessToken = accessToken
    if (selectedImageUri != null) {
        lifecycleScope.launch {
            try {
                uploadProfileImageAfterSignup(this@onSignUpSuccess, selectedImageUri)
                // (원하면) Toast 등 성공 처리
            } catch (e: Exception) {
                // (원하면) 실패 처리
            }
        }
    }
}

/** ✅ 전체 업로드 플로우: Uri → PNG Multipart → upload → objectKey → finalize */
suspend fun uploadProfileImageAfterSignup(context: Context, uri: Uri) {
    val part = withContext(Dispatchers.IO) {
        makeImagePartFromUriPNG(context, uri, partName = "file")
            ?: error("이미지 PNG 변환에 실패했습니다.")
    }

    val stage1Res = ApiClient.myPageService.uploadProfileImageStage1(part)
    if (!stage1Res.success) error(stage1Res.message.ifBlank { "1단계 업로드 실패" })
    val objectKey = stage1Res.data?.objectKey ?: error("objectKey가 비어 있습니다.")

    val finalizeRes = ApiClient.myPageService.finalizeProfileImage(ImageObjectKey(objectKey))
    if (!finalizeRes.success) error(finalizeRes.message.ifBlank { "2단계 finalize 실패" })
    // 필요하면 finalizeRes.data?.imageUrl 사용
}

/**
 * ✅ PNG 강제 변환 버전
 * - 서버가 "PNG만 지원"하므로, 무조건 PNG로 인코딩해서 전송
 * - filename: profile.png
 * - Content-Type: image/png
 */
suspend fun makeImagePartFromUriPNG(
    context: Context,
    uri: Uri,
    partName: String = "file"
): MultipartBody.Part? = withContext(Dispatchers.IO) {
    try {
        // 1) Uri → Bitmap
        val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val src = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                decoder.isMutableRequired = false
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { ins ->
                BitmapFactory.decodeStream(ins)
            } ?: return@withContext null
        }

        // 2) PNG로 임시 파일 저장
        val tempFile = File.createTempFile("profile_", ".png", context.cacheDir).apply {
            deleteOnExit()
        }
        FileOutputStream(tempFile).use { out ->
            // PNG는 무손실이므로 quality 값은 무시되지만 100으로 둠
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                return@withContext null
            }
        }

        // 3) Multipart 생성 (image/png, filename=profile.png)
        val mediaType = "image/png".toMediaType()
        val reqBody = tempFile.asRequestBody(mediaType)
        MultipartBody.Part.createFormData(partName, "profile.png", reqBody)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
