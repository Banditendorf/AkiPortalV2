package com.example.akiportal.ui.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.akiportal.ui.theme.RedPrimary

// RedTopBar ölçüleri - ayarlanabilir değişkenler
private val RedTopBarHeight = 56.dp               // 🛠️ Top bar içeriğinin yüksekliği
private val RedTopBarHorizontalPadding = 6.dp     // 🛠️ Sağ-sol boşluk
private val RedTopBarTitleFontSize = 20.sp        // 🛠️ Başlık yazı boyutu
private val RedTopBarTitleFontWeight = FontWeight.Bold // 🛠️ Başlık yazı kalınlığı
private val RedTopBarTitlePaddingStart = 8.dp     // 🛠️ Başlık başından boşluk
private val RedTopBarIconTint = Color.White       // 🛠️ Icon rengi
private val RedTopBarBackgroundColor = RedPrimary // 🛠️ Arka plan rengi
private val RedTopBarIconSize = 24.dp             // 🛠️ Icon boyutu
private val RedTopBarDropDownWidth = 180.dp       // 🛠️ Dropdown Menü genişliği

/**
 * RedTopBar with optional left back button, title, dropdown menu, leading and trailing images.
 *
 * @param title Başlık metni
 * @param showMenu Menü ikonunu gösterir
 * @param showBackButton Geri ikonunu gösterir
 * @param onBackClick Geri tuşuna tıklama callback
 * @param leadingImage Opsiyonel: Sol tarafa eklenecek Painter
 * @param leadingImageDescription Erişilebilirlik açıklaması (sol)
 * @param leadingImageSize Sol görsel boyutu
 * @param trailingImage Opsiyonel: Sağ tarafa eklenecek Painter
 * @param trailingImageDescription Erişilebilirlik açıklaması (sağ)
 * @param trailingImageSize Sağ görsel boyutu
 * @param menuContent Menü içeriği (DropdownMenuItem lambdaları)
 */
@Composable
fun RedTopBar(
    title: String,
    showMenu: Boolean = false,
    showBackButton: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    leadingImage: Painter? = null,
    leadingImageDescription: String? = null,
    leadingImageSize: Dp = RedTopBarIconSize,
    trailingImage: Painter? = null,
    trailingImageDescription: String? = null,
    trailingImageSize: Dp = RedTopBarIconSize,
    menuContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(RedTopBarBackgroundColor)
            .statusBarsPadding()
            .height(RedTopBarHeight)
            .padding(horizontal = RedTopBarHorizontalPadding)
    ) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Sol alan: geri butonu, opsiyonel resim ve başlık
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showBackButton && onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = RedTopBarIconTint,
                            modifier = Modifier.size(RedTopBarIconSize)
                        )
                    }
                }
                leadingImage?.let { painter ->
                    Image(
                        painter = painter,
                        contentDescription = leadingImageDescription,
                        modifier = Modifier
                            .size(leadingImageSize)
                            .padding(start = RedTopBarTitlePaddingStart)
                    )
                }
                Text(
                    text = title,
                    color = RedTopBarIconTint,
                    fontWeight = RedTopBarTitleFontWeight,
                    fontSize = RedTopBarTitleFontSize,
                    modifier = Modifier.padding(start = RedTopBarTitlePaddingStart)
                )
            }

            // Sağ alan: opsiyonel resim, dropdown menü
            Row(verticalAlignment = Alignment.CenterVertically) {
                trailingImage?.let { painter ->
                    Image(
                        painter = painter,
                        contentDescription = trailingImageDescription,
                        modifier = Modifier
                            .size(trailingImageSize)
                            .padding(end = RedTopBarTitlePaddingStart)
                    )
                }
                if (showMenu && menuContent != null) {
                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "Menü",
                                tint = RedTopBarIconTint,
                                modifier = Modifier.size(RedTopBarIconSize)
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.width(RedTopBarDropDownWidth)
                        ) {
                            menuContent()
                        }
                    }
                }
            }
        }
    }
}
