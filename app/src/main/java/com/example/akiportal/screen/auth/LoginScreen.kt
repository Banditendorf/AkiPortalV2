package com.example.akiportal.screen.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.akiportal.ui.theme.*
import com.example.akiportal.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    // State'ler
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var animateCard by remember { mutableStateOf(false) }

    // Giriş başarılıysa animasyon ile çıkış
    val currentUser by authViewModel.currentUser.collectAsState()
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            animateCard = false
            delay(350)
            onLoginSuccess(email)
        }
    }

    // Card giriş animasyonu
    LaunchedEffect(Unit) {
        delay(250)
        animateCard = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(BackgroundDark, SurfaceDark, RedPrimary.copy(alpha = 0.3f))
                )
            )
    ) {
        // Arka Plan Fancy Gradient Layer
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            RedPrimary.copy(alpha = 0.7f),
                            BackgroundDark,
                            BluePrimary.copy(alpha = 0.5f),
                            White.copy(alpha = 0.02f)
                        ),
                        start = Offset.Zero,
                        end = Offset(600f, 1800f)
                    )
                )
        )

        // Giriş Card Animasyonlu
        AnimatedVisibility(
            visible = animateCard,
            enter = fadeIn(tween(600)),
            exit = fadeOut(tween(350))
        ) {
            Box(
                Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .fillMaxWidth(0.92f)
                        .shadow(18.dp, RoundedCornerShape(32.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardDark)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(32.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Makina Takip Sistemi",
                            fontSize = 28.sp,
                            color = RedPrimary,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = "Teknik Ekip Girişi",
                            color = SoftBlue,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // E-Posta Alanı
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Kurumsal E-posta", color = LightGray) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.MailOutline,
                                    contentDescription = null,
                                    tint = RedPrimary
                                )
                            },
                            colors = customOutlinedTextFieldColors(),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Şifre Alanı
                        OutlinedTextField(
                            value = password,
                            onValueChange = { if (it.length <= 6) password = it },
                            label = { Text("Şifre (6 haneli)", color = LightGray) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = RedPrimary
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = Gray
                                    )
                                }
                            },
                            colors = customOutlinedTextFieldColors(),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = RedPrimary,
                                    uncheckedColor = BorderGray
                                )
                            )
                            Text(
                                "Beni Hatırla",
                                color = LightGray,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Giriş Butonu
                        Button(
                            onClick = {
                                if (email.endsWith("@akiendüstri.com") && password.length == 6)
                                    authViewModel.login(email, password)
                                else
                                    errorMessage = "Lütfen kurumsal e-posta ve 6 haneli şifre girin."
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                        ) {
                            Text("Giriş Yap", color = White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        }

                        // Hata mesajı
                        errorMessage?.let {
                            Text(
                                text = it,
                                color = Orange,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Renkli Outline TextField helper
@Composable
fun customOutlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = RedPrimary,
    unfocusedBorderColor = BorderGray,
    cursorColor = RedPrimary,
    focusedLabelColor = RedPrimary,
    unfocusedLabelColor = LightGray,
    focusedTextColor = White,
    unfocusedTextColor = White,
    disabledTextColor = BorderGray,
    errorTextColor = Orange
)
