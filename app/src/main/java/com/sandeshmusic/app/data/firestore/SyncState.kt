package com.sandeshmusic.app.data.firestore

sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data class Synced(val count: Int, val timestamp: Long) : SyncState
    data class Error(val message: String) : SyncState
}
