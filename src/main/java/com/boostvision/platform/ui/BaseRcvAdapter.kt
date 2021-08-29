package com.boostvision.platform.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

abstract class BaseRcvAdapter<T>(private var layoutId: Int, private var dataList:List<T>):
    RecyclerView.Adapter<BaseViewHolder?>() {

    protected abstract fun onBindView(itemView: View, position: Int, data: T)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val itemView: View = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return BaseViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        onBindView(holder.itemView, position, dataList[position])
    }

    override fun getItemCount(): Int {
        return dataList.size
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
                return dataList[oldItemPosition] == newDataList[newItemPosition]
            }
        })
    }
}