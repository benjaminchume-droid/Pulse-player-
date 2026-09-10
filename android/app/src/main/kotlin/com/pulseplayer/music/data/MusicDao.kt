package com.pulseplayer.music.data

import androidx.room.*

@Dao
interface MusicDao {
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    suspend fun getAllSongs(): List<Song>

    @Query("SELECT * FROM songs WHERE id = :songId LIMIT 1")
    suspend fun getSongById(songId: Long): Song?

    @Query("SELECT * FROM songs WHERE isDownloaded = 1 ORDER BY title ASC")
    suspend fun getDownloadedSongs(): List<Song>

    @Query("SELECT * FROM songs WHERE isFavorite = 1")
    suspend fun getFavoriteSongs(): List<Song>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)

    @Update
    suspend fun updateSong(song: Song)

    @Delete
    suspend fun deleteSong(song: Song)

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE songs SET playCount = playCount + 1 WHERE id = :id")
    suspend fun incrementPlayCount(id: Long)

    @Query("UPDATE songs SET lyrics = :lyrics, syncedLyrics = :syncedLyrics WHERE id = :id")
    suspend fun updateLyrics(id: Long, lyrics: String, syncedLyrics: String)

    @Query("""
        UPDATE songs SET title = :title, artist = :artist, album = :album,
        albumArtist = :albumArtist, year = :year, genre = :genre,
        coverUrl = :coverUrl, metadataEnriched = 1,
        titleProvenance = :titleProvenance, artistProvenance = :artistProvenance,
        albumProvenance = :albumProvenance, recognitionId = :recognitionId,
        lastRecognizedAt = :lastRecognizedAt
        WHERE id = :id
    """)
    suspend fun updateMetadata(
        id: Long,
        title: String,
        artist: String,
        album: String,
        albumArtist: String,
        year: Int,
        genre: String,
        coverUrl: String,
        titleProvenance: String = "automatic",
        artistProvenance: String = "automatic",
        albumProvenance: String = "automatic",
        recognitionId: String = "",
        lastRecognizedAt: Long = 0L
    )

    @Query("SELECT DISTINCT album FROM songs WHERE album != '' AND album != 'Unknown Album' ORDER BY album ASC")
    suspend fun getAlbums(): List<String>

    @Query("SELECT * FROM songs WHERE album = :album ORDER BY trackNumber ASC, title ASC")
    suspend fun getSongsByAlbum(album: String): List<Song>

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    suspend fun getAllPlaylists(): List<Playlist>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist)

    @Update
    suspend fun updatePlaylist(playlist: Playlist)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: String): Playlist?
}
