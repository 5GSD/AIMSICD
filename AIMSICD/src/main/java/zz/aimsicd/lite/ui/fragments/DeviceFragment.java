/* Android IMSI-Catcher Detector | (c) AIMSICD Privacy Project
 * -----------------------------------------------------------
 * LICENSE:  http://git.io/vki47 | TERMS:  http://git.io/vki4o
 * -----------------------------------------------------------
 */
package zz.aimsicd.lite.ui.fragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.TableRow;

import com.kaichunlin.transition.animation.AnimationManager;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


import io.freefair.android.injection.annotation.Inject;
import io.freefair.android.injection.annotation.InjectView;
import io.freefair.android.injection.annotation.XmlLayout;
import io.freefair.android.injection.app.InjectionFragment;


import zz.aimsicd.lite.R;
import zz.aimsicd.lite.service.AimsicdService;
import zz.aimsicd.lite.service.CellTracker;
import zz.aimsicd.lite.ui.widget.HighlightTextView;
import zz.aimsicd.lite.utils.Cell;
import zz.aimsicd.lite.utils.Device;
import zz.aimsicd.lite.utils.Helpers;


@XmlLayout(R.layout.fragment_device)
public class DeviceFragment extends InjectionFragment implements SwipeRefreshLayout.OnRefreshListener {

    public static final String TAG = "AICDL";
    public static final String mTAG = "DeviceFragment";



    @Inject
    OkHttpClient okHttpClient;

    @InjectView(R.id.swipeRefresLayout)
    private SwipeRefreshLayout swipeRefreshLayout;

    private AimsicdService mAimsicdService;
    private boolean mBound;
    private Context mContext;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mContext = getActivity().getBaseContext();
        // Bind to LocalService
        Intent intent = new Intent(mContext, AimsicdService.class);
        mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        swipeRefreshLayout.setOnRefreshListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mBound) {
            // Bind to LocalService
            Intent intent = new Intent(mContext, AimsicdService.class);
            mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
        updateUI();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unbind from the service
        if (mBound) {
            mContext.unbindService(mConnection);
            mBound = false;
        }
    }

    /**
     * Service Connection to bind the activity to the service
     */
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            mAimsicdService = ((AimsicdService.AimscidBinder) service).getService();
            mBound = true;
            updateUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
           Log.e(TAG, mTAG + "Service Disconnected");
            mBound = false;
        }
    };

    private void updateUI() {
        HighlightTextView content;
        TableRow tr;
        if (mBound) {
            final AnimationManager ani = new AnimationManager();

            mAimsicdService.getCellTracker().refreshDevice();
            Device mDevice = mAimsicdService.getCellTracker().getDevice();
            switch (mDevice.getPhoneID()) {

                case TelephonyManager.PHONE_TYPE_NONE:  // Maybe bad!
                case TelephonyManager.PHONE_TYPE_SIP:   // Maybe bad!
                case TelephonyManager.PHONE_TYPE_GSM: {
                    content = (HighlightTextView)  getView().findViewById(R.id.network_lac);
                    content.updateText(String.valueOf(mAimsicdService.getCell().getLAC()), ani);
                    tr = (TableRow) getView().findViewById(R.id.gsm_cellid);
                    tr.setVisibility(View.VISIBLE);
                    content = (HighlightTextView)  getView().findViewById(zz.aimsicd.lite.R.id.network_cellid);
                    content.updateText(String.valueOf(mAimsicdService.getCell().getCID()), ani);
                    break;
                }

                case TelephonyManager.PHONE_TYPE_CDMA: {
                    tr = (TableRow) getView().findViewById(zz.aimsicd.lite.R.id.cdma_netid);
                    tr.setVisibility(View.VISIBLE);
                    content = (HighlightTextView)  getView().findViewById(R.id.network_netid);
                    content.updateText(String.valueOf(mAimsicdService.getCell().getLAC()), ani);
                    tr = (TableRow) getView().findViewById(R.id.cdma_sysid);
                    tr.setVisibility(View.VISIBLE);
                    content = (HighlightTextView)  getView().findViewById(zz.aimsicd.lite.R.id.network_sysid);
                    content.updateText(String.valueOf(mAimsicdService.getCell().getSID()), ani);
                    tr = (TableRow) getView().findViewById(zz.aimsicd.lite.R.id.cdma_baseid);
                    tr.setVisibility(View.VISIBLE);
                    content = (HighlightTextView)  getView().findViewById(zz.aimsicd.lite.R.id.network_baseid);
                    content.updateText(String.valueOf(mAimsicdService.getCell().getCID()), ani);
                    break;
                }
                default:
                    Log.e(TAG, mTAG + ": unknown phone type: " + mDevice.getPhoneID());
            }

            if (mAimsicdService.getCell().getTimingAdvance() != Integer.MAX_VALUE) {
                tr = (TableRow) getView().findViewById(R.id.lte_timing_advance);
                tr.setVisibility(View.VISIBLE);
                content = (HighlightTextView)  getView().findViewById(zz.aimsicd.lite.R.id.network_lte_timing_advance);
                content.updateText(String.valueOf(mAimsicdService.getCell().getTimingAdvance()), ani);
            } else {
                tr = (TableRow) getView().findViewById(zz.aimsicd.lite.R.id.lte_timing_advance);
                tr.setVisibility(View.GONE);
            }

            if (mAimsicdService.getCell().getPSC() != Integer.MAX_VALUE) {
                content = (HighlightTextView)  getView().findViewById(R.id.network_psc);
                content.updateText(String.valueOf(mAimsicdService.getCell().getPSC()), ani);
                tr = (TableRow) getView().findViewById(zz.aimsicd.lite.R.id.primary_scrambling_code);
                tr.setVisibility(View.VISIBLE);
            }

            String notAvailable = getString(R.string.n_a);

            content = (HighlightTextView)  getView().findViewById(R.id.sim_country);
            content.updateText(mDevice.getSimCountry().orElse(notAvailable), ani);
            content = (HighlightTextView)  getView().findViewById(zz.aimsicd.lite.R.id.sim_operator_id);
            content.updateText(mDevice.getSimOperator().orElse(notAvailable), ani);
            content = (HighlightTextView) getView().findViewById(zz.aimsicd.lite.R.id.sim_operator_name);
            content.updateText(mDevice.getSimOperatorName().orElse(notAvailable), ani);
            content = (HighlightTextView)  getView().findViewById(zz.aimsicd.lite.R.id.sim_imsi);
            content.updateText(mDevice.getSimSubs().orElse(notAvailable), ani);
            content = (HighlightTextView)  getView().findViewById(zz.aimsicd.lite.R.id.sim_serial);
            content.updateText(mDevice.getSimSerial().orElse(notAvailable), ani);

            content = (HighlightTextView)  getView().findViewById(R.id.device_type);
            content.updateText(mDevice.getPhoneType(), ani);
            content = (HighlightTextView)  getView().findViewById(zz.aimsicd.lite.R.id.device_imei);
            content.updateText(mDevice.getIMEI(), ani);
            content = (HighlightTextView)  getView().findViewById(R.id.device_version);
            content.updateText(mDevice.getIMEIv(), ani);

            content = (HighlightTextView)  getView().findViewById(R.id.network_name);
            content.updateText(mDevice.getNetworkName(), ani);
            content = (HighlightTextView)  getView().findViewById(zz.aimsicd.lite.R.id.network_code);
            content.updateText(mDevice.getMncMcc(), ani);
            content = (HighlightTextView)  getView().findViewById(R.id.network_type);
            content.updateText(mDevice.getNetworkTypeName(), ani);

            content = (HighlightTextView)  getView().findViewById(zz.aimsicd.lite.R.id.data_activity);
            content.updateText(mDevice.getDataActivity(), ani);
            content = (HighlightTextView)  getView().findViewById(R.id.data_status);
            content.updateText(mDevice.getDataState(), ani);
            content = (HighlightTextView)  getView().findViewById(zz.aimsicd.lite.R.id.network_roaming);
            content.updateText(mDevice.isRoaming(), ani);

            ani.startAnimation(5000);
        }
    }

    @Override
    public void onRefresh() {
        if (CellTracker.OCID_API_KEY != null && !CellTracker.OCID_API_KEY.equals("NA")) {
            Request request = createOpenCellIdApiCall();
            okHttpClient.newCall(request).enqueue(getOpenCellIdResponseCallback());
        } else {
            Handler refresh = new Handler(Looper.getMainLooper());
            refresh.post(new Runnable() {
                public void run() {
                    Helpers.sendMsg(getActivity(), getString(R.string.no_opencellid_key_detected));
                    swipeRefreshLayout.setRefreshing(false);
                }
            });
        }
    }

    @NonNull
    private Callback getOpenCellIdResponseCallback() {
        return new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                Handler refresh = new Handler(Looper.getMainLooper());
                refresh.post(new Runnable() {
                    public void run() {
                        refreshFailed();
                    }
                });
            }

            @Override
            public void onResponse(final Response response) throws IOException {
                Handler refresh = new Handler(Looper.getMainLooper());
                refresh.post(new Runnable() {
                    public void run() {
                        Cell cell = responseToCell(response);
                        processFinish(cell);
                    }
                });
            }
        };
    }

    //TODO: Use Retrofit for this
    private Request createOpenCellIdApiCall() {
        StringBuilder sb = new StringBuilder();
        sb.append("http://www.opencellid.org/cell/get?key=").append(CellTracker.OCID_API_KEY);

        if (mAimsicdService.getCell().getMCC() != Integer.MAX_VALUE) {
            sb.append("&mcc=").append(mAimsicdService.getCell().getMCC());
        }
        if (mAimsicdService.getCell().getMNC() != Integer.MAX_VALUE) {
            sb.append("&mnc=").append(mAimsicdService.getCell().getMNC());
        }
        if (mAimsicdService.getCell().getLAC() != Integer.MAX_VALUE) {
            sb.append("&lac=").append(mAimsicdService.getCell().getLAC());
        }
        if (mAimsicdService.getCell().getCID() != Integer.MAX_VALUE) {
            sb.append("&cellid=").append(mAimsicdService.getCell().getCID());
        }
        sb.append("&format=json");
        return new Request.Builder()
                .url(sb.toString())
                .get()
                .build();
    }

    private Cell responseToCell(Response response) {
        try {
            JSONObject jsonCell = new JSONObject(response.body().string());
            Cell cell = new Cell();
            cell.setLat(jsonCell.getDouble("lat"));
            cell.setLon(jsonCell.getDouble("lon"));
            cell.setMCC(jsonCell.getInt("mcc"));
            cell.setMNC(jsonCell.getInt("mnc"));
            cell.setCID(jsonCell.getInt("cellid"));
            cell.setLAC(jsonCell.getInt("lac"));
            return cell;
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void processFinish(Cell cell) {
        if (cell != null) {
            Log.i(TAG, mTAG + "processFinish - Cell =" + cell.toString());
            if (cell.isValid()) {
                mAimsicdService.setCell(cell);
                Helpers.msgShort(mContext, getActivity().getString(zz.aimsicd.lite.R.string.refreshed_cell_id_info));  // TODO re-translating other languages
                updateUI();
                swipeRefreshLayout.setRefreshing(false);
            }
        }
    }

    private void refreshFailed() {
        Helpers.msgShort(mContext, "Failed to refresh CellId. Check network connection.");
        swipeRefreshLayout.setRefreshing(false);
    }
}
