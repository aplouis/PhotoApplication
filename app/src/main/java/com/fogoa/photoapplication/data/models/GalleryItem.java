package com.fogoa.photoapplication.data.models;

public class GalleryItem {
    public String img_uri;
    public String id;
    public String name;
    public String bucket_id;
    public String bucket_name;
    public boolean isUrlImage;

    public GalleryItem() { }

    public GalleryItem(String imgUri, String id, String name, String buckerId, String bucketName) {
        this(imgUri, id, name, buckerId, bucketName, false);
    }

    public GalleryItem(String imgUri, String id, String name, String buckerId, String bucketName, boolean bUrlImage) {
        this.img_uri=imgUri;
        this.id=id;
        this.name=name;
        this.bucket_id=buckerId;
        this.bucket_name=bucketName;
        this.isUrlImage=bUrlImage;
    }

    public String toString() {
        //String os = this.img_uri;
        String os = "name: "+this.name + " uri: " + this.img_uri+ " id: " + this.id+ " bucketid: " + this.bucket_id+ " bucketname: " + this.bucket_name;
        return os;
    }

}
