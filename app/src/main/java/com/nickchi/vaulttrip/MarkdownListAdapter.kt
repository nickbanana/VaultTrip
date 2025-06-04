import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nickchi.vaulttrip.MarkdownFile
import com.nickchi.vaulttrip.MarkdownViewerActivity

class MarkdownListAdapter(
    private val items: List<MarkdownFile>,
    private val context: Context
) : RecyclerView.Adapter<MarkdownListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.textView.text = item.name
        holder.itemView.setOnClickListener {
            val intent = Intent(context, MarkdownViewerActivity::class.java)
            intent.putExtra("uri", item.uri.toString())
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1)
    }
}