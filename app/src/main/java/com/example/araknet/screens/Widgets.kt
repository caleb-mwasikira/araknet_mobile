package com.example.araknet.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.araknet.R
import com.example.araknet.ui.theme.AraknetTheme

@Composable
fun NoItemsFound(
    modifier: Modifier = Modifier,
    errMessage: String,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.empty_box_icon),
            contentDescription = "No Items Found",
            modifier = Modifier
                .size(240.dp)
                .padding(vertical=18.dp),
        )

        Text(
            errMessage,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewNoItemFound() {
    AraknetTheme {
        NoItemsFound(
            errMessage = "No items found"
        )
    }
}