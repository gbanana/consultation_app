package com.consultation.app.fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageLoader.ImageListener;
import com.android.volley.toolbox.Volley;
import com.consultation.app.R;
import com.consultation.app.activity.CaseInfoNewActivity;
import com.consultation.app.activity.LoginActivity;
import com.consultation.app.activity.SearchConsulationActivity;
import com.consultation.app.exception.ConsultationCallbackException;
import com.consultation.app.listener.ConsultationCallbackHandler;
import com.consultation.app.model.CasesTo;
import com.consultation.app.model.PatientTo;
import com.consultation.app.service.OpenApiService;
import com.consultation.app.util.BitmapCache;
import com.consultation.app.util.ClientUtil;
import com.consultation.app.util.CommonUtil;
import com.consultation.app.util.SharePreferencesEditor;
import com.consultation.app.view.PullToRefreshLayout;
import com.consultation.app.view.PullToRefreshLayout.OnRefreshListener;
import com.consultation.app.view.PullableListView;
import com.consultation.app.view.PullableListView.OnLoadListener;

@SuppressLint({"HandlerLeak", "SimpleDateFormat"})
public class ConsultationDiscussionFragment extends Fragment implements OnLoadListener {

    private View primaryConsultationAllFragment;

    private PullableListView patientListView;

    private List<CasesTo> patientList=new ArrayList<CasesTo>();

    private MyAdapter myAdapter;

    private PatientViewHolder holder;

    private SharePreferencesEditor editor;

    private int page=1;

    private boolean hasMore=true;

    private boolean isInit=false;

    private RequestQueue mQueue;

    private ImageLoader mImageLoader;

    private Handler handler=new Handler() {

        public void handleMessage(Message msg) {
            switch(msg.what) {
                case 0:
                    myAdapter.notifyDataSetChanged();
                    page=1;
                    ((PullToRefreshLayout)msg.obj).refreshFinish(PullToRefreshLayout.SUCCEED);
                    break;
                case 1:
                    if(hasMore) {
                        ((PullableListView)msg.obj).finishLoading();
                    } else {
                        patientListView.setHasMoreData(false);
                    }
                    myAdapter.notifyDataSetChanged();
                    break;
                case 2:
                    page=1;
                    ((PullToRefreshLayout)msg.obj).refreshFinish(PullToRefreshLayout.FAIL);
                    break;
            }
        };
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        primaryConsultationAllFragment=inflater.inflate(R.layout.consulation_list_all_layout, container, false);
        editor=new SharePreferencesEditor(primaryConsultationAllFragment.getContext());
        myAdapter=new MyAdapter();
        mQueue=Volley.newRequestQueue(primaryConsultationAllFragment.getContext());
        mImageLoader=new ImageLoader(mQueue, new BitmapCache());
        initData(1);
        initLayout();
        return primaryConsultationAllFragment;
    }

    public static ConsultationDiscussionFragment getInstance(Context context) {
        return new ConsultationDiscussionFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(!isInit) {
            Map<String, String> parmas=new HashMap<String, String>();
            parmas.put("page", "1");
            parmas.put("rows", "10");
            parmas.put("accessToken", ClientUtil.getToken());
            parmas.put("uid", editor.get("uid", ""));
            parmas.put("userTp", editor.get("userType", ""));
            OpenApiService.getInstance(primaryConsultationAllFragment.getContext()).getPatientCaseList(mQueue, parmas,
                new Response.Listener<String>() {

                    @Override
                    public void onResponse(String arg0) {
                        try {
                            JSONObject responses=new JSONObject(arg0);
                            if(responses.getInt("rtnCode") == 1) {
                                JSONArray infos=responses.getJSONArray("pcases");
                                patientList.clear();
                                for(int i=0; i < infos.length(); i++) {
                                    JSONObject info=infos.getJSONObject(i);
                                    CasesTo pcasesTo=new CasesTo();
                                    pcasesTo.setId(info.getString("id"));
                                    pcasesTo.setStatus(info.getString("status"));
                                    pcasesTo.setStatus_des(info.getString("status_desc"));
                                    pcasesTo.setDestination(info.getString("destination"));
                                    String createTime=info.getString("create_time");
                                    if(createTime.equals("null")) {
                                        pcasesTo.setCreate_time(0);
                                    } else {
                                        pcasesTo.setCreate_time(Long.parseLong(createTime));
                                    }
                                    pcasesTo.setTitle(info.getString("title"));
                                    pcasesTo.setToReadMsgCount(info.getInt("toReadMsgCount"));
                                    pcasesTo.setDepart_id(info.getString("depart_id"));
                                    pcasesTo.setDoctor_userid(info.getString("doctor_userid"));
                                    pcasesTo.setPatient_name(info.getString("patient_name"));
                                    String consult_fee=info.getString("consult_fee");
                                    if(consult_fee.equals("null")) {
                                        pcasesTo.setConsult_fee("0");
                                    } else {
                                        pcasesTo.setConsult_fee(consult_fee);
                                    }
                                    pcasesTo.setDoctor_name(info.getString("doctor_name"));
                                    pcasesTo.setExpert_userid(info.getString("expert_userid"));
                                    pcasesTo.setExpert_name(info.getString("expert_name"));
                                    pcasesTo.setProblem(info.getString("problem"));
                                    pcasesTo.setConsult_tp(info.getString("consult_tp"));
                                    pcasesTo.setOpinion(info.getString("opinion"));
                                    PatientTo patientTo=new PatientTo();
                                    JSONObject pObject=info.getJSONObject("user");
                                    patientTo.setAddress(pObject.getString("address"));
                                    patientTo.setId(pObject.getInt("id") + "");
                                    patientTo.setState(pObject.getString("state"));
                                    patientTo.setTp(pObject.getString("tp"));
                                    patientTo.setUserBalance(pObject.getString("userBalance"));
                                    patientTo.setMobile_ph(pObject.getString("mobile_ph"));
                                    patientTo.setPwd(pObject.getString("pwd"));
                                    patientTo.setReal_name(pObject.getString("real_name"));
                                    patientTo.setSex(pObject.getString("sex"));
                                    patientTo.setBirth_year(pObject.getString("birth_year"));
                                    patientTo.setBirth_month(pObject.getString("birth_month"));
                                    patientTo.setBirth_day(pObject.getString("birth_day"));
                                    patientTo.setIdentity_id(pObject.getString("identity_id"));
                                    patientTo.setArea_province(pObject.getString("area_province"));
                                    patientTo.setArea_city(pObject.getString("area_city"));
                                    patientTo.setArea_county(pObject.getString("area_county"));
                                    patientTo.setIcon_url(pObject.getString("icon_url"));
                                    patientTo.setModify_time(pObject.getString("modify_time"));
                                    pcasesTo.setPatient(patientTo);
                                    patientList.add(pcasesTo);
                                }
                                if(infos.length() == 10) {
                                    patientListView.setHasMoreData(true);
                                } else {
                                    patientListView.setHasMoreData(false);
                                }
                                myAdapter.notifyDataSetChanged();
                            } else if(responses.getInt("rtnCode") == 10004) {
                                Toast.makeText(primaryConsultationAllFragment.getContext(), responses.getString("rtnMsg"),
                                    Toast.LENGTH_SHORT).show();
                                LoginActivity.setHandler(new ConsultationCallbackHandler() {

                                    @Override
                                    public void onSuccess(String rspContent, int statusCode) {
                                        // initData();
                                    }

                                    @Override
                                    public void onFailure(ConsultationCallbackException exp) {
                                    }
                                });
                                startActivity(new Intent(primaryConsultationAllFragment.getContext(), LoginActivity.class));
                            } else {
                                Toast.makeText(primaryConsultationAllFragment.getContext(), responses.getString("rtnMsg"),
                                    Toast.LENGTH_SHORT).show();
                            }
                        } catch(JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError arg0) {
                        Toast.makeText(primaryConsultationAllFragment.getContext(), "网络连接失败,请稍后重试", Toast.LENGTH_SHORT).show();
                    }
                });
        }
        isInit=false;
    }

    public void initData(final int isShow) {
        isInit=true;
        Map<String, String> parmas=new HashMap<String, String>();
        parmas.put("page", "1");
        parmas.put("rows", "10");
        parmas.put("accessToken", ClientUtil.getToken());
        parmas.put("uid", editor.get("uid", ""));
        parmas.put("userTp", editor.get("userType", ""));
        parmas.put("status", "bbs");
        if(isShow == 1) {
            CommonUtil.showLoadingDialog(primaryConsultationAllFragment.getContext());
        }
        OpenApiService.getInstance(primaryConsultationAllFragment.getContext()).getPatientCaseList(mQueue, parmas,
            new Response.Listener<String>() {

                @Override
                public void onResponse(String arg0) {
                    try {
                        JSONObject responses=new JSONObject(arg0);
                        if(responses.getInt("rtnCode") == 1) {
                            JSONArray infos=responses.getJSONArray("pcases");
                            patientList.clear();
                            for(int i=0; i < infos.length(); i++) {
                                JSONObject info=infos.getJSONObject(i);
                                CasesTo pcasesTo=new CasesTo();
                                pcasesTo.setId(info.getString("id"));
                                pcasesTo.setStatus(info.getString("status"));
                                pcasesTo.setStatus_des(info.getString("status_desc"));
                                pcasesTo.setDestination(info.getString("destination"));
                                String createTime=info.getString("create_time");
                                if(createTime.equals("null")) {
                                    pcasesTo.setCreate_time(0);
                                } else {
                                    pcasesTo.setCreate_time(Long.parseLong(createTime));
                                }
                                pcasesTo.setTitle(info.getString("title"));
                                pcasesTo.setToReadMsgCount(info.getInt("toReadMsgCount"));
                                pcasesTo.setDepart_id(info.getString("depart_id"));
                                pcasesTo.setDoctor_userid(info.getString("doctor_userid"));
                                pcasesTo.setPatient_name(info.getString("patient_name"));
                                String consult_fee=info.getString("consult_fee");
                                if(consult_fee.equals("null")) {
                                    pcasesTo.setConsult_fee("0");
                                } else {
                                    pcasesTo.setConsult_fee(consult_fee);
                                }
                                pcasesTo.setDoctor_name(info.getString("doctor_name"));
                                pcasesTo.setExpert_userid(info.getString("expert_userid"));
                                pcasesTo.setExpert_name(info.getString("expert_name"));
                                pcasesTo.setProblem(info.getString("problem"));
                                pcasesTo.setConsult_tp(info.getString("consult_tp"));
                                pcasesTo.setOpinion(info.getString("opinion"));
                                PatientTo patientTo=new PatientTo();
                                JSONObject pObject=info.getJSONObject("user");
                                patientTo.setAddress(pObject.getString("address"));
                                patientTo.setId(pObject.getInt("id") + "");
                                patientTo.setState(pObject.getString("state"));
                                patientTo.setTp(pObject.getString("tp"));
                                patientTo.setUserBalance(pObject.getString("userBalance"));
                                patientTo.setMobile_ph(pObject.getString("mobile_ph"));
                                patientTo.setPwd(pObject.getString("pwd"));
                                patientTo.setReal_name(pObject.getString("real_name"));
                                patientTo.setSex(pObject.getString("sex"));
                                patientTo.setBirth_year(pObject.getString("birth_year"));
                                patientTo.setBirth_month(pObject.getString("birth_month"));
                                patientTo.setBirth_day(pObject.getString("birth_day"));
                                patientTo.setIdentity_id(pObject.getString("identity_id"));
                                patientTo.setArea_province(pObject.getString("area_province"));
                                patientTo.setArea_city(pObject.getString("area_city"));
                                patientTo.setArea_county(pObject.getString("area_county"));
                                patientTo.setIcon_url(pObject.getString("icon_url"));
                                patientTo.setModify_time(pObject.getString("modify_time"));
                                pcasesTo.setPatient(patientTo);
                                patientList.add(pcasesTo);
                            }
                            if(infos.length() == 10) {
                                patientListView.setHasMoreData(true);
                            } else {
                                patientListView.setHasMoreData(false);
                            }
                            if(isShow == 1) {
                                CommonUtil.closeLodingDialog();
                            }
                            myAdapter.notifyDataSetChanged();
                            patientListView.setSelection(0);
                        } else if(responses.getInt("rtnCode") == 10004) {
                            if(isShow == 1) {
                                CommonUtil.closeLodingDialog();
                            }
                            Toast.makeText(primaryConsultationAllFragment.getContext(), responses.getString("rtnMsg"),
                                Toast.LENGTH_SHORT).show();
                            LoginActivity.setHandler(new ConsultationCallbackHandler() {

                                @Override
                                public void onSuccess(String rspContent, int statusCode) {
                                    // initData();
                                }

                                @Override
                                public void onFailure(ConsultationCallbackException exp) {
                                }
                            });
                            startActivity(new Intent(primaryConsultationAllFragment.getContext(), LoginActivity.class));
                        } else {
                            if(isShow == 1) {
                                CommonUtil.closeLodingDialog();
                            }
                            Toast.makeText(primaryConsultationAllFragment.getContext(), responses.getString("rtnMsg"),
                                Toast.LENGTH_SHORT).show();
                        }
                    } catch(JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError arg0) {
                    if(isShow == 1) {
                        CommonUtil.closeLodingDialog();
                    }
                    Toast.makeText(primaryConsultationAllFragment.getContext(), "网络连接失败,请稍后重试", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void initLayout() {
        TextView header_text=(TextView)primaryConsultationAllFragment.findViewById(R.id.header_text);
        header_text.setText("我的讨论区");
        header_text.setTextSize(20);
        TextView searchText=(TextView)primaryConsultationAllFragment.findViewById(R.id.consulation_list_search_text);
        searchText.setTextSize(15);
        LinearLayout searchLayout=(LinearLayout)primaryConsultationAllFragment.findViewById(R.id.consulation_list_search_layout);
        searchLayout.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // 搜索
                Intent intent = new Intent(primaryConsultationAllFragment.getContext(), SearchConsulationActivity.class);
                intent.putExtra("isBBS", true);
                startActivity(intent);
            }
        });
        ((PullToRefreshLayout)primaryConsultationAllFragment.findViewById(R.id.consulation_list_all_refresh_view))
            .setOnRefreshListener(new OnRefreshListener() {

                @Override
                public void onRefresh(final PullToRefreshLayout pullToRefreshLayout) {
                    Map<String, String> parmas=new HashMap<String, String>();
                    parmas.put("page", "1");
                    parmas.put("rows", "10");
                    parmas.put("accessToken", ClientUtil.getToken());
                    parmas.put("uid", editor.get("uid", ""));
                    parmas.put("userTp", editor.get("userType", ""));
                    parmas.put("status", "bbs");
                    OpenApiService.getInstance(primaryConsultationAllFragment.getContext()).getPatientCaseList(mQueue, parmas,
                        new Response.Listener<String>() {

                            @Override
                            public void onResponse(String arg0) {
                                try {
                                    JSONObject responses=new JSONObject(arg0);
                                    if(responses.getInt("rtnCode") == 1) {
                                        JSONArray infos=responses.getJSONArray("pcases");
                                        patientList.clear();
                                        for(int i=0; i < infos.length(); i++) {
                                            JSONObject info=infos.getJSONObject(i);
                                            CasesTo pcasesTo=new CasesTo();
                                            pcasesTo.setId(info.getString("id"));
                                            pcasesTo.setStatus(info.getString("status"));
                                            pcasesTo.setStatus_des(info.getString("status_desc"));
                                            pcasesTo.setDestination(info.getString("destination"));
                                            String createTime=info.getString("create_time");
                                            if(createTime.equals("null")) {
                                                pcasesTo.setCreate_time(0);
                                            } else {
                                                pcasesTo.setCreate_time(Long.parseLong(createTime));
                                            }
                                            pcasesTo.setTitle(info.getString("title"));
                                            pcasesTo.setToReadMsgCount(info.getInt("toReadMsgCount"));
                                            pcasesTo.setDepart_id(info.getString("depart_id"));
                                            pcasesTo.setDoctor_userid(info.getString("doctor_userid"));
                                            String consult_fee=info.getString("consult_fee");
                                            if(consult_fee.equals("null")) {
                                                pcasesTo.setConsult_fee("0");
                                            } else {
                                                pcasesTo.setConsult_fee(consult_fee);
                                            }
                                            pcasesTo.setPatient_name(info.getString("patient_name"));
                                            pcasesTo.setDoctor_name(info.getString("doctor_name"));
                                            pcasesTo.setExpert_userid(info.getString("expert_userid"));
                                            pcasesTo.setExpert_name(info.getString("expert_name"));
                                            pcasesTo.setProblem(info.getString("problem"));
                                            pcasesTo.setConsult_tp(info.getString("consult_tp"));
                                            pcasesTo.setOpinion(info.getString("opinion"));
                                            PatientTo patientTo=new PatientTo();
                                            JSONObject pObject=info.getJSONObject("user");
                                            patientTo.setAddress(pObject.getString("address"));
                                            patientTo.setId(pObject.getInt("id") + "");
                                            patientTo.setState(pObject.getString("state"));
                                            patientTo.setTp(pObject.getString("tp"));
                                            patientTo.setUserBalance(pObject.getString("userBalance"));
                                            patientTo.setMobile_ph(pObject.getString("mobile_ph"));
                                            patientTo.setPwd(pObject.getString("pwd"));
                                            patientTo.setReal_name(pObject.getString("real_name"));
                                            patientTo.setSex(pObject.getString("sex"));
                                            patientTo.setBirth_year(pObject.getString("birth_year"));
                                            patientTo.setBirth_month(pObject.getString("birth_month"));
                                            patientTo.setBirth_day(pObject.getString("birth_day"));
                                            patientTo.setIdentity_id(pObject.getString("identity_id"));
                                            patientTo.setArea_province(pObject.getString("area_province"));
                                            patientTo.setArea_city(pObject.getString("area_city"));
                                            patientTo.setArea_county(pObject.getString("area_county"));
                                            patientTo.setIcon_url(pObject.getString("icon_url"));
                                            patientTo.setModify_time(pObject.getString("modify_time"));
                                            pcasesTo.setPatient(patientTo);
                                            patientList.add(pcasesTo);
                                        }
                                        if(infos.length() == 10) {
                                            patientListView.setHasMoreData(true);
                                        } else {
                                            patientListView.setHasMoreData(false);
                                        }
                                        Message msg=handler.obtainMessage();
                                        msg.what=0;
                                        msg.obj=pullToRefreshLayout;
                                        handler.sendMessage(msg);
                                    } else if(responses.getInt("rtnCode") == 10004) {
                                        Message msg=handler.obtainMessage();
                                        msg.what=2;
                                        msg.obj=pullToRefreshLayout;
                                        handler.sendMessage(msg);
                                        Toast.makeText(primaryConsultationAllFragment.getContext(), responses.getString("rtnMsg"),
                                            Toast.LENGTH_SHORT).show();
                                        LoginActivity.setHandler(new ConsultationCallbackHandler() {

                                            @Override
                                            public void onSuccess(String rspContent, int statusCode) {
                                                // initData();
                                            }

                                            @Override
                                            public void onFailure(ConsultationCallbackException exp) {
                                            }
                                        });
                                        startActivity(new Intent(primaryConsultationAllFragment.getContext(), LoginActivity.class));
                                    } else {
                                        Message msg=handler.obtainMessage();
                                        msg.what=2;
                                        msg.obj=pullToRefreshLayout;
                                        handler.sendMessage(msg);
                                        Toast.makeText(primaryConsultationAllFragment.getContext(), responses.getString("rtnMsg"),
                                            Toast.LENGTH_SHORT).show();
                                    }
                                } catch(JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, new Response.ErrorListener() {

                            @Override
                            public void onErrorResponse(VolleyError arg0) {
                                Message msg=handler.obtainMessage();
                                msg.what=2;
                                msg.obj=pullToRefreshLayout;
                                handler.sendMessage(msg);
                                Toast.makeText(primaryConsultationAllFragment.getContext(), "网络连接失败,请稍后重试", Toast.LENGTH_SHORT)
                                    .show();
                            }
                        });
                }
            });
        patientListView=(PullableListView)primaryConsultationAllFragment.findViewById(R.id.consulation_list_all_listView);
        patientListView.setAdapter(myAdapter);
        patientListView.setOnLoadListener(this);
        patientListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PatientViewHolder holder=(PatientViewHolder)view.getTag();
                holder.countButton.setVisibility(View.INVISIBLE);
                Intent intent=new Intent(primaryConsultationAllFragment.getContext(), CaseInfoNewActivity.class);
                intent.putExtra("caseId", patientList.get(position).getId());
                intent.putExtra("type", 0);
                startActivityForResult(intent, 0);
            }
        });
        patientListView.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                show(primaryConsultationAllFragment.getContext(), arg2);
                return true;
            }
        });
        // MyBroadcastReceiver.setHander(new ConsultationCallbackHandler() {
        //
        // @Override
        // public void onSuccess(String rspContent, int statusCode) {
        // patientList.clear();
        // initData();
        // myAdapter.notifyDataSetChanged();
        // }
        //
        // @Override
        // public void onFailure(ConsultationCallbackException exp) {
        //
        // }
        // });
    }

    // @Override
    // public void onActivityResult(int requestCode, int resultCode, Intent data) {
    // if(resultCode == Activity.RESULT_OK) {
    // patientList.clear();
    // initData();
    // }
    // super.onActivityResult(requestCode, resultCode, data);
    // }

    private class PatientViewHolder {

        ImageView photo;

        TextView titleText;

        TextView doctorText;

        TextView dateText;

        TextView stateText;

        TextView countButton;
    }

    private class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return patientList.size();
        }

        @Override
        public Object getItem(int location) {
            return patientList.get(location);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                holder=new PatientViewHolder();
                convertView=
                    LayoutInflater.from(primaryConsultationAllFragment.getContext()).inflate(
                        R.layout.consulation_primary_list_all_item, null);
                holder.photo=(ImageView)convertView.findViewById(R.id.consulation_primary_list_all_item_image);
                holder.titleText=(TextView)convertView.findViewById(R.id.consulation_primary_list_all_item_title);
                holder.doctorText=(TextView)convertView.findViewById(R.id.consulation_primary_list_all_item_doctor);
                holder.dateText=(TextView)convertView.findViewById(R.id.consulation_primary_list_all_item_date);
                holder.stateText=(TextView)convertView.findViewById(R.id.consulation_primary_list_all_item_state);
                holder.countButton=(TextView)convertView.findViewById(R.id.consulation_primary_list_all_item_image_count);
                convertView.setTag(holder);
            } else {
                holder=(PatientViewHolder)convertView.getTag();
            }
            holder.titleText.setText(patientList.get(position).getTitle());
            holder.titleText.setTextSize(18);
            if(patientList.get(position).getConsult_tp().equals("公开讨论")) {
                holder.doctorText.setText(patientList.get(position).getPatient_name() + "(患者)|"
                    + patientList.get(position).getDoctor_name() + "(初诊)");
            } else {
                holder.doctorText.setText(patientList.get(position).getPatient_name() + "(患者)|"
                    + patientList.get(position).getExpert_name() + "(专家)");
            }
            holder.doctorText.setTextSize(16);
            if(patientList.get(position).getToReadMsgCount() != 0) {
                holder.countButton.setText(patientList.get(position).getToReadMsgCount() + "");
                holder.countButton.setTextSize(12);
                holder.countButton.setVisibility(View.VISIBLE);
            }
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
            String sd=sdf.format(new Date(patientList.get(position).getCreate_time()));
            holder.dateText.setText(sd);
            holder.dateText.setTextSize(14);
            holder.stateText.setText(patientList.get(position).getStatus_des());
            holder.stateText.setTextSize(14);
            final String imgUrl=patientList.get(position).getPatient().getIcon_url();
            holder.photo.setTag(imgUrl);
            holder.photo.setImageResource(R.drawable.photo_patient);
            if(!"null".equals(imgUrl) && !"".equals(imgUrl)) {
                ImageListener listener=
                    ImageLoader.getImageListener(holder.photo, R.drawable.photo_patient, R.drawable.photo_patient);
                mImageLoader.get(imgUrl, listener);
            }
            return convertView;
        }
    }

    private void show(final Context context, final int index) {
        AlertDialog.Builder builder=new AlertDialog.Builder(context);
        builder.setMessage("删除该病例?").setCancelable(false).setPositiveButton("确定", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                Map<String, String> parmas=new HashMap<String, String>();
                parmas.put("caseId", patientList.get(index).getId());
                parmas.put("accessToken", ClientUtil.getToken());
                parmas.put("uid", editor.get("uid", ""));
                CommonUtil.showLoadingDialog(context);
                OpenApiService.getInstance(context).getDeleteCase(mQueue, parmas, new Response.Listener<String>() {

                    @Override
                    public void onResponse(String arg0) {
                        try {
                            JSONObject responses=new JSONObject(arg0);
                            if(responses.getInt("rtnCode") == 1) {
                                patientList.remove(index);
                                myAdapter.notifyDataSetChanged();
                                CommonUtil.closeLodingDialog();
                            } else if(responses.getInt("rtnCode") == 10004) {
                                CommonUtil.closeLodingDialog();
                                Toast.makeText(context, responses.getString("rtnMsg"), Toast.LENGTH_SHORT).show();
                                LoginActivity.setHandler(new ConsultationCallbackHandler() {

                                    @Override
                                    public void onSuccess(String rspContent, int statusCode) {
                                    }

                                    @Override
                                    public void onFailure(ConsultationCallbackException exp) {
                                    }
                                });
                                startActivity(new Intent(context, LoginActivity.class));
                            } else {
                                CommonUtil.closeLodingDialog();
                                Toast.makeText(context, responses.getString("rtnMsg"), Toast.LENGTH_SHORT).show();
                            }
                        } catch(JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError arg0) {
                        CommonUtil.closeLodingDialog();
                        Toast.makeText(context, "网络连接失败,请稍后重试", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).setNegativeButton("取消", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        }).create().show();
    }

    @Override
    public void onLoad(final PullableListView pullableListView) {
        Map<String, String> parmas=new HashMap<String, String>();
        page++;
        parmas.put("page", String.valueOf(page));
        parmas.put("rows", "10");
        parmas.put("accessToken", ClientUtil.getToken());
        parmas.put("uid", editor.get("uid", ""));
        parmas.put("userTp", editor.get("userType", ""));
        parmas.put("status", "bbs");
        OpenApiService.getInstance(primaryConsultationAllFragment.getContext()).getPatientCaseList(mQueue, parmas,
            new Response.Listener<String>() {

                @Override
                public void onResponse(String arg0) {
                    try {
                        JSONObject responses=new JSONObject(arg0);
                        if(responses.getInt("rtnCode") == 1) {
                            JSONArray infos=responses.getJSONArray("pcases");
                            for(int i=0; i < infos.length(); i++) {
                                JSONObject info=infos.getJSONObject(i);
                                CasesTo pcasesTo=new CasesTo();
                                pcasesTo.setId(info.getString("id"));
                                pcasesTo.setStatus(info.getString("status"));
                                pcasesTo.setStatus_des(info.getString("status_desc"));
                                pcasesTo.setDestination(info.getString("destination"));
                                String createTime=info.getString("create_time");
                                if(createTime.equals("null")) {
                                    pcasesTo.setCreate_time(0);
                                } else {
                                    pcasesTo.setCreate_time(Long.parseLong(createTime));
                                }
                                pcasesTo.setTitle(info.getString("title"));
                                pcasesTo.setToReadMsgCount(info.getInt("toReadMsgCount"));
                                pcasesTo.setDepart_id(info.getString("depart_id"));
                                pcasesTo.setDoctor_userid(info.getString("doctor_userid"));
                                String consult_fee=info.getString("consult_fee");
                                if(consult_fee.equals("null")) {
                                    pcasesTo.setConsult_fee("0");
                                } else {
                                    pcasesTo.setConsult_fee(consult_fee);
                                }
                                pcasesTo.setPatient_name(info.getString("patient_name"));
                                pcasesTo.setDoctor_name(info.getString("doctor_name"));
                                pcasesTo.setExpert_userid(info.getString("expert_userid"));
                                pcasesTo.setExpert_name(info.getString("expert_name"));
                                pcasesTo.setProblem(info.getString("problem"));
                                pcasesTo.setConsult_tp(info.getString("consult_tp"));
                                pcasesTo.setOpinion(info.getString("opinion"));
                                PatientTo patientTo=new PatientTo();
                                JSONObject pObject=info.getJSONObject("user");
                                patientTo.setAddress(pObject.getString("address"));
                                patientTo.setId(pObject.getInt("id") + "");
                                patientTo.setState(pObject.getString("state"));
                                patientTo.setTp(pObject.getString("tp"));
                                patientTo.setUserBalance(pObject.getString("userBalance"));
                                patientTo.setMobile_ph(pObject.getString("mobile_ph"));
                                patientTo.setPwd(pObject.getString("pwd"));
                                patientTo.setReal_name(pObject.getString("real_name"));
                                patientTo.setSex(pObject.getString("sex"));
                                patientTo.setBirth_year(pObject.getString("birth_year"));
                                patientTo.setBirth_month(pObject.getString("birth_month"));
                                patientTo.setBirth_day(pObject.getString("birth_day"));
                                patientTo.setIdentity_id(pObject.getString("identity_id"));
                                patientTo.setArea_province(pObject.getString("area_province"));
                                patientTo.setArea_city(pObject.getString("area_city"));
                                patientTo.setArea_county(pObject.getString("area_county"));
                                patientTo.setIcon_url(pObject.getString("icon_url"));
                                patientTo.setModify_time(pObject.getString("modify_time"));
                                pcasesTo.setPatient(patientTo);
                                patientList.add(pcasesTo);
                            }
                            if(infos.length() == 10) {
                                hasMore=true;
                            } else {
                                hasMore=false;
                            }
                            Message msg=handler.obtainMessage();
                            msg.what=1;
                            msg.obj=pullableListView;
                            handler.sendMessage(msg);
                        } else if(responses.getInt("rtnCode") == 10004) {
                            hasMore=true;
                            Message msg=handler.obtainMessage();
                            msg.what=1;
                            msg.obj=pullableListView;
                            handler.sendMessage(msg);
                            Toast.makeText(primaryConsultationAllFragment.getContext(), responses.getString("rtnMsg"),
                                Toast.LENGTH_SHORT).show();
                            LoginActivity.setHandler(new ConsultationCallbackHandler() {

                                @Override
                                public void onSuccess(String rspContent, int statusCode) {
                                    // initData();
                                }

                                @Override
                                public void onFailure(ConsultationCallbackException exp) {
                                }
                            });
                            startActivity(new Intent(primaryConsultationAllFragment.getContext(), LoginActivity.class));
                        } else {
                            hasMore=true;
                            Message msg=handler.obtainMessage();
                            msg.what=1;
                            msg.obj=pullableListView;
                            handler.sendMessage(msg);
                            Toast.makeText(primaryConsultationAllFragment.getContext(), responses.getString("rtnMsg"),
                                Toast.LENGTH_SHORT).show();
                        }
                    } catch(JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError arg0) {
                    hasMore=true;
                    Message msg=handler.obtainMessage();
                    msg.what=1;
                    msg.obj=pullableListView;
                    handler.sendMessage(msg);
                    Toast.makeText(primaryConsultationAllFragment.getContext(), "网络连接失败,请稍后重试", Toast.LENGTH_SHORT).show();
                }
            });
    }
}