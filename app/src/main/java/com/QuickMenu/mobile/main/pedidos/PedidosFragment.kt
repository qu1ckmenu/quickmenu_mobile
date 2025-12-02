package com.QuickMenu.mobile.main.pedidos

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.QuickMenu.mobile.databinding.FragmentPedidosBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.firebase.Timestamp

class PedidosFragment : Fragment() {

    private var _binding: FragmentPedidosBinding? = null
    private val binding get() = _binding!!
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPedidosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        fetchOrders()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = PedidosAdapter(emptyList())
    }

    private fun fetchOrders() {
        val userId = auth.currentUser?.uid
        if (userId == null) return

        lifecycleScope.launch {
            try {
                // 1. Busca os pedidos do usuário
                val pedidosSnapshot = db.collection("Usuario")
                    .document(userId)
                    .collection("Pedidos")
                    .orderBy("dataPedido", Query.Direction.DESCENDING)
                    .limit(10)
                    .get()
                    .await()

                val listaPedidosMontada = mutableListOf<Pedido>()

                for (document in pedidosSnapshot.documents) {
                    val pedidoId = document.id
                    val dados = document.data

                    // --- LEITURA DOS DADOS BÁSICOS ---
                    val idRestaurante = dados?.get("idRestaurante") as? String ?: ""
                    // IMPORTANTE: Precisamos do donoId para achar o restaurante no banco
                    val donoId = dados?.get("donoId") as? String ?: ""
                    val precoTotal = dados?.get("precoTotal") as? Double ?: 0.0

                    // --- NOVA LÓGICA: BUSCAR DADOS DO RESTAURANTE ---
                    var nomeRestaurante = "Restaurante Desconhecido"
                    var fotoRestaurante = ""

                    // Verifica se temos os dois IDs necessários para o caminho
                    if (idRestaurante.isNotEmpty() && donoId.isNotEmpty()) {

                        // Caminho corrigido baseado na estrutura do HomeFragment:
                        // operadores -> {donoId} -> restaurantes -> {idRestaurante}
                        val docRestaurante = db.collection("operadores")
                            .document(donoId)
                            .collection("restaurantes")
                            .document(idRestaurante)
                            .get()
                            .await()

                        if (docRestaurante.exists()) {
                            nomeRestaurante = docRestaurante.getString("nome") ?: "Nome Indisponível"
                            // Confirme se o campo é 'imageUrl', 'logoUrl' ou 'foto' no seu banco
                            fotoRestaurante = docRestaurante.getString("imageUrl") ?: ""
                        }
                    } else if (idRestaurante.isNotEmpty() && donoId.isEmpty()) {
                        // FALLBACK: Se o pedido for antigo e não tiver 'donoId',
                        // tentamos achar o restaurante via CollectionGroup (mais lento, mas funciona)
                        val querySnapshot = db.collectionGroup("restaurantes")
                            .whereEqualTo(com.google.firebase.firestore.FieldPath.documentId(), idRestaurante)
                            .get()
                            .await()

                        if (!querySnapshot.isEmpty) {
                            val doc = querySnapshot.documents[0]
                            nomeRestaurante = doc.getString("nome") ?: "Nome Indisponível"
                            fotoRestaurante = doc.getString("imageUrl") ?: ""
                        }
                    }
                    // ------------------------------------------------

                    // LER STATUS
                    val statusString = dados?.get("status") as? String ?: "Ativo"
                    val statusPedido = if (statusString == "Encerrado") Status.Encerrado else Status.Ativo

                    // TRATAR HORÁRIOS
                    val horaCompra = converterIdParaHorario(pedidoId)
                    var horaRetirada: String? = null
                    if (statusPedido == Status.Encerrado) {
                        val timestampEncerrado = dados?.get("dataStatusEncerrado") as? Timestamp
                        horaRetirada = formatarTimestamp(timestampEncerrado)
                    }

                    // BUSCAR ITENS DO PEDIDO
                    val itensSnapshot = document.reference.collection("Itens").get().await()
                    val produtosList = itensSnapshot.documents.mapNotNull { itemDoc ->
                        itemDoc.toObject(ProdutoPedido::class.java)
                    }

                    val novoPedido = Pedido(
                        id = pedidoId,
                        restauranteId = idRestaurante,
                        nomeRestaurante = nomeRestaurante,
                        fotoRestaurante = fotoRestaurante,
                        produtoPedidos = produtosList,
                        precoTotal = precoTotal,
                        status = statusPedido,
                        horarioCompraFormatado = horaCompra,
                        horarioRetiradaFormatado = horaRetirada
                    )

                    listaPedidosMontada.add(novoPedido)
                }

                if (listaPedidosMontada.isNotEmpty()) {
                    binding.recyclerView.adapter = PedidosAdapter(listaPedidosMontada)
                }

            } catch (e: Exception) {
                Log.e("PedidosFragment", "Erro: ", e)
                Toast.makeText(context, "Erro ao carregar pedidos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun converterIdParaHorario(pedidoId: String): String {
        return try {
            val formatoEntrada = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val data = formatoEntrada.parse(pedidoId)
            val formatoSaida = SimpleDateFormat("HH:mm - dd/MM", Locale("pt", "BR"))
            if (data != null) formatoSaida.format(data) else "--:--"
        } catch (e: Exception) {
            pedidoId
        }
    }

    private fun formatarTimestamp(timestamp: Timestamp?): String {
        if (timestamp == null) return "--:--"
        val sdf = SimpleDateFormat("HH:mm - dd/MM", Locale("pt", "BR"))
        return sdf.format(timestamp.toDate())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}