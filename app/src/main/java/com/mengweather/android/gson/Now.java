package com.mengweather.android.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by wumen on 2017/3/1.
 */

public class Now
{
    @SerializedName("tmp")
    public String temperature;

    @SerializedName("cond")
    public More more;

    public class More
    {
        @SerializedName("txt")
        public String info;
    }
}
