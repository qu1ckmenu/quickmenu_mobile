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

        // Garante a sincronia
        startRealtimeCartListener()

        //BOTÃO DE FINALIZAR COMPRA




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

        // Esta linha associa a toolbar ao NavController, mas vamos adicionar o comportamento de "voltar".
        toolbar.setupWithNavController(findNavController())

        // Adicione esta linha para garantir que o botão "voltar" use a pilha de navegação corretamente.
        toolbar.setNavigationOnClickListener {
            // Este comando simula o pressionar do botão "voltar" do sistema,
            // que sempre retorna para a tela anterior na pilha.
            findNavController().navigateUp()
        }
    }




    // FINALIZAR PEDIDO
    // DENTRO DA CLASSE CarrinhoFragment

    // FINALIZAR PEDIDO
    private fun finalizarPedido() {
        val userId = auth.currentUser?.uid ?: return
        // Adiciona uma verificação de segurança extra
        if (listaItens.isEmpty()) {
            Toast.makeText(context, "Seu carrinho está vazio!", Toast.LENGTH_SHORT).show()
            return
        }

        // Gerar ID baseado na Data e Hora (yyyyMMdd_HHmmss)
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val dataAtual = Date()
        val pedidoId = sdf.format(dataAtual)

        //  Calcular preço total
        val precoTotal = calcularTotal()

        //  Criar referência para o novo pedido
        val pedidoRef = banco.collection("Usuario").document(userId)
            .collection("Pedidos").document(pedidoId)

        // ✅ **INÍCIO DA CORREÇÃO**
        // Pega os IDs do primeiro item da lista.
        // Assumimos que todos os itens no carrinho são do mesmo restaurante.
        val primeiroItem = listaItens.first()
        val idRestauranteFinal = primeiroItem.idRestaurante
        val donoIdFinal = primeiroItem.donoId

        // Dados do cabeçalho do pedido
        val dadosPedido = hashMapOf(
            "idRestaurante" to idRestauranteFinal, // Correto!
            "donoId" to donoIdFinal,               // Correto!
            "idPedido" to pedidoId,
            "precoTotal" to precoTotal,
            "dataPedido" to dataAtual,
            "status" to "Ativo",

        )
        // ✅ **FIM DA CORREÇÃO**

        // Iniciar um BATCH (Lote de escrita).
        // garante que cria o pedido E apaga o carrinho ao mesmo tempo.
        val batch = banco.batch()

        // A) Salva os dados principais do pedido
        batch.set(pedidoRef, dadosPedido)

        //Move os itens do Carrinho para dentro do Pedido e prepara a deleção do Carrinho
        val carrinhoRef = banco.collection("Usuario").document(userId).collection("Carrinho")

        for (item in listaItens) {
            // Copia item para coleção Pedidos -> Itens
            val itemNoPedidoRef = pedidoRef.collection("Itens").document(item.produtoId)
            batch.set(itemNoPedidoRef, item)

            // Deleta item da coleção Carrinho
            val itemNoCarrinhoRef = carrinhoRef.document(item.produtoId)
            batch.delete(itemNoCarrinhoRef)
        }

        // Executa todas as operações
        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(context, "Pedido realizado com sucesso!", Toast.LENGTH_LONG).show()
                // A UI vai limpar sozinha porque temos o startRealtimeCartListener ouvindo que deletamos os itens!
                // Aqui você pode navegar para uma tela de "Sucesso" ou "Meus Pedidos"
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
        // Retorna a soma de todas as propriedades 'quantidade' de cada item na lista
        return listaItens.sumOf { it.quantidade }
    }

    // Chamado quando clica no botão (+)
    override fun onUpdateItem(item: ItemCarrinho) {
        saveOrUpdateCartItem(item)
    }

    // Chamado quando clica no lixo ou (-) chega a zero
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
        // Para de ouvir o banco quando sair da tela para economizar dados
        firestoreListener?.remove()

        _binding = null
    }
}