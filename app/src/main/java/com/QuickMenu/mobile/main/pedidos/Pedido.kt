package com.QuickMenu.mobile.main.pedidos

// Adicione os campos novos na sua data class existente
data class Pedido(
    val id: String,
    val restauranteId: String,
    val nomeRestaurante: String, // <--- NOVO
    val fotoRestaurante: String, // <--- NOVO
    val produtoPedidos: List<ProdutoPedido>,
    val precoTotal: Double,
    val status: Status,
    val horarioCompraFormatado: String,
    val horarioRetiradaFormatado: String?
)