package com.QuickMenu.mobile.main.cardapio


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.QuickMenu.mobile.databinding.FragmentCardapioBinding
import com.QuickMenu.mobile.main.carrinho.ItemCarrinho
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.QuickMenu.mobile.R



class CardapioFragment : Fragment() {

    private var _binding: FragmentCardapioBinding? = null
    private val binding get() = _binding!!

    private val db = Firebase.firestore
    private val auth = Firebase.auth


    private var currentRestauranteId: String = ""
    private var currentDonoId: String = ""

    private val listaCategoriasCompletas = mutableListOf<Categoria>()
    private lateinit var categoriaAdapter: CategoriaAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCardapioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        arguments?.let { bundle ->
            currentRestauranteId = bundle.getString("restauranteId") ?: ""
            currentDonoId = bundle.getString("donoId") ?: ""
        }

        if (currentRestauranteId.isEmpty() || currentDonoId.isEmpty()) {
            Toast.makeText(context, "Erro ao carregar restaurante", Toast.LENGTH_SHORT).show()
            return
        }


        setupRecyclerView()
        carregarDadosRestaurante()
        carregarCardapio()

        initListeners()

    }

    private fun initListeners(){
        binding.btnVoltar.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnVerCarrinho.setOnClickListener {
            findNavController().navigate(R.id.action_cardapioFragment_to_carrinhoFragment)
        }

    }


    private fun setupRecyclerView() {
        categoriaAdapter = CategoriaAdapter(listaCategoriasCompletas) { produtoClicado ->
            navegarParaProduto(produtoClicado)
        }

        binding.rvCategorias.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = categoriaAdapter
            isNestedScrollingEnabled = false
        }
    }


    private fun navegarParaProduto(produto: ProdutoCardapio) {
        val bundle = Bundle().apply {
            putString("produtoId", produto.produtoId)
            putString("donoId", currentDonoId)
            putString("idRestaurante", currentRestauranteId) // ID do restaurante que você já tem
            putString("nomeProduto", produto.nome)
            putDouble("precoUnitario", produto.preco)
            putString("descricaoProduto", produto.descricao)
            putString("imageUrlProduto", produto.imageUrl)
        }

        try {
            findNavController().navigate(R.id.action_cardapioFragment_to_produtoFragment, bundle)
        } catch (e: Exception) {
            Log.e("Cardapio", "Erro ao navegar para Produto: ${e.message}")
            Toast.makeText(context, "Erro na navegação.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun carregarDadosRestaurante() {
        val restauranteDocRef = db.collection("operadores").document(currentDonoId)
            .collection("restaurantes").document(currentRestauranteId)

        restauranteDocRef.get()
            .addOnSuccessListener { document ->

                if (_binding == null) return@addOnSuccessListener

                if (document.exists()) {
                    val nome = document.getString("nome")
                    val descricao = document.getString("descricao")
                    val imageUrl = document.getString("imageUrl")

                    binding.txtNomeRestaurante.text = nome ?: "Restaurante"
                    binding.txtDescricaoRestaurante.text = descricao

                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this).load(imageUrl).centerCrop().into(binding.imgBanner)
                    }
                } else {
                    Log.e("Cardapio", "Restaurante não encontrado no banco.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Cardapio", "Erro conexao: $e")
            }
    }

    private fun carregarCardapio() {
        val BASE_PATH = "operadores/$currentDonoId/restaurantes/$currentRestauranteId"

        val pathCategorias = "$BASE_PATH/categorias"
        val pathProduto = "$BASE_PATH/produtos"

        db.collection(pathCategorias).get()
            .addOnSuccessListener { querySnapshot ->
                listaCategoriasCompletas.clear()

                for (catDoc in querySnapshot) {
                    val catNome = catDoc.getString("nome") ?: "Categoria"

                    catDoc.reference.collection("produtosCategoria").get()
                        .addOnSuccessListener { produtosSnapshot ->

                            val produtosDestaCategoria = mutableListOf<ProdutoCardapio>()
                            val totalProdutosParaBuscar = produtosSnapshot.size()
                            var produtosProcessados = 0


                            for (prodLinkDoc in produtosSnapshot) {
                                val produtoIdReal = prodLinkDoc.id

                                db.collection(pathProduto).document(produtoIdReal).get()
                                    .addOnSuccessListener { prodDetailDoc ->
                                        if (prodDetailDoc.exists()) {
                                            val prodObj = prodDetailDoc.toObject(ProdutoCardapio::class.java)
                                            prodObj?.let {
                                                it.produtoId = produtoIdReal
                                                produtosDestaCategoria.add(it)
                                            }
                                        }
                                        produtosProcessados++
                                        if (produtosProcessados == totalProdutosParaBuscar) {
                                            organizarEAtualizarLista(Categoria(catDoc.id, catNome, produtosDestaCategoria))
                                        }
                                    }
                                    .addOnFailureListener {
                                        produtosProcessados++
                                        if (produtosProcessados == totalProdutosParaBuscar) {
                                            organizarEAtualizarLista(Categoria(catDoc.id, catNome, produtosDestaCategoria))
                                        }
                                    }
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Cardapio", "Erro ao carregar categorias", e)
            }
    }

    private fun organizarEAtualizarLista(novaCategoria: Categoria) {
        listaCategoriasCompletas.add(novaCategoria)
        categoriaAdapter.notifyDataSetChanged()
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}