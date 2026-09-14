package com.sandeshmusic.app

import com.sandeshmusic.app.data.model.Song
import com.sandeshmusic.app.player.PlayerState
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SongModelTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun testSongJsonParsing() {
        val sampleJson = """
            [
              {
                "id": "1",
                "title": "75 Ka Chini 75 Ka Paua",
                "artist": "Pravesh Premi",
                "album": "Single",
                "audioUrl": "https://song.codewithsandesh.com/music/song1.mp3",
                "coverUrl": "https://song.codewithsandesh.com/covers/song01.png"
              }
            ]
        """.trimIndent()

        val songs = json.decodeFromString<List<Song>>(sampleJson)
        assertEquals(1, songs.size)
        val first = songs.first()
        assertEquals("1", first.id)
        assertEquals("75 Ka Chini 75 Ka Paua", first.title)
        assertEquals("Pravesh Premi", first.artist)
        assertEquals("Single", first.album)
        assertTrue(first.audioUrl.endsWith("song1.mp3"))
        assertNotNull(first.coverUrl)
    }

    @Test
    fun testCoverUrlResolution() {
        val song1 = Song(
            id = "1",
            title = "Test",
            coverUrl = "https://song.codewithsandesh.com/covers/song1.png"
        ).withResolvedCovers()
        assertEquals("https://song.codewithsandesh.com/covers/song01.png", song1.coverUrl)

        val songJpg = Song(
            id = "1",
            title = "Test",
            coverUrl = "https://song.codewithsandesh.com/covers/song1.jpg"
        ).withResolvedCovers()
        assertEquals("https://song.codewithsandesh.com/covers/song01.png", songJpg.coverUrl)

        val relativeSong = Song(
            id = "1",
            title = "Test",
            coverUrl = "covers/song01.png"
        ).withResolvedCovers()
        assertEquals("https://song.codewithsandesh.com/covers/song01.png", relativeSong.coverUrl)
    }

    @Test
    fun testUnclosedJsonHealing() {
        var unclosed = """[{"id":"1","title":"Song"}"}" """.trim() // unclosed array
        unclosed = """[{"id":"1","title":"Song"}"""
        if (unclosed.startsWith("[") && !unclosed.endsWith("]")) {
            unclosed = "$unclosed\n]"
        }
        val songs = json.decodeFromString<List<Song>>(unclosed)
        assertEquals(1, songs.size)
        assertEquals("Song", songs.first().title)
    }

    @Test
    fun testPlayerStateTimeFormatting() {
        assertEquals("0:00", PlayerState.formatTime(0L))
        assertEquals("0:45", PlayerState.formatTime(45000L))
        assertEquals("3:24", PlayerState.formatTime(204000L))
        assertEquals("1:05:30", PlayerState.formatTime(3930000L))
    }

    @Test
    fun testAuthUserAndSyncResult() {
        val user = com.sandeshmusic.app.data.auth.AuthUser(
            uid = "user123",
            email = "test@example.com"
        )
        assertEquals("user123", user.uid)
        assertEquals("test@example.com", user.email)

        val syncResult = com.sandeshmusic.app.data.firestore.SyncResult(
            totalCount = 5,
            addedToLocal = listOf("1", "2"),
            uploadedToCloud = listOf("3")
        )
        assertEquals(5, syncResult.totalCount)
        assertEquals(2, syncResult.addedToLocal.size)
        assertEquals(1, syncResult.uploadedToCloud.size)
    }

    @Test
    fun testPlayerStateVolumeBoost() {
        val defaultState = PlayerState()
        assertEquals(100, defaultState.volumeBoostPercent)
        assertEquals(0, defaultState.bassBoostPercent)

        val boostedState = defaultState.copy(volumeBoostPercent = 175, bassBoostPercent = 50)
        assertEquals(175, boostedState.volumeBoostPercent)
        assertEquals(50, boostedState.bassBoostPercent)
    }

    @Test
    fun testSongSortOrders() {
        val s1 = Song(id = "1", title = "Zebra Track", artist = "Bravo")
        val s2 = Song(id = "2", title = "Alpha Track", artist = "Zulu")
        val s3 = Song(id = "3", title = "Middle Track", artist = "Alpha")
        val list = listOf(s1, s2, s3)

        val byTitle = list.sortedBy { it.title.lowercase() }
        assertEquals("Alpha Track", byTitle[0].title)
        assertEquals("Middle Track", byTitle[1].title)
        assertEquals("Zebra Track", byTitle[2].title)

        val byArtist = list.sortedBy { it.artist.lowercase() }
        assertEquals("Alpha", byArtist[0].artist)
        assertEquals("Bravo", byArtist[1].artist)
        assertEquals("Zulu", byArtist[2].artist)

        val shuffled = list.shuffled()
        assertEquals(3, shuffled.size)
        assertTrue(shuffled.containsAll(list))
    }
}
