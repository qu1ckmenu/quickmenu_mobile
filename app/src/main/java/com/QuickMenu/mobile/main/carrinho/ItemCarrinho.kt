package com.QuickMenu.mobile.main.carrinho

data class ItemCarrinho(
    val produtoId: String = "",
    val nome: String = "",
    val preco: Double = 0.0,
    var quantidade: Int = 0,
    val imageUrl: String? = null,
    val idRestaurante: String = "",
    val donoId: String = ""
) {
    constructor() : this("", "", 0.0, 0, null)
}