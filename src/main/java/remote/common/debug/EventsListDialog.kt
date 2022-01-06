package remote.common.debug

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import remote.common.ui.BaseDialog
import kotlinx.android.synthetic.main.dialog_events.*
import kotlinx.android.synthetic.main.item_events.view.*
import kotlinx.android.synthetic.main.item_param.view.*
import remote.common.firebase.analytics.EventCache
import remote.common.firebase.analytics.EventItem
import remote.common.ui.BaseRcvAdapter
import tv.remote.platform.R

class EventsListDialog: BaseDialog() {
    override fun getLayoutRes(): Int {
        return R.layout.dialog_events
    }

    override fun getGravity(): Int {
        return Gravity.CENTER
    }

    override fun getWidthPaddingDp(): Int {
        return 100
    }

    override fun getLayoutHeight(): Int {
        return ViewGroup.LayoutParams.MATCH_PARENT
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(true)
        val adapter = EventListAdapter(arrayListOf())
        rv_list.adapter = adapter
        rv_list.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
        adapter.setDatas(EventCache.getLogList())
        tv_close.setOnClickListener {
            dismiss()
        }
    }

    class EventListAdapter(dataList: List<EventItem>): BaseRcvAdapter<EventItem>(R.layout.item_events, dataList) {
        override fun onBindView(itemView: View, position: Int, data: EventItem) {
            itemView.tv_time.text = data.eventTime
            itemView.tv_events_id.text = data.eventId
            (itemView.rv_params.adapter as ParamsListAdapter).setDatas(data.eventParams)
        }

        override fun onCreateView(itemView: View) {
            val adapter = ParamsListAdapter(arrayListOf())
            itemView.rv_params.adapter = adapter
            itemView.rv_params.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.VERTICAL, false)
        }
    }

    class ParamsListAdapter(dataList: List<Pair<String?, Any?>>): BaseRcvAdapter<Pair<String?, Any?>>(R.layout.item_param, dataList) {
        override fun onBindView(itemView: View, position: Int, data: Pair<String?, Any?>) {
            itemView.tv_param.text = "- ${data.first}: ${data.second}"
        }
    }
}