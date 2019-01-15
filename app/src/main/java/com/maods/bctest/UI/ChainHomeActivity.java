package com.maods.bctest.UI;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.maods.bctest.ChainCommonOperations;
import com.maods.bctest.EOS.EOSOperations;
import com.maods.bctest.EOS.EOSUtils;
import com.maods.bctest.GlobalConstants;
import com.maods.bctest.GlobalUtils;
import com.maods.bctest.R;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.plactal.eoscommander.data.wallet.EosWallet;
import io.plactal.eoscommander.data.wallet.EosWalletManager;

/**
 * Created by MAODS on 2018/7/17.
 */

public class ChainHomeActivity extends Activity {
    private static final String TAG="ChainHomeActivity";
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE=1;
    private static final int MAX_DISPLAY_AVAILABLE_SERVERS=5;

    private static final String GET_CHAIN_INFO="get_chain_info";
    private static final String[] BTC_actions=new String[]{};
    private static final String[] ETH_actions=new String[]{};
    private static final String[] EOS_actions=new String[]{
            EOSOperations.FUNCTION_MY_PROPERTY,
            EOSOperations.FUNCTION_LIST_WALLETS,
            EOSOperations.ACTION_GET_RAM_PRICE,
            EOSOperations.FUNCTION_RAM_TRADE_DEFINE_PRICE,
            EOSOperations.ACTION_BUYRAMBYTES,
            EOSOperations.ACTION_SELLRAM,
            EOSOperations.ACTION_DELEGATEBW,
            EOSOperations.ACTION_UNDELEGATEBW,
            EOSOperations.ACTION_TRANSFER,
            EOSOperations.FUNCTION_BROWSER,
            EOSOperations.FUNCTION_GET_AVAILABLE_BP_API_SERVER,
            EOSOperations.FUNCTION_GET_AVAILABLE_HISTORY_API_SERVER,
            EOSOperations.ACTION_GET_ACCOUNT,
            EOSOperations.ACTION_GET_TRANSACTION,
            EOSOperations.ACTION_GET_ABI,
            EOSOperations.ACTION_GET_INFO,
            EOSOperations.ACTION_GET_ACTIONS,
            EOSOperations.ACTION_GET_PRODUCERS,
            EOSOperations.ACTION_GET_BLOCK,
            EOSOperations.ACTION_GET_CODE,
            EOSOperations.ACTION_GET_TABLE_ROWS,
            EOSOperations.ACTION_JSON_TO_BIN,
            EOSOperations.ACTION_BIN_TO_JSON,
            EOSOperations.FUNCTION_GET_PRICE
    };
    private static final String[] Fabric_actions=new String[]{};

    String mTarget;
    String[] mTargetActions;
    private ChainCommonOperations mCommonOps=null;
    private ArrayList<EosWallet.Status> mWalletStatus;

    private LinearLayout mContentView;
    private TextView mTitleView;
    private TextView mInfoView;
    private ListView mListView;
    private TextView mEmptyView;
    private TextView mCurrNetView;
    private Button mChooseNetView;
    private ArrayAdapter<String> mAdapter;
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.chain_home);
        mContentView=(LinearLayout)findViewById(R.id.container);
        mTitleView=(TextView)findViewById(R.id.title);
        mInfoView=(TextView)findViewById(R.id.info);
        mEmptyView=(TextView)findViewById(R.id.empty);
        mListView=(ListView)findViewById(R.id.list);
        mCurrNetView=findViewById(R.id.curr_net);
        mCurrNetView.setText(EOSUtils.getCurrentNet());
        mChooseNetView=findViewById(R.id.choose_net);
        mChooseNetView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onChooseNetClicked();
            }
        });

        Intent intent=getIntent();
        if(intent.hasExtra(GlobalConstants.EXTRA_KEY_CHAIN)) {
            mTarget = intent.getStringExtra(GlobalConstants.EXTRA_KEY_CHAIN);
        }else {
            mTarget = GlobalConstants.EOS;
        }
        switch(mTarget) {
            case GlobalConstants.BTC:
                mTargetActions = BTC_actions;
                break;
            case GlobalConstants.ETH:
                mTargetActions = ETH_actions;
                break;
            case GlobalConstants.EOS:
                mTargetActions = EOS_actions;
                mCommonOps=new EOSOperations();
                break;
            case GlobalConstants.FABRIC:
                mTargetActions = Fabric_actions;
                break;
            default:
                mTargetActions = EOS_actions;
                break;
        }

        if(mTargetActions.length==0){
            mListView.setVisibility(View.GONE);
        }else{
            mEmptyView.setVisibility(View.GONE);
            mAdapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,mTargetActions);
            mListView.setAdapter(mAdapter);
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    handleOnItemClickInUIThread(view,position);
                }
            });
        }

        if(mCommonOps!=null){
            Thread t=new Thread(new Runnable() {
                @Override
                public void run() {
                    final List<String> serverNodes=mCommonOps.getServerNode();
                    ChainHomeActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateInfo(serverNodes);
                        }
                    });
                }
            });
            t.start();
        }
        checkAndLoadWallets();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults){
        return;
    }

    private void checkAndLoadWallets(){
        //only support EOS now.
        if(mTarget!= GlobalConstants.EOS){
            return;
        }
        EosWalletManager manager=EosWalletManager.getInstance(this);
        //openAllWallets();
        mWalletStatus= manager.listWallets(null);
        int size=mWalletStatus.size();
        if(size==0){
            GlobalUtils.showAlertMsg(ChainHomeActivity.this,R.string.no_wallet_alert);
        }else{
            SharedPreferences pref= PreferenceManager.getDefaultSharedPreferences(this);
            boolean rememberPswd=pref.getBoolean(EOSUtils.REMEMBER_WALLET_PSWD,false);
            if(rememberPswd){
                for(int i=0;i<mWalletStatus.size();i++){
                    EosWallet.Status status=mWalletStatus.get(i);
                    if(status.locked){
                        String name=status.walletName;
                        String pswd=pref.getString(name,null);
                        if(!TextUtils.isEmpty(pswd)){
                            manager.getWallet(name).unlock(pswd);
                        }
                    }
                }
            }
        }
    }

    private void updateInfo(List<String>servers){
        if(servers.size()==0){
            mInfoView.setText(R.string.no_server_available);
        }else{
            StringBuilder sb=new StringBuilder();
            int serverCount=servers.size();
            for(int i=0;i<serverCount && i<MAX_DISPLAY_AVAILABLE_SERVERS;i++){
                sb.append(servers.get(i)+"\n");
            }
            if(serverCount>MAX_DISPLAY_AVAILABLE_SERVERS){
                sb.append("...");
            }
            String serversStr=String.format(getResources().getString(R.string.available_servers),sb.toString());
            mInfoView.setText(serversStr);
        }
    }

    private void handleOnItemClickInUIThread(View view,int position){
        switch(mTarget){
            case GlobalConstants.EOS:{
                switch(mTargetActions[position]){
                    case EOSOperations.ACTION_GET_INFO:
                    case EOSOperations.ACTION_GET_PRODUCERS:
                    case EOSOperations.FUNCTION_GET_AVAILABLE_BP_API_SERVER:
                    case EOSOperations.ACTION_GET_ACCOUNT:
                    case EOSOperations.ACTION_GET_BLOCK:
                    case EOSOperations.ACTION_GET_ABI:
                    case EOSOperations.ACTION_GET_CODE:
                    case EOSOperations.ACTION_GET_TABLE_ROWS:
                    case EOSOperations.ACTION_GET_RAM_PRICE:
                    case EOSOperations.FUNCTION_CREATE_WALLET:
                    case EOSOperations.ACTION_JSON_TO_BIN:
                    case EOSOperations.ACTION_TRANSFER:
                    case EOSOperations.ACTION_BUYRAMBYTES:
                    case EOSOperations.ACTION_SELLRAM:
                    case EOSOperations.ACTION_DELEGATEBW:
                    case EOSOperations.ACTION_UNDELEGATEBW:
                    case EOSOperations.ACTION_GET_ACTIONS:
                    case EOSOperations.ACTION_GET_TRANSACTION:
                    case EOSOperations.ACTION_BIN_TO_JSON:
                    case EOSOperations.FUNCTION_GET_PRICE:
                    case EOSOperations.FUNCTION_MY_PROPERTY:
                    case EOSOperations.FUNCTION_GET_AVAILABLE_HISTORY_API_SERVER:
                        startEOSGetInfo(mTargetActions[position]);
                        break;
                    case EOSOperations.FUNCTION_LIST_WALLETS:
                        startEOSList(mTargetActions[position]);
                        break;
                    case EOSOperations.FUNCTION_BROWSER:
                        startEOSBrowser();
                        break;
                    case EOSOperations.FUNCTION_RAM_TRADE_DEFINE_PRICE:
                        startRamTrade();
                        break;
                    default:
                        break;
                }
                break;
            }
            default:
                break;
        }
    }
    private void startEOSGetInfo(String action){
        Intent intent=new Intent();
        intent.setClass(this,EOSInfoActivity.class);
        intent.putExtra(GlobalConstants.EXTRA_KEY_ACTION,action);
        startActivity(intent);
    }

    private void startEOSList(String action){
        Intent intent=new Intent();
        intent.setClass(this,EOSListActivity.class);
        intent.putExtra(GlobalConstants.EXTRA_KEY_ACTION,action);
        startActivity(intent);
    }
    private void startEOSBrowser(){
        Intent intent=new Intent();
        intent.setClass(this,EOSBrowser.class);
        startActivity(intent);
    }

    private void startRamTrade(){
        Intent intent=new Intent();
        intent.setClass(this,EOSRamTradeActivity.class);
        startActivity(intent);
    }

    private void onChooseNetClicked(){
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("Choose net willing to operate");
        View view=View.inflate(this,R.layout.choose_net,null);
        builder.setView(view);
        final AlertDialog dialog=builder.create();
        dialog.show();
        ListView list=view.findViewById(R.id.list);
        List<Map<String,String>>netsArray=new ArrayList<Map<String,String>>();
        final String[] nets=EOSUtils.AVAILABLE_EOS_NETS;
        for(int i=0;i<nets.length;i++){
            String net=nets[i];
            Map<String,String>map=new HashMap<String,String>();
            map.put("name",net);
            netsArray.add(map);
        }
        SimpleAdapter adapter=new SimpleAdapter(this,netsArray,android.R.layout.simple_list_item_1,new String[]{"name"},new int[]{android.R.id.text1});
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String listItem=nets[i];
                EOSUtils.setCurrentNet(ChainHomeActivity.this,listItem);
                mCurrNetView.setText(EOSUtils.getCurrentNet());
                dialog.dismiss();
            }
        });
    }
}
