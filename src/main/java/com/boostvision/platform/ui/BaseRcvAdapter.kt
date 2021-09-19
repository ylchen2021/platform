package com.boostvision.platform.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView


abstract class BaseRcvAdapter<T: Any>(private var layoutId: Int, private var dataList:List<T>):
    RecyclerView.Adapter<BaseViewHolder?>() {

    private var onItemClickListener: ((View, T)->Unit)? = null

    private var itemComparator: ((T, T)->Boolean) = { a, b ->
        false
    }

    protected abstract fun onBindView(itemView: View, position: Int, data: T)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val itemView: View = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return BaseViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        onBindView(holder.itemView, position, dataList[position])
        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(holder.itemView, dataList[position])
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    fun setOnItemClickListener(listener: ((View, T)->Unit)) {
        onItemClickListener = listener
    }

    fun setItemComparator(comparator: ((T, T)->Boolean)) {
        itemComparator = comparator
    }

    fun setDatas(newList: List<T>) {
        val diff = calculateDiff(newList)
        this.dataList = newList
        diff.dispatchUpdatesTo(this)
    }

    private fun calculateDiff(newDataList: List<T>): DiffUtil.DiffResult {
        return DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return true
            }

            override fun getOldListSize(): Int {
                return dataList.size
            }

            override fun getNewListSize(): Int {
                return newDataList.size
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return itemComparator.invoke(dataList[oldItemPosition], newDataList[newItemPosition])
            }
        })
    }
}