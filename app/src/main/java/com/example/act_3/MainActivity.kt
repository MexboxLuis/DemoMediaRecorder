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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.ImeAction
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

                    /**  File Information */
                    val metadataExtractor = AudioMetadataExtractor()
                    val maxDuration = 60200
                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

                    /**  Icons from @param drawer */
                    val micDefaultIcon: Painter = painterResource(id = R.drawable.ic_mic_high)
                    val playIcon: Painter = painterResource(id = R.drawable.ic_play)
                    val pauseIcon: Painter = painterResource(id = R.drawable.ic_pause)
                    val stopIcon: Painter = painterResource(id = R.drawable.ic_stop_circle)
                    val downloadIcon: Painter = painterResource(id = R.drawable.ic_download)
                    val menuIcon: Painter = painterResource(id = R.drawable.ic_menu)
                    val infoIcon: Painter = painterResource(id = R.drawable.ic_info)
                    val reloadIcon: Painter = painterResource(id = R.drawable.ic_reload)

                    /**  States of Media Recorder */
                    var remainingTime by remember { mutableIntStateOf(maxDuration) }
                    var isRecording by remember { mutableStateOf(false) }
                    var isRecorded by remember { mutableStateOf(false) }

                    /**  States of Media Player */
                    var isPlaying by remember { mutableStateOf(false) }
                    var audioSlider by remember { mutableIntStateOf(0) }

                    /** States of AlertDialog*/
                    var alertRecordedAudio by remember { mutableStateOf(false) }
                    var alertInformation by remember { mutableStateOf(false) }
                    var isError by remember { mutableStateOf(false) }
                    var downloadFireBase by remember { mutableStateOf(true) }
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
                        tween(durationMillis = 1000), label = ""
                    )

                    /** get Strings URIs from Storage Cloud*/
                    var generatedMedia by remember { mutableStateOf<List<String>>(listOf()) }
                    LaunchedEffect(Unit) {
                        generatedMedia = storage.getAudioFiles()
                    }

                    /** list of filters*/
//                    val listFilters = listOf("Normal", "SpeedUp", "Slowed")
//                    var isExpanded by remember {(mutableStateOf(false)}


                    LaunchedEffect(isRecording) {
                        if (isRecording) {
                            remainingTime = maxDuration
                            val endTime = System.currentTimeMillis() + maxDuration
                            while (remainingTime > 200) {
                                delay(1000)
                                remainingTime = (endTime - System.currentTimeMillis()).toInt()
                            }
                            recorder.stop()
                            isRecording = recorder.isRecordingMic()
                            alertRecordedAudio = true
                            isRecorded = true
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
                                    IconButton(onClick = { }) {
                                        Icon(painter = reloadIcon, contentDescription = "reload")
                                    }
                                }



                                Divider()
                                LazyColumn {
                                    items(generatedMedia.count()) { audioUrl ->
                                        val metadata =
                                            remember { mutableStateOf<AudioMetadata?>(null) }
                                        LaunchedEffect(audioUrl) {
                                            metadata.value =
                                                AudioMetadataExtractor().extractMetadataURI(
                                                    URL(generatedMedia[audioUrl])
                                                )
                                        }

                                        metadata.value?.let { audioMetadata ->
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
                                                onClick = {}
                                            )
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
                                    IconButton(onClick = { /*TODO*/ }) {
                                        Icon(painter = infoIcon, contentDescription = "information")
                                    }
                                }


                            )
                        }
                        )
                        {
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
                                                    TextField(
                                                        value = filenameInput,
                                                        onValueChange = {
                                                            filenameInput = it
                                                            isError = if (it.isEmpty())
                                                                true
                                                            else
                                                                false
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        singleLine = true,
                                                        isError = isError,
                                                        keyboardOptions = KeyboardOptions.Default.copy(
                                                            imeAction = ImeAction.Done
                                                        )
                                                    )
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center,

                                                        ) {
                                                        IconButton(
                                                            onClick = {
                                                                isPlaying = !isPlaying
                                                                if (isPlaying) {
//                                            val url = URL("https://firebasestorage.googleapis.com/v0/b/demoaudiorecorder.appspot.com/o/audio%2Fecstasy.mp3?alt=media&token=da81b865-47ab-40bf-b8fa-7a57d698844a")
//                                            player.playUrl(url)
                                                                    player.playFile(
                                                                        audioFile
                                                                            ?: return@IconButton
                                                                    )

                                                                    val metadata =
                                                                        metadataExtractor.extractMetadata(
                                                                            audioFile!!
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
                                                        LinearProgressIndicator(
                                                            progress = 1f,
                                                            modifier = Modifier.width(16.dp)
                                                        )

                                                        IconButton(
                                                            onClick = { downloadAudio(context) }
                                                        ) {
                                                            Icon(
                                                                painter = downloadIcon,
                                                                contentDescription = null
                                                            )
                                                        }
                                                    }
                                                    Spacer(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(32.dp)
                                                    )
                                                    Text(
                                                        text = "00:00 / 00:00",
                                                        modifier = Modifier.fillMaxWidth(),
                                                        textAlign = TextAlign.Center
                                                    )
                                                    Spacer(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(32.dp)
                                                    )
                                                    Row(
                                                        modifier = Modifier
                                                            .padding(bottom = 32.dp)
                                                            .fillMaxWidth()
                                                            .size(48.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(text = stringResource(R.string.save_fb))
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
                                                        Text(text = stringResource(R.string.save_storage))
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
//                                                    ExposedDropdownMenuBox(
//                                                        expanded = ,
//                                                        onExpandedChange =
//                                                    ) {
//
//                                                    }
                                                    println("Firebase: $downloadFireBase")
                                                    println("Storage: $downloadStorageMedia")
                                                }
                                            },
                                            confirmButton = {
                                                Button(
                                                    onClick = {

                                                        val fileNameFinal = ("$filenameInput.mp3")
                                                        val uri = FileProvider.getUriForFile(
                                                            Objects.requireNonNull(context),
                                                            "com.example.act_3" + ".provider",
                                                            audioFile!!
                                                        )

                                                        capturedAudioUri = uri
                                                        capturedAudioUri.let { capturedUri ->
                                                            scope.launch {
                                                                storage.uploadFile(
                                                                    fileNameFinal,
                                                                    capturedUri
                                                                )

                                                            }
                                                        }
                                                        alertRecordedAudio = false
                                                        filenameInput = getFileName()
                                                    }
                                                ) {
                                                    Text("Confirm")
                                                }
                                            },
                                            dismissButton = {
                                                Button(
                                                    onClick = {
                                                        alertRecordedAudio = false
                                                        filenameInput = getFileName()
                                                    }
                                                ) {
                                                    Text("Cancel")
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
    private fun downloadAudio(context: Context) {
        val permission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
                1
            )
        } else {
            val audioFileName = getFileName() + ".mp3"
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
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
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
    fun playFile(file: File)
    fun stop()
    fun playUrl(url: URL)
}

class AndroidAudioPlayer(private val context: Context) : AudioPlayer {

    private var player: MediaPlayer? = null

    override fun playFile(file: File) {
        player?.stop()
        player = MediaPlayer.create(context, file.toUri())
        player?.playbackParams =
            player?.playbackParams?.setSpeed(1f)!!

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

    fun extractFileNameFromUrl(url: String): String {
        val startIndex = url.lastIndexOf("/") + 9
        val endIndex = url.indexOf("?alt=media")
        if (startIndex < 0 || endIndex < 0 || startIndex >= endIndex) {
            return ""
        }
        return url.substring(startIndex, endIndex)
    }


}

data class AudioMetadata(val title: String, val durationMillis: Long)

