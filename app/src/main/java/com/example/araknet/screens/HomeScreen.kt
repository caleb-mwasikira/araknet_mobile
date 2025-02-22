package com.example.araknet.screens

import android.annotation.SuppressLint
import android.graphics.BlurMaskFilter
import android.util.Log
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.araknet.R
import com.example.araknet.data.HomeScreenViewModel
import com.example.araknet.data.ProxyServer
import com.example.araknet.data.ProxyStatus
import com.example.araknet.ui.theme.AraknetTheme
import com.example.araknet.ui.theme.connectingColor
import com.example.araknet.ui.theme.onlineColor
import com.example.araknet.utils.shortString
import kotlin.math.round


@Composable
fun HomeScreen(
    viewModel: HomeScreenViewModel = viewModel()
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) {  innerPadding ->

        val proxyServers by viewModel.proxyServers.collectAsState()

        if (proxyServers.isEmpty()) {
            NoItemsFound(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                errMessage="No proxy servers found"
            )
            return@Scaffold
        }

        val currentIndex by viewModel.currentIndex.collectAsState()
        val currentProxyServer = proxyServers[currentIndex!!]

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProxyServerHeader(
                currentProxyServer,
                onClickNext = {
                    viewModel.nextProxyServer()
                }
            )

            PowerButton(
                currentProxyServer,
                initialWidth = 256.dp,
                onPressedWidth = 300.dp,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BandwidthSection(
                    label = "Download",
                    icon = R.drawable.baseline_download_24,
                    speed = currentProxyServer.downloadSpeed,
                    color = connectingColor,
                )

                BandwidthSection(
                    label = "Upload",
                    icon = R.drawable.baseline_upload_24,
                    speed = currentProxyServer.uploadSpeed,
                    color = onlineColor,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PowerButton(
    currentProxyServer: ProxyServer,
    initialWidth: Dp,
    onPressedWidth: Dp,
    viewModel: HomeScreenViewModel = viewModel(),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedWidth by animateDpAsState(
        targetValue=if (isPressed) onPressedWidth else initialWidth,
        animationSpec = if (isPressed) {
            InfiniteRepeatableSpec(
                animation = tween(durationMillis = 400),
                repeatMode = RepeatMode.Reverse,
            )
        } else tween(durationMillis = 400),
        label = ""
    )

    val color = if(currentProxyServer.status is ProxyStatus.Online) {
        if (currentProxyServer.isConnected) {
            onlineColor
        } else {
            connectingColor
        }
    } else {
        currentProxyServer.status.primaryColor
    }

    val localContext = LocalContext.current

    if (isPressed) {
        // create or test existing connection
        LaunchedEffect(currentProxyServer.id) {
            val result = viewModel.testProxyConnection(currentProxyServer.id)
            val resultMsg: String = if(result) {
                "Success connecting to proxy server ${currentProxyServer.id.shortString()}"
            } else {
                "Error connecting to proxy server ${currentProxyServer.id.shortString()}"
            }

            Log.d(HomeScreenViewModel.TAG, resultMsg)
            Toast.makeText(localContext, resultMsg, Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(animatedWidth)
                .fancyShadow(
                    color = color,
                    blurRadius = animatedWidth
                )
                .clip(CircleShape)
                .background(color)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                ){},
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.baseline_power_settings_new_24),
                contentDescription = "Connect to VPN",
                modifier = Modifier
                    .size(initialWidth * 0.5f),
                colorFilter = ColorFilter.tint(
                    color= Color.White,
                )
            )
        }

    }
}

@SuppressLint("SuspiciousModifierThen")
fun Modifier.fancyShadow(
    color: Color = Color.Black,
    blurRadius: Dp = 0.dp,
) = then(
    drawBehind {
        drawIntoCanvas { canvas ->
            val paint = Paint()
            val frameworkPaint = paint.asFrameworkPaint()
            if (blurRadius != 0.dp) {
                frameworkPaint.maskFilter = (BlurMaskFilter(blurRadius.toPx(), BlurMaskFilter.Blur.NORMAL))
            }
            frameworkPaint.color = color.toArgb()

            val center = Offset(size.width/2, size.width/2)
            val radius = size.width/2

            canvas.drawCircle(
                center = center,
                radius = radius,
                paint = paint,
            )
        }
    }
)

@Composable
fun BandwidthSection(
    label: String,
    @DrawableRes icon: Int,
    speed: Float = 0f,
    color: Color = Color.Gray,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                label,
                style = MaterialTheme.typography.titleLarge,
                color = color,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Text(
            "${round(speed)} Mbps",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}


@Composable
fun ProxyServerHeader(
    proxyServer: ProxyServer,
    onClickNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color = proxyServer.status.primaryColor)
                ) {}

                Column {
                    Text(
                        proxyServer.id.shortString(),
                        style = MaterialTheme.typography.labelMedium,
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            proxyServer.country.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )

                        Text(
                            "${proxyServer.status}",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            // arrow to move to next proxy server
            IconButton(
                onClick = onClickNext
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_arrow_forward_ios_24),
                    contentDescription = "Next Proxy Server",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        PingCounter(proxyServer.ping)
    }
}

@Composable
fun PingCounter(
    ping: Int
) {
    val iconDrawableRes: Int = when {
        ping < 50 -> R.drawable.baseline_wifi_24
        ping < 100 -> R.drawable.baseline_wifi_2_bar_24
        else -> R.drawable.baseline_wifi_1_bar_24
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(iconDrawableRes),
            contentDescription = "Ping",
            modifier = Modifier
                .size(24.dp)
                .padding(4.dp)
        )
        Text("Ping $ping ms", style = MaterialTheme.typography.labelMedium)
    }
}

@Preview(showBackground=true)
@Composable
fun PreviewHomeScreen() {
    AraknetTheme {
        HomeScreen()
    }
}