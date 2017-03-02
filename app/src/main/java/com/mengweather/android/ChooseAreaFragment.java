package com.mengweather.android;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mengweather.android.db.City;
import com.mengweather.android.db.Country;
import com.mengweather.android.db.Province;
import com.mengweather.android.util.HttpUtil;
import com.mengweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by wumen on 2017/2/28.
 */

public class ChooseAreaFragment extends Fragment
{
    public static final int LEVEL_PROVINCE = 0; //省

    public static final int LEVEL_CITY = 1;   //市

    public static final int LEVEL_COUNTY = 2;  //县

    private ProgressDialog progressDialog;

    private TextView titleText;  //标题栏中的题目

    private Button backButton;   //返回按钮

    private ListView listView;

    private ArrayAdapter<String> adapter;

    private List<String> dataList = new ArrayList<>();

    private List<Province> provinceList;  //省列表

    private List<City> cityList;     //市列表

    private List<Country> countryList;  //县列表

    private Province selectedProvince;  //选中的省份

    private City selectedCity;   //选中的市

    private int currentLevel;  //当前选中的级别

    private static final String TAG = "ChooseAreaFragment";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.choose_area,container,false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<String>(getContext(),android.R.layout.simple_list_item_1,
                dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if (currentLevel == LEVEL_PROVINCE)
                {
                    selectedProvince = provinceList.get(position);
                    queryCities();
                }
                else if (currentLevel == LEVEL_CITY)
                {
                    selectedCity = cityList.get(position);
                    queryCounties();
                }
                else if (currentLevel == LEVEL_COUNTY)
                {
                    Intent intent = new Intent(getActivity(),WeatherActivity.class);
                    intent.putExtra("weatherId",countryList.get(position).getWeatherId());
                    startActivity(intent);
                    getActivity().finish();
                }
            }
        });

        //返回按钮的点击事件
        backButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (currentLevel == LEVEL_COUNTY)
                {
                    queryCities();
                }
                else if (currentLevel == LEVEL_CITY)
                {
                    queryProvinces();
                }
            }
        });

        queryProvinces();
    }

    /**
     *
     */
    private void queryProvinces()
    {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);//从数据库中取出全部数据
        if (provinceList.size() > 0)
        {
            dataList.clear();
            for (Province province : provinceList)
            {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        }
        else
        {
            //将数据查询的接口传递给服务器查询
            String address = "http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }

    private void queryCities()
    {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid = ?",String.valueOf(selectedProvince
                .getId())).find(City.class);
        if (cityList.size() > 0)
        {
            dataList.clear();
            for (City city : cityList)
            {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        }
        else
        {
            //省份的id
            int provinceCode = selectedProvince.getProvinceCode();
            //对应id省份下面的全部市
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address,"city");
        }
    }

    private void queryCounties()
    {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countryList = DataSupport.where("cityid = ?",
                String.valueOf(selectedCity.getId())).find(Country.class);
        if (countryList.size() > 0)
        {
            dataList.clear();
            for (Country county : countryList)
            {
                dataList.add(county.getCountryName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        }
        else
        {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromServer(address,"county");
        }
    }

    /**
     * 从服务器查询数据
     * @param address
     * @param type
     */
    private void queryFromServer(String address,final String type)
    {
        showProgressDialog(); //显示进度条
        HttpUtil.sendOkHttpRequest(address, new Callback()
        {
            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                String responseText = response.body().string(); //查询的数据
                boolean result = false;
                if ("province".equals(type))
                {
                    //解析省的数据并保存到数据库
                    result = Utility.handleProvinceResponse(responseText);
                }
                else if ("city".equals(type))
                {
                    //将解析的市的数据保存到数据库
                    result = Utility.handleCityResponse(responseText,selectedProvince.getId());
                }
                else if ("county".equals(type))
                {
                    //将解析的县的数据保存到数据库
                    result = Utility.handleCountyResponse(responseText,selectedCity.getId());
                }

                if (result)
                {
                    //切换到主线程
                    getActivity().runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            //关闭进度条
                            closeProgressDialog();
                            if ("province".equals(type))
                            {
                                queryProvinces();
                            }
                            else if ("city".equals(type))
                            {
                                queryCities();
                            }
                            else if ("county".equals(type))
                            {
                                queryCounties();
                            }
                        }
                    });
                }
            }
            @Override
            public void onFailure(Call call, IOException e)
            {
                getActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void showProgressDialog()
    {
        if (progressDialog == null)
        {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void closeProgressDialog()
    {
        if (progressDialog != null)
        {
            progressDialog.dismiss();
        }
    }
}
