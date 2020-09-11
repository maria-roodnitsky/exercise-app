package com.example.myruns.Model;

import android.os.Parcel;
import android.os.Parcelable;

/** MYRUNS2: A ManualEntryStructure stores two pieces of information: a title for the data, and the data itself
 *  EDIT 3/23: Converted to a parcelable for ease in saving data between instance states
 */
public class ManualEntryStructure implements Parcelable {
    // Title and data for an entry object
    String title;
    String data;

     static final Parcelable.Creator<ManualEntryStructure> CREATOR
            = new Parcelable.Creator<ManualEntryStructure>() {
        public ManualEntryStructure createFromParcel(Parcel in) {
            return new ManualEntryStructure(in);
        }


        public ManualEntryStructure[] newArray(int size) {
            return new ManualEntryStructure[size];
        }
    };


    /**
     * ManualEntryStructure constructor, sets private variables
     * @param title title of entry
     * @param data  data in entry
     */
    public ManualEntryStructure(String title, String data){
        this.title = title;
        this.data = data;
    }

    /**
     * Getter for title
     * @return title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Setter for the title
     * @param title title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Getter for data
     * @return data
     */
    public String getData() {
        return data;
    }

    /**
     * Setter for Data
     * @param data data
     */
    public void setData(String data) {
        this.data = data;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /** save object in parcel */
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(title);
        out.writeString(data);
    }


    /** recreate object from parcel */
    private ManualEntryStructure(Parcel in) {
        title = in.readString();
        data= in.readString();
    }
}
