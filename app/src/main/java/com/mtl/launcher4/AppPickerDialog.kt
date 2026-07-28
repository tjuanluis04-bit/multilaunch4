package com.mtl.launcher4

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

object AppPickerDialog {

    fun show(context: Context, onPicked: (SlotApp) -> Unit) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)
            .sortedBy { it.loadLabel(pm).toString().lowercase() }

        val adapter = object : ArrayAdapter<ResolveInfo>(context, R.layout.item_app, resolveInfos) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.item_app, parent, false)
                val info = resolveInfos[position]
                view.findViewById<TextView>(R.id.label).text = info.loadLabel(pm)
                view.findViewById<ImageView>(R.id.icon).setImageDrawable(info.loadIcon(pm))
                return view
            }
        }

        AlertDialog.Builder(context)
            .setTitle("Elige una app")
            .setAdapter(adapter) { _, which ->
                val info = resolveInfos[which]
                val slotApp = SlotApp(
                    packageName = info.activityInfo.packageName,
                    activityName = info.activityInfo.name,
                    label = info.loadLabel(pm).toString()
                )
                onPicked(slotApp)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
