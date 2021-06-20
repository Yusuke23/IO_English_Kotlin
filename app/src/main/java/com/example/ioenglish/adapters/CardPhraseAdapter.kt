package com.example.ioenglish.adapters

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ioenglish.activities.CardPhraseActivity
import com.example.ioenglish.activities.EditCardPhraseActivity
import com.example.ioenglish.databinding.ItemCardPhraseBinding
import com.example.ioenglish.models.Phrase
import com.example.ioenglish.models.Situation
import com.example.ioenglish.utils.Constants
import kotlin.collections.ArrayList

open class CardPhraseAdapter(private val context: CardPhraseActivity,
                             private val list: ArrayList<Phrase>
): RecyclerView.Adapter<CardPhraseAdapter.MyViewHolder>() {

    private var onClickListener: OnClickListener? = null

    class MyViewHolder(val binding: ItemCardPhraseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            ItemCardPhraseBinding.inflate(LayoutInflater.from(context), parent, false)
        )
    }

    override fun getItemCount(): Int {
        return list.size

    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val model = list[position]

        Glide
            .with(context)
            .load(model.imagePhrase)
            .centerCrop()
            .into(holder.binding.ivCardPhraseImage)

        holder.binding.tvPhraseInJapanese.text = model.japanese
        holder.binding.tvPhraseInEnglish.text = model.english
        holder.binding.tvKeyWord.text = model.keyWord

        if (model.labelColor.isNotEmpty()) {
        holder.binding.viewLabelColor.setBackgroundColor(Color.parseColor(model.labelColor))}

        holder.itemView.setOnClickListener {
            if (onClickListener != null) {
                onClickListener!!.onClick(position, model)
            }
        }

    }

    interface OnClickListener {
        fun onClick(position: Int, model: Phrase)
    }

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    // CardPhraseActivity から EditCardPhraseActivity へ Card の情報を持って遷移
    fun notifyEditCardPhrase(activity: Activity, model: Situation, cardListPosition: Int, requestCode: Int) {
        val intent = Intent(context, EditCardPhraseActivity::class.java)
        intent.putExtra(Constants.CARD_DETAIL, model)
        intent.putExtra(Constants.PHRASE_LIST_ITEM_POSITION, cardListPosition)
        activity.startActivityForResult(intent, requestCode)
        notifyItemChanged(cardListPosition)
    }

}