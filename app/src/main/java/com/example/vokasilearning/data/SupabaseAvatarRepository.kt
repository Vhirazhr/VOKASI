package com.example.vokasilearning.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.jan.supabase.storage.storage
import java.io.ByteArrayOutputStream

/**
 * Shared profile pictures in the PRIVATE vokasi-avatars bucket.
 * Storage RLS restricts writes to each pupil's own username folder.
 * Only authenticated school members may download.
 */
internal object SupabaseAvatarRepository {
    private const val BUCKET = "vokasi-avatars"
    private const val MAX_BYTES = 1048576
    private data class CachedPhoto(val image: ImageBitmap, val fetchedAt: Long)
    private val photos = mutableMapOf<String, CachedPhoto>()
    private const val CACHE_AGE_MS = 90_000L

    private fun path(username: String) = "$username/avatar.jpg"

    suspend fun download(username: String, refresh: Boolean = false): ImageBitmap? {
        if (username.isBlank()) return null
        val now = System.currentTimeMillis()
        val cached = photos[username]
        if (!refresh && cached != null && now - cached.fetchedAt < CACHE_AGE_MS) {
            return cached.image
        }
        return try {
            val bytes = vokasiSupabase.storage.from(BUCKET)
                .downloadAuthenticated(path(username))
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return null
            bitmap.asImageBitmap().also {
                photos[username] = CachedPhoto(it, now)
            }
        } catch (_: Exception) {
            // No avatar yet, offline connection, or storage not provisioned.
            null
        }
    }

    /** Upsert the JPEG after Android activity permission + Supabase Auth. */
    suspend fun upload(username: String, jpeg: ByteArray) {
        require(username.isNotBlank()) { "Sesi siswa diperlukan untuk mengunggah foto." }
        require(jpeg.isNotEmpty() && jpeg.size <= MAX_BYTES) {
            "Foto maksimal 1 MB. Pilih foto yang lebih kecil."
        }
        vokasiSupabase.storage.from(BUCKET).upload(path(username), jpeg) {
            upsert = true
        }
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)?.asImageBitmap()?.let {
            photos[username] = CachedPhoto(it, System.currentTimeMillis())
        }
    }

    /** Save as compressed JPEG, bounded to the bucket's safe size. */
    suspend fun uploadBitmap(username: String, bitmap: Bitmap) {
        val scale = (512f / maxOf(bitmap.width, bitmap.height)).coerceAtMost(1f)
        val small = Bitmap.createScaledBitmap(bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1), true)
        val buffer = ByteArrayOutputStream()
        check(small.compress(Bitmap.CompressFormat.JPEG, 75, buffer)) {
            "Gagal menyiapkan foto."
        }
        upload(username, buffer.toByteArray())
    }

    /**
     * One-time migration for photos previously saved on this same device.
     * Never overwrite an existing server photo with an old device copy.
     */
    suspend fun restoreExisting(context: Context, username: String, rosterId: Long): ImageBitmap? {
        val cloud = download(username, refresh = true)
        if (cloud != null) return cloud
        val bytes = StudentAccountStore.photoJpegBytes(context, rosterId)
        if (bytes != null && bytes.size <= MAX_BYTES) {
            try {
                // A legacy picture is only sent if no remote avatar is visible.
                vokasiSupabase.storage.from(BUCKET).upload(path(username), bytes) {
                    upsert = false
                }
                return download(username, refresh = true)
            } catch (_: Exception) {
                // A concurrent upload or unavailable storage should not block login.
            }
        }
        return StudentAccountStore.photo(context, rosterId)
    }
}
