package com.example.act_3


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.example.act_3.storage.CloudStorageManager
import com.example.act_3.ui.theme.ACT_3Theme
import com.example.act_3.ui.theme.jerseyRegularAppFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Objects

class MainActivity : ComponentActivity() {

    private val recorder by lazy {
        AndroidAudioRecorder(applicationContext)
    }

    private val player by lazy {
        AndroidAudioPlayer(applicationContext)
    }

    private var audioFile: File? = null

    private val storage by lazy {
        CloudStorageManager(applicationContext)
    }


    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            0
        )
        setContent {

            ACT_3Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    /**  Strings from @param strings */

                    val appName: String = stringResource(id = R.string.app_name)
                    val timeString: String = stringResource(id = R.string.time_format)
                    val history: String = stringResource(id = R.string.history)
                    val fileName: String = stringResource(id = R.string.alert_dialog_name)
                    val audioFilter: String = stringResource(id = R.string.filter_audio)
                    val aboutTheApp: String = stringResource(id = R.string.about_app)
                    val bugAdvice: String = stringResource(id = R.string.bug_advice)
                    val saveFirebaseText: String = stringResource(R.string.save_fb)
                    val saveStorageText: String = stringResource(R.string.save_storage)
                    val filterAdviceText: String = stringResource(R.string.filter_advice)
                    val confirmText: String = stringResource(id = R.string.confirm)
                    val cancelText: String = stringResource(id = R.string.cancel)
                    val durationText: String = stringResource(id = R.string.duration)
                    val playText: String = stringResource(id = R.string.play)

                    /**  File Information */
                    val metadataExtractor = AudioMetadataExtractor()
                    val maxDuration = 180100


                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                    var refreshRequested by remember { mutableStateOf(false) }

                    /**  Icons from @param drawer */
                    val micDefaultIcon: Painter = painterResource(id = R.drawable.ic_mic_high)
                    val playIcon: Painter = painterResource(id = R.drawable.ic_play)
                    val pauseIcon: Painter = painterResource(id = R.drawable.ic_pause)
                    val stopIcon: Painter = painterResource(id = R.drawable.ic_stop_circle)
                    val menuIcon: Painter = painterResource(id = R.drawable.ic_menu)
                    val infoIcon: Painter = painterResource(id = R.drawable.ic_info)
                    val reloadIcon: Painter = painterResource(id = R.drawable.ic_reload)
                    val closeIcon: Painter = painterResource(id = R.drawable.ic_close)
                    val iconPainter: Painter =
                        painterResource(id = R.drawable.logo_the_best_recorder)

                    /**  States of Media Recorder */
                    var remainingTime by remember { mutableIntStateOf(maxDuration) }
                    var isRecording by remember { mutableStateOf(false) }
                    var isRecorded by remember { mutableStateOf(false) }

                    /**  States of Media Player */
                    var isPlaying by remember { mutableStateOf(false) }

                    /** States of AlertDialog*/
                    var alertRecordedAudio by remember { mutableStateOf(false) }
                    var alertInformation by remember { mutableStateOf(false) }
                    var isError by remember { mutableStateOf(false) }
                    var downloadFireBase by remember { mutableStateOf(false) }
                    var downloadStorageMedia by remember { mutableStateOf(false) }

                    /** Edit text values*/
                    var filenameInput by remember { mutableStateOf(getFileName()) }

                    /**  Fire Base storage  */
                    val scope = rememberCoroutineScope()
                    val context = LocalContext.current
                    var capturedAudioUri by remember { mutableStateOf<Uri>(Uri.EMPTY) }

                    /**  Animation */
                    val animatedIconSize by animateDpAsState(
                        if (isRecording) 132.dp else 160.dp,
                        tween(durationMillis = 1200), label = ""
                    )
                    var progress by remember { mutableFloatStateOf(0f) }
                    var totalDuration by remember {
                        mutableFloatStateOf(0f)
                    }
                    var currentPositionText by remember { mutableStateOf("00:00") }
                    var totalDurationText by remember { mutableStateOf("") }

                    /** get Strings URIs from Storage Cloud*/
                    var generatedMedia by remember { mutableStateOf<List<String>>(listOf()) }
                    LaunchedEffect(Unit) {
                        generatedMedia = storage.getAudioFiles()
                    }

                    /** list of filters*/
                    val listFilters = listOf("Normal", "SpeedUp", "Slowed")
                    var isExpanded by remember { mutableStateOf(false) }
                    var selectedFilter by remember { mutableStateOf(listFilters[0]) }


                    LaunchedEffect(isRecording) {
                        if (isRecording) {
                            remainingTime = maxDuration

                            while (remainingTime > 100) {
                                remainingTime -= 100
                                delay(100)
                            }
                            recorder.stop()
                            isRecording = recorder.isRecordingMic()
                            alertRecordedAudio = true
                            isRecorded = true
                            downloadFireBase = false
                            downloadStorageMedia = false
                            selectedFilter = listFilters[0]
                            filenameInput = getFileName()
                            progress = 0f
                            currentPositionText = "00:00"
                        }
                    }

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        history,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    IconButton(onClick = {
                                        refreshRequested = true
                                    }
                                    ) {
                                        Icon(painter = reloadIcon, contentDescription = "reload")
                                        if (refreshRequested) {
                                            LaunchedEffect(Unit) {
                                                generatedMedia = storage.getAudioFiles()
                                            }
                                            refreshRequested = false
                                        }

                                    }
                                }

                                Divider()

                                LazyColumn {
                                    items(generatedMedia.count()) { index ->
                                        val audioUrl = generatedMedia[index]
                                        val metadata =
                                            remember { mutableStateOf<AudioMetadata?>(null) }
                                        val isPlayingDialog = remember { mutableStateOf(false) }

                                        LaunchedEffect(audioUrl) {
                                            metadata.value =
                                                AudioMetadataExtractor().extractMetadataURI(
                                                    URL(audioUrl)
                                                )
                                        }

                                        metadata.value?.let { audioMetadata ->
                                            val onClick = {
                                                isPlayingDialog.value = true
                                            }

                                            NavigationDrawerItem(
                                                label = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = audioMetadata.title,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f),
                                                        )
                                                        Text(text = "${formatDuration(audioMetadata.durationMillis.toInt())} min")
                                                    }
                                                },
                                                selected = false,
                                                onClick = onClick
                                            )

                                            if (isPlayingDialog.value) {
                                                AlertDialog(
                                                    onDismissRequest = {
                                                        isPlayingDialog.value = false
                                                    },
                                                    title = {
                                                        Text(text = audioMetadata.title)
                                                    },
                                                    text = {
                                                        Text(
                                                            text = "$durationText ${
                                                                formatDuration(
                                                                    audioMetadata.durationMillis.toInt()
                                                                )
                                                            }"
                                                        )
                                                    },
                                                    confirmButton = {
                                                        Button(
                                                            onClick = {
                                                                player.playUrl(URL(audioUrl))
                                                            }
                                                        ) {
                                                            Text(text = playText)
                                                        }
                                                    },
                                                    dismissButton = {
                                                        Button(
                                                            onClick = {
                                                                player.stop()
                                                                isPlayingDialog.value = false
                                                            }
                                                        ) {
                                                            Text(text = cancelText)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }


                            }
                        }
                    )

                    {
                        Scaffold(topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = appName,
                                        fontFamily = jerseyRegularAppFont,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 30.sp,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                },

                                navigationIcon = {
                                    IconButton(onClick = {
                                        scope.launch {
                                            if (drawerState.isClosed) {
                                                drawerState.open()
                                            } else {
                                                drawerState.close()
                                            }
                                        }
                                    }) {
                                        Icon(
                                            painter = menuIcon, contentDescription = "menu"
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { alertInformation = true }) {
                                        Icon(painter = infoIcon, contentDescription = "information")
                                    }
                                }


                            )
                            if (alertInformation) {
                                AlertDialog(
                                    onDismissRequest = { alertInformation = false },
                                    confirmButton = { },
                                    text = {
                                        Column(
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .fillMaxWidth(),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            IconButton(onClick = { alertInformation = false }) {
                                                Icon(
                                                    painter = closeIcon,
                                                    contentDescription = "Close",
                                                    modifier = Modifier
                                                        .align(Alignment.Start)
                                                )
                                            }


                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Image(
                                                    painter = iconPainter,
                                                    contentDescription = "Icon",
                                                    modifier = Modifier.size(128.dp)
                                                )

                                                Text(
                                                    text = appName,
                                                    fontWeight = FontWeight.Normal,
                                                    fontFamily = jerseyRegularAppFont,
                                                    fontSize = 22.sp,
                                                )
                                                Text(
                                                    text = "${stringResource(id = R.string.version)} 1.0.0",
                                                )
                                                Text(
                                                    text = aboutTheApp,
                                                    textAlign = TextAlign.Justify
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Row {
                                                    Icon(
                                                        painter = infoIcon,
                                                        contentDescription = null
                                                    )
                                                    Spacer(modifier = Modifier.width(16.dp))
                                                    Text(
                                                        text = bugAdvice,
                                                        fontFamily = FontFamily.Cursive
                                                    )
                                                }

                                            }
                                        }
                                    }
                                )
                            }
                        }
                        )
                        { it ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceAround,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(
                                        top = it.calculateTopPadding(),
                                        bottom = it.calculateBottomPadding()
                                    ),
                            ) {
                                Spacer(
                                    modifier = Modifier
                                        .height(32.dp)
                                        .fillMaxWidth()
                                )
                                IconButton(
                                    onClick = {
                                        if (isRecording) {
                                            recorder.stop()
                                            isRecording = recorder.isRecordingMic()
                                            alertRecordedAudio = true
                                            isRecorded = true
                                        } else {
                                            player.stop()
                                            isPlaying = false
                                            isRecorded = false
                                            audioFile = createAudioFile(filenameInput).also {
                                                recorder.start(it)
                                                isRecording = recorder.isRecordingMic()
                                            }


                                        }

                                    }, modifier = Modifier.size(animatedIconSize)
                                ) {


                                    Icon(
                                        painter = if (isRecording) stopIcon else micDefaultIcon,
                                        contentDescription = null,
                                        tint = if (isRecording) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .then(
                                                if (isRecording) {
                                                    Modifier
                                                        .fillMaxSize()
                                                        .clip(RoundedCornerShape(32.dp))
                                                        .background(color = MaterialTheme.colorScheme.tertiaryContainer)
                                                        .padding(32.dp)
                                                } else {
                                                    Modifier
                                                        .padding(32.dp)
                                                        .fillMaxSize()
                                                        .clip(RoundedCornerShape(32.dp))
                                                }
                                            )
                                    )
                                    isRecording = recorder.isRecordingMic()

                                }


                                if (isRecorded) {
                                    if (alertRecordedAudio)
                                        AlertDialog(
                                            onDismissRequest = {
                                                downloadFireBase = false
                                                downloadStorageMedia = false
                                                selectedFilter = listFilters[0]
                                                alertRecordedAudio = false
                                                filenameInput = getFileName()
                                                progress = 0f
                                                currentPositionText = "00:00"
                                                player.stop()
                                            },
                                            text = {
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.Top,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = fileName,
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    val allowedCharactersRegex =
                                                        Regex("^[a-zA-Z0-9_-]*\$")

                                                    TextField(
                                                        value = filenameInput,
                                                        onValueChange = { newValue ->
                                                            if (newValue.isEmpty() || !allowedCharactersRegex.matches(newValue)) {
                                                                filenameInput = newValue
                                                                isError = true
                                                            } else {
                                                                filenameInput = newValue
                                                                isError = false
                                                            }
                                                        },
                                                        supportingText = {
                                                            if (isError)
                                                                Text(text = stringResource(id = R.string.advice_file_name))
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        singleLine = true,
                                                        maxLines = 1,
                                                        isError = isError,
                                                        trailingIcon = {
                                                            Icon(
                                                                imageVector = Icons.Outlined.Create,
                                                                contentDescription = null
                                                            )
                                                        }
                                                    )

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center,

                                                        ) {
                                                        IconButton(
                                                            onClick = {
                                                                isPlaying = !isPlaying
                                                                if (isPlaying) {
                                                                    player.playFile(
                                                                        audioFile
                                                                            ?: return@IconButton,
                                                                        selectedFilter
                                                                    )
                                                                } else {
                                                                    player.stop()
                                                                }
                                                            },
                                                            enabled = !isRecording
                                                        ) {
                                                            Icon(
                                                                painter = if (isPlaying) pauseIcon else playIcon,
                                                                contentDescription = null
                                                            )
                                                        }
                                                        val metadata =
                                                            metadataExtractor.extractMetadata(
                                                                audioFile!!
                                                            )


                                                        totalDuration =
                                                            metadata.durationMillis.toFloat()


                                                        val speedText: Float =
                                                            when (selectedFilter) {
                                                                "Normal" -> 1f
                                                                "Slowed" -> 2f
                                                                "SpeedUp" -> 0.5f
                                                                else -> 1f
                                                            }
                                                        totalDurationText =
                                                            formatDuration((totalDuration * speedText).toInt())

                                                        LaunchedEffect(isPlaying) {

                                                            while (isPlaying) {
                                                                val currentPosition =
                                                                    player.getCurrentPosition()
                                                                        .toFloat()
                                                                currentPositionText =
                                                                    formatDuration((currentPosition * speedText).toInt())
                                                                val currentProgress =
                                                                    (currentPosition / totalDuration) * 100f
                                                                progress = currentProgress
                                                                delay(100L)

                                                            }
                                                        }


                                                        Slider(
                                                            value = progress,
                                                            onValueChange = { newProgress ->
                                                                progress = newProgress
                                                                val seekPosition =
                                                                    (newProgress / 100f) * totalDuration
                                                                player.seekTo(seekPosition.toLong())

                                                            },
                                                            valueRange = 0f..100f,
                                                            steps = 100,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )


                                                    }
                                                    Spacer(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(32.dp)
                                                    )

                                                    Text(
                                                        text = "$currentPositionText / $totalDurationText",
                                                        modifier = Modifier.fillMaxWidth(),
                                                        textAlign = TextAlign.Center
                                                    )
                                                    Spacer(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(32.dp)
                                                    )
                                                    if (selectedFilter == listFilters[0]) {
                                                        Row(
                                                            modifier = Modifier
                                                                .padding(bottom = 32.dp)
                                                                .fillMaxWidth()
                                                                .size(48.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(text = saveFirebaseText)
                                                            Switch(
                                                                checked = downloadFireBase,
                                                                onCheckedChange = {
                                                                    downloadFireBase = it
                                                                },
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .wrapContentWidth(Alignment.End)
                                                            )
                                                        }
                                                        Row(
                                                            modifier = Modifier
                                                                .padding(bottom = 32.dp)
                                                                .fillMaxWidth()
                                                                .size(48.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(text = saveStorageText)
                                                            Switch(
                                                                checked = downloadStorageMedia,
                                                                onCheckedChange = {
                                                                    downloadStorageMedia = it
                                                                },
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .wrapContentWidth(Alignment.End)
                                                            )

                                                        }
                                                    }
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(bottom = 16.dp),
                                                        verticalAlignment = Alignment.Top,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = audioFilter,
                                                        )
                                                    }
                                                    ExposedDropdownMenuBox(
                                                        expanded = isExpanded,
                                                        onExpandedChange = {
                                                            isExpanded = !isExpanded
                                                        }
                                                    ) {
                                                        TextField(
                                                            modifier = Modifier
                                                                .menuAnchor()
                                                                .fillMaxWidth(),
                                                            value = selectedFilter,
                                                            onValueChange = {},
                                                            readOnly = true,
                                                            trailingIcon = {
                                                                ExposedDropdownMenuDefaults.TrailingIcon(
                                                                    expanded = isExpanded
                                                                )
                                                            }
                                                        )
                                                        ExposedDropdownMenu(
                                                            expanded = isExpanded,
                                                            onDismissRequest = {
                                                                isExpanded = false
                                                            }) {
                                                            listFilters.forEachIndexed { index, text ->
                                                                DropdownMenuItem(
                                                                    text = { Text(text = text) },
                                                                    onClick = {
                                                                        selectedFilter =
                                                                            listFilters[index]
                                                                        isExpanded = false
                                                                    },
                                                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                                                )
                                                            }
                                                        }

                                                    }

                                                    if (selectedFilter != listFilters[0]) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(top = 16.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Icon(
                                                                painter = infoIcon,
                                                                contentDescription = null
                                                            )
                                                            Text(
                                                                text = filterAdviceText,
                                                                textAlign = TextAlign.Center
                                                            )
                                                        }
                                                        downloadFireBase = false
                                                        downloadStorageMedia = false
                                                    }
                                                }
                                            },
                                            confirmButton = {
                                                if (selectedFilter == listFilters[0]) {
                                                    Button(
                                                        onClick = {

                                                            val fileNameFinal =
                                                                ("$filenameInput.mp3")
                                                            val uri = FileProvider.getUriForFile(
                                                                Objects.requireNonNull(context),
                                                                "com.example.act_3" + ".provider",
                                                                audioFile!!
                                                            )
                                                            if (downloadFireBase) {
                                                                capturedAudioUri = uri
                                                                capturedAudioUri.let { capturedUri ->
                                                                    scope.launch {
                                                                        storage.uploadFile(
                                                                            fileNameFinal,
                                                                            capturedUri
                                                                        )

                                                                    }
                                                                }
                                                            }


                                                            if (downloadStorageMedia)
                                                                downloadAudio(
                                                                    context,
                                                                    fileNameFinal
                                                                )

                                                            downloadFireBase = false
                                                            downloadStorageMedia = false
                                                            selectedFilter = listFilters[0]
                                                            alertRecordedAudio = false
                                                            filenameInput = getFileName()
                                                            progress = 0f
                                                            currentPositionText = "00:00"
                                                            player.stop()
                                                        },
                                                        enabled = !isError
                                                    ) {
                                                        Text(text = confirmText)
                                                    }
                                                } else {
                                                    Button(onClick = {
                                                        downloadFireBase = false
                                                        downloadStorageMedia = false
                                                        selectedFilter = listFilters[0]
                                                        alertRecordedAudio = false
                                                        filenameInput = getFileName()
                                                        progress = 0f
                                                        currentPositionText = "00:00"
                                                        player.stop()

                                                    }
                                                    ) {
                                                        Text("OK")
                                                    }

                                                }

                                            },
                                            dismissButton = {
                                                if (selectedFilter == listFilters[0]) {
                                                    Button(
                                                        onClick = {
                                                            downloadFireBase = false
                                                            downloadStorageMedia = false
                                                            selectedFilter = listFilters[0]
                                                            alertRecordedAudio = false
                                                            filenameInput = getFileName()
                                                            progress = 0f
                                                            currentPositionText = "00:00"
                                                            player.stop()
                                                        }
                                                    ) {
                                                        Text(text = cancelText)
                                                    }
                                                }
                                            }
                                        )


                                } else
                                    Text(
                                        text = timeString + " " + formatDuration(remainingTime),
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 18.sp
                                    )
                                Spacer(
                                    modifier = Modifier
                                        .height(32.dp)
                                        .fillMaxWidth()
                                )
                                Spacer(
                                    modifier = Modifier
                                        .height(32.dp)
                                        .fillMaxWidth()
                                )

                            }


                        }
                    }


                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun downloadAudio(context: Context, audioFileName: String) {
        val permission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
                1
            )
        } else {
            saveAudioToStorage(audioFileName, context)
        }
    }

    private fun saveAudioToStorage(audioFileName: String, context: Context) {

        val audioFileDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val destFile = File(audioFileDir, audioFileName)

        try {
            audioFile?.inputStream()?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            Toast.makeText(this, context.getString(R.string.toast_successfully), Toast.LENGTH_SHORT)
                .show()
        } catch (e: IOException) {
            Toast.makeText(this, context.getString(R.string.toast_error), Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun formatDuration(duration: Int): String {
        val minutes = (duration / 1000) / 60
        val seconds = (duration / 1000) % 60
        return "%02d:%02d".format(minutes, seconds)
    }
}

fun Context.createAudioFile(audioFileName: String): File {
    return File.createTempFile(
        audioFileName,
        ".mp3",
        externalCacheDir
    )
}


@SuppressLint("SimpleDateFormat")
fun getFileName(): String {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm_ss").format(Date())
    return "AUDIO_" + timeStamp + "_"

}


interface AudioRecorder {
    fun start(outputFile: File)
    fun stop()

}

class AndroidAudioRecorder(private val context: Context) : AudioRecorder {

    private var recorder: MediaRecorder? = null
    private var isRecording: Boolean = false


    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }


    override fun start(outputFile: File) {
        createRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(FileOutputStream(outputFile).fd)
            prepare()
            start()
            isRecording = true
            recorder = this


        }
    }

    override fun stop() {
        recorder?.stop()
        recorder?.reset()
        recorder = null
        isRecording = false

    }


    fun isRecordingMic(): Boolean {
        return isRecording
    }

}


interface AudioPlayer {
    fun playFile(file: File, pseudoFilter: String)
    fun stop()

    fun playUrl(url: URL)
    fun seekTo(positionMillis: Long)
}

class AndroidAudioPlayer(private val context: Context) : AudioPlayer {

    private var player: MediaPlayer? = null

    override fun playFile(file: File, pseudoFilter: String) {
        val speed: Float = when (pseudoFilter) {
            "Normal" -> 1f
            "SpeedUp" -> 2f
            "Slowed" -> 0.5f
            else -> 1f
        }
        player?.stop()
        player = MediaPlayer.create(context, file.toUri())
        player?.playbackParams =
            player?.playbackParams?.setSpeed(speed)!!

        player?.start()


    }


    override fun stop() {
        player?.stop()
        player?.release()
        player = null
    }

    override fun playUrl(url: URL) {
        player?.stop()
        player = MediaPlayer()
        player?.apply {
            setDataSource(url.toString())
            prepareAsync()
            setOnPreparedListener {
                start()
            }
            setOnErrorListener { _, _, _ ->
                false
            }
            setOnCompletionListener {
                stop()
            }
        }
    }


    override fun seekTo(positionMillis: Long) {
        player?.seekTo(positionMillis.toInt())
    }

    fun getCurrentPosition(): Int {
        return player?.currentPosition ?: 0
    }


}


class AudioMetadataExtractor {

    fun extractMetadata(file: File): AudioMetadata {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(file.absolutePath)

        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

        val duration = durationStr?.toLong() ?: 0

        retriever.release()

        return AudioMetadata(title ?: "", duration)
    }

    fun extractMetadataURI(url: URL): AudioMetadata? {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(url.toString(), hashMapOf<String, String>())
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
            val durationStr =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

            val duration = durationStr?.toLong() ?: 0

            retriever.release()

            return AudioMetadata(title ?: extractFileNameFromUrl(url.toString()), duration)
        } catch (e: Exception) {
            // Handle exception if unable to extract metadata
            e.printStackTrace()
        } finally {
            retriever.release()
        }
        return null
    }

    private fun extractFileNameFromUrl(url: String): String {
        val startIndex = url.lastIndexOf("/") + 9
        val endIndex = url.indexOf("?alt=media")
        if (startIndex < 0 || endIndex < 0 || startIndex >= endIndex) {
            return ""
        }
        return url.substring(startIndex, endIndex)
    }


}

data class AudioMetadata(val title: String, val durationMillis: Long)

