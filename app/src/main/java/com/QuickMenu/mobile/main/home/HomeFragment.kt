package com.QuickMenu.mobile.main.home

import ItemProdutoHomeAdapter
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.QuickMenu.mobile.R
import com.QuickMenu.mobile.databinding.FragmentHomeBinding
import com.QuickMenu.mobile.main.pedidos.ProdutoPedido
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.util.Locale

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var recentAdapter: ItemProdutoHomeAdapter
    private lateinit var restaurantAdapter: ItemRestauranteAdapter
    private lateinit var db: FirebaseFirestore
    private val auth = Firebase.auth

    private var allRestaurants = mutableListOf<ItemRestaurante>()
    private var lastSearchedRestaurants = mutableListOf<ItemRestaurante>()
    private var favoriteRestaurants = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = Firebase.firestore

        setupRecyclerViews()

        loadFavorites()
        loadRestaurantsFromFirestore()
        fetchRecentOrderedProducts()

        setupSearchBar()
        setupFilterButtons()
        voltar()
    }
    private fun setupRecyclerViews() {
        recentAdapter = ItemProdutoHomeAdapter(emptyList()) { produtoClicado ->

            val bundle = Bundle().apply {
                putString("produtoId", produtoClicado.produtoId)
                putString("donoId", produtoClicado.donoId)
                putString("idRestaurante", produtoClicado.idRestaurante)
                putString("nomeProduto", produtoClicado.nome)
                putDouble("precoUnitario", produtoClicado.precoUnitario)
                putString("descricaoProduto", produtoClicado.descricao)
                putString("imageUrlProduto", produtoClicado.imageUrl)
            }

            try {
                findNavController().navigate(R.id.action_homeFragment_to_produtoFragment, bundle)
            } catch (e: Exception) {
                Log.e("HomeFragment", "Falha ao navegar para ProdutoFragment: ${e.message}", e)
            }
        }


        binding.recyclerItemProduto.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = recentAdapter
        }

        restaurantAdapter = ItemRestauranteAdapter(mutableListOf(),
            onFavoriteClick = { toggleFavorite(it) },
            onItemClick = { restaurante ->
                updateLastSearched(restaurante)
                val bundle = Bundle().apply {
                    putString("restauranteId", restaurante.id)
                    putString("donoId", restaurante.userId)
                }
                try {
                    findNavController().navigate(R.id.cardapioFragment, bundle)
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Erro ao navegar: ${e.message}")
                }
            }
        )
        binding.recyclerRestaurantList.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = restaurantAdapter
        }
    }

    private fun fetchRecentOrderedProducts() {
        val userId = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                val pedidosSnapshot = db.collection("Usuario")
                    .document(userId)
                    .collection("Pedidos")
                    .orderBy("dataPedido", Query.Direction.DESCENDING)
                    .limit(5)
                    .get()
                    .await()

                val produtosRecentes = mutableListOf<ItemProdutoHome>()
                val idsProdutosAdicionados = mutableSetOf<String>()

                for (pedidoDoc in pedidosSnapshot.documents) {
                    if (produtosRecentes.size >= 3) break

                    val restauranteId = pedidoDoc.getString("idRestaurante")
                    val donoId = pedidoDoc.getString("donoId")

                    if (restauranteId.isNullOrEmpty() || donoId.isNullOrEmpty()) {
                        Log.w("HomeFragment", "Pedido ${pedidoDoc.id} incompleto: restauranteId=$restauranteId, donoId=$donoId")
                        continue
                    }

                    val itensSnapshot = pedidoDoc.reference.collection("Itens").get().await()

                    for (itemDoc in itensSnapshot.documents) {
                        val produtoPedidoInfo = itemDoc.toObject(ProdutoPedido::class.java)

                        if (produtoPedidoInfo != null && !idsProdutosAdicionados.contains(produtoPedidoInfo.produtoId)) {

                            val produtoRef = db.collection("operadores").document(donoId)
                                .collection("restaurantes").document(restauranteId)
                                .collection("produtos").document(produtoPedidoInfo.produtoId)

                            val produtoCompletoDoc = produtoRef.get().await()

                            if (produtoCompletoDoc.exists()) {
                                val itemHome = ItemProdutoHome(
                                    produtoId = produtoCompletoDoc.id,
                                    donoId = donoId,
                                    idRestaurante = restauranteId,
                                    nome = produtoCompletoDoc.getString("nome") ?: "",
                                    precoUnitario = produtoCompletoDoc.getDouble("preco") ?: 0.0,
                                    descricao = produtoCompletoDoc.getString("descricao"),
                                    imageUrl = produtoCompletoDoc.getString("imageUrl")
                                )

                                produtosRecentes.add(itemHome)
                                idsProdutosAdicionados.add(itemHome.produtoId)

                                if (produtosRecentes.size >= 3) break
                            }
                        }
                    }
                }

                if (isAdded) {
                    if (produtosRecentes.isNotEmpty()) {
                        recentAdapter.updateList(produtosRecentes)
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Erro ao buscar produtos recentes", e)
            }
        }
    }




    private fun loadRestaurantsFromFirestore() {
        db.collectionGroup("restaurantes").get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) return@addOnSuccessListener
                allRestaurants.clear()
                for (document in result) {
                    val restaurante = document.toObject(ItemRestaurante::class.java)
                    restaurante.id = document.id
                    val operadorId = document.reference.parent.parent?.id ?: ""
                    restaurante.userId = operadorId
                    allRestaurants.add(restaurante)
                }
                filterAndDisplayRestaurants("")
            }
    }

    private fun loadFavorites() {
        val prefs = activity?.getSharedPreferences("RestaurantPreferences", Context.MODE_PRIVATE) ?: return
        favoriteRestaurants = prefs.getStringSet("favorite_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
    }

    private fun saveFavorites() {
        val prefs = activity?.getSharedPreferences("RestaurantPreferences", Context.MODE_PRIVATE) ?: return
        with(prefs.edit()) {
            putStringSet("favorite_ids", favoriteRestaurants)
            apply()
        }
    }

    private fun setupSearchBar() {
        binding.searchBar.doOnTextChanged { text, _, _, _ ->
            filterAndDisplayRestaurants(text.toString().trim())
        }
    }

    private fun setupFilterButtons() {
        binding.btnFavoritos.setOnClickListener {
            val favoritedList = allRestaurants.filter { favoriteRestaurants.contains(it.id) }
            restaurantAdapter.updateList(favoritedList, favoriteRestaurants)
        }
        binding.root.setOnClickListener {
            filterAndDisplayRestaurants("")
        }
    }

    private fun filterAndDisplayRestaurants(query: String) {
        val filteredList = if (query.isEmpty()) {
            (lastSearchedRestaurants + allRestaurants).distinctBy { it.id }
        } else {
            allRestaurants.filter { it.nome.contains(query, ignoreCase = true) }
        }
        val sortedList = filteredList.sortedByDescending { favoriteRestaurants.contains(it.id) }
        restaurantAdapter.updateList(sortedList, favoriteRestaurants)
    }

    private fun toggleFavorite(restaurante: ItemRestaurante) {
        if (favoriteRestaurants.contains(restaurante.id)) {
            favoriteRestaurants.remove(restaurante.id)
        } else {
            favoriteRestaurants.add(restaurante.id)
        }
        saveFavorites()
        filterAndDisplayRestaurants(binding.searchBar.text.toString().trim())
    }

    private fun updateLastSearched(restaurante: ItemRestaurante) {
        lastSearchedRestaurants.removeIf { it.id == restaurante.id }
        lastSearchedRestaurants.add(0, restaurante)
        if (lastSearchedRestaurants.size > 5) {
            lastSearchedRestaurants = lastSearchedRestaurants.take(5).toMutableList()
        }
    }

    private fun voltar(){
        val navController = findNavController()
        val isCardapio = navController.previousBackStackEntry?.destination?.id == R.id.cardapioFragment
        val backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isCardapio) {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}