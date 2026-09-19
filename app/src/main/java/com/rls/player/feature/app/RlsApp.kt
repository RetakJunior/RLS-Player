package com.rls.player.feature.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rls.player.core.designsystem.*
import com.rls.player.core.domain.model.Album
import com.rls.player.core.domain.model.Artist
import com.rls.player.core.domain.model.Folder
import com.rls.player.core.domain.model.RepeatSetting
import com.rls.player.core.domain.model.Song
import com.rls.player.core.domain.model.SongSort
import com.rls.player.core.player.SleepTimerState
import com.rls.player.feature.library.LibraryUiState
import com.rls.player.feature.library.LibraryViewModel
import com.rls.player.feature.player.PlayerUiState
import com.rls.player.feature.player.PlayerViewModel
import com.rls.player.feature.player.QueueItem
import com.rls.player.feature.settings.SettingsViewModel
import kotlinx.coroutines.delay

private enum class Tab(val label: String) {
    SONGS("Şarkılar"),
    ALBUMS("Albümler"),
    ARTISTS("Sanatçılar"),
    FOLDERS("Klasörler"),
    COLLECTION("Kitaplık")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RlsApp(
    library: LibraryViewModel = viewModel(),
    player: PlayerViewModel = viewModel(),
    settings: SettingsViewModel = viewModel()
) {
    val libraryState by library.state.collectAsStateWithLifecycle()
    val playerState by player.state.collectAsStateWithLifecycle()
    val settingsState by settings.state.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(Tab.SONGS) }
    var showPlayer by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val audioPermission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        granted = isGranted
        if (isGranted) library.rescan()
    }

    LaunchedEffect(granted) {
        if (granted && libraryState.songs.isEmpty() && !libraryState.scanning) {
            library.rescan()
        }
    }

    libraryState.message?.let { msg ->
        LaunchedEffect(msg) {
            delay(2800)
            library.consumeMessage()
        }
        Snackbar(
            modifier = Modifier.padding(16.dp),
            containerColor = SurfaceSelected,
            contentColor = Cream
        ) {
            Text(msg)
        }
    }

    if (!granted) {
        PermissionScreen { launcher.launch(audioPermission) }
    } else if (showSettings) {
        SettingsScreen(
            reduced = settingsState.reduceMotion,
            onReducedMotion = settings::setReducedMotion,
            onBack = { showSettings = false },
            onRescan = library::rescan
        )
    } else {
        Scaffold(
            containerColor = Night,
            topBar = {
                TopAppBar(
                    title = { Text("RLS Player", style = MaterialTheme.typography.titleLarge) },
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Outlined.Settings, "Ayarlar", tint = Cream)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Night,
                        titleContentColor = Cream
                    )
                )
            },
            bottomBar = {
                Column {
                    if (playerState.song != null) {
                        MiniPlayerBar(
                            state = playerState,
                            onTogglePlay = player::togglePlay,
                            onNext = player::next,
                            onToggleShuffle = player::toggleShuffle,
                            onCycleRepeat = player::cycleRepeat,
                            onOpenPlayer = { showPlayer = true }
                        )
                    }
                    NavigationBar(
                        containerColor = Surface,
                        tonalElevation = 0.dp
                    ) {
                        Tab.entries.forEach { item ->
                            NavigationBarItem(
                                selected = tab == item,
                                onClick = { tab = item },
                                icon = { Icon(tabIcon(item), item.label) },
                                label = { Text(item.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Night,
                                    selectedTextColor = Cream,
                                    indicatorColor = Cream,
                                    unselectedIconColor = MutedCream,
                                    unselectedTextColor = MutedCream
                                )
                            )
                        }
                    }
                }
            }
        ) { padding ->
            LibraryContent(
                tab = tab,
                state = libraryState,
                playerState = playerState,
                player = player,
                library = library,
                modifier = Modifier.padding(padding)
            )
        }
    }

    if (showPlayer) {
        FullScreenPlayerSheet(
            state = playerState,
            player = player,
            onDismiss = { showPlayer = false }
        )
    }
}

@Composable
private fun PermissionScreen(onGrant: () -> Unit) = Surface(
    color = Night,
    modifier = Modifier.fillMaxSize()
) {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.LibraryMusic,
            contentDescription = null,
            tint = Cream,
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Müziğin, cihazında kalır.",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "RLS Player yalnızca yerel ses dosyalarını kütüphanende göstermek için depolama erişimi ister. İnternet bağlantısı, reklam veya veri aktarımı bulunmaz.",
            color = MutedCream,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onGrant,
            colors = ButtonDefaults.buttonColors(containerColor = Cream, contentColor = Night),
            shape = RlsShapes.medium
        ) {
            Text(
                text = "Müziğe İzin Ver",
                style = MaterialTheme.typography.labelLarge,
                color = Night
            )
        }
    }
}

@Composable
private fun LibraryContent(
    tab: Tab,
    state: LibraryUiState,
    playerState: PlayerUiState,
    player: PlayerViewModel,
    library: LibraryViewModel,
    modifier: Modifier
) = Column(modifier.fillMaxSize()) {
    when (tab) {
        Tab.SONGS -> SongsPage(
            songs = state.songs,
            query = state.query,
            sort = state.sort,
            scanning = state.scanning,
            playingSongId = playerState.song?.id,
            onQuery = library::setQuery,
            onSortChange = library::setSort,
            onPlay = { list, idx -> player.play(list, idx) },
            onFavorite = library::toggleFavorite
        )
        Tab.ALBUMS -> AlbumsPage(
            albums = state.albums,
            allSongs = state.songs,
            onPlayAlbum = { albumSongs -> player.play(albumSongs, 0) }
        )
        Tab.ARTISTS -> ArtistsPage(
            artists = state.artists,
            allSongs = state.songs,
            onPlayArtist = { artistSongs -> player.play(artistSongs, 0) }
        )
        Tab.FOLDERS -> FoldersPage(
            folders = state.folders,
            allSongs = state.songs,
            onPlayFolder = { folderSongs -> player.play(folderSongs, 0) }
        )
        Tab.COLLECTION -> CollectionPage(
            state = state,
            onCreatePlaylist = library::createPlaylist,
            onDeletePlaylist = library::deletePlaylist,
            onPlaySongs = { list, idx -> player.play(list, idx) }
        )
    }
}

@Composable
private fun SongsPage(
    songs: List<Song>,
    query: String,
    sort: SongSort,
    scanning: Boolean,
    playingSongId: Long?,
    onQuery: (String) -> Unit,
    onSortChange: (SongSort) -> Unit,
    onPlay: (List<Song>, Int) -> Unit,
    onFavorite: (Long) -> Unit
) {
    var showSortDialog by remember { mutableStateOf(false) }

    val filtered = remember(songs, query) {
        if (query.isBlank()) songs
        else songs.filter { "${it.title} ${it.artist} ${it.album}".contains(query, ignoreCase = true) }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = Dimens.ScreenPadding)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Parça, sanatçı veya albüm ara") },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = MutedCream) },
                singleLine = true,
                shape = RlsShapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Cream,
                    unfocusedBorderColor = SurfaceSelected,
                    focusedTextColor = Cream,
                    unfocusedTextColor = Cream,
                    cursorColor = Cream
                )
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { showSortDialog = true },
                modifier = Modifier
                    .clip(RlsShapes.medium)
                    .background(SurfaceSelected)
                    .size(54.dp)
            ) {
                Icon(Icons.Outlined.Sort, "Sırala", tint = Cream)
            }
        }

        if (scanning) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = Cream,
                trackColor = SurfaceSelected
            )
        }

        if (filtered.isEmpty() && !scanning) {
            EmptyView(
                title = if (songs.isEmpty()) "Müzik Bulunamadı" else "Sonuç Yok",
                message = if (songs.isEmpty()) "Cihazında kayıtlı ses dosyası bulunamadı. Ayarlar'dan yeniden tarayabilirsin." else "'$query' aramasıyla eşleşen parça yok."
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                itemsIndexed(filtered, key = { _, s -> s.id }) { index, song ->
                    SongRowItem(
                        song = song,
                        isPlaying = song.id == playingSongId,
                        onClick = { onPlay(filtered, index) },
                        onFavoriteToggle = { onFavorite(song.id) }
                    )
                }
            }
        }
    }

    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("Şarkıları Sırala") },
            text = {
                Column {
                    SongSort.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSortChange(option)
                                    showSortDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = sort == option,
                                onClick = {
                                    onSortChange(option)
                                    showSortDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Cream)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(option.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortDialog = false }) { Text("Kapat") }
            }
        )
    }
}

@Composable
private fun AlbumsPage(
    albums: List<Album>,
    allSongs: List<Song>,
    onPlayAlbum: (List<Song>) -> Unit
) {
    if (albums.isEmpty()) {
        EmptyView(title = "Albüm Yok", message = "Kütüphanende henüz taranmış albüm bulunmuyor.")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(albums, key = { it.id }) { album ->
            Surface(
                color = Surface,
                shape = RlsShapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val albumSongs = allSongs.filter { it.albumId == album.id }
                        if (albumSongs.isNotEmpty()) onPlayAlbum(albumSongs)
                    }
            ) {
                Column(Modifier.padding(12.dp)) {
                    AlbumArtImage(
                        albumId = album.id,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${album.artist} · ${album.songCount} parça",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedCream,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistsPage(
    artists: List<Artist>,
    allSongs: List<Song>,
    onPlayArtist: (List<Song>) -> Unit
) {
    if (artists.isEmpty()) {
        EmptyView(title = "Sanatçı Yok", message = "Kütüphanende sanatçı bilgisi bulunamadı.")
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(artists, key = { it.name }) { artist ->
            Surface(
                color = Surface,
                shape = RlsShapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val artistSongs = allSongs.filter { it.artist == artist.name }
                        if (artistSongs.isNotEmpty()) onPlayArtist(artistSongs)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RlsShapes.medium)
                            .background(SurfaceSelected),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Person, null, tint = Cream)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${artist.songCount} parça",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedCream
                        )
                    }
                    Icon(Icons.Outlined.PlayArrow, "Çal", tint = Cream)
                }
            }
        }
    }
}

@Composable
private fun FoldersPage(
    folders: List<Folder>,
    allSongs: List<Song>,
    onPlayFolder: (List<Song>) -> Unit
) {
    if (folders.isEmpty()) {
        EmptyView(title = "Klasör Yok", message = "Cihazında müzik klasörü bulunamadı.")
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(folders, key = { it.path }) { folder ->
            Surface(
                color = Surface,
                shape = RlsShapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val folderSongs = allSongs.filter { it.folder == folder.path }
                        if (folderSongs.isNotEmpty()) onPlayFolder(folderSongs)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RlsShapes.medium)
                            .background(SurfaceSelected),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Folder, null, tint = Cream)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = folder.path,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${folder.songCount} parça",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedCream
                        )
                    }
                    Icon(Icons.Outlined.PlayArrow, "Klasörü Çal", tint = Cream)
                }
            }
        }
    }
}

@Composable
private fun CollectionPage(
    state: LibraryUiState,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onPlaySongs: (List<Song>, Int) -> Unit
) {
    var dialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Kitaplık", style = MaterialTheme.typography.headlineMedium)
                IconButton(onClick = { dialog = true }) {
                    Icon(Icons.Outlined.Add, "Yeni Liste", tint = Cream)
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Favoriler Kartı
                Surface(
                    color = Surface,
                    shape = RlsShapes.medium,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1.2f)
                        .clickable {
                            if (state.favorites.isNotEmpty()) onPlaySongs(state.favorites, 0)
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RlsShapes.medium)
                                .background(SurfaceSelected),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Favorite, null, tint = Cream)
                        }
                        Column {
                            Text("Favoriler", style = MaterialTheme.typography.titleMedium)
                            Text("${state.favorites.size} parça", style = MaterialTheme.typography.bodySmall, color = MutedCream)
                        }
                    }
                }

                // Son Çalınanlar Kartı
                Surface(
                    color = Surface,
                    shape = RlsShapes.medium,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1.2f)
                        .clickable {
                            if (state.recent.isNotEmpty()) onPlaySongs(state.recent, 0)
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RlsShapes.medium)
                                .background(SurfaceSelected),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.History, null, tint = Cream)
                        }
                        Column {
                            Text("Son Çalınanlar", style = MaterialTheme.typography.titleMedium)
                            Text("${state.recent.size} parça", style = MaterialTheme.typography.bodySmall, color = MutedCream)
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            Text("Çalma Listeleri", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
        }

        if (state.playlists.isEmpty()) {
            item {
                Text(
                    text = "Henüz çalma listesi oluşturmadın. Sağ üstteki '+' butonu ile oluşturabilirsin.",
                    color = MutedCream,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(state.playlists, key = { it.id }) { pl ->
                Surface(
                    color = Surface,
                    shape = RlsShapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RlsShapes.medium)
                                .background(SurfaceSelected),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.QueueMusic, null, tint = Cream)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(pl.name, style = MaterialTheme.typography.titleMedium)
                            Text("${pl.songCount} parça", style = MaterialTheme.typography.bodySmall, color = MutedCream)
                        }
                        IconButton(onClick = { onDeletePlaylist(pl.id) }) {
                            Icon(Icons.Outlined.Delete, "Listeyi Sil", tint = MutedCream)
                        }
                    }
                }
            }
        }
    }

    if (dialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { dialog = false },
            title = { Text("Yeni Çalma Listesi") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Liste Adı") },
                    singleLine = true,
                    shape = RlsShapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cream,
                        unfocusedBorderColor = SurfaceSelected,
                        focusedTextColor = Cream,
                        unfocusedTextColor = Cream
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCreatePlaylist(name)
                        dialog = false
                    },
                    enabled = name.isNotBlank()
                ) { Text("Oluştur") }
            },
            dismissButton = {
                TextButton(onClick = { dialog = false }) { Text("Vazgeç") }
            }
        )
    }
}

@Composable
private fun MiniPlayerBar(
    state: PlayerUiState,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenPlayer: () -> Unit
) = Surface(
    color = SurfaceSelected,
    shape = RlsShapes.medium,
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 4.dp)
        .clickable(onClick = onOpenPlayer)
) {
    Row(
        modifier = Modifier
            .height(Dimens.MiniPlayerHeight)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArtImage(
            albumId = state.song?.albumId ?: -1,
            modifier = Modifier.size(Dimens.AlbumArtSmall)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = state.song?.title.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = state.song?.artist.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MutedCream,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onToggleShuffle) {
            Icon(
                Icons.Outlined.Shuffle, "Karıştır",
                tint = if (state.shuffle) Cream else MutedCream,
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = onCycleRepeat) {
            Icon(
                when (state.repeat) {
                    RepeatSetting.ONE -> Icons.Outlined.RepeatOne
                    else -> Icons.Outlined.Repeat
                }, "Tekrar",
                tint = if (state.repeat == RepeatSetting.OFF) MutedCream else Cream,
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = onTogglePlay) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = if (state.isPlaying) "Duraklat" else "Çal",
                tint = Cream
            )
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Outlined.SkipNext, "Sonraki", tint = Cream)
        }
    }
}

@Composable
private fun FullScreenPlayerSheet(
    state: PlayerUiState,
    player: PlayerViewModel,
    onDismiss: () -> Unit
) {
    var showTimer by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Night,
            shape = RlsShapes.extraLarge,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showQueue = true }) {
                        Icon(Icons.Outlined.QueueMusic, "Kuyruk", tint = Cream)
                    }
                    Row {
                        IconButton(onClick = { showTimer = true }) {
                            Icon(Icons.Outlined.Timer, "Uyku Zamanlayıcısı", tint = Cream)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Outlined.KeyboardArrowDown, "Kapat", tint = Cream)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                AlbumArtImage(
                    albumId = state.song?.albumId ?: -1,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = state.song?.title.orEmpty(),
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = state.song?.artist.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MutedCream
                )

                Spacer(Modifier.height(16.dp))

                val positionMs by player.positionMs.collectAsStateWithLifecycle()

                Slider(
                    value = positionMs.toFloat(),
                    onValueChange = { player.seek(it.toLong()) },
                    valueRange = 0f..state.durationMs.coerceAtLeast(1).toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = Cream,
                        activeTrackColor = Cream,
                        inactiveTrackColor = SurfaceSelected
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatDuration(positionMs), color = MutedCream)
                    Text(formatDuration(state.durationMs), color = MutedCream)
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = player::toggleShuffle) {
                        Icon(
                            imageVector = Icons.Outlined.Shuffle,
                            contentDescription = "Karıştır",
                            tint = if (state.shuffle) Cream else MutedCream
                        )
                    }
                    IconButton(onClick = player::previous, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Outlined.SkipPrevious, "Önceki", Modifier.size(36.dp), tint = Cream)
                    }
                    FilledIconButton(
                        onClick = player::togglePlay,
                        modifier = Modifier.size(72.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Cream,
                            contentColor = Night
                        )
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = "Çal/Duraklat",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    IconButton(onClick = player::next, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Outlined.SkipNext, "Sonraki", Modifier.size(36.dp), tint = Cream)
                    }
                    IconButton(onClick = player::cycleRepeat) {
                        Icon(
                            imageVector = when (state.repeat) {
                                RepeatSetting.ONE -> Icons.Outlined.RepeatOne
                                else -> Icons.Outlined.Repeat
                            },
                            contentDescription = "Tekrar: ${state.repeat}",
                            tint = if (state.repeat == RepeatSetting.OFF) MutedCream else Cream
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                IconButton(onClick = player::toggleFavorite) {
                    Icon(
                        imageVector = if (state.song?.isFavorite == true) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favori",
                        tint = if (state.song?.isFavorite == true) Cream else MutedCream
                    )
                }
            }
        }
    }

    if (showTimer) {
        SleepTimerDialog(
            state = state.sleepTimer,
            start = player::startSleepTimer,
            stopAfterTrack = player::stopAfterCurrentTrack,
            cancel = player::cancelSleepTimer,
            dismiss = { showTimer = false }
        )
    }

    if (showQueue) {
        QueueSheet(queue = state.queue, dismiss = { showQueue = false })
    }
}

@Composable
private fun SleepTimerDialog(
    state: SleepTimerState,
    start: (Int) -> Unit,
    stopAfterTrack: () -> Unit,
    cancel: () -> Unit,
    dismiss: () -> Unit
) = AlertDialog(
    onDismissRequest = dismiss,
    title = { Text("Uyku Zamanlayıcısı") },
    text = {
        Column {
            Text(
                text = when (state) {
                    SleepTimerState.Inactive -> "Oynatmayı ne zaman durduralım?"
                    is SleepTimerState.Until -> "Kapanış zamanı: ${java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(state.endsAtEpochMs))}"
                    SleepTimerState.EndOfCurrentTrack -> "Bu şarkı bittiğinde müzik nazikçe duracak."
                },
                color = MutedCream
            )
            Spacer(Modifier.height(16.dp))
            listOf(10, 15, 30, 45, 60).chunked(3).forEach { row ->
                Row {
                    row.forEach { min ->
                        TextButton(onClick = { start(min); dismiss() }) {
                            Text("$min dk")
                        }
                    }
                }
            }
            TextButton(onClick = { stopAfterTrack(); dismiss() }) {
                Text("Şarkı Bitiminde Dur")
            }
        }
    },
    confirmButton = {
        if (state !is SleepTimerState.Inactive) {
            TextButton(onClick = { cancel(); dismiss() }) { Text("İptal Et") }
        }
    },
    dismissButton = {
        TextButton(onClick = dismiss) { Text("Kapat") }
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSheet(queue: List<QueueItem>, dismiss: () -> Unit) = ModalBottomSheet(
    onDismissRequest = dismiss,
    containerColor = Surface
) {
    Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
        Text("Çalma Kuyruğu", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        if (queue.isEmpty()) {
            Text("Kuyruk boş.", color = MutedCream, modifier = Modifier.padding(vertical = 20.dp))
        } else {
            LazyColumn {
                itemsIndexed(queue) { idx, itm ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text("${idx + 1}. ${itm.title}", style = MaterialTheme.typography.titleMedium)
                        Text(itm.artist, color = MutedCream, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    reduced: Boolean,
    onReducedMotion: (Boolean) -> Unit,
    onBack: () -> Unit,
    onRescan: () -> Unit
) = Scaffold(
    containerColor = Night,
    topBar = {
        TopAppBar(
            title = { Text("Ayarlar") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, "Geri", tint = Cream)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Night,
                titleContentColor = Cream
            )
        )
    }
) { pad ->
    Column(Modifier.padding(pad).padding(24.dp)) {
        Text("Görünüm", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Animasyonları Azalt", Modifier.weight(1f))
            Switch(
                checked = reduced,
                onCheckedChange = onReducedMotion,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Cream,
                    checkedTrackColor = SurfaceSelected
                )
            )
        }

        HorizontalDivider(thickness = Dimens.DividerThickness, color = DividerColor)
        Spacer(Modifier.height(16.dp))

        Text("Kütüphane", style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onRescan) {
            Text("Kütüphaneyi Yeniden Tara")
        }

        HorizontalDivider(thickness = Dimens.DividerThickness, color = DividerColor)
        Spacer(Modifier.height(16.dp))

        Text("Hakkında", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "RLS Player v1.0.0\nTamamen yerel, offline-first müzik deneyimi.\nInter yazı tipi SIL Open Font License ile lisanslanmıştır.",
            color = MutedCream
        )
    }
}

private fun tabIcon(tab: Tab) = when (tab) {
    Tab.SONGS -> Icons.Outlined.MusicNote
    Tab.ALBUMS -> Icons.Outlined.Album
    Tab.ARTISTS -> Icons.Outlined.Person
    Tab.FOLDERS -> Icons.Outlined.Folder
    Tab.COLLECTION -> Icons.Outlined.LibraryMusic
}
