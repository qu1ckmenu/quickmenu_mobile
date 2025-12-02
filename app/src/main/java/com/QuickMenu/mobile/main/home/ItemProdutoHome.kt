package com.QuickMenu.mobile.main.home

// Altere a data class para conter todos os campos necessários
data class ItemProdutoHome(
    val produtoId: String,
    val donoId: String,
    val idRestaurante: String,
    val nome: String,
    val precoUnitario: Double,
    val descricao: String?,
    val imageUrl: String?
)
