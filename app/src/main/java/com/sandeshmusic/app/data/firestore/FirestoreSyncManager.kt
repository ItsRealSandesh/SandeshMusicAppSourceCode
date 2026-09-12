package com.sandeshmusic.app.data.firestore

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.sandeshmusic.app.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class SyncResult(
    val totalCount: Int,
    val addedToLocal: List<String>,
    val uploadedToCloud: List<String>
)

class FirestoreSyncManager(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "FirestoreSync"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_FAVORITES = "favorites"
    }

    suspend fun uploadFavorite(userId: String, song: Song): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (userId.isBlank() || song.id.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("User ID and Song ID must not be empty"))
            }

            val data = hashMapOf(
                "songId" to song.id,
                "title" to song.title,
                "artist" to song.artist,
                "album" to (song.album ?: ""),
                "coverUrl" to (song.coverUrl ?: ""),
                "audioUrl" to song.audioUrl,
                "updatedAt" to System.currentTimeMillis()
            )

            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_FAVORITES)
                .document(song.id)
                .set(data, SetOptions.merge())
                .await()

            Log.d(TAG, "Uploaded favorite ${song.id} to Firestore for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving favorite to Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun removeFavorite(userId: String, songId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (userId.isBlank() || songId.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("User ID and Song ID must not be empty"))
            }

            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_FAVORITES)
                .document(songId)
                .delete()
                .await()

            Log.d(TAG, "Removed favorite $songId from Firestore for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing favorite from Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun fetchFavoriteSongIds(userId: String): Result<Set<String>> = withContext(Dispatchers.IO) {
        try {
            if (userId.isBlank()) {
                return@withContext Result.success(emptySet())
            }

            val snapshot = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_FAVORITES)
                .get()
                .await()

            val songIds = snapshot.documents.map { doc ->
                doc.getString("songId") ?: doc.id
            }.toSet()

            Result.success(songIds)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching favorites from Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun syncFavorites(
        userId: String,
        localFavoriteIds: Set<String>,
        getSongById: (String) -> Song?
    ): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            if (userId.isBlank()) {
                return@withContext Result.failure(IllegalStateException("No user signed in"))
            }

            // 1. Fetch remote favorite IDs
            val remoteResult = fetchFavoriteSongIds(userId)
            val remoteIds = remoteResult.getOrThrow()

            val newlyAddedToLocal = mutableListOf<String>()
            val newlyUploadedToCloud = mutableListOf<String>()

            // 2. Upload any local favorites missing from Firestore
            val missingInCloud = localFavoriteIds - remoteIds
            for (id in missingInCloud) {
                val song = getSongById(id)
                if (song != null) {
                    uploadFavorite(userId, song)
                    newlyUploadedToCloud.add(id)
                }
            }

            // 3. Mark remote favorites missing locally to be added to Room
            val missingInLocal = remoteIds - localFavoriteIds
            newlyAddedToLocal.addAll(missingInLocal)

            val totalCount = (remoteIds + localFavoriteIds).size
            Log.i(TAG, "Sync complete. Total: $totalCount, Added to local: ${newlyAddedToLocal.size}, Uploaded to cloud: ${newlyUploadedToCloud.size}")

            Result.success(
                SyncResult(
                    totalCount = totalCount,
                    addedToLocal = newlyAddedToLocal,
                    uploadedToCloud = newlyUploadedToCloud
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Favorites sync failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
