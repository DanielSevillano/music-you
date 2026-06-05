package com.github.musicyou.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import com.github.musicyou.R
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun TextFieldDialog(
    title: String,
    hintText: String,
    onDismiss: () -> Unit,
    onDone: (String) -> Unit,
    modifier: Modifier = Modifier,
    cancelText: String = stringResource(id = R.string.cancel),
    doneText: String = stringResource(id = R.string.done),
    initialTextInput: String = "",
    singleLine: Boolean = true,
    maxLines: Int = 1,
    onCancel: () -> Unit = onDismiss,
    isTextInputValid: (String) -> Boolean = { it.isNotEmpty() }
) {
    val focusRequester = remember {
        FocusRequester()
    }

    var textFieldValue by rememberSaveable(initialTextInput, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                text = initialTextInput,
                selection = TextRange(initialTextInput.length)
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (isTextInputValid(textFieldValue.text)) {
                        onDismiss()
                        onDone(textFieldValue.text)
                    }
                }
            ) {
                Text(text = doneText)
            }
        },
        modifier = modifier,
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(text = cancelText)
            }
        },
        title = {
            Text(text = title)
        },
        text = {
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                singleLine = singleLine,
                maxLines = maxLines,
                placeholder = {
                    Text(text = hintText)
                },
                keyboardOptions = KeyboardOptions(imeAction = if (singleLine) ImeAction.Done else ImeAction.None),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (isTextInputValid(textFieldValue.text)) {
                            onDismiss()
                            onDone(textFieldValue.text)
                        }
                    }
                ),
                modifier = Modifier.focusRequester(focusRequester)
            )
        }
    )

    LaunchedEffect(Unit) {
        delay(duration = 300.milliseconds)
        focusRequester.requestFocus()
    }
}

@Composable
fun ConfirmationDialog(
    title: String,
    text: String? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    cancelText: String = stringResource(id = R.string.cancel),
    confirmText: String = stringResource(id = R.string.confirm),
    onCancel: () -> Unit = onDismiss
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            ) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel
            ) {
                Text(text = cancelText)
            }
        },
        title = {
            Text(text = title)
        },
        text = {
            text?.let {
                Text(text = it)
            }
        }
    )
}

@Composable
inline fun <T> ValueSelectorDialog(
    noinline onDismiss: () -> Unit,
    title: String,
    selectedValue: T,
    values: List<T>,
    crossinline onValueSelected: (T) -> Unit,
    crossinline valueText: (T) -> String = { it.toString() }
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
        title = {
            Text(text = title)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                values.forEach { value ->
                    ListItem(
                        headlineContent = { Text(text = valueText(value)) },
                        modifier = Modifier.clickable(
                            onClick = {
                                onDismiss()
                                onValueSelected(value)
                            }
                        ),
                        leadingContent = {
                            RadioButton(
                                selected = selectedValue == value,
                                onClick = {
                                    onDismiss()
                                    onValueSelected(value)
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = AlertDialogDefaults.containerColor
                        )
                    )
                }
            }
        }
    )
}