package com.RIKAPLAY.zhirpem_app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val sharedPrefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val myUsername = sharedPrefs.getString("username", "") ?: ""

    var realBackupCode by remember { mutableStateOf("") }
    var isVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    
    var is2faEnabled by remember { mutableStateOf(false) }
    var totpSecret by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    
    val isAppProtectionEnabled = remember { sharedPrefs.getBoolean("use_biometric", false) }
    var isTotpVisible by remember { mutableStateOf(!isAppProtectionEnabled) }
    
    var showVerifyDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    
    val clipboardManager = LocalClipboardManager.current
    val qrBitmap = remember(totpSecret) {
        if (totpSecret.isNotEmpty()) {
            val uri = TotpUtils.getQrCodeUri(totpSecret, myUsername, "Zhirpem")
            TotpUtils.generateQrCodeBitmap(uri)
        } else null
    }

    LaunchedEffect(myUsername) {
        if (myUsername.isNotEmpty()) {
            db.collection("users").document(myUsername).get().addOnSuccessListener { doc ->
                realBackupCode = doc.getString("backupCode") ?: "Не создан"
                is2faEnabled = doc.getBoolean("is2faEnabled") ?: false
                totpSecret = doc.getString("totpSecret") ?: ""
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Безопасность", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Код восстановления",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                "Используйте этот код, если забудете пароль. Никому не сообщайте его!",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Ваш Backup Code", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (isVisible) realBackupCode else "******",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 4.sp
                            )
                        }
                    }

                    IconButton(onClick = { isVisible = !isVisible }) {
                        Icon(
                            imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isVisible) "Скрыть" else "Показать"
                        )
                    }
                }
            }

            if (!isLoading && realBackupCode == "Не создан") {
                Button(
                    onClick = {
                        val newCode = (100000..999999).random().toString()
                        db.collection("users").document(myUsername).update("backupCode", newCode)
                            .addOnSuccessListener {
                                realBackupCode = newCode
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Сгенерировать код")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Двухфакторная аутентификация",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                "Дополнительная защита вашего аккаунта при входе через приложение-аутентификатор (Google Authenticator, Authy и др.)",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            if (totpSecret.isEmpty()) {
                Button(
                    onClick = {
                        totpSecret = TotpUtils.generateSecretKey()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Сгенерировать ключ 2FA")
                }
            } else {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ваш секретный ключ:", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { isTotpVisible = !isTotpVisible }) {
                                Icon(
                                    imageVector = if (isTotpVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        Text(
                            text = if (isTotpVisible) totpSecret else "• ".repeat(totpSecret.length / 2).trim(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .clickable {
                                    if (isTotpVisible) {
                                        clipboardManager.setText(AnnotatedString(totpSecret))
                                        Toast.makeText(context, "Ключ скопирован!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Сначала сделайте ключ видимым", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(vertical = 8.dp)
                        )
                        Text(
                            if (isTotpVisible) "(Нажмите на ключ, чтобы скопировать)" else "(Ключ скрыт защитой)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isTotpVisible) {
                            qrBitmap?.let { bitmap ->
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "QR Code",
                                    modifier = Modifier
                                        .size(200.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = {
                                if (isTotpVisible) {
                                    val uri = TotpUtils.getQrCodeUri(totpSecret, myUsername, "Zhirpem")
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Приложение-аутентификатор не найдено", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Сначала сделайте ключ видимым", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Открыть в аутентификаторе")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Включить 2FA (TOTP)", fontWeight = FontWeight.Medium)
                Switch(
                    checked = is2faEnabled && totpSecret.isNotEmpty(),
                    onCheckedChange = { is2faEnabled = it },
                    enabled = totpSecret.isNotEmpty()
                )
            }

            Button(
                onClick = {
                    if (is2faEnabled) {
                        showVerifyDialog = true
                    } else {
                        isSaving = true
                        db.collection("users").document(myUsername)
                            .update(
                                mapOf(
                                    "totpSecret" to totpSecret,
                                    "is2faEnabled" to false
                                )
                            )
                            .addOnSuccessListener { isSaving = false }
                            .addOnFailureListener { isSaving = false }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving && totpSecret.isNotEmpty(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Сохранить настройки 2FA")
                }
            }
        }
    }

    if (showVerifyDialog) {
        TotpVerificationDialog(
            secret = totpSecret,
            onSuccess = {
                showVerifyDialog = false
                isSaving = true
                db.collection("users").document(myUsername)
                    .update(
                        mapOf(
                            "totpSecret" to totpSecret,
                            "is2faEnabled" to true
                        )
                    )
                    .addOnSuccessListener { 
                        isSaving = false
                        Toast.makeText(context, "2FA успешно включена!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { isSaving = false }
            },
            onDismiss = { showVerifyDialog = false }
        )
    }
}

@Composable
fun TotpVerificationDialog(
    secret: String,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Подтверждение 2FA") },
        text = {
            Column {
                Text("Введите 6-значный код из вашего приложения-аутентификатора, чтобы подтвердить настройку.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) code = it },
                    label = { Text("Код подтверждения") },
                    isError = error,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (error) {
                    Text("Неверный код. Попробуйте еще раз.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (TotpUtils.verifyTotp(secret, code)) {
                        onSuccess()
                    } else {
                        error = true
                    }
                },
                enabled = code.length == 6
            ) {
                Text("Подтвердить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
