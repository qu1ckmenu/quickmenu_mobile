package com.QuickMenu.mobile.main.home

data class ItemProdutoHome(
    val nome: String,
    val preco: String,
    val imageUrl: String? = null // Agora aceita URL
)