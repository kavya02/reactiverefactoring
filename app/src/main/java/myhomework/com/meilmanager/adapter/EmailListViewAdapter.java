package myhomework.com.meilmanager.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

import myhomework.com.meilmanager.ReadActivity;
import myhomework.com.meilmanager.model.EmailData;
import myhomework.com.meilmanager.R;

public class EmailListViewAdapter extends BaseAdapter {

    ArrayList<EmailData> marrList;
    private LayoutInflater layoutInflater;
    private Context mContext;

    public EmailListViewAdapter(Context context, ArrayList listData) {
        this.marrList = listData;
        layoutInflater = LayoutInflater.from(context);
        mContext = context;
    }

    public void swapData(ArrayList<EmailData> newData) {
        this.marrList = newData;
        notifyDataSetChanged();
    }

    public int getCount() {
        return marrList.size();
    }
    @Override
    public Object getItem(int position) {
        return marrList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {

            convertView = layoutInflater.inflate(R.layout.row_email, null);

            holder = new ViewHolder();
            holder.txtSender = (TextView) convertView.findViewById(R.id.txtSender);
            holder.txtSubject = (TextView) convertView.findViewById(R.id.txtSubject);
            holder.btnTTL = (Button) convertView.findViewById(R.id.btnTTL);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final EmailData newsItem = marrList.get(position);
        holder.txtSender.setText(newsItem.getSender());
        holder.txtSubject.setText(newsItem.getSubject());


        holder.btnTTL.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Intent myIntent = new Intent(mContext, ReadActivity.class);
                myIntent.putExtra("EmailID", newsItem.getEmailID());
                mContext.startActivity(myIntent);
            }
        });

        return convertView;
    }

//row datas to viewed in a line recored
    static class ViewHolder {
        protected TextView txtSender, txtSubject;
        protected Button btnTTL;
    }
}
