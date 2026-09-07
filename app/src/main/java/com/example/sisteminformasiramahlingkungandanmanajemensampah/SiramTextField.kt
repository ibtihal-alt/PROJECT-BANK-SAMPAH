package com.example.sisteminformasiramahlingkungandanmanajemensampah

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun SiramTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    modifier: Modifier = Modifier
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,

        label = {
            Text(label)
        },

        placeholder = {
            Text(placeholder)
        },

        modifier = modifier.fillMaxWidth(),

        singleLine = true,

        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),

        visualTransformation =
            if (isPassword) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },

        shape = MaterialTheme.shapes.medium
    )
}