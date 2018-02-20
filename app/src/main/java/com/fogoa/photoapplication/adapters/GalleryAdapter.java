package com.fogoa.photoapplication.adapters;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.fogoa.photoapplication.R;
import com.fogoa.photoapplication.data.listeners.OnLoadMore;
import com.fogoa.photoapplication.data.models.GalleryItem;
import com.fogoa.photoapplication.misc.ImageDownloaderCache;

import java.util.ArrayList;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {
    private static final String TAG = GalleryAdapter.class.getSimpleName();
    private final Context context;
    private final ArrayList<GalleryItem> rvItemList;
    private ImageDownloaderCache imgDownloader;
    private final OnLoadMore mLoadMoreListener;
    private boolean bSelMultiple = false;
    private ArrayList<GalleryItem> rvItemListSelected = new ArrayList<GalleryItem>();
    private static final int SELECT_MAX = 6;

    public GalleryAdapter(Context context, ArrayList<GalleryItem> items, OnLoadMore listener) {
        this(context, items, listener, false);
    }

    public GalleryAdapter(Context context, ArrayList<GalleryItem> items, OnLoadMore listener, boolean selmulti) {
        this.context = context;
        this.rvItemList = items;
        imgDownloader = new ImageDownloaderCache();
        mLoadMoreListener = listener;
        bSelMultiple = selmulti;
    }

    public ArrayList getSelectedItems() {
        return rvItemListSelected;
    }

    @Override
    public int getItemCount() {
        return rvItemList.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_gallery_item, parent, false);
        int layoutId = R.layout.list_gallery_item;
        if (bSelMultiple) {
            layoutId = R.layout.list_gallery_item_multisel;
        }
        //View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_gallery_item_multisel, parent, false);
        // set the view's size, margins, paddings and layout parameters

        ViewHolder vh = new ViewHolder(view);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = rvItemList.get(position);

        if (holder.mItem.img_uri != null && !holder.mItem.img_uri.isEmpty()) {

            int valueInPx = context.getResources().getDimensionPixelSize(R.dimen.gallery_image_width);
            //float valueInDp = context.getResources().getDimension(R.dimen.gallery_image_width);
            //int valueInPx = context.getResources().getDimensionPixelOffset(R.dimen.gallery_image_width);

            holder.ivGalleryItem.setImageResource(R.drawable.ic_photo_black_48dp);
            imgDownloader.loadBitmapPath(holder.mItem.img_uri, holder.ivGalleryItem, valueInPx);
            holder.ivGalleryItem.setOnClickListener(ivGalleryItem_OnClickListener);
            if (bSelMultiple) {
                holder.ivGalleryItem.setOnLongClickListener(ivGalleryItem_OnLongClickListener);
            }
            holder.ivGalleryItem.setTag(position);

            holder.rlSelectedBorder.setVisibility(View.GONE);
            if (bSelMultiple) {
                int itemIdx = rvItemListSelected.indexOf(holder.mItem);
                if (itemIdx > -1) {
                    holder.tvSelNum.setText(String.valueOf(itemIdx+1));
                    holder.rlSelectedBorder.setVisibility(View.VISIBLE);
                }
            }

        }

        //check for last item and load more if displayed
        if ((position >= getItemCount() - 1)) {
            mLoadMoreListener.onLoadMore();
        }

    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        public GalleryItem mItem;
        public final View mView;
        public final ImageView ivGalleryItem;
        public final RelativeLayout rlSelectedBorder;
        public final TextView tvSelNum;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            ivGalleryItem = (ImageView) view.findViewById(R.id.ivGalleryItem);
            rlSelectedBorder = (RelativeLayout) view.findViewById(R.id.rlSelectedBorder);
            tvSelNum = (TextView) view.findViewById(R.id.tvSelNum);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mItem.toString() + "'";
        }
    }

    public AppCompatActivity getActivity() {
        return (AppCompatActivity)context;
    }

    private View.OnClickListener ivGalleryItem_OnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            int pos = (Integer)v.getTag();
            GalleryItem item = rvItemList.get(pos);

            if (bSelMultiple) {
                int itemIdx = rvItemListSelected.indexOf(item);
                if (itemIdx > -1) {
                    rvItemListSelected.remove(itemIdx);
                    notifyItemChanged(pos);

                    //update the numbers on the previously selected images
                    for (GalleryItem sgi : rvItemListSelected) {
                        int sgiIdx = rvItemList.indexOf(sgi);
                        if (sgiIdx > -1) {
                            notifyItemChanged(sgiIdx);
                        }
                    }
                } else {
                    if (rvItemListSelected.size() < SELECT_MAX) {
                        rvItemListSelected.add(item);
                        notifyItemChanged(pos);
                    }
                }
            } else {
                Intent data = new Intent();
                data.putExtra("image_path", item.img_uri);
                getActivity().setResult(getActivity().RESULT_OK, data);
                getActivity().finish();
            }

        }
    };

    private View.OnLongClickListener ivGalleryItem_OnLongClickListener = new View.OnLongClickListener() {
        public boolean onLongClick(View v) {
            int pos = (Integer)v.getTag();
            GalleryItem item = rvItemList.get(pos);
            if (item!=null) {
                Intent data = new Intent();
                data.putExtra("image_path", item.img_uri);
                getActivity().setResult(getActivity().RESULT_OK, data);
                getActivity().finish();
                return true;
            }
            return false;
        }
    };


}
