package com.atpdev.paltoscan.features.history.adapter

import android.animation.Animator
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.atpdev.paltoscan.R
import com.atpdev.paltoscan.databinding.ItemHistoryCardBinding
import com.atpdev.paltoscan.features.diseaseInfo.DiseaseRepository
import com.atpdev.paltoscan.features.history.HistoryItemDiffCallback
import io.github.muddz.styleabletoast.StyleableToast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onDeleteClick: (HistoryItem) -> Unit,
    private val onItemClick: (HistoryItem) -> Unit,
    // private val showDeleteDialog: (HistoryItem, LottieAnimationView) -> Unit // Nueva función para mostrar el diálogo
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {
    // private var items = mutableListOf<HistoryItem>()
    private var historyItems = emptyList<HistoryItem>()
    private val differ = AsyncListDiffer(this, HistoryItemDiffCallback())

    /*private val diffCallback = object : DiffUtil.ItemCallback<HistoryItem>() {
        override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
            return oldItem.diseaseName == newItem.diseaseName &&
                    oldItem.section == newItem.section
        }

        override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
            return oldItem == newItem
        }
    }*/

    fun submitList(newItems: List<HistoryItem>) {
        differ.submitList(newItems)
        /*val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = historyItems.size
            override fun getNewListSize(): Int = newItems.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return diffCallback.areItemsTheSame(
                    historyItems[oldItemPosition],
                    newItems[newItemPosition]
                )
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return diffCallback.areContentsTheSame(
                    historyItems[oldItemPosition],
                    newItems[newItemPosition]
                )
            }
        })

        historyItems = newItems
        diffResult.dispatchUpdatesTo(this)*/
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): HistoryViewHolder {
        val binding =
            ItemHistoryCardBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return HistoryViewHolder(binding)
        /*return HistoryViewHolder(
            ItemHistoryCardBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )*/
    }

    override fun onBindViewHolder(
        holder: HistoryViewHolder,
        position: Int,
    ) {
        // holder.bind(historyItems[position])
        val historyItem = differ.currentList[position]
        holder.bind(historyItem, onDeleteClick, onItemClick)
        // holder.bind(differ.currentList[position])
    }

    // override fun getItemCount() = items.size
    // override fun getItemCount(): Int = historyItems.size
    override fun getItemCount(): Int = differ.currentList.size

    inner class HistoryViewHolder(
        private val binding: ItemHistoryCardBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: HistoryItem,
            onDeleteClick: (HistoryItem) -> Unit,
            onItemClick: (HistoryItem) -> Unit,
        ) {
            binding.apply {
                // Carga de la imagen con Glide
                /*Glide.with(imageViewDisease.context)
                    .load(item.imageUri) // Carga el Uri de la imagen
                    //.placeholder(R.drawable.placeholder_image) // Imagen de carga por defecto
                    //.error(R.drawable.ic_bar_exit) // Imagen en caso de error
                    .into(imageViewDisease) // ImageView donde se cargará la imagen*/

                // Cargar la imagen desde imagePath
                val bitmap = BitmapFactory.decodeFile(item.imagePath)
                binding.imageViewDisease.setImageBitmap(bitmap) // Asegúrate de que este ImageView exista

                textViewName.text = DiseaseRepository.getDisplayName(item.diseaseName)
                textViewSection.text = item.section
                // textViewSection.text = if (item.section == "main") "PaltoScan" else item.section

                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val formattedDate = dateFormat.format(Date(item.timestamp))
                textViewTimestamp.text = formattedDate

                // Configurar el clic en el ítem para mostrar detalles
                /*root.setOnClickListener {
                    onItemClick(item)  // Llamar al método onItemClick pasando el ítem
                }*/
                binding.root.setOnClickListener {
                    onItemClick(item)
                }

                // Configurar el botón de eliminar

                // Configurar el botón de eliminar con Lottie
                /*-buttonDelete.setOnClickListener {
                    showDeleteDialog(item, binding.btnDeletehistorylottieAnimationView) // Asume que tienes un LottieAnimationView en tu layout
                }*/

                buttonDelete.setOnClickListener {
                    showClearHistoryDialog(item, binding.btnDeletehistorylottieAnimationView, onDeleteClick)
                    // onDeleteClick(item)
                }
            }
        }
    }
}

private fun showClearHistoryDialog(
    item: HistoryItem,
    lottieAnimation: LottieAnimationView,
    onDeleteClick: (HistoryItem) -> Unit,
) {
    lottieAnimation.speed = 1.5f
    lottieAnimation.playAnimation()

    lottieAnimation.addAnimatorListener(
        object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationCancel(animation: Animator) {}

            override fun onAnimationRepeat(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                onDeleteClick(item)

                StyleableToast.makeText(lottieAnimation.context, "Registro eliminado", R.style.exampleToastCorrect).show()

                lottieAnimation.frame = 0
                lottieAnimation.removeAllAnimatorListeners()
            }
        },
    )
}

/*class HistoryAdapter(private val historyList: List<HistoryItem>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewDisease: ImageView = itemView.findViewById(R.id.imageViewDisease)
        val textViewStatus: TextView = itemView.findViewById(R.id.textViewStatus)
        val textViewType: TextView = itemView.findViewById(R.id.textViewType)
        val textViewName: TextView = itemView.findViewById(R.id.textViewName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_card, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = historyList[position]
        holder.imageViewDisease.setImageResource(item.imageRes)
        holder.textViewStatus.text = item.status
        holder.textViewType.text = item.type
        holder.textViewName.text = item.name
    }

    override fun getItemCount(): Int = historyList.size
}*/
