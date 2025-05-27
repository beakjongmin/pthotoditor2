package com.ruto.pthotoditor2.feature.editor.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ConfirmReplaceImageDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
    val warningColor = MaterialTheme.colorScheme.errorContainer

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        )
    ) {
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(200)
            ) + fadeIn(animationSpec = tween(200))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(containerColor)
                        .padding(vertical = 24.dp, horizontal = 16.dp)
                ) {
                    // 상단 아이콘 (카메라 아이콘)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.2f))
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = "이미지 변경",
                            tint = primaryColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 제목
                    Text(
                        text = "이미지를 변경하시겠습니까?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 설명 텍스트
                    Text(
                        text = "현재 편집중인 이미지가 있습니다.\n새로운 이미지를 선택하면 기존 작업은 사라집니다.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

//                    // 경고 메시지 박스
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .clip(RoundedCornerShape(12.dp))
//                            .background(warningColor.copy(alpha = 0.15f))
//                            .padding(12.dp)
//                    ) {
//                        Row(
//                            verticalAlignment = Alignment.CenterVertically,
//                            modifier = Modifier.fillMaxWidth()
//                        ) {
//                            Icon(
//                                imageVector = Icons.Rounded.Warning,
//                                contentDescription = "경고",
//                                tint = MaterialTheme.colorScheme.error,
//                                modifier = Modifier.size(20.dp)
//                            )
//
//                            Spacer(modifier = Modifier.width(8.dp))
//
//                            Text(
//                                text = "저장되지 않은 변경사항은 복구할 수 없습니다",
//                                fontSize = 14.sp,
//                                color = MaterialTheme.colorScheme.error,
//                                fontWeight = FontWeight.Medium
//                            )
//                        }
//                    }

//                    Spacer(modifier = Modifier.height(24.dp))

                    // 버튼 영역
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 취소 버튼
                        OutlinedButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = "취소",
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // 변경 버튼
                        Button(
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = onPrimaryColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = "변경하기",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}