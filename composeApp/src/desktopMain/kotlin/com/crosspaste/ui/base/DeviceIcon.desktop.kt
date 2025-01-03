package com.crosspaste.ui.base

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.crosspaste.composeapp.generated.resources.Res
import com.crosspaste.composeapp.generated.resources.allow_receive
import com.crosspaste.composeapp.generated.resources.allow_send
import com.crosspaste.composeapp.generated.resources.block
import com.crosspaste.composeapp.generated.resources.devices
import com.crosspaste.composeapp.generated.resources.qr_code
import com.crosspaste.composeapp.generated.resources.sync
import com.crosspaste.composeapp.generated.resources.token
import com.crosspaste.composeapp.generated.resources.unverified
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun allowReceive(): Painter {
    return painterResource(Res.drawable.allow_receive)
}

@Composable
actual fun allowSend(): Painter {
    return painterResource(Res.drawable.allow_send)
}

@Composable
actual fun block(): Painter {
    return painterResource(Res.drawable.block)
}

@Composable
actual fun devices(): Painter {
    return painterResource(Res.drawable.devices)
}

@Composable
actual fun qrCode(): Painter {
    return painterResource(Res.drawable.qr_code)
}

@Composable
actual fun sync(): Painter {
    return painterResource(Res.drawable.sync)
}

@Composable
actual fun token(): Painter {
    return painterResource(Res.drawable.token)
}

@Composable
actual fun unverified(): Painter {
    return painterResource(Res.drawable.unverified)
}
