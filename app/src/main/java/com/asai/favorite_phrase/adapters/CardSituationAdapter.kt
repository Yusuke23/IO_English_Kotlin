package com.example.ioenglish.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ioenglish.activities.EditCardSituationActivity
import com.example.ioenglish.activities.MainActivity
import com.example.ioenglish.databinding.ItemCardSituationBinding
import com.example.ioenglish.models.Situation
import com.example.ioenglish.utils.Constants

class CardSituationAdapter (private val context: MainActivity,
                            private val list: ArrayList<Situation>
): RecyclerView.Adapter<CardSituationAdapter.ItemCardSituationViewHolder>() {

    private var onClickListener: OnClickListener? = null

    class ItemCardSituationViewHolder(val binding: ItemCardSituationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemCardSituationViewHolder {
        return ItemCardSituationViewHolder(
            ItemCardSituationBinding.inflate(LayoutInflater.from(context), parent, false)
        )
    }

    override fun getItemCount(): Int {
        return list.size

    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ItemCardSituationViewHolder, position: Int) {
        val model = list[position]

        Glide
            .with(context)
            .load(model.imageSituation)
            .centerCrop()
            .into(holder.binding.ivCardSituationImage)

        holder.binding.tvSituation.text = model.situation

        holder.itemView.setOnClickListener {
            if (onClickListener != null) {
                onClickListener!!.onClick(position, model)
            }
        }

    }

    interface OnClickListener {
        fun onClick(position: Int, model: Situation)
    }

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    // MainActivity から EditCardSituationActivity へ Card の情報を持って遷移
    fun notifyEditCardSituation(activity: Activity, position: Int, requestCode: Int) {
        val intent = Intent(context, EditCardSituationActivity::class.java)
        intent.putExtra(Constants.DOCUMENT_ID, list[position].documentId)
        activity.startActivityForResult(intent, requestCode)
        notifyItemChanged(position)
    }

}