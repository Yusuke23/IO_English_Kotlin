package com.example.ioenglish.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ioenglish.databinding.ItemLabelColorBinding

class LabelColorListItemsAdapter (private val context: Context,
                                  private val list: ArrayList<String>,
                                  private val mSelectedColor: String):
    RecyclerView.Adapter<LabelColorListItemsAdapter.MyLabelColorListItemsViewHolder>() {

    var onItemClickListener: OnItemClickListener? = null

    class MyLabelColorListItemsViewHolder(val binding: ItemLabelColorBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyLabelColorListItemsViewHolder {
        return MyLabelColorListItemsViewHolder(
            ItemLabelColorBinding.inflate(LayoutInflater.from(context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: MyLabelColorListItemsViewHolder, position: Int) {
        val item = list[position]
        if (holder is MyLabelColorListItemsViewHolder)
            holder.binding.viewMain.setBackgroundColor(Color.parseColor(item))
        if (item == mSelectedColor) {
            holder.binding.ivSelectedColor.visibility = View.VISIBLE
        } else {
            holder.binding.ivSelectedColor.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            if (onItemClickListener != null) {
                onItemClickListener!!.onClick(position, item)
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    interface OnItemClickListener {
        fun onClick(position: Int, color: String)
    }
}
