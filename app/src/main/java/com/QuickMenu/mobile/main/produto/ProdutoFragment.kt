package com.QuickMenu.mobile.main.produto

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.QuickMenu.mobile.R
import com.QuickMenu.mobile.databinding.FragmentProdutoBinding
import com.bumptech.glide.Glide
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class ProdutoFragment : Fragment() {

    private var _binding: FragmentProdutoBinding? = null
    private val binding get() = _binding!!

    private var quantidade: Int = 1
    private var precoUnitario: Double = 0.0

    private var produtoId: String? = null
    private var donoId: String? = null
    private var nomeProduto: String? = null

    private var descricaoProduto: String? = null
    private var imageUrlProduto: String? = null

    private var idRestaurante: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProdutoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadArguments()

        loadProductData()

        initListeners()

        updateQuantityDisplay()
    }

    private fun loadArguments() {
        arguments?.let {
            produtoId = it.getString("produtoId")
            donoId = it.getString("donoId")
            idRestaurante = it.getString("idRestaurante")
            nomeProduto = it.getString("nomeProduto")
            precoUnitario = it.getDouble("precoUnitario")
            descricaoProduto = it.getString("descricaoProduto")
            imageUrlProduto = it.getString("imageUrlProduto")
        }
    }

    private fun loadProductData() {
        binding.txtNomeProduto.text = nomeProduto ?: "Produto"
        binding.txtDescricaoProduto.text = descricaoProduto

        imageUrlProduto?.let { url ->
            if (url.isNotEmpty()) {
                Glide.with(this)
                    .load(url)
                    .centerCrop()
                    .into(binding.imgProduto)
            }
        }

        updateQuantityDisplay()
    }

    private fun initListeners() {
        binding.btnVoltar.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnAumentarQtd.setOnClickListener {
            quantidade++
            updateQuantityDisplay()
        }

        binding.btnDiminuirQtd.setOnClickListener {
            if (quantidade > 1) {
                quantidade--
                updateQuantityDisplay()
            }
        }

        binding.btnAdicionarCarrinho.setOnClickListener {
            adicionarAoCarrinho()
        }
    }

    private fun updateQuantityDisplay() {
        binding.txtQuantidade.text = quantidade.toString()

        val precoTotal = quantidade * precoUnitario

        val precoFormatado = String.format("R$ %.2f", precoTotal)
        binding.txtPreco.text = precoFormatado
    }

    private fun adicionarAoCarrinho() {
        val db = com.google.firebase.Firebase.firestore
        val auth = com.google.firebase.Firebase.auth
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(context, "Faça login para adicionar ao carrinho", Toast.LENGTH_SHORT).show()
            return
        }

        val itemProdutoId = produtoId
        val quantidadeFinal = quantidade
        val nomeFinal = nomeProduto ?: "Produto"
        val imageUrlFinal = imageUrlProduto ?: ""

        if (itemProdutoId == null) {
            Toast.makeText(context, "Erro: ID do produto não encontrado.", Toast.LENGTH_SHORT).show()
            return
        }

        val carrinhoRef = db.collection("Usuario").document(userId).collection("Carrinho")
        val docProduto = carrinhoRef.document(itemProdutoId)

        docProduto.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val qtdAtual = document.getLong("quantidade")?.toInt() ?: 0

                val updates = hashMapOf<String, Any>(
                    "quantidade" to (qtdAtual + quantidadeFinal),
                    "imageUrl" to imageUrlFinal
                )

                docProduto.update(updates as Map<String, Any>)
                    .addOnSuccessListener {
                        Toast.makeText(context, "+$quantidadeFinal de $nomeFinal. Indo para o Carrinho!", Toast.LENGTH_SHORT).show()
                        navegarParaCarrinho()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Erro ao atualizar carrinho.", Toast.LENGTH_SHORT).show()
                    }

            } else {
                val novoItem = com.QuickMenu.mobile.main.carrinho.ItemCarrinho(
                    produtoId = itemProdutoId,
                    nome = nomeFinal,
                    preco = precoUnitario,
                    quantidade = quantidadeFinal,
                    imageUrl = imageUrlFinal,
                    idRestaurante = idRestaurante ?: "",
                    donoId = donoId ?: ""
                )

                docProduto.set(novoItem)
                    .addOnSuccessListener {
                        Toast.makeText(context, "$nomeFinal (x$quantidadeFinal) adicionado ao carrinho!", Toast.LENGTH_SHORT).show()
                        navegarParaCarrinho()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Erro ao adicionar item.", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun navegarParaCarrinho() {
        try {
            findNavController().navigate(R.id.action_produtoFragment_to_carrinhoFragment)
        } catch (e: Exception) {
            Log.e("ProdutoFragment", "Erro ao navegar para o Carrinho: ${e.message}")
            Toast.makeText(context, "Erro de navegação interna.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}