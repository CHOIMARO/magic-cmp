package com.company.feature.editor.export

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.ByteArrayOutputStream

internal actual fun ImageBitmap.encodeToPng(): ByteArray {
    // 레이어에서 만든 비트맵은 GPU 메모리에 있을 수 있다. PNG로 바꾸려면 일반 비트맵으로 복사한다.
    val bitmap = asAndroidBitmap().let { if (it.config == Bitmap.Config.HARDWARE) it.copy(Bitmap.Config.ARGB_8888, false) else it }
    return ByteArrayOutputStream().use { stream ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.toByteArray()
    }
}
