package com.QuickMenu.mobile.main.carrinho

import android.annotation.SuppressLint
import android.icu.text.NumberFormat
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.QuickMenu.mobile.databinding.FragmentCarrinhoBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import java.util.Date
import java.util.Locale

interface CarrinhoActionsListener {
    fun onRemoverItem(position: Int)
    fun onUpdateItem(item: ItemCarrinho)
}

class CarrinhoFragment : Fragment(), CarrinhoActionsListener {

    private var _binding: FragmentCarrinhoBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var banco: FirebaseFirestore
    private var firestoreListener: ListenerRegistration? = null

    private lateinit var carrinhoAdapter: CarrinhoAdapter
    private val listaItens = mutableListOf<ItemCarrinho>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCarrinhoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        banco = Firebase.firestore

        setupToolbar()
        setupRecyclerView()

        startRealtimeCartListener()

        binding.btnComprar.setOnClickListener {
            if (listaItens.isNotEmpty()) {
                finalizarPedido()
            } else {
                Toast.makeText(context, "Seu carrinho está vazio!", Toast.LENGTH_SHORT).show()
            }

        }


    }

    private fun setupRecyclerView() {
        carrinhoAdapter = CarrinhoAdapter(listaItens, this)
        binding.rvCarrinhoItens.layoutManager = LinearLayoutManager(context)
        binding.rvCarrinhoItens.adapter = carrinhoAdapter
    }
    private fun setupToolbar() {
        val toolbar = binding.toolbar

        toolbar.setupWithNavController(findNavController())

        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
    private fun finalizarPedido() {
        val userId = auth.currentUser?.uid ?: return

        if (listaItens.isEmpty()) {
            Toast.makeText(context, "Seu carrinho está vazio!", Toast.LENGTH_SHORT).show()
            return
        }

        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val dataAtual = Date()
        val pedidoId = sdf.format(dataAtual)

        val precoTotal = calcularTotal()

        val pedidoRef = banco.collection("Usuario").document(userId)
            .collection("Pedidos").document(pedidoId)

        val primeiroItem = listaItens.first()
        val idRestauranteFinal = primeiroItem.idRestaurante
        val donoIdFinal = primeiroItem.donoId

        val dadosPedido = hashMapOf(
            "idRestaurante" to idRestauranteFinal,
            "donoId" to donoIdFinal,
            "idPedido" to pedidoId,
            "precoTotal" to precoTotal,
            "dataPedido" to dataAtual,
            "status" to "Ativo",

        )

        val batch = banco.batch()
        batch.set(pedidoRef, dadosPedido)

        val carrinhoRef = banco.collection("Usuario").document(userId).collection("Carrinho")

        for (item in listaItens) {
            val itemNoPedidoRef = pedidoRef.collection("Itens").document(item.produtoId)
            batch.set(itemNoPedidoRef, item)

            val itemNoCarrinhoRef = carrinhoRef.document(item.produtoId)
            batch.delete(itemNoCarrinhoRef)
        }

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(context, "Pedido realizado com sucesso!", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erro ao finalizar: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("Carrinho", "Erro batch: ", e)
            }
    }



    private fun getCartRef() =
        auth.currentUser?.uid?.let { uid ->
            banco.collection("Usuario").document(uid).collection("Carrinho")
        }

    @SuppressLint("NotifyDataSetChanged")
    private fun startRealtimeCartListener() {
        val cartRef = getCartRef() ?: return

        firestoreListener = cartRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("Carrinho", "Erro ao ouvir carrinho", e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                listaItens.clear()
                val novosItens = snapshot.documents.mapNotNull { it.toObject(ItemCarrinho::class.java) }
                listaItens.addAll(novosItens)

                carrinhoAdapter.notifyDataSetChanged()
                onTotalChanged(calcularTotal(), calcularTotalUnidades())
            }
        }
    }
    private fun calcularTotalUnidades(): Int {
        return listaItens.sumOf { it.quantidade }
    }

    override fun onUpdateItem(item: ItemCarrinho) {
        saveOrUpdateCartItem(item)
    }

    override fun onRemoverItem(position: Int) {
        val itemToRemove = listaItens.getOrNull(position) ?: return
        removeCartItem(itemToRemove.produtoId)
    }

    private fun saveOrUpdateCartItem(item: ItemCarrinho) {
        getCartRef()?.document(item.produtoId)?.set(item)
            ?.addOnFailureListener { Log.e("Carrinho", "Erro update: $it") }
    }

    private fun removeCartItem(produtoId: String) {
        getCartRef()?.document(produtoId)?.delete()
            ?.addOnFailureListener { Log.e("Carrinho", "Erro delete: $it") }
    }

    fun onTotalChanged(novoTotal: Double, totalItens: Int) {
        val formatador = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        binding.total.text = formatador.format(novoTotal)
        binding.totalTitle.text = "Total ($totalItens)"
    }

    private fun calcularTotal(): Double {
        return listaItens.sumOf { it.preco * it.quantidade }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        firestoreListener?.remove()

        _binding = null
    }
}